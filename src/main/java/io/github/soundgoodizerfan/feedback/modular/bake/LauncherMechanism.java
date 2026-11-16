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

import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.item.ModularElectricGun;
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
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;

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
    private record Prepared(LauncherSpec spec, Cartridge round, GunFigures figures, float available) {
    }

    private static Prepared prepare(LivingEntity shooter, ItemStack gun, long now) {
        LauncherSpec spec = ModularLauncher.spec(gun).orElse(null);
        if (spec == null || ModularStacks.broken(gun))
            return null;
        Optional<Cartridge> next = GunAmmo.next(gun);
        if (next.isEmpty() || next.get().boreMm() != spec.boreInt() || !takes(next.get()))
            return null;
        boolean rocket = next.get().rocket();
        float available = rocket ? 1f : available(spec, gun, shooter.level());
        if (available <= 0f)
            return null;
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        Optional<Cartridge.Slug> slug = next.get().slug(materials);
        if (slug.isEmpty())
            return null;
        float shellKg = spec.pelletLitres() * slug.get().density();
        // A rocket is only ejected from the tube; its motor does the work.
        if (rocket)
            available = spec.ejectWork(shellKg, GunTuning.GUN_ROCKET_EJECT_SPEED);
        ModularFigures baked = ModularStacks.figures(gun);
        float gunKg = baked == null || baked.get(ModularFigures.Fig.MASS) <= 0f ? spec.pistonMass() : baked.get(ModularFigures.Fig.MASS);
        GunFigures figures = spec.figures(shellKg, available, gunKg, GunCondition.condition(gun));
        if (shooter instanceof Player player) {
            figures = io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits.gunAimed(player, gun, figures);
            if (GunItem.dualWielding(player))
                figures = figures.oneHanded();
        }
        return new Prepared(spec, next.get(), figures, available);
    }

    /** Whether a launcher takes this round: a shell (no powder) or a rocket (its grain is its motor); never a powder round. */
    private static boolean takes(Cartridge round) {
        return round.charges().isEmpty() || round.rocket();
    }

    /** The work the seat can give this shot, 0 if it cannot fire: a spring only when cocked, a reservoir by its pressure, a solenoid by its cell, a boiler by its heat and water. */
    private static float available(LauncherSpec spec, ItemStack gun, Level level) {
        switch (spec.seat()) {
            case RESERVOIR -> {
                float p = ModularLauncher.pressure(gun);
                return p < 0.05f ? 0f : spec.energy() * Math.min(1f, p) * ModularTuning.SEAT_RES_SPEND;
            }
            case SOLENOID -> {
                ItemStack cell = ModularElectricGun.cell(gun);
                if (cell.isEmpty() || !BatteryStack.is(cell))
                    return 0f;
                var state = BatteryStack.state(cell);
                if (!BatteryStack.ready(state))
                    return 0f;
                return Math.min(spec.energy(), BatteryStack.charge(state) * ModularTuning.EGUN_WORK_PER_EU * ModularTuning.SEAT_SOL_EFF);
            }
            case STEAM -> {
                if (ModularLauncher.water(gun) <= 0f
                        || io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(gun, level).value() <= io.github.soundgoodizerfan.feedback.core.FTuning.POT_BOIL_TU.value())
                    return 0f;
                return spec.energy() * Math.min(1f, (io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(gun, level).value()
                        - io.github.soundgoodizerfan.feedback.core.FTuning.POT_BOIL_TU.value())
                        / (ModularTuning.SEAT_STEAM_REF_TU - io.github.soundgoodizerfan.feedback.core.FTuning.POT_BOIL_TU.value()));
            }
            default -> {
                return ModularLauncher.cocked(gun) ? spec.energy() * (1f - SpringSet.set(gun)) : 0f;
            }
        }
    }

    /** The shot leaves the seat spent: the spring let go, a share of the gas out, the cell drawn, the boiler's water and heat used. */
    private static void spend(LauncherSpec spec, ItemStack gun, Prepared p, Level level) {
        switch (spec.seat()) {
            case RESERVOIR -> ModularLauncher.setPressure(gun, ModularLauncher.pressure(gun) * (1f - ModularTuning.SEAT_RES_SPEND));
            case SOLENOID -> {
                ItemStack cell = ModularElectricGun.cell(gun);
                var drawn = BatteryStack.state(cell).copy();
                BatteryStack.draw(drawn, p.available() / (ModularTuning.EGUN_WORK_PER_EU * ModularTuning.SEAT_SOL_EFF));
                ItemStack out = cell.copy();
                out.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BATTERY.get(), drawn);
                ModularElectricGun.setCell(gun, out);
            }
            case STEAM -> {
                ModularLauncher.setWater(gun, ModularLauncher.water(gun) - ModularTuning.SEAT_STEAM_WATER_PER_SHOT);
                var now = io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(gun, level);
                io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.set(gun,
                        new io.github.soundgoodizerfan.feedback.core.unit.Tu(now.value() - ModularTuning.SEAT_STEAM_COOL_TU), level);
            }
            default -> ModularLauncher.setCocked(gun, false);
        }
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
        if (!p.round().rocket())
            spend(p.spec(), gun, p, level);
        GunFigures figures = p.figures().withSpread(GunStance.of(shooter, aimed) * p.round().shape().spread()
                * io.github.soundgoodizerfan.feedback.modular.attunement.MomentumKinds.braceSpread(shooter, gun));
        GunShots.fire(level, shooter, figures, yaw, pitch, p.round().tracer(materials), p.round().incendiary(materials), p.round().fuse().isPresent() ? 1 : p.round().pellets(),
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
        // A spring is recocked by hand and cannot cycle; the others fire as fast as the seat recovers.
        boolean auto = ModularLauncher.spec(gun).map(sp -> sp.seat() != LauncherSpec.Seat.SPRING).orElse(false);
        return auto && set == FireMode.AUTO ? FireMode.AUTO : FireMode.SEMI;
    }

    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        boolean auto = ModularLauncher.spec(gun).map(sp -> sp.seat() != LauncherSpec.Seat.SPRING).orElse(false);
        return auto && current == FireMode.SEMI ? FireMode.AUTO : FireMode.SEMI;
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
        return ModularLauncher.spec(gun).map(sp -> sp.seat() != LauncherSpec.Seat.SPRING
                || !ModularLauncher.cocked(gun) && ModularLauncher.cockAt(gun) == 0L).orElse(false);
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
        if (spec == null || ModularStacks.broken(gun))
            return;
        // An empty chamber takes a round of the bore from the pack first.
        boolean loaded = false;
        if (GunAmmo.next(gun).isEmpty())
            loaded = loadFromPack(player, gun, spec) || loadMagazine(player, gun, spec);
        switch (spec.seat()) {
            case RESERVOIR -> {
                pump(level, player, gun, spec);
                return;
            }
            case STEAM -> {
                if (!loaded)
                    fillWater(level, player, gun);
                return;
            }
            case SOLENOID -> {
                return;
            }
            default -> { }
        }
        if (ModularLauncher.cocked(gun) || ModularLauncher.cockAt(gun) != 0L)
            return;
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
    private static boolean loadFromPack(Player player, ItemStack gun, LauncherSpec spec) {
        for (ItemStack s : player.getInventory().items) {
            Cartridge round = s.get(FWeapons.CARTRIDGE.get());
            if (s.isEmpty() || !s.is(FWeapons.CARTRIDGE_ITEM.get()) || round == null || round.boreMm() != spec.boreInt() || !takes(round))
                continue;
            gun.set(FWeapons.CHAMBER.get(), round);
            if (!player.getAbilities().instabuild)
                s.shrink(1);
            return true;
        }
        return false;
    }

    /** A magazine in the pack whose top round the launcher takes: seated in the gun as it is, and the chamber fed from it. */
    private static boolean loadMagazine(Player player, ItemStack gun, LauncherSpec spec) {
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty() || !s.is(FWeapons.MAGAZINE_ITEM.get()))
                continue;
            var mag = s.getOrDefault(FWeapons.MAGAZINE.get(), io.github.soundgoodizerfan.feedback.weapon.Magazine.EMPTY);
            if (mag.isEmpty() || mag.top().boreMm() != spec.boreInt() || !takes(mag.top()))
                continue;
            gun.set(FWeapons.MAGAZINE.get(), mag);
            gun.set(FWeapons.MAGAZINE_SIZE.get(), GunAmmo.size(s));
            s.shrink(1);
            GunAmmo.feed(gun);
            return true;
        }
        return false;
    }

    /**
     * A stroke of the hand pump: more gas in. The walls are rated for a pressure; pumping on past it weeps (the extra is lost and the
     * seat wears), and far past it the walls let go and hurt the wielder. Misuse only: pumping to the rating never wears it.
     */
    private static void pump(ServerLevel level, Player player, ItemStack gun, LauncherSpec spec) {
        float p = ModularLauncher.pressure(gun) + ModularTuning.SEAT_RES_PUMP;
        level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, ModularTuning.AIR_VOLUME, 1.6f);
        if (p > ModularTuning.SEAT_RES_BURST) {
            ModularLauncher.setPressure(gun, 0f);
            GunCondition.misuse(gun, ModularTuning.GUN_WEAR_SPRING_SNAP);
            player.hurt(level.damageSources().explosion(null, null), ModularTuning.SEAT_BURST_HURT);
            level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getEyeY(), player.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            return;
        }
        if (p > ModularTuning.SEAT_RES_WEEP) {
            GunCondition.misuse(gun, ModularTuning.GUN_WEAR_SPRING_SET);
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getEyeY() - 0.3, player.getZ(), 4, 0.1, 0.05, 0.1, 0.02);
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.8f);
        }
        ModularLauncher.setPressure(gun, p);
    }

    /** A water bucket in the pack fills the boiler; the empty bucket comes back. */
    private static void fillWater(ServerLevel level, Player player, ItemStack gun) {
        if (ModularLauncher.water(gun) >= 1f)
            return;
        for (ItemStack s : player.getInventory().items)
            if (!s.isEmpty() && net.neoforged.neoforge.fluids.FluidUtil.getFluidContained(s)
                    .filter(f -> f.getAmount() >= 1000 && f.is(net.minecraft.tags.FluidTags.WATER)).isPresent()) {
                if (!player.getAbilities().instabuild) {
                    ItemStack back = s.hasCraftingRemainingItem() ? s.getCraftingRemainingItem() : new ItemStack(net.minecraft.world.item.Items.BUCKET);
                    s.shrink(1);
                    GunAmmo.give(player, back);
                }
                ModularLauncher.setWater(gun, 1f);
                level.playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.6f, 1.2f);
                return;
            }
    }
}
