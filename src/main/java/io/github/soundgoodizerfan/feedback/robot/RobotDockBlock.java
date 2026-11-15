// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A robot's charging point on an electrical line: a <b>dock</b> charges the robot standing on it, a <b>reel</b> feeds the robot tied to it
 * by a tether cable for as long as it stays within the cable's length. Both are one sink; see {@link RobotDockBlockEntity}.
 */
public class RobotDockBlock extends Block implements EntityBlock {

    private final boolean reel;

    public RobotDockBlock(Properties properties, boolean reel) {
        super(properties);
        this.reel = reel;
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
