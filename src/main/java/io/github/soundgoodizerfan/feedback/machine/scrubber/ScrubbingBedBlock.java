// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.scrubber;

import org.jetbrains.annotations.Nullable;

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
 * The Scrubbing Bed: right-click with a catalyst-bearing item (charcoal, a molecular sieve) to pack it in, sneak with an empty hand to take
 * the bed out, an empty hand to read how full it is in words. A fire set directly beneath burns the load off.
 */
public class ScrubbingBedBlock extends Block implements EntityBlock {

    public ScrubbingBedBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ScrubbingBedBlockEntity bed) || !ScrubbingBedBlockEntity.isBed(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack old = bed.seat(stack);
            if (!old.isEmpty() && !player.getInventory().add(old))
                player.drop(old, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ScrubbingBedBlockEntity bed))
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            ItemStack out = bed.take();
            if (out.isEmpty())
                return InteractionResult.PASS;
            if (!level.isClientSide && !player.getInventory().add(out))
                player.drop(out, false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            String key;
            if (bed.bed().isEmpty())
                key = "message.feedback.scrubbing_bed.empty";
            else if (bed.activity() <= 0f)
                key = "message.feedback.scrubbing_bed.ruined";
            else if (bed.saturated())
                key = "message.feedback.scrubbing_bed.saturated";
            else if (bed.fill() >= 0.5f)
                key = "message.feedback.scrubbing_bed.loaded";
            else
                key = "message.feedback.scrubbing_bed.fresh";
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable(key));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ScrubbingBedBlockEntity bed)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bed.take());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScrubbingBedBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ScrubbingBedBlockEntity) be).tickServer();
    }
}
