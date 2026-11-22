// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.winch;

import io.github.soundgoodizerfan.feedback.combat.tether.FixedLines;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A drum on a shaft. Turned, it draws in a fixed line tied to it ({@link WinchBlockEntity}); turned the other way it pays the line out. The
 * shaft runs through along {@link #AXIS}; the line is tied to the winch with a grapple, as to a piton.
 */
public class WinchBlock extends Block implements EntityBlock, Rotatable {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    private static final VoxelShape X = Shapes.or(Block.box(0, 4, 4, 16, 12, 12), Block.box(1, 1, 1, 3, 15, 15), Block.box(13, 1, 1, 15, 15, 15));
    private static final VoxelShape Y = Shapes.or(Block.box(4, 0, 4, 12, 16, 12), Block.box(1, 1, 1, 15, 3, 15), Block.box(1, 13, 1, 15, 15, 15));
    private static final VoxelShape Z = Shapes.or(Block.box(4, 4, 0, 12, 12, 16), Block.box(1, 1, 1, 15, 15, 3), Block.box(1, 1, 13, 15, 15, 15));

    public WinchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    /** The shaft runs through the face the player clicked, so it joins the shaft they clicked on. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    /** A winch that goes takes its line with it. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide)
            FixedLines.onBroken(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WinchBlockEntity(pos, state);
    }
}
