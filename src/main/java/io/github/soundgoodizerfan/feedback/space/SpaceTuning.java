// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

/** Placeholders for the Shelf crossing: tuned by play. */
public final class SpaceTuning {
    private SpaceTuning() {
    }

    /** Where a body arrives in the Shelf: well above its floor, near the middle of the height range. */
    public static final double ENTRY_Y = 64;
    /** A body in the Shelf below this returns to the Overworld. */
    public static final double FLOOR_Y = 0;
    /** Where a body arrives in the Overworld: where the air is thin but not yet zero, so it does not go straight back up. */
    public static final double RETURN_Y = 300;
    /** Ticks a body must sit in air of density zero before it leaves the atmosphere. */
    public static final int HOLD_TICKS = 20;
    /** The trigger looks at bodies this often, in ticks. */
    public static final int CHECK_INTERVAL = 5;
    /** A body in the Shelf slower than Sable's sleep threshold (0.15 b/s) but faster than this is woken each tick so it keeps coasting. */
    public static final double DRIFT_MIN_SPEED = 0.005;
    /** Ticks the crossing overlay takes to clear. */
    public static final int FADE_TICKS = 60;
}
