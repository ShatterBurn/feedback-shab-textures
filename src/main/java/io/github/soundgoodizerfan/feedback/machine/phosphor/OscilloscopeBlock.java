// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.phosphor;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** An oscilloscope. Put it against a cable and a phosphor screen; right-click prints the trace. See {@link OscilloscopeBlockEntity}. */
public class OscilloscopeBlock extends BaseEntityBlock {

    public static final MapCodec<OscilloscopeBlock> CODEC = simpleCodec(OscilloscopeBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * The boxes of models/block/oscilloscope.json (front on north), as {x1, y1, z1, x2, y2, z2} in sixteenths: case, screen bezel,
     * the carry handle as one box (posts and bar), probe jack. Keep in step with scripts/oscilloscope_model.py.
     */
    private static final int[][] BOXES = {
            {0, 0, 2, 14, 10, 14}, {1, 4, 0, 13, 10, 2}, {1, 10, 6, 13, 14, 8}, {14, 4, 6, 16, 6, 8}};

    private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int[] b : BOXES) {
                // blockstate y rotation is clockwise from above: north -> east maps (x, z) to (16 - z, x)
                int x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                int[] r = switch (d) {
                    case EAST -> new int[]{16 - z2, x1, 16 - z1, x2};
                    case SOUTH -> new int[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case WEST -> new int[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new int[]{x1, z1, x2, z2};
                };
                shape = Shapes.or(shape, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
            }
            SHAPES.put(d, shape);
        }
    }

    public OscilloscopeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The screen faces the player who placed it. */
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

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof OscilloscopeBlockEntity scope))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, scope.trace());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OscilloscopeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((OscilloscopeBlockEntity) be).tickServer();
    }
}
