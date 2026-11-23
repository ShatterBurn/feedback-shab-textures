// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * The warmth a player feels from Feedback machines near them. Walks the block entities of the loaded chunks the radius overlaps
 * (every block entity there, cables and pipes included, each costing one type test, never the block volume), reads a fire's flame
 * or a body's temperature against the room, and adds a share that falls off with distance. Nothing ticks for it. The answer is
 * kept per position for {@link #KEEP_TICKS}, so LSO asking again (it recomputes the target about once a second per player, and
 * a client HUD may ask more) costs a map lookup.
 */
final class LsoHeat {

    private static final long KEEP_TICKS = 20L;
    private static final java.util.Map<Long, float[]> KEPT = new java.util.HashMap<>();

    private LsoHeat() {
    }

    static float near(Level level, BlockPos pos) {
        long now = level.getGameTime();
        long key = pos.asLong() ^ ((long) level.dimension().location().hashCode() << 32) ^ (level.isClientSide ? 1L : 0L);
        float[] kept = KEPT.get(key);
        if (kept != null && now - (long) kept[1] < KEEP_TICKS && now >= (long) kept[1])
            return kept[0];
        float found = scan(level, pos);
        if (KEPT.size() > 64)
            KEPT.clear();
        KEPT.put(key, new float[] {found, now});
        return found;
    }

    private static float scan(Level level, BlockPos pos) {
        int r = LsoTuning.HEAT_RADIUS;
        float sum = 0f;
        int minX = (pos.getX() - r) >> 4, maxX = (pos.getX() + r) >> 4, minZ = (pos.getZ() - r) >> 4, maxZ = (pos.getZ() + r) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    Tu tu;
                    if (be instanceof HeatSource source)
                        tu = source.getFireTu();
                    else if (be instanceof ThermalBody body)
                        tu = body.getTemperature();
                    else
                        continue;
                    double dist = Math.sqrt(be.getBlockPos().distSqr(pos));
                    if (dist >= r)
                        continue;
                    float share = (tu.value() - AmbientTable.at(level).value()) * LsoTuning.HEAT_PER_TU * (float) (1.0 - dist / r);
                    sum += Mth.clamp(share, -LsoTuning.HEAT_PER_SOURCE_MAX, LsoTuning.HEAT_PER_SOURCE_MAX);
                }
            }
        }
        return Mth.clamp(sum, -LsoTuning.HEAT_TOTAL_MAX, LsoTuning.HEAT_TOTAL_MAX);
    }
}
