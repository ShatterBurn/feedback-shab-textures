// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;

import net.minecraft.util.Mth;

/**
 * How a skin's controls move, from two numbers: {@code weight} (0 weightless and instant, like a key on a screen, to 1 heavy iron)
 * and {@code slop} (0 crisp, 1 rough and hand-made). A screen never passes numbers: shared widgets ask {@code skin.feel()}. Press
 * depth, ease time, give and the counter's style all derive from the two, so a new skin is one pair of figures in its JSON.
 * Motion is a pure function of time since an event, so nothing here keeps state; reduce motion makes every answer the end state.
 *
 * @param weight 0 to 1, how heavy the thing is: deeper, slower presses
 * @param slop   0 to 1, how rough the craft is: a press settles with give instead of stopping dead
 */
public record Feel(float weight, float slop) {

    /** Nothing moves: a key on a screen. */
    public static final Feel DIGITAL = new Feel(0f, 0f);

    /** Pixels a pressed control drops: none for a light one, one for most, two for heavy iron. */
    public int pressDepth() {
        return weight < 0.3f ? 0 : weight < 0.9f ? 1 : 2;
    }

    /** Ticks a press or release takes to settle; 0 is instant. */
    public float settleTicks() {
        return weight * 5f;
    }

    /** How far past its end a press swings back (0..0.5 of the travel): rough and light overshoots, heavy and crisp does not. */
    public float give() {
        return slop * (1f - weight) * 0.6f;
    }

    /** Whether number readouts roll like an odometer; a digital skin snaps. */
    public boolean rolls() {
        return weight >= 0.3f;
    }

    /** Ticks an odometer digit takes to turn. */
    public float rollTicks() {
        return 4f + 6f * weight;
    }

    /**
     * Where a control is {@code age} ticks after it was set heading to {@code to} from {@code from}: eased with the skin's give,
     * landing exactly on {@code to}. Instant under reduce motion or with no settle time.
     */
    public float travel(float from, float to, float age) {
        float settle = settleTicks();
        if (settle <= 0f || ClientSettings.reduceMotion() || age >= settle)
            return to;
        float t = Math.max(0f, age) / settle;
        return Mth.lerp(GuiStyle.outBack(t, give()), from, to);
    }

    /** Ticks a slide (stepper reading, tab change) takes; 0 snaps. Very short, and quicker the crisper the skin (iron about 2 ticks, wood about 4). */
    public float slideTicks() {
        return weight <= 0f ? 0f : 1.5f + 4f * slop;
    }

    /**
     * Progress 0..1 at time fraction {@code t}. Every skin starts at full speed; the weight and slop of the material decide the end.
     * Rough, soft skins (wood) brake gradually and slow to a stop; hard, crisp ones (iron) hold their speed and stop dead
     * ({@link #thunks}). The braking exponent runs from 1 (no braking at all) up with the slop.
     */
    public float slide(float t) {
        float u = 1f - Mth.clamp(t, 0f, 1f);
        return 1f - (float) Math.pow(u, 1f + 3f * slop);
    }

    /** Whether a slide ends in a thud: heavy and crisp, so it arrives at speed and stops at once. */
    public boolean thunks() {
        return weight >= 0.7f && slop < 0.25f;
    }

    /** Per-second rate a slider's thumb closes on its value: weightless snaps, heavy lags behind the pointer. */
    public float followRate() {
        return weight <= 0f ? 0f : 30f - 22f * weight;
    }

    /** Per-second rate a list's scroll closes on its target: stone ramps slowly, glass is quick, a weightless skin jumps. */
    public float scrollRate() {
        return weight <= 0f ? 0f : 26f - 16f * weight;
    }
}
