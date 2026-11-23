// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.scrubber;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Essentia Vent: set it against an essentia store; an empty hand reads in words whether it is open, closed or touching nothing. */
public class EssentiaVentBlock extends Block implements EntityBlock {

    public EssentiaVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EssentiaVentBlockEntity vent))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            String key = vent.gauge() < 0 ? "message.feedback.essentia_vent.nothing"
                    : vent.open() ? "message.feedback.essentia_vent.open" : "message.feedback.essentia_vent.closed";
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable(key));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssentiaVentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((EssentiaVentBlockEntity) be).tickServer();
    }
}
