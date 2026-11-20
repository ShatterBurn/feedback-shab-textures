// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The bearing sensor (docs/specs/feedback_space_spec.md 4.5): a block in a craft that reads how far off the End its front points, and
 * how far away it is, and feeds a Controller's Input card. The gas giant in the sky marks the way by eye; this makes it reliable.
 * Sneak-use steps the reading (yaw, pitch, distance); the facing is its front. Model: placeholder cube, art not drawn.
 */
public class BearingSensorBlock extends BaseEntityBlock {

    public static final MapCodec<BearingSensorBlock> CODEC = simpleCodec(BearingSensorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public BearingSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof BearingSensorBlockEntity sensor) {
            sensor.nextMode();
            io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, state.getBlock().getName(),
                    io.github.soundgoodizerfan.feedback.net.AimPayloads.keys("feedback.bearing_sensor.", "yaw", "pitch", "distance"),
                    sensor.mode().ordinal(), Component.empty());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BearingSensorBlockEntity(pos, state);
    }
}
