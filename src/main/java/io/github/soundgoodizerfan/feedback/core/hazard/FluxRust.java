// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Flux eats bare metal machines. In a chunk whose Thaumaturge flux stands past {@link HazardTuning#FLUX_RUST_THRESHOLD} of its base, every
 * {@link Corrodible} machine there loses a little condition each {@link HazardTuning#FLUX_RUST_PERIOD_TICKS}, more the further past the line,
 * unless its block wears a glaze ({@link Coats}). It is the place doing it and not the machine's use, so it is not uptime wear: a coat, or
 * a scrubbing bed that brings the flux down, stops it. Only chunks near a player are looked at (the others are not loaded for anyone), and
 * with Thaumaturge absent the whole sweep is skipped.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FluxRust {

    private FluxRust() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % HazardTuning.FLUX_RUST_PERIOD_TICKS != 0
                || !ThaumaturgeFlux.present())
            return;
        Set<Long> seen = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            ChunkPos centre = player.chunkPosition();
            int r = HazardTuning.FLUX_RUST_RADIUS_CHUNKS;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    ChunkPos pos = new ChunkPos(centre.x + dx, centre.z + dz);
                    if (seen.add(pos.toLong()))
                        sweep(level, pos);
                }
        }
    }

    private static void sweep(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null || chunk.getBlockEntities().isEmpty())
            return;
        float saturation = ThaumaturgeFlux.saturation(level, pos.getWorldPosition());
        if (saturation <= HazardTuning.FLUX_RUST_THRESHOLD)
            return;
        float over = (saturation - HazardTuning.FLUX_RUST_THRESHOLD) / (1f - HazardTuning.FLUX_RUST_THRESHOLD);
        float bite = HazardTuning.FLUX_RUST_PER_PERIOD * Math.min(1f, over);
        Coats coats = null;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (!(be instanceof Corrodible machine))
                continue;
            BlockPos at = be.getBlockPos();
            if (coats == null)
                coats = Coats.get(level);
            if (!coats.has(at, be.getBlockState()))
                machine.corrode(bite);
        }
    }
}
