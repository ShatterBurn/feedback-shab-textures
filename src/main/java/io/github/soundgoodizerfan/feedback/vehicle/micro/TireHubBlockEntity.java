// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A coarse-space tire hub's block entity: no data, only what a renderer binds to (the round tire is drawn about the axle, beyond the block). */
public class TireHubBlockEntity extends BlockEntity {

    public TireHubBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.TIRE_HUB_ENTITY.get(), pos, state);
    }
}
