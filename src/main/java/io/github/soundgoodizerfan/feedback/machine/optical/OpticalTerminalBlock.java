// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.optical;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.machine.CircuitSlot;

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

/**
 * The Optical Terminal: one end of a fibre link. Right-click with a crystal or a circuit seats it (the one there comes back), sneak with an
 * empty hand takes the circuit out and then the crystal, and an empty hand reads what the terminal is doing.
 */
public class OpticalTerminalBlock extends Block implements EntityBlock {

    public OpticalTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OpticalTerminalBlockEntity terminal))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean circuit = CircuitSlot.accepts(stack);
        if (!circuit && !OpticalTerminalBlockEntity.isCrystal(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack old = circuit ? terminal.circuit().seat(stack) : terminal.seatCrystal(stack);
            terminal.setChanged();
            if (!old.isEmpty() && !player.getInventory().add(old))
                player.drop(old, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OpticalTerminalBlockEntity terminal))
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            ItemStack out = !terminal.circuit().isEmpty() ? terminal.circuit().take() : terminal.takeCrystal();
            if (out.isEmpty())
                return InteractionResult.PASS;
            if (!level.isClientSide) {
                terminal.setChanged();
                if (!player.getInventory().add(out))
                    player.drop(out, false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            String key = terminal.crystal().isEmpty() ? "message.feedback.optical_terminal.no_crystal"
                    : terminal.lit() ? "message.feedback.optical_terminal.lit" : "message.feedback.optical_terminal.dark";
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable(key));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof OpticalTerminalBlockEntity terminal) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), terminal.circuit().take());
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), terminal.takeCrystal());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OpticalTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((OpticalTerminalBlockEntity) be).tickServer();
    }
}
