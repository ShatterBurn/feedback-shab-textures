// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A steel spike with an eye, driven into the face of a block. {@code facing} is the way the eye points, away from the block it is driven
 * into. A grapple used on one piton and then on another ties a fixed line between them ({@link PitonBlockEntity}, {@link FixedLines}); the
 * piton is the line's anchor, and it holds only as well as the block it is driven into does.
 */
public class PitonBlock extends Block implements EntityBlock {

    public static final MapCodec<PitonBlock> CODEC = simpleCodec(PitonBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape UP = Block.box(6, 0, 6, 10, 9, 10);
    private static final VoxelShape DOWN = Block.box(6, 7, 6, 10, 16, 10);
    private static final VoxelShape NORTH = Block.box(6, 6, 7, 10, 10, 16);
    private static final VoxelShape SOUTH = Block.box(6, 6, 0, 10, 10, 9);
    private static final VoxelShape WEST = Block.box(7, 6, 6, 16, 10, 10);
    private static final VoxelShape EAST = Block.box(0, 6, 6, 9, 10, 10);

    public PitonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    /** A piton is driven into a face, so it is placed on the face clicked and needs a sturdy block behind it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction behind = state.getValue(FACING).getOpposite();
        BlockPos support = pos.relative(behind);
        return level.getBlockState(support).isFaceSturdy(level, support, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** A piton that goes takes its line with it (both ends let go); a chunk unloading does not come through here. */
    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide)
            FixedLines.onBroken(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** The point the line is fixed at: the eye, a little out from the block it is driven into. */
    public static net.minecraft.world.phys.Vec3 eye(BlockState state, BlockPos pos) {
        Direction out = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.UP;
        return net.minecraft.world.phys.Vec3.atCenterOf(pos).add(out.getStepX() * 0.3, out.getStepY() * 0.3, out.getStepZ() * 0.3);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PitonBlockEntity(pos, state);
    }
}
