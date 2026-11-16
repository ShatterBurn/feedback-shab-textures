// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.modular.item.ModularLauncher;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.FireMode;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.weapon.GunSound;
import io.github.soundgoodizerfan.feedback.weapon.GunStance;

/**
 * A launcher's mechanism: the seat gives the push, the round is the shell. A pull needs a cocked seat and a chambered round of the
 * bore; it spends the round, lets the seat go, and sends the shell down the shared swept flight ({@link GunShots#fire}). The reload
 * key loads a round of the bore from the pack if the chamber is empty, then cocks: the time is the spring's energy over the wielder's
 * strength, and a spring stronger than its receiver holds takes a set each cocking, or far past it snaps ({@link SpringSet}).
 */
public final class LauncherMechanism implements GunMechanism {

    public static final LauncherMechanism INSTANCE = new LauncherMechanism();

    private LauncherMechanism() {
    }

    /** The next shot: the round, its figures. Changes nothing. */
    private record Prepared(LauncherSpec spec, Cartridge round, GunFigures figures) {
    }

    private static Prepared prepare(LivingEntity shooter, ItemStack gun, long now) {
        LauncherSpec spec = ModularLauncher.spec(gun).orElse(null);
        if (spec == null || ModularStacks.broken(gun) || !ModularLauncher.cocked(gun))
            return null;
        Optional<Cartridge> next = GunAmmo.next(gun);
        if (next.isEmpty() || next.get().boreMm() != spec.boreInt())
            return null;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Optional<Cartridge.Slug> slug = next.get().slug(materials);
        if (slug.isEmpty())
            return null;
        float shellKg = spec.pelletLitres() * slug.get().density();
        ModularFigures baked = ModularStacks.figures(gun);
        float gunKg = baked == null || baked.get(ModularFigures.Fig.MASS) <= 0f ? spec.pistonMass() : baked.get(ModularFigures.Fig.MASS);
        GunFigures figures = spec.figures(shellKg, SpringSet.set(gun), gunKg, GunCondition.condition(gun));
        if (shooter instanceof Player player) {
            figures = io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits.gunAimed(player, gun, figures);
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        return new Prepared(spec, next.get(), figures);
    }

    @Override
    public boolean built(ItemStack gun) {
        return ModularLauncher.spec(gun).isPresent();
    }

    @Override
    public boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        Prepared p = prepare(shooter, gun, level.getGameTime());
        if (p == null)
            return false;
        if (!GunShots.accept(shooter, p.figures(), nowMs, hand))
            return false;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        GunAmmo.spend(gun);
        ModularLauncher.setCocked(gun, false);
        GunFigures figures = p.figures().withSpread(GunStance.of(shooter, aimed) * p.round().shape().spread()
                * io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds.braceSpread(shooter, gun));
        GunShots.fire(level, shooter, figures, yaw, pitch, p.round().tracer(materials), p.round().incendiary(materials), 1,
                p.round().hardness(materials), gun, p.round().blast(materials), false, GunShots.Flight.of(p.round(), materials));
        GunShots.bookShot(level, shooter, gun, figures, 0f, hand, aimed);
        return true;
    }

    @Override
    public boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        return false;
    }

    @Override
    public GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand) {
        Prepared p = prepare(player, gun, level.getGameTime());
        return p == null ? null : p.figures();
    }

    @Override
    public float sightZoom(ItemStack gun) {
        return 1f;
    }

    @Override
    public int raiseMs(ItemStack gun) {
        return 250;
    }

    @Override
    public FireMode allow(ItemStack gun, FireMode set) {
        return FireMode.SEMI;
    }

    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        return FireMode.SEMI;
    }

    @Override
    public List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random) {
        float jitter = 0.95f + random.nextFloat() * 0.1f;
        return List.of(new GunSound.Layer(SoundEvents.CROSSBOW_SHOOT, ModularTuning.AIR_VOLUME * 3f * Math.max(0.3f, figures.report() * 4f), 1.4f * jitter));
    }

    @Override
    public SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures) {
        return ParticleTypes.POOF;
    }

    // --- cocking and loading: the reload key -------------------------------------------------

    @Override
    public boolean reloadKeyWorks(ItemStack gun) {
        return true;
    }

    @Override
    public boolean reloadKeyReady(ItemStack gun) {
        return !ModularLauncher.cocked(gun) && ModularLauncher.cockAt(gun) == 0L;
    }

    /** A cocking is strength-timed: the baked time over the wielder's pull, less what a set spring has lost. */
    private static long cockTicks(LauncherSpec spec, LivingEntity wielder, ItemStack gun) {
        float pull = Mth.clamp((float) wielder.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                ModularTuning.AIR_PULL_MIN, ModularTuning.AIR_PULL_MAX);
        return Math.round(spec.cockTicks() * (1f - SpringSet.set(gun)) / pull);
    }

    @Override
    public void reloadKey(ServerLevel level, Player player, ItemStack gun) {
        LauncherSpec spec = ModularLauncher.spec(gun).orElse(null);
        if (spec == null || ModularStacks.broken(gun) || ModularLauncher.cocked(gun) || ModularLauncher.cockAt(gun) != 0L)
            return;
        // An empty chamber takes a round of the bore from the pack first.
        if (GunAmmo.next(gun).isEmpty())
            loadFromPack(player, gun, spec);
        long now = level.getGameTime();
        long done = now + cockTicks(spec, player, gun);
        ModularLauncher.setCockAt(gun, done);
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.PLAYERS, ModularTuning.AIR_VOLUME, 1.4f);
        io.github.soundgoodizerfan.feedback.core.Later.in((int) (done - now), () -> finishCock(level, player, gun, spec));
    }

    /** The stroke ends: a spring too strong for its receiver sets or snaps, else the seat is cocked. */
    private static void finishCock(ServerLevel level, Player player, ItemStack gun, LauncherSpec spec) {
        if (ModularLauncher.cockAt(gun) == 0L)
            return;
        ModularLauncher.setCockAt(gun, 0L);
        float ratio = spec.load(SpringSet.set(gun));
        SpringSet.Outcome outcome = SpringSet.overload(gun, ratio, player);
        if (outcome == SpringSet.Outcome.SET)
            GunCondition.misuse(gun, ModularTuning.GUN_WEAR_SPRING_SET + ModularTuning.GUN_WEAR_SPRING_SET_SPAN * Math.min(1f, (ratio - 1f) / 0.6f));
        if (outcome == SpringSet.Outcome.SNAP) {
            GunCondition.misuse(gun, ModularTuning.GUN_WEAR_SPRING_SNAP);
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9f, 1.2f);
            return;
        }
        ModularLauncher.setCocked(gun, true);
        level.playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, ModularTuning.AIR_VOLUME,
                outcome == SpringSet.Outcome.SET ? 0.7f : 1.3f);
    }

    /** The first round in the pack of the bore, with no powder in it (the seat gives the push): chambered, one taken. */
    private static void loadFromPack(Player player, ItemStack gun, LauncherSpec spec) {
        for (ItemStack s : player.getInventory().items) {
            Cartridge round = s.get(FWeapons.CARTRIDGE.get());
            if (s.isEmpty() || !s.is(FWeapons.CARTRIDGE_ITEM.get()) || round == null || round.boreMm() != spec.boreInt() || !round.charges().isEmpty())
                continue;
            gun.set(FWeapons.CHAMBER.get(), round);
            if (!player.getAbilities().instabuild)
                s.shrink(1);
            return;
        }
    }
}
