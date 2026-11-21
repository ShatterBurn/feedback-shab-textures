// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;

import org.jetbrains.annotations.Nullable;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Finds the connected run a node belongs to and hands it to the resolver. The
 * {@code RotationPropagator} pattern: when topology changes, flood the whole run once and
 * rebuild it; never patch one.
 *
 * <h2>Why a rebuild and not an incremental update</h2>
 * Same argument as the rotation network, and it is stronger here: a run's cached routing tree is
 * only as good as the graph it was built from, and keeping a tree correct across a wire being
 * broken in its middle is exactly the accumulated-ownership bug a rebuild cannot have. A flood
 * fill visits each node once and cannot loop, however the player wires a ring.
 *
 * <h2>Who is adjacent to whom</h2>
 * Touching blocks are connected unless they are the same non-wire role: a wire joins anything, a
 * source can feed a sink it touches directly, but two sinks or two sources touching conduct
 * nothing between them. Nothing is sided -- a wire connects on all six faces -- because routing a
 * cable round a corner is the busywork philosophy 3 cuts.
 *
 * <p>One exception reaches further: a bare wire also joins a bare wire one air block away along a
 * straight line (an arc, 6.4). Only a straight hop over air, so only the two blocks either side of
 * the gap ever need to rebuild when it starts or stops.
 */
public final class ElectricPropagator {

    private ElectricPropagator() {
    }

    /**
     * Queued, not rebuilt at once, for the same reason as the rotation propagator: a chunk
     * loading a thousand-wire run calls this a thousand times, and rebuilding each time floods
     * the whole run every call.
     */
    static void onAdded(Level level, BlockPos pos) {
        ElectricRuns.of(level).queueAdded(pos);
    }

    /** Rebuild the run at {@code pos} at the start of the next tick, for a change that is not a block coming or going. */
    public static void requeue(Level level, BlockPos pos) {
        onAdded(level, pos);
    }

    static void flushAdded(Level level, Set<BlockPos> added) {
        Set<ElectricNode> covered = new HashSet<>();
        for (BlockPos pos : added)
            for (ElectricNode node : nodesAt(level, pos))
                if (!covered.contains(node))
                    covered.addAll(rebuild(level, node));
    }

    static void onRemoved(Level level, BlockPos pos, ElectricNode removed) {
        ElectricRuns runs = ElectricRuns.of(level);
        ElectricRun run = removed.getRun();
        // Who the node touched is read off its run (arcs and finespace voxels included), before the run is discarded.
        List<ElectricMember> around = run == null ? List.of() : run.neighboursOf(removed);
        if (run != null)
            runs.discard(run);
        removed.setRun(null);
        // The run may have been cut in two: rebuild from each neighbour, skipping any that an
        // earlier rebuild already covered.
        Set<ElectricNode> covered = new HashSet<>();
        for (ElectricMember member : around)
            if (member instanceof ElectricNode node && node != removed && !node.isGone() && !covered.contains(node))
                covered.addAll(rebuild(level, node));
    }

    /** Re-resolve the run at {@code pos} (each node a finespace cell hosts there too). For tests and for anything that changes a node's role. */
    public static void rebuildFrom(Level level, BlockPos pos) {
        Set<ElectricNode> covered = new HashSet<>();
        for (ElectricNode node : nodesAt(level, pos))
            if (!covered.contains(node))
                covered.addAll(rebuild(level, node));
    }

    private static List<ElectricNode> rebuild(Level level, ElectricNode start) {
        // Flood fill, keeping an index per node so adjacency is built in the same pass.
        List<ElectricNode> found = new ArrayList<>();
        Map<ElectricNode, Integer> index = new HashMap<>();
        Deque<ElectricNode> queue = new ArrayDeque<>();
        queue.add(start);
        index.put(start, 0);
        found.add(start);
        List<List<Integer>> edges = new ArrayList<>();
        edges.add(new ArrayList<>());
        List<ElectricNode> touching = new ArrayList<>();

        while (!queue.isEmpty()) {
            ElectricNode current = queue.poll();
            int ci = index.get(current);
            for (Direction face : Direction.values()) {
                touching.clear();
                neighbours(level, current, face, touching);
                if (touching.isEmpty()) {
                    ElectricNode far = arcAcross(level, current, face);
                    if (far != null)
                        touching.add(far);
                }
                for (ElectricNode other : touching) {
                    if (!connects(current, other))
                        continue;
                    Integer oi = index.get(other);
                    if (oi == null) {
                        oi = found.size();
                        index.put(other, oi);
                        found.add(other);
                        edges.add(new ArrayList<>());
                        queue.add(other);
                    }
                    edges.get(ci).add(oi);
                }
            }
        }

        int[][] adjacent = new int[found.size()][];
        for (int i = 0; i < adjacent.length; i++)
            adjacent[i] = edges.get(i).stream().mapToInt(Integer::intValue).toArray();

        ElectricRuns runs = ElectricRuns.of(level);
        for (ElectricNode node : found)
            if (node.getRun() != null)
                runs.discard(node.getRun());
        ElectricRun run = new ElectricRun(found, adjacent);
        runs.add(run);
        for (ElectricNode node : found)
            node.setRun(run);
        return found;
    }

    /**
     * A member started or stopped conducting: rebuild its own run and every neighbour's, since
     * opening splits one run into two and closing joins two into one.
     */
    public static void reconnect(Level level, BlockPos pos) {
        Set<ElectricNode> covered = new HashSet<>();
        List<ElectricNode> touching = new ArrayList<>();
        for (ElectricNode node : nodesAt(level, pos)) {
            if (!covered.contains(node))
                covered.addAll(rebuild(level, node));
            touching.clear();
            for (Direction face : Direction.values())
                neighbours(level, node, face, touching);
            for (ElectricNode other : touching)
                if (!covered.contains(other))
                    covered.addAll(rebuild(level, other));
        }
    }

    /** The bare wire a bare {@code from} arcs to over an air gap in direction {@code face}, if any (whole blocks only). */
    @Nullable
    private static ElectricNode arcAcross(Level level, ElectricNode from, Direction face) {
        if (level.getBlockEntity(from.getBlockPos()) instanceof MicroCellBlockEntity cell)
            return voxelArc(level, cell, from, face);
        if (!from.bare() || level.getBlockEntity(from.getBlockPos()) != from)
            return null;
        BlockPos gap = from.getBlockPos().relative(face);
        if (!level.isLoaded(gap) || !level.getBlockState(gap).isAir())
            return null;
        BlockPos beyond = gap.relative(face);
        ElectricNode far = level.isLoaded(beyond) && level.getBlockEntity(beyond) instanceof ElectricNode n && !n.isGone() ? n : null;
        return far != null && far.bare() ? far : null;
    }

    private static boolean connects(ElectricMember a, ElectricMember b) {
        if (!a.conducting() || !b.conducting() || !a.joins(b) || !b.joins(a))
            return false;
        return a.role() == ElectricMember.Role.WIRE || b.role() == ElectricMember.Role.WIRE
                || a.role() != b.role();
    }

    /**
     * The bare wire a bare finespace wire {@code from} arcs to across {@code face}: straight over exactly one empty voxel (in this cell, the
     * next, or the air of a block with no cell in it), to a bare hosted wire. Never to a whole block's cable, nor to a full voxel.
     */
    @Nullable
    private static ElectricNode voxelArc(Level level, MicroCellBlockEntity cell, ElectricNode from, Direction face) {
        int voxel = cell.voxelOf(from);
        if (voxel < 0 || !from.bare())
            return null;
        int n = MicroCellBlockEntity.N;
        BlockPos pos = from.getBlockPos();
        int x = (voxel & 3) + face.getStepX(), y = (voxel >> 2 & 3) + face.getStepY(), z = (voxel >> 4) + face.getStepZ();
        BlockPos gap = pos.offset(Math.floorDiv(x, n), Math.floorDiv(y, n), Math.floorDiv(z, n));
        int gapVoxel = MicroCellBlockEntity.index(Math.floorMod(x, n), Math.floorMod(y, n), Math.floorMod(z, n));
        x += face.getStepX();
        y += face.getStepY();
        z += face.getStepZ();
        BlockPos beyond = pos.offset(Math.floorDiv(x, n), Math.floorDiv(y, n), Math.floorDiv(z, n));
        if (!level.isLoaded(gap) || !level.isLoaded(beyond))
            return null;
        boolean empty = level.getBlockEntity(gap) instanceof MicroCellBlockEntity between ? between.get(gapVoxel) == null : level.getBlockState(gap).isAir();
        if (!empty || !(level.getBlockEntity(beyond) instanceof MicroCellBlockEntity far))
            return null;
        return far.hosted(MicroCellBlockEntity.index(Math.floorMod(x, n), Math.floorMod(y, n), Math.floorMod(z, n))) instanceof ElectricNode node
                && node.bare() && !node.isGone() ? node : null;
    }

    /** Every live node in the block at {@code pos}: the block entity itself, or each machine a finespace cell hosts. */
    private static List<ElectricNode> nodesAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return List.of();
        BlockEntity be = level.getBlockEntity(pos);
        List<ElectricNode> out = new ArrayList<>();
        if (be instanceof MicroCellBlockEntity cell)
            for (ElectricNode node : cell.hostedElectrics())
                add(out, node);
        else if (be instanceof ElectricNode node)
            add(out, node);
        return out;
    }

    private static void add(List<ElectricNode> out, ElectricNode node) {
        if (!node.isGone())
            out.add(node);
    }

    /**
     * Adds to {@code out} the nodes touching {@code node} across {@code face}, by voxel: a node in a finespace cell touches the voxel beside
     * it (in the cell, or the matching voxel of the next cell, or the whole block there); a whole block's node touches the voxels on the
     * face of a cell beside it.
     */
    private static void neighbours(Level level, ElectricNode node, Direction face, List<ElectricNode> out) {
        BlockPos pos = node.getBlockPos();
        MicroCellBlockEntity home = level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell ? cell : null;
        int voxel = home == null ? -1 : home.voxelOf(node);
        if (voxel >= 0) {
            int next = MicroCellBlockEntity.beside(voxel, face);
            if (next >= 0) {
                if (home.hosted(next) instanceof ElectricNode other)
                    add(out, other);
                return;
            }
        }
        BlockPos at = pos.relative(face);
        if (!level.isLoaded(at))
            return;
        BlockEntity be = level.getBlockEntity(at);
        if (be instanceof MicroCellBlockEntity cell) {
            if (voxel >= 0) {
                if (cell.hosted(MicroCellBlockEntity.wrapped(voxel, face)) instanceof ElectricNode other)
                    add(out, other);
            } else
                for (int i : MicroCellBlockEntity.plane(face.getOpposite()))
                    if (cell.hosted(i) instanceof ElectricNode other)
                        add(out, other);
        } else if (be instanceof ElectricNode other)
            add(out, other);
    }
}
