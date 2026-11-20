// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.waterwheel;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Continuous rotation, paid for with real estate and a river rather than with fuel.
 * <p>
 * Slice 1 gives it 256 Su against the hammer's 80, which is the arithmetic the player is meant
 * to run: three hammers fit, four do not, and nothing anywhere says "requires tier 2".
 */
public class WaterWheelBlock extends RotatedPillarBlock implements EntityBlock, Rotatable {

    public WaterWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    /**
     * The wheel's bucketed drum (rims at 2..4 and 12..14 of the axle, buckets between) with the axle through it, one shape per axle axis.
     * Mirrors models/block/water_wheel.json (scripts/water_wheel_model.py); the shape does not turn with the wheel.
     */
    private static final VoxelShape SHAPE_X = Shapes.or(Block.box(2, 0, 0, 14, 16, 16), Block.box(0, 6, 6, 16, 10, 10));
    private static final VoxelShape SHAPE_Y = Shapes.or(Block.box(0, 2, 0, 16, 14, 16), Block.box(6, 0, 6, 10, 16, 10));
    private static final VoxelShape SHAPE_Z = Shapes.or(Block.box(0, 0, 2, 16, 16, 14), Block.box(6, 6, 0, 10, 10, 16));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            case Z -> SHAPE_Z;
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

    /**
     * Kept out of the chunk mesh, because {@code RotatingVisual} draws this block itself and would
     * otherwise put a turning copy on top of a motionless one. With Flywheel's backend off,
     * {@code RotatingRenderer} draws it instead; there is no path where nothing does.
     * <p>
     * {@code ENTITYBLOCK_ANIMATED} rather than {@code INVISIBLE}: the chunk mesh skips both, but
     * only {@code INVISIBLE} also suppresses block-breaking particles, and a shaft that shatters
     * silently is a sense taken away for nothing.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaterWheelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((WaterWheelBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((WaterWheelBlockEntity) be).tickServer();
    }
}
