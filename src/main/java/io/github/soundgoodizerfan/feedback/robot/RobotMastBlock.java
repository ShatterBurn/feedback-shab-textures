// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/** The base end of a robot radio link: pair it to a robot with the Data Connector (select the mast, then the robot). See {@link RobotMastBlockEntity}. */
public class RobotMastBlock extends Block implements EntityBlock {

    /** Mirrors models/block/robot_mast.json (scripts/robot_models.py): base, four legs, radio box, platform, whip. The open faces are not filled. */
    private static final int[][] BOXES = {
            {3, 0, 3, 13, 2, 13}, {4, 2, 4, 6, 12, 6}, {10, 2, 4, 12, 12, 6}, {4, 2, 10, 6, 12, 12}, {10, 2, 10, 12, 12, 12},
            {6, 2, 6, 10, 6, 10}, {3, 12, 3, 13, 14, 13}, {7, 14, 7, 9, 16, 9}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public RobotMastBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RobotMastBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RobotMastBlockEntity) be).tickServer();
    }
}
