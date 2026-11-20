// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A lattice cell for a Nether star: the star in hand seats it, an empty hand takes it out. */
public class StarSeatBlock extends Block implements EntityBlock {

    /** Mirrors models/block/star_seat.json: four corner posts, four walls and a floor, the funnel rim, cone and spout. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0, 0, 0, 3, 16, 3), Block.box(13, 0, 0, 16, 16, 3), Block.box(0, 0, 13, 3, 16, 16),
            Block.box(13, 0, 13, 16, 16, 16), Block.box(3, 0, 1, 13, 16, 3), Block.box(3, 0, 13, 13, 16, 15),
            Block.box(1, 0, 3, 3, 16, 13), Block.box(13, 0, 3, 15, 16, 13), Block.box(3, 0, 3, 13, 2, 13),
            Block.box(3, 13, 3, 13, 15, 13), Block.box(5, 9, 5, 11, 13, 11), Block.box(7, 5, 7, 9, 9, 9));

    public StarSeatBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StarSeatBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof StarSeatBlockEntity seat && seat.seat(stack)) {
            if (!level.isClientSide && !player.isCreative())
                stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof StarSeatBlockEntity seat && seat.seated()) {
            if (!level.isClientSide)
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, seat.take());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof StarSeatBlockEntity seat && seat.seated())
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, seat.take());
        super.onRemove(state, level, pos, next, moved);
    }
}
