// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A cabin is the open volume inside a Sable body that its hull closes. {@link #scan} floods the cells with nothing to
 * collide with from a starting cell, within the plot's box: a face that reaches the edge of the box is a hole to space,
 * and a volume that grows past {@link SpaceTuning#CABIN_CELL_CAP} cells is called open. A sealed cabin holds air
 * (mB, {@link SpaceTuning#CABIN_AIR_MB_PER_CELL} a cell); a person in it breathes it down, a hole bleeds it by its size against a
 * vacuum, and an Air Valve feeds it from a pipe. A person in thin air with air in their cabin breathes
 * as at sea level. Once a second, for people in thin air only.
 * shortcut: the air lives in memory, keyed by body and the cabin's smallest cell, so a restart refills every cabin and a
 * cabin that nobody is in does not leak; upgrade to saved data when cabins matter between sessions.
 */
public final class Cabin {
    /** One cabin: the body and its smallest cell, which names it while its walls do not move. */
    public record Key(UUID body, long anchor) {
    }

    /** What a scan found: the cabin, its size in cells, and the faces open to space. */
    public record Region(Key key, int cells, int holes) {
    }

    private record Seen(long tick, UUID body, @Nullable Region region) {
    }

    private static final Map<Key, float[]> AIR = new HashMap<>();
    private static final Map<UUID, Seen> SEEN = new HashMap<>();

    private Cabin() {
    }

    /** The cabin around plot cell {@code start} of {@code body}, or null if that cell is solid. */
    @Nullable
    public static Region scan(ServerLevel level, ServerSubLevel body, BlockPos start) {
        if (!open(level, start))
            return null;
        var box = body.getPlot().getBoundingBox();
        Set<Long> seen = new HashSet<>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(start);
        seen.add(start.asLong());
        long anchor = start.asLong();
        int holes = 0;
        while (!queue.isEmpty()) {
            BlockPos at = queue.poll();
            anchor = Math.min(anchor, at.asLong());
            if (seen.size() > SpaceTuning.CABIN_CELL_CAP)
                return new Region(new Key(body.getUniqueId(), anchor), SpaceTuning.CABIN_CELL_CAP, SpaceTuning.CABIN_CELL_CAP);
            for (var d : net.minecraft.core.Direction.values()) {
                BlockPos n = at.relative(d);
                if (n.getX() < box.minX() || n.getX() > box.maxX() || n.getY() < box.minY() || n.getY() > box.maxY()
                        || n.getZ() < box.minZ() || n.getZ() > box.maxZ()) {
                    holes++;
                    continue;
                }
                if (open(level, n) && seen.add(n.asLong()))
                    queue.add(n);
            }
        }
        return new Region(new Key(body.getUniqueId(), anchor), seen.size(), holes);
    }

    /** Nothing here to collide with: air to breathe, a doorway. A closed door, glass or a plate walls it. */
    private static boolean open(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /** Puts up to {@code mb} into the cabin; returns what it took (a cabin holds {@link SpaceTuning#CABIN_AIR_MB_PER_CELL} a cell). */
    public static int add(Region region, int mb) {
        float cap = region.cells() * SpaceTuning.CABIN_AIR_MB_PER_CELL;
        float[] air = AIR.computeIfAbsent(region.key(), k -> new float[] {cap});
        int taken = (int) Math.max(0, Math.min(mb, cap - air[0]));
        air[0] += taken;
        return taken;
    }

    /**
     * Whether {@code p} breathes the cabin they are in, this tick: spends the air they use and what a hole loses.
     * Called from {@link Breath} while the air outside is too thin.
     */
    public static boolean breathes(ServerPlayer p, float outside) {
        Seen seen = SEEN.get(p.getUUID());
        if (seen == null || p.tickCount - seen.tick() >= SpaceTuning.CABIN_RESCAN_TICKS) {
            seen = rescan(p);
            SEEN.put(p.getUUID(), seen);
        }
        Region r = seen.region();
        if (r == null)
            return false;
        float cap = r.cells() * SpaceTuning.CABIN_AIR_MB_PER_CELL;
        // A cabin first seen sealed is full; one first seen open has already lost its air to what is outside.
        float[] air = AIR.computeIfAbsent(r.key(), k -> new float[] {r.holes() == 0 ? cap : cap * outside});
        float density = AirDensityTable.at(p.level());
        if (r.holes() > 0) {
            air[0] -= r.holes() * SpaceTuning.CABIN_LEAK_MB_PER_HOLE_TICK * (1f - density);
            if (p.tickCount % 20 == 0)
                // shortcut: a vanilla hiss stands in until the add-sound skill brings a leak sample.
                p.level().playSound(null, p.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.6f);
        }
        air[0] -= SpaceTuning.CABIN_USE_MB_PER_TICK;
        if (air[0] <= 0f) {
            air[0] = 0f;
            return false;
        }
        return true;
    }

    private static Seen rescan(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)
                || !(SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(p) instanceof ServerSubLevel body))
            return new Seen(p.tickCount, p.getUUID(), null);
        Vec3 plot = body.logicalPose().transformPositionInverse(p.getEyePosition());
        return new Seen(p.tickCount, body.getUniqueId(), scan(level, body, BlockPos.containing(plot)));
    }
}
