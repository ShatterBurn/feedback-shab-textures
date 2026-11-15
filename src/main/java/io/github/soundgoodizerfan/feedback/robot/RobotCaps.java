// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * How many robots stand in each dimension, and the check a new one passes before it is set down: no more than {@link RobotConfig#PER_CHUNK}
 * in its chunk and {@link RobotConfig#PER_DIMENSION} in its dimension. A robot carries no owner, so the caps are by place, not by player.
 * The count is server memory, rebuilt as robots load.
 */
public final class RobotCaps {

    private RobotCaps() {
    }

    private static final Map<ResourceKey<Level>, Integer> COUNT = new HashMap<>();

    /** A new server: no robots are counted. */
    public static void reset() {
        COUNT.clear();
    }

    static void add(Level level) {
        COUNT.merge(level.dimension(), 1, Integer::sum);
    }

    static void remove(Level level) {
        COUNT.merge(level.dimension(), -1, (a, b) -> Math.max(0, a + b));
    }

    /** The lang key of the cap a robot set down at {@code pos} would break, or null if it may stand there. */
    @Nullable
    public static String refusal(Level level, BlockPos pos) {
        if (COUNT.getOrDefault(level.dimension(), 0) >= RobotConfig.PER_DIMENSION.get())
            return "robot.feedback.cap_dimension";
        ChunkPos chunk = new ChunkPos(pos);
        AABB box = new AABB(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunk.getMaxBlockZ() + 1);
        if (level.getEntitiesOfClass(RobotEntity.class, box).size() >= RobotConfig.PER_CHUNK.get())
            return "robot.feedback.cap_chunk";
        return null;
    }
}
