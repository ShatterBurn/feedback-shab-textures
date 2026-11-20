// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;

/**
 * What a body's scale does to the forces on it (docs/specs/feedback_space_spec.md 2.4). Time is unchanged and lengths scale by
 * {@code k}, so a craft's speeds and accelerations scale by {@code k}. Sable Scaling scales the mass with the volume ({@code k^3}),
 * so a force that makes the same craft-lengths-per-second2 must scale by {@code k^4}. A force worked out from block counts (a
 * thruster's, a plot box's cross-section) is the full-size one, so the scaled body is pushed by these factors of it.
 * <p>
 * shortcut: assumes Sable Scaling scales mass as the cube; if it does not, only {@link #MASS_EXPONENT} changes.
 */
public final class BodyScale {
    /** Sable Scaling scales a body's mass with its volume. */
    private static final int MASS_EXPONENT = 3;

    private BodyScale() {
    }

    /** The body's uniform scale, 1 for a full-size body. */
    public static double of(ServerSubLevel body) {
        return body.logicalPose().scale().x();
    }

    /** Factor on a force worked out for a full-size body, so the scaled body speeds up {@code k} times as fast in blocks. */
    public static double force(ServerSubLevel body) {
        return Math.pow(of(body), MASS_EXPONENT + 1);
    }

    /**
     * Factor on an air-drag impulse worked out from the body's block counts and its own speed. Drag goes with speed squared, and
     * the full-size body's speed is {@code speed / k}, so the force factor {@code k^4} leaves {@code k^2} on the scaled speed.
     */
    public static double drag(ServerSubLevel body) {
        double k = of(body);
        return k * k;
    }
}
