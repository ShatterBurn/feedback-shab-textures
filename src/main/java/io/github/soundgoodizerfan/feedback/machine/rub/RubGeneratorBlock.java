// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.rub;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * The Rub Generator: right-click with a rod of an insulator (amber, a polymer) to seat it; right-click with an empty hand to rub it (each rub
 * puts charge on the rod); sneak with an empty hand to take it out. Power flows into the wire it is joined to as a small burst.
 */
public class RubGeneratorBlock extends Block implements EntityBlock {

    public RubGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RubGeneratorBlockEntity gen) || !RubGeneratorBlockEntity.isRod(stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack old = gen.seat(stack);
            if (!old.isEmpty() && !player.getInventory().add(old))
                player.drop(old, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RubGeneratorBlockEntity gen))
            return InteractionResult.PASS;
        if (gen.rod().isEmpty()) {
            if (!level.isClientSide)
                io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable("message.feedback.rub_generator.no_rod"));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown()) {
            ItemStack out = gen.take();
            if (!level.isClientSide && !player.getInventory().add(out))
                player.drop(out, false);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            if (gen.rub())
                level.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 0.8f, 1.5f);
            else
                io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable("message.feedback.rub_generator.full"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof RubGeneratorBlockEntity gen)
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), gen.take());
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RubGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RubGeneratorBlockEntity) be).tickServer();
    }
}
