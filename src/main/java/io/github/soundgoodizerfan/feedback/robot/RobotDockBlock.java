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

/**
 * A robot's charging point on an electrical line: a <b>dock</b> charges the robot standing on it, a <b>reel</b> feeds the robot tied to it
 * by a tether cable for as long as it stays within the cable's length. Both are one sink; see {@link RobotDockBlockEntity}.
 */
public class RobotDockBlock extends Block implements EntityBlock {

    private final boolean reel;

    /** Mirrors models/block/robot_reel.json (scripts/robot_models.py): base, two flanges, the wound core, hub, cable lead. The dock is a full cube. */
    private static final int[][] REEL_BOXES = {
            {2, 0, 2, 14, 2, 14}, {3, 2, 3, 13, 4, 13}, {5, 4, 5, 11, 12, 11}, {3, 12, 3, 13, 14, 13}, {7, 14, 7, 9, 16, 9}, {11, 4, 7, 13, 6, 9}};
    private static final VoxelShape REEL_SHAPE = reelShape();

    private static VoxelShape reelShape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : REEL_BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public RobotDockBlock(Properties properties, boolean reel) {
        super(properties);
        this.reel = reel;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return reel ? REEL_SHAPE : Shapes.block();
    }

    /** Whether this is the tether reel rather than the standing dock. */
    public boolean reel() {
        return reel;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RobotDockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RobotDockBlockEntity) be).tickServer();
    }
}
