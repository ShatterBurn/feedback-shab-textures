// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/** A length of casing, stacked on the derrick base for every 8 m of hole. */
public class DerrickCasingBlock extends Block implements EntityBlock {

    /** Mirrors models/block/derrick_casing.json: the cage, so the player can reach through its openings. */
    private static final int[][] BOXES = {
            {0, 0, 0, 2, 16, 2}, {0, 0, 14, 2, 16, 16}, {14, 0, 0, 16, 16, 2}, {14, 0, 14, 16, 16, 16},
            {0, 0, 2, 2, 2, 14}, {0, 7, 2, 2, 9, 14}, {0, 14, 2, 2, 16, 14},
            {14, 0, 2, 16, 2, 14}, {14, 7, 2, 16, 9, 14}, {14, 14, 2, 16, 16, 14},
            {2, 0, 0, 14, 2, 2}, {2, 14, 0, 14, 16, 2}, {2, 0, 14, 14, 2, 16}, {2, 14, 14, 14, 16, 16}};

    /** The cage's open ends face this way (the side frames are the other pair), drawn facing north. */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = io.github.soundgoodizerfan.feedback.core.FacingShapes.horizontal(BOXES);

    public DerrickCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DerrickCasingBlockEntity(pos, state);
    }
}
