// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.List;

/**
 * A creative source: a block that emits one energy type at whatever figures an operator sets, for
 * building and testing everything downstream of that energy without first building its supply.
 * <p>
 * Free scalars are right here and nowhere else: philosophy 3's discrete-options rule is about
 * what a <i>player</i> tunes inside a factory, and a test bench has no factory to protect. A
 * creative source is never a survival route -- it lives in the operator tab and answers only to
 * {@code canUseGameMasterBlocks}.
 */
public interface CreativeSource {

    /**
     * One setting. {@code key} names its lang entry under {@code screen.feedback.creative.}; {@code name}, when set, says what the current
     * whole value stands for (a gas picked by number), so the screen can print it beside the key.
     */
    record Param(String key, float value, float step, float min, float max,
                 java.util.function.IntFunction<net.minecraft.network.chat.Component> name) {
        public Param(String key, float value, float step, float min, float max) {
            this(key, value, step, min, max, null);
        }
    }

    boolean on();

    List<Param> params();

    /** Server side: take new settings, in {@link #params()} order, clamped to each range. */
    void configure(boolean on, float[] values);

    static float clamp(Param p, float v) {
        return Math.max(p.min(), Math.min(p.max(), v));
    }
}
