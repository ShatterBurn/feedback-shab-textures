// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.essentiaadapter;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/** From a Thaumaturge essentia store to a Feedback gas. See {@link EssentiaAdapterBlockEntity}. Its gas port is published here. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class EssentiaAdapterBlock extends Block implements EntityBlock {

    public EssentiaAdapterBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaAdapterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((EssentiaAdapterBlockEntity) be).tickServer();
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FBlockEntities.ESSENTIA_ADAPTER.get(), (adapter, side) -> adapter.port());
    }
}
