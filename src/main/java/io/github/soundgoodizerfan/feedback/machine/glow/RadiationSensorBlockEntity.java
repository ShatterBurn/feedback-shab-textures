// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reads the dose rate where it stands, the way a Geiger tube does, and gives it to a controller: channel 0 is the rate as a share of
 * {@link ElectricMachineTuning#RADIATION_SENSOR_FULL_RU} (saturating at 1), and channel 1 to 15 is whether that share is at least the
 * channel's fifteenth. It ticks nothing; the rate comes from {@link RadiationField}, which caches it per cell. Measurement only: it
 * controls nothing, and what a controller does with a reading is the controller's business.
 */
public class RadiationSensorBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    public RadiationSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.RADIATION_SENSOR.get(), pos, state);
    }

    /** The rate here as a share of the sensor's full scale, 0 to 1. */
    private float share() {
        if (level == null)
            return 0f;
        return Math.max(0f, Math.min(1f, RadiationField.at(level, worldPosition) / ElectricMachineTuning.RADIATION_SENSOR_FULL_RU));
    }

    @Override
    public float read(int channel) {
        float share = share();
        return channel <= 0 ? share : (share * 15f >= channel ? 1f : 0f);
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
