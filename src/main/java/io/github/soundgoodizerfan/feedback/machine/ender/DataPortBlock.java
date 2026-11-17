// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** One end of an entangled data pair (endernet layer 1). A chunk anchor used on it keeps its chunk loaded; an empty hand reads its tank and the pair. */
public class DataPortBlock extends Block implements EntityBlock {

    public DataPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(FItems.ENDER_ANCHOR.get()) && level.getBlockEntity(pos) instanceof DataPortBlockEntity port) {
            if (!level.isClientSide && !port.anchored()) {
                port.anchor();
                if (!player.isCreative())
                    stack.shrink(1);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (io.github.soundgoodizerfan.feedback.machine.CircuitSlot.accepts(stack) && level.getBlockEntity(pos) instanceof DataPortBlockEntity port) {
            if (!level.isClientSide) {
                ItemStack old = port.circuit().seat(stack);
                port.setChanged();
                if (!old.isEmpty() && !player.getInventory().add(old))
                    player.drop(old, false);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof DataPortBlockEntity taking && !taking.circuit().isEmpty()) {
            if (!level.isClientSide) {
                ItemStack out = taking.circuit().take();
                taking.setChanged();
                if (!player.getInventory().add(out))
                    player.drop(out, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DataPortBlockEntity port)
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable("message.feedback.data_port",
                    port.teleportantMb(), port.anchored() ? Component.translatable("message.feedback.ender_port.anchored")
                            : Component.translatable("message.feedback.ender_port.free"), Component.translatable(port.stateKey())));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DataPortBlockEntity port && !level.isClientSide)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), port.circuit().take());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataPortBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((DataPortBlockEntity) be).tickServer();
    }
}
