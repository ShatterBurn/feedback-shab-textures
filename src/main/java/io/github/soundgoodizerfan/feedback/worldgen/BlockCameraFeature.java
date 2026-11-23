// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.machine.blockcamera.BlockCameraBlock;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Stands traffic cameras on their poles: a Block Camera on three to five lengths of steel pole, set on firm dry ground anywhere,
 * but hundreds of times more often beside a road. A chunk takes {@value #SAMPLES} random columns. A column of road (a block in
 * {@code feedback:camera_roads}) raises a camera one or two blocks to the side with the chance {@value #ROADSIDE}, facing the road;
 * any other column does with the chance {@value #ANYWHERE}. So a village's paths and a cobbled track hold cameras and open
 * country almost never does. The chances are placeholders for the author to tune. Which blocks count as a road is data.
 * <p>
 * Every draw comes from the feature's own chunk-seeded random, so a chunk generates the same cameras every time.
 */
public class BlockCameraFeature extends Feature<NoneFeatureConfiguration> {

    static final int SAMPLES = 12;
    static final float ANYWHERE = 0.00015f;
    static final float ROADSIDE = 0.015f;
    static final int MIN_POLE = 3, MAX_POLE = 5;

    public BlockCameraFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        boolean placed = false;
        for (int i = 0; i < SAMPLES; i++) {
            int x = origin.getX() + random.nextInt(16), z = origin.getZ() + random.nextInt(16);
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int gap = 1 + random.nextInt(2);
            int height = MIN_POLE + random.nextInt(MAX_POLE - MIN_POLE + 1);
            float roll = random.nextFloat();
            BlockPos top = top(level, x, z);
            if (top == null)
                continue;
            BlockPos ground = top;
            if (road(level, top)) {
                if (roll >= ROADSIDE)
                    continue;
                // Beside the road, looking at it.
                BlockPos side = top(level, x + facing.getStepX() * gap, z + facing.getStepZ() * gap);
                if (side == null || road(level, side))
                    continue;
                ground = side;
                facing = facing.getOpposite();
            } else if (roll >= ANYWHERE)
                continue;
            if (firm(level, ground))
                placed |= stand(level, ground, facing, height);
        }
        return placed;
    }

    /** The top solid block of a column, or null where it is out of reach of this chunk's writes. */
    private static BlockPos top(WorldGenLevel level, int x, int z) {
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1, z);
        return level.ensureCanWrite(top) ? top : null;
    }

    /** Whether a column is road: its top block is, or the block laid on it is (a rail does not count as the column's top). */
    private static boolean road(WorldGenLevel level, BlockPos top) {
        return level.getBlockState(top).is(FTags.CAMERA_ROADS) || level.getBlockState(top.above()).is(FTags.CAMERA_ROADS);
    }

    /** Whether a camera can stand on this block: it has a firm upward face and holds no fluid. A path's top is a pixel low, so roads are judged before this. */
    private static boolean firm(WorldGenLevel level, BlockPos ground) {
        BlockState state = level.getBlockState(ground);
        return state.isFaceSturdy(level, ground, Direction.UP) && state.getFluidState().isEmpty();
    }

    private static boolean stand(WorldGenLevel level, BlockPos ground, Direction facing, int height) {
        for (int y = 1; y <= height + 1; y++) {
            BlockPos at = ground.above(y);
            BlockState state = level.getBlockState(at);
            if (!level.ensureCanWrite(at) || !state.getFluidState().isEmpty() || !(state.isAir() || state.canBeReplaced()))
                return false;
        }
        for (int y = 1; y <= height; y++)
            level.setBlock(ground.above(y), FBlocks.CAMERA_POLE.get().defaultBlockState(), 2);
        level.setBlock(ground.above(height + 1), FBlocks.BLOCK_CAMERA.get().defaultBlockState().setValue(BlockCameraBlock.FACING, facing), 2);
        return true;
    }
}
