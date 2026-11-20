// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.seat;

import com.mojang.serialization.MapCodec;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A Driver's Seat: use it to sit; then the keyboard is the seat's ({@link DriverSeatBlockEntity}). */
public class DriverSeatBlock extends BaseEntityBlock {

    public static final MapCodec<DriverSeatBlock> CODEC = simpleCodec(DriverSeatBlock::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    /** Mirrors models/block/driver_seat.json (scripts/driver_seat_model.py), facing north: base, cushion at the back, column, the wheel as one plate. */
    private static final int[][] BOXES = {{0, 0, 0, 16, 4, 16}, {2, 4, 6, 14, 8, 16}, {7, 4, 2, 9, 8, 4}, {2, 8, 1, 14, 16, 5}};
    /** One shape per horizontal facing, indexed by {@code Direction.get2DDataValue()} (south, west, north, east). */
    private static final VoxelShape[] SHAPES = shapes();

    private static VoxelShape[] shapes() {
        VoxelShape[] out = new VoxelShape[4];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int quarter = switch (d) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
            VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
            for (int[] b : BOXES) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                for (int i = 0; i < quarter; i++) { // clockwise from above: (x, z) -> (16 - z, x)
                    double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                    x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
                }
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(x0, b[1], z0, x1, b[4], z1));
            }
            out[d.get2DDataValue()] = shape;
        }
        return out;
    }

    public DriverSeatBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The wheel is ahead of the driver: the seat faces the way the player looks when placing it. */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() || player.isPassenger())
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!level.getEntitiesOfClass(DriverSeatEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty())
            return InteractionResult.CONSUME; // taken
        DriverSeatEntity seat = new DriverSeatEntity(FVehicles.SEAT_RIDE.get(), level);
        seat.setPos(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5);
        level.addFreshEntity(seat);
        player.startRiding(seat);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DriverSeatBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FVehicles.DRIVER_SEAT_ENTITY.get(), DriverSeatBlockEntity::serverTick);
    }
}
