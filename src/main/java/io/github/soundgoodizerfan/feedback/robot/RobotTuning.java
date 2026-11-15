// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

/** Placeholder figures for robots (spec {@code FTuning.ROBOT_*}); none is tuned yet. */
public final class RobotTuning {

    private RobotTuning() {
    }

    /** Ticks between program steps: a robot's Controller runs at a reduced rate, its movement every tick. */
    public static final int STEP_TICKS = 4;
    /** Blocks per tick of a massless robot on its drive, and the mass (Mu) at which that halves. */
    public static final float SPEED_MAX = 0.14f, SPEED_HALF_MASS = 40f;
    /** Eu drawn per tick while moving, and per tick idle. */
    public static final float EU_MOVE_PER_TICK = 0.6f, EU_IDLE_PER_TICK = 0.02f;
    /** Eu to break a block, per unit of its hardness. */
    public static final float EU_PER_HARDNESS = 8f;
    /** Ticks to break a block, per unit of hardness, at a head strength of {@link #HEAD_ST_REF}. */
    public static final float DIG_TICKS_PER_HARDNESS = 20f;
    /** The head's yield strength that digs at the base rate, and the strength per unit of hardness below which it cannot break a block at all. */
    public static final float HEAD_ST_REF = 250f, HEAD_ST_PER_HARDNESS = 6f;
    /** Ticks a quarter turn, a place, a pick-up and a drop take. */
    public static final int TURN_TICKS = 8, PLACE_TICKS = 6, HANDLE_TICKS = 6;
    /** Blocks of position error a wheeled robot gathers per block rolled; an inertial unit keeps {@link #INERTIAL_DRIFT_SHARE} of it. */
    public static final float DRIFT_PER_BLOCK = 0.06f, INERTIAL_DRIFT_SHARE = 0.25f;
    /** Blocks within which a beacon corrects a robot's position; how far a rangefinder reaches. */
    public static final int BEACON_RANGE = 3, RANGE_MAX = 8;
    /** Blocks a tethered robot may roam from its reel. */
    public static final float TETHER_LENGTH = 12f;
    /** Slots of cargo a robot carries. */
    public static final int CARGO = 9;
}
