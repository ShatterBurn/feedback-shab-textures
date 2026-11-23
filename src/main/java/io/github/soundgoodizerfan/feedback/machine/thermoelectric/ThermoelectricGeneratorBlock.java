// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.thermoelectric;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A thermoelectric generator (see {@link ThermoelectricGeneratorBlockEntity}). The block carries nothing of its own: the legs it is built from
 * ride on the item, and its hot and cold sides are whatever touches it.
 */
public class ThermoelectricGeneratorBlock extends Block implements EntityBlock {

    public ThermoelectricGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThermoelectricGeneratorBlockEntity(pos, state);
    }
}
