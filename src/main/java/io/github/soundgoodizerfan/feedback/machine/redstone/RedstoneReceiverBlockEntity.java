// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.redstone;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The redstone-to-data converter: a data node whose one reading, on every channel, is the strongest redstone signal reaching it (0 to 15).
 * A redstone signal is a strength with no unit, so it is read as the plain number it is; the program decides what a level means.
 */
public class RedstoneReceiverBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    public RedstoneReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REDSTONE_RECEIVER.get(), pos, state);
    }

    @Override
    public float read(int channel) {
        return level == null ? 0f : level.getBestNeighborSignal(worldPosition);
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }
}
