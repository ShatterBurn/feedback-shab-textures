// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.control.FacedShape;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Where a Punch Card's program is authored. Right click to open the node editor. */
public class ProgrammerBlock extends Block implements EntityBlock {

    private final boolean electric;


    /** Which way the front faces: toward the player who placed it. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Mirrors models/block/programmer.json (the desk) and electric_programmer.json (the cabinet), scripts/control_models.py. */
    private static final java.util.Map<Direction, VoxelShape> DESK = FacedShape.of(new int[][]{
            {0, 0, 0, 16, 14, 16}, {1, 14, 11, 15, 16, 13}});
    private static final java.util.Map<Direction, VoxelShape> CABINET = FacedShape.of(new int[][]{{1, 0, 1, 15, 16, 15}});

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (electric ? CABINET : DESK).get(state.getValue(FACING));
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public ProgrammerBlock(Properties properties) {
        this(properties, false);
    }

    /** {@code electric}: the Electric Programmer, which takes every program medium; the plain one takes the Punch Card alone. */
    public ProgrammerBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean electric() {
        return electric;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        ProgrammerBlockEntity programmer = ProgrammerBlockEntity.at(level, pos);
        if (programmer == null)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            programmer.sync();
            player.openMenu(programmer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProgrammerBlockEntity programmer)
            Containers.dropContents(level, pos, programmer);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProgrammerBlockEntity(pos, state);
    }
}
