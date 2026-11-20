// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.flare;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** A flare stack. Lit while it burns gas, which makes it a light and an ignition source. */
public class FlareBlock extends Block implements EntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** The feed pipe is on the front. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Mirrors models/block/flare.json drawn facing north: base plate, stack, open mouth, feed pipe with its drop. */
    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = io.github.soundgoodizerfan.feedback.core.FacingShapes.horizontal(new int[][]{
            {4, 0, 4, 12, 2, 12}, {6, 2, 6, 10, 14, 10}, {5, 14, 5, 11, 16, 11}, {7, 2, 0, 9, 4, 6}, {7, 0, 0, 9, 2, 2}});

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    public FlareBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlareBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((FlareBlockEntity) be).tickServer();
    }
}
