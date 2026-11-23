// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads the aura of the chunk it stands in and gives it to a controller: channel 0 is the vis as a share of the chunk's base aura (0 to 1,
 * saturating), channel 1 is the flux saturation (0 to 1: 1 is a chunk at its limit), and channels 2 to 15 switch on as the flux saturation
 * climbs past each fourteenth. Without Thaumaturge every channel reads 0. Measurement only: it ticks nothing and controls nothing, and what
 * a controller does with a reading is the controller's business.
 */
public class AuraSensorBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    public AuraSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.AURA_SENSOR.get(), pos, state);
    }

    @Override
    public float read(int channel) {
        if (level == null)
            return 0f;
        if (channel <= 0)
            return Math.min(1f, ThaumaturgeFlux.visShare(level, worldPosition));
        float flux = Math.min(1f, ThaumaturgeFlux.saturation(level, worldPosition));
        return channel == 1 ? flux : (flux * 14f >= channel - 1 ? 1f : 0f);
    }

    // --- DataNode -----------------------------------------------------------------------------

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
