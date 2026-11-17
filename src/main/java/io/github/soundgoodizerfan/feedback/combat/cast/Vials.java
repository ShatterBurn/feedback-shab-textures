// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.potion.PotionFluids;
import io.github.soundgoodizerfan.feedback.potion.PotionSpray;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The potion a fighter carries into a fight: a casts' cost is millilitres out of a bottle, never a count. The first draw opens the nearest
 * live potion bottle in the inventory (the off hand first), which is spent and returns a glass bottle; the opened bottle's {@link PotionTuning#BOTTLE_MB}
 * is then drawn down by every cast and rider that carries potion, and the next draw past it opens another. A dud or a faded potion is never
 * opened, and nothing is drawn if none is carried, so a cast that needs the potion fails and costs nothing. What is drawn lands through
 * {@link PotionSpray}, the same place-and-strength dosing a caster's stream gets, so a potion does whatever its own properties say.
 */
public final class Vials {

    private Vials() {
    }

    private static final Map<UUID, FluidStack> OPEN = new HashMap<>();

    /** Up to {@code mb} of the opened bottle, opening another if it is empty; empty if the fighter carries no live potion. */
    public static FluidStack draw(ServerPlayer p, int mb) {
        FluidStack open = OPEN.getOrDefault(p.getUUID(), FluidStack.EMPTY);
        if (open.isEmpty()) {
            open = openBottle(p);
            if (open.isEmpty())
                return FluidStack.EMPTY;
        }
        int take = Math.min(mb, open.getAmount());
        FluidStack drawn = open.copyWithAmount(take);
        FluidStack rest = open.copyWithAmount(open.getAmount() - take);
        if (rest.isEmpty())
            OPEN.remove(p.getUUID());
        else
            OPEN.put(p.getUUID(), rest);
        return drawn;
    }

    /** Whether a draw of any size would find a potion. */
    public static boolean has(ServerPlayer p) {
        return !OPEN.getOrDefault(p.getUUID(), FluidStack.EMPTY).isEmpty() || find(p) != null;
    }

    private static ItemStack find(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        ItemStack off = p.getOffhandItem();
        if (live(lv, off))
            return off;
        for (ItemStack s : p.getInventory().items)
            if (live(lv, s))
                return s;
        return null;
    }

    private static boolean live(ServerLevel lv, ItemStack s) {
        if (!s.is(FItems.POTION.get()))
            return false;
        var state = s.get(FDataComponents.POTION_STATE.get());
        return state != null && PotionSpray.active(lv, PotionFluids.of(state, PotionTuning.BOTTLE_MB));
    }

    private static FluidStack openBottle(ServerPlayer p) {
        ItemStack bottle = find(p);
        if (bottle == null)
            return FluidStack.EMPTY;
        FluidStack fluid = PotionFluids.of(bottle.get(FDataComponents.POTION_STATE.get()), PotionTuning.BOTTLE_MB);
        bottle.shrink(1);
        ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
        if (!p.getInventory().add(glass))
            p.drop(glass, false);
        return fluid;
    }

    /** {@code drawn} lands on one body: its status, and any act that happens where it stands (a blast, a fire, a place effect). */
    public static void dose(ServerPlayer p, LivingEntity body, FluidStack drawn) {
        if (drawn.isEmpty())
            return;
        ServerLevel lv = p.serverLevel();
        PotionSpray.body(lv, p, body, drawn, drawn.getAmount());
        PotionSpray.landing(lv, p, body.position(), drawn, drawn.getAmount());
        splash(lv, body.position().add(0, body.getBbHeight() * 0.6, 0), drawn);
    }

    /** The bottle breaking: droplets in the potion's colour and glass. */
    public static void splash(ServerLevel lv, Vec3 at, FluidStack fluid) {
        int rgb = PotionSpray.colour(fluid);
        lv.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | rgb), at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.0);
        lv.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 14, 0.3, 0.2, 0.3, 0.1);
        CastFx.sound(lv, at, SoundEvents.GLASS_BREAK, 0.8f, 1.1f);
    }
}
