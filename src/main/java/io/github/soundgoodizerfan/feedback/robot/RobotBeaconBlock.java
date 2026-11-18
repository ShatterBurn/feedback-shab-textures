// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A landmark that wipes a robot's dead-reckoning error (see {@code RobotEntity}): a banded post with a lamp, the shape a robot's sensors pick out. */
public class RobotBeaconBlock extends Block {

    /** Mirrors models/block/robot_beacon.json (scripts/robot_models.py): base, post, collar, dome, cap. */
    private static final int[][] BOXES = {
            {4, 0, 4, 12, 2, 12}, {6, 2, 6, 10, 10, 10}, {5, 10, 5, 11, 12, 11}, {6, 12, 6, 10, 14, 10}, {7, 14, 7, 9, 16, 9}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public RobotBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
