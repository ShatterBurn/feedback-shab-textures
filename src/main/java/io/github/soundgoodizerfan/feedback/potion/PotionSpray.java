// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A potion let go as a stream (a fluid caster's tank): the same place-and-strength acts a thrown bottle does ({@link PotionEffects}),
 * scaled by how much of it lands. A full dose is {@link PotionTuning#SPRAY_DOSE_MB} (a fifth of a bottle): a body soaked with that much carries the effect for its
 * whole length, and a thinner stream carries it for the share of the length the millilitres make. A status adds to what the body has left
 * of the same effect (never stacks levels, never past the bottle's own length); an act that happens once a bottle (a refill, a launch, the
 * landing pulse of a place effect, a blast) happens with the chance of that share each tick, so a dose's worth does it once on average.
 * Dispatch is on the potion's own properties, never the fluid's id. Create's open pipe applies a full flat dose whatever the flow; that is
 * the thing not taken.
 */
public final class PotionSpray {

    private PotionSpray() {}

    /** The potion a stack carries, or null: a dud or a faded one acts as nothing, so callers skip it. */
    private static PotionState live(ServerLevel level, FluidStack fluid) {
        PotionState state = PotionFluids.state(fluid);
        return state == null || state.dud() || state.amplifierNow(level) < 0 ? null : state;
    }

    /** Whether the stream is a live potion: one that does something to what it hits. */
    public static boolean active(ServerLevel level, FluidStack fluid) {
        return live(level, fluid) != null;
    }

    /** Whether the potion is carried in water, so it wets what it hits as water does (and puts out a body on fire, and hurts what hates it). */
    public static boolean wet(FluidStack fluid) {
        PotionState state = PotionFluids.state(fluid);
        return state != null && "water".equals(state.solvent());
    }

    /** The stream's colour (24-bit RGB): a dud's own tint, else the effect's colour, else a pale draught. */
    public static int colour(FluidStack fluid) {
        PotionState state = PotionFluids.state(fluid);
        if (state == null)
            return 0xB0C8E8;
        if (state.tint() >= 0)
            return state.tint() & 0xFFFFFF;
        return state.effect().flatMap(id -> BuiltInRegistries.MOB_EFFECT.getHolder(id)).map(h -> h.value().getColor() & 0xFFFFFF).orElse(0xB0C8E8);
    }

    /** What the stream does to a body that takes {@code mb} of it this tick. */
    public static void body(ServerLevel level, Entity owner, LivingEntity body, FluidStack fluid, int mb) {
        PotionState state = live(level, fluid);
        if (state == null || mb <= 0)
            return;
        int amplifier = state.amplifierNow(level);
        var id = state.effect().get();
        float share = Math.min(1f, mb / (float) PotionTuning.SPRAY_DOSE_MB);
        if (PotionEffects.EXPLOSION.equals(id) || PotionEffects.placeOnly(id))
            return; // acts where it lands
        if (PotionEffects.IGNITE.equals(id)) {
            body.igniteForSeconds(Math.max(1f, state.duration() / FTuning.TICKS_PER_SECOND * (1 + amplifier) * share));
            return;
        }
        if (PotionEffects.oneShot(id)) {
            if (level.random.nextFloat() < share)
                PotionEffects.touch(body, id, amplifier, state.duration(), 1f);
            return;
        }
        int add = Math.max(1, Math.round(state.duration() * share));
        int left = BuiltInRegistries.MOB_EFFECT.getHolder(id).map(h -> {
            MobEffectInstance have = body.getEffect(h);
            return have != null && have.getAmplifier() >= amplifier ? have.getDuration() : 0;
        }).orElse(0);
        PotionEffects.touch(body, id, amplifier, Math.min(state.duration(), left + add), 1f);
    }

    /** What the stream does where it lands: a place effect, a fire, a blast, each with the chance of its share of a bottle. */
    public static void landing(ServerLevel level, Entity owner, Vec3 at, FluidStack fluid, int mb) {
        PotionState state = live(level, fluid);
        if (state == null || mb <= 0)
            return;
        float share = Math.min(1f, mb / (float) PotionTuning.SPRAY_DOSE_MB);
        if (level.random.nextFloat() >= share)
            return;
        int amplifier = state.amplifierNow(level);
        var id = state.effect().get();
        if (PotionEffects.EXPLOSION.equals(id))
            PotionEffects.explode(level, owner, at, amplifier);
        else if (PotionEffects.IGNITE.equals(id)) {
            BlockPos fire = BlockPos.containing(at);
            if (level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK) && BaseFireBlock.canBePlacedAt(level, fire, Direction.UP))
                level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
        } else
            PotionEffects.place(level, owner, at, PotionTuning.SPRAY_PLACE_REACH, id, amplifier, Math.round(state.duration() * PotionTuning.THROW_DURATION));
    }

    /** A mist of the stream where it stops: it hangs there and doses what stands in it ({@link PotionMist}). */
    public static void mist(ServerLevel level, Vec3 at, FluidStack fluid, int mb) {
        if (live(level, fluid) != null && mb > 0)
            PotionMist.add(level, at, fluid.copyWithAmount(Math.max(1, Math.round(mb * PotionTuning.MIST_YIELD))));
    }
}
