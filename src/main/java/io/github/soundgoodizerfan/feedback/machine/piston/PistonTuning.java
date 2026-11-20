// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** The piston's constants. All invented except where a comment says otherwise. */
public final class PistonTuning {

    private PistonTuning() {
    }

    /** Pressure a full gas holder (tank, vessel, pipe run) puts on the cylinder; a part-empty one gives its fill share of it, never under {@link #MIN_FILL}. */
    public static final float SUPPLY_PU = 2f;
    public static final float MIN_FILL = 0.25f;
    /** Mu of block a cylinder can push for each Pu of supply on each cm2 of bore. */
    public static final float MU_PER_PU_CM2 = 6f;
    /** mB of gas one block of stroke takes for each cm2 of bore. */
    public static final float MB_PER_CM2 = 4f;
    /**
     * STAND-IN: a block weighs this many Mu for each point of its hardness, never under {@link #MIN_BLOCK_MU}; blocks carry no mass of their
     * own yet, and when one does this is the one place to replace.
     */
    public static final float BLOCK_MU_PER_HARDNESS = 8f;
    public static final float MIN_BLOCK_MU = 4f;
    /** Blocks in a pushed line, whatever the mass. */
    public static final int MAX_LINE = 24;
    /** Ticks a stroke takes a block of travel. */
    public static final int STEP_TICKS = 2;
    /** Cylinder danger line in Pu: this times the shell's yield (St) times wall over inner radius. */
    public static final float CYLINDER_PU_PER_STRESS = 2.5f;
    public static final float CRITICAL_OVER_DANGER = 1.4f;
    /** Gas a seal lets by, as a share of the stroke: the seal material's hardness (St) over {@link #LEAK_REF_HARDNESS} times this. */
    public static final float LEAK_BASE = 0.05f;
    public static final float LEAK_REF_HARDNESS = 3f;
    /** A worn seal leaks up to this many times more. */
    public static final float LEAK_WORN_GAIN = 3f;
    /** Seal condition lost each time a push stalls on a line the cylinder cannot move. */
    public static final float SEAL_WEAR_PER_STALL = 0.05f;
    /** Explosion power of a cylinder letting go. */
    public static final float BURST_POWER = 1.5f;

    /** The fixed block's old figures: what a piston placed from no design runs on (an iron cylinder, 40 mm bore, 4 mm wall, leather seal). */
    public static final float DEFAULT_AREA_CM2 = 12.57f;
    public static final float DEFAULT_DANGER_PU = 4f;
    public static final float DEFAULT_LEAK = 0.1f;

    /** What the block at {@code pos} weighs when pushed, in Mu. */
    public static float massOf(BlockState state, BlockGetter level, BlockPos pos) {
        return Math.max(MIN_BLOCK_MU, state.getDestroySpeed(level, pos) * BLOCK_MU_PER_HARDNESS);
    }
}
