// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.blockcamera;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One length of the thin steel pole a Block Camera stands on. It holds nothing and ticks nothing. Take any length away and
 * everything standing on it comes down with it, so a camera on a felled pole still drops what a broken one does.
 */
public class BlockCameraPoleBlock extends Block {

    /** Mirrors models/block/camera_pole.json: a square steel column four pixels across. */
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public BlockCameraPoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** A pole needs the pole below it or firm ground; without them it goes, and onRemove takes the rest down. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
            BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.DOWN && !(neighbour.getBlock() instanceof BlockCameraPoleBlock)
                && !neighbour.isFaceSturdy(level, neighbourPos, Direction.UP))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos above = pos.above();
            Block block = level.getBlockState(above).getBlock();
            if (block instanceof BlockCameraPoleBlock || block instanceof BlockCameraBlock)
                level.destroyBlock(above, true);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
