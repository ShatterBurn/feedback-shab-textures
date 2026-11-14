// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Resonance weight: a weight threaded on the barrel tunes its node to one muzzle speed ({@code tuned_ms}, m/s). A
 * round near that speed shoots a tight group ({@code 0.7}); the further off it is, in ratio, the wider the group
 * opens, past 1 (worse than a bare barrel) beyond about a third off. Where it sits is the part's drawn measure. Read off the figures as aimed, so wear and heat that slow the round
 * detune it. The weight's metal sets how far off a round may stray: a denser metal damps more ({@link HandlingPhysics#resonanceDetune}).
 */
public class GunResonance implements ModularTrait {

    /** Tuned speed is {@code TUNED_BASE_MS + TUNED_MS_PER_PERCENT * position}: 250 m/s at 20 %, 810 m/s at 90 %. */
    private static final float TUNED_BASE_MS = 90f, TUNED_MS_PER_PERCENT = 8f;

    /** Muzzle speed (m/s) the weight is tuned to: the further toward the muzzle it sits (axis {@code position}, percent of the barrel from the breech), the faster the round it suits. */
    static float tunedMs(ItemStack gun) {
        var design = io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.design(gun);
        if (design == null)
            return 0f;
        for (var part : design.parts())
            if (part.kind().equals("resonance_weight"))
                return TUNED_BASE_MS + TUNED_MS_PER_PERCENT * part.axis("position", 50f);
        return 0f;
    }

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float speed = f.muzzleSpeed() * 20f, tuned = tunedMs(gun);
        if (speed <= 0f || tuned <= 0f)
            return f;
        float off = Math.abs((float) Math.log(speed / tuned)) * HandlingPhysics.resonanceDetune(GunFitting.of(gun, "resonance_weight"));
        return f.withSpread(Mth.clamp(0.7f + off * 1.2f, 0.7f, 1.5f));
    }
}
