// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

/**
 * Figures for the LSO bridge. LSO's body scale runs 0 to 40 with 20 normal, and Feedback's room is 20 Tu with the Nether at
 * 40, so a Tu goes across as the same number. Every constant here is invented, there being no world figure behind a game's
 * comfort scale.
 */
final class LsoTuning {

    private LsoTuning() {
    }

    /** How far, in blocks, a hot or cold machine warms or chills a player. */
    static final int HEAT_RADIUS = 5;
    /** Body points per Tu a machine stands above or below the room, at point-blank range. */
    static final float HEAT_PER_TU = 0.02f;
    /** The most one machine adds, and the most all machines together add (either way), in body points. */
    static final float HEAT_PER_SOURCE_MAX = 8f, HEAT_TOTAL_MAX = 15f;

    /** Thermal resistance, in body points, a full set of perfectly insulating armour gives; a piece gets its share. */
    static final float INSULATION_SET = 12f;
    static final float SHARE_HEAD = 0.15f, SHARE_CHEST = 0.40f, SHARE_LEGS = 0.30f, SHARE_FEET = 0.15f;
    /** The insulation of an organic material whose file states no conductivity: leather and cloth hold some. */
    static final float ORGANIC_INSULATION = 0.5f;

    /** Hydration per point of a food's nutrition, at full water. A dry food gives proportionally less. */
    static final float HYDRATION_PER_NUTRITION = 1.0f;
    /** A meal this far (Tu) from the room warms or chills; each step of this many more Tu is one stronger level. */
    static final float MEAL_DEADBAND = 15f, MEAL_LEVEL_STEP = 25f;
    /** Ticks of warmth or chill per Tu past the deadband, within the bounds. */
    static final int MEAL_TICKS_PER_TU = 20, MEAL_MIN_TICKS = 400, MEAL_MAX_TICKS = 3600;
    /** A food this wet or wetter counts as a drink for LSO's effect group. */
    static final float DRINK_MOISTURE = 0.6f;

    /** The body-scale readout steps: an instrument's step for a quantity is for the Tu scale of hot work, so the body (0 to 40) takes a tenth of it. */
    static final float BODY_STEP_SHARE = 0.1f;
}
