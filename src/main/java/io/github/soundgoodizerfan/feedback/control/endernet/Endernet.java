// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.endernet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.Level;

/**
 * Endernet, layer 1: entangled pairs. Two ends bonded under one pair id (the same id the ender ports use; a pair is made by crafting) are
 * one data link across any distance: a data node linked to one end can be read from the other, through {@code DataWires.reach}, which
 * treats a live pair as a hop of the best grade. It is the one wireless exception to "data is never wireless".
 *
 * <p>An end is a {@link DataNode} that is also an {@link End}: a Data Port block, or an Electric Controller or Computer with an endernet
 * upgrade fitted. A pair carries only while both ends are loaded (an unloaded end is simply absent from the live registry, so the pair
 * pauses, as an ender port's does; an ender anchor on a Data Port is the answer), both are powered (each pays standing power), and the
 * crossing is paid for: one end of the pair must be a Data Port, whose tank of teleportant and per-minute bandwidth pay each signal
 * ({@link End#pay}). A pair with no port at either end carries nothing. Both ends must be in one dimension. Nothing is destroyed and
 * nothing refused with a message: a dry or saturated pair is dark until it recovers.
 *
 * <p>Pair ids, never names, bind the ends. Layer 2 is the channel bus ({@link EndernetBand}: tuned channel numbers, one shared bandwidth cap per
 * channel, signal slots a Data Port writes and hears) and layer 3 the addressable packets ({@link Packets}: messages and files between Computers).
 */
public final class Endernet {

    /** One end of a pair. */
    public interface End extends DataNode {

        /** The pair this end is bonded under, or null for none. */
        @Nullable
        UUID pairId();

        /** Whether this end is loaded and powered, ready to carry. */
        boolean live();

        /**
         * Pays for one signal crossing to {@code other}, {@code blocks} away: teleportant from this end's tank and one unit of its
         * bandwidth. Ends with no tank (a host's upgrade) never pay.
         */
        default boolean pay(float blocks) {
            return false;
        }
    }

    /** Ends whose chunk is loaded, by pair id: an unloaded end is absent. */
    private static final Map<UUID, Set<End>> LIVE = new HashMap<>();
    private static final Map<End, UUID> JOINED = new WeakHashMap<>();

    private Endernet() {
    }

    /** Registers {@code end} under its pair id as it stands now, moving it if the id changed; call each tick or when it loads. */
    public static void sync(End end) {
        UUID want = end.pairId();
        UUID have = JOINED.get(end);
        if (java.util.Objects.equals(want, have))
            return;
        leave(end);
        if (want != null) {
            LIVE.computeIfAbsent(want, k -> new HashSet<>()).add(end);
            JOINED.put(end, want);
        }
    }

    /** Takes {@code end} out of the registry: its chunk unloaded or it was removed. */
    public static void leave(End end) {
        UUID have = JOINED.remove(end);
        if (have != null && LIVE.containsKey(have)) {
            LIVE.get(have).remove(end);
            if (LIVE.get(have).isEmpty())
                LIVE.remove(have);
        }
    }

    /** Every registered end in {@code level} (the packet layer's address book); a snapshot. */
    public static List<End> live(Level level) {
        List<End> out = new ArrayList<>();
        for (Set<End> ends : LIVE.values())
            for (End e : ends)
                if (e.getNodeLevel() == level && !e.getNodeOwner().isRemoved())
                    out.add(e);
        return out;
    }

    /** The other live end of {@code end}'s pair in the same level, or empty. */
    public static Optional<End> partner(End end) {
        UUID pair = end.pairId();
        if (pair == null || !end.live())
            return Optional.empty();
        for (End other : LIVE.getOrDefault(pair, Set.of()))
            if (other != end && other.live() && other.getNodeLevel() == end.getNodeLevel() && !other.getNodeOwner().isRemoved())
                return Optional.of(other);
        return Optional.empty();
    }

    /** A pair crossing a path search would use. */
    public record Crossing(End from, End to) {
        /** Pays for one signal over it: one of the two ends must be a Data Port that can. */
        public boolean pay() {
            float blocks = (float) Math.sqrt(from.getNodePos().distSqr(to.getNodePos()));
            return from.pay(blocks) || to.pay(blocks);
        }
    }

    /** The crossing {@code node} offers, if it is an end whose pair has a live partner in this level; nothing is paid yet. */
    @Nullable
    public static Crossing peek(DataNode node) {
        if (!(node instanceof End end))
            return null;
        End other = partner(end).orElse(null);
        return other == null ? null : new Crossing(end, other);
    }

    /** What the Network app shows of one node a pair reaches. */
    public record Entry(DataNodeRef ref, String blockKey, Kind kind, boolean switchable, boolean engaged) {
        public enum Kind { MACHINE, COMPUTER, ROBOT, PORT }
    }

    /**
     * The nodes on the far side of {@code from}'s pair: a breadth-first walk of data links from the partner end, up to
     * {@code ControlTuning.NETWORK_MAX_HOPS} hops and {@code NETWORK_MAX_ENTRIES} entries. Empty when the pair is not carrying. Server only.
     */
    public static List<Entry> reachable(End from) {
        List<Entry> out = new ArrayList<>();
        End far = partner(from).orElse(null);
        Level level = from.getNodeLevel();
        if (far == null || level == null)
            return out;
        Set<DataNodeRef> seen = new HashSet<>();
        ArrayDeque<DataNodeRef> frontier = new ArrayDeque<>();
        frontier.add(far.getNodeRef());
        seen.add(far.getNodeRef());
        seen.add(from.getNodeRef());
        for (int hop = 0; hop < ControlTuning.NETWORK_MAX_HOPS && !frontier.isEmpty(); hop++) {
            ArrayDeque<DataNodeRef> next = new ArrayDeque<>();
            for (DataNodeRef at : frontier) {
                DataNode node = DataNode.resolveOrNull(level, at);
                if (node == null)
                    continue;
                if (node != far)
                    out.add(entry(level, at, node));
                if (out.size() >= ControlTuning.NETWORK_MAX_ENTRIES)
                    return out;
                for (DataNodeRef other : Set.copyOf(node.getNodeLinks()))
                    if (seen.add(other))
                        next.add(other);
            }
            frontier = next;
        }
        return out;
    }

    private static Entry entry(Level level, DataNodeRef ref, DataNode node) {
        var owner = node.getNodeOwner();
        boolean leashed = node instanceof io.github.soundgoodizerfan.feedback.machine.ender.DataPortBlockEntity p && p.hasLeash();
        Entry.Kind kind = leashed ? Entry.Kind.ROBOT : node instanceof End ? Entry.Kind.PORT
                : node instanceof io.github.soundgoodizerfan.feedback.control.controller.ControllerHost ? Entry.Kind.COMPUTER
                : owner instanceof io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity ? Entry.Kind.ROBOT : Entry.Kind.MACHINE;
        // a Data Port is a switch only as a robot's base: otherwise it moves nothing
        boolean switchable = node instanceof io.github.soundgoodizerfan.feedback.control.Switchable && (!(node instanceof End) || leashed);
        boolean engaged = node instanceof io.github.soundgoodizerfan.feedback.control.Switchable sw && sw.isEngaged();
        return new Entry(ref, owner.getBlockState().getBlock().getDescriptionId(), kind, switchable, engaged);
    }

    /**
     * Flips the switch at {@code ref} for a Computer whose pair reaches it: server-side only, and only if {@code ref} is among what
     * {@link #reachable} gives now and is a {@code Switchable} (a switch, never a dial). Returns whether anything was set.
     */
    public static boolean flip(End from, DataNodeRef ref) {
        Level level = from.getNodeLevel();
        if (level == null || reachable(from).stream().noneMatch(e -> e.ref().equals(ref)))
            return false;
        if (!(DataNode.resolveOrNull(level, ref) instanceof io.github.soundgoodizerfan.feedback.control.Switchable sw))
            return false;
        sw.setEngaged(!sw.isEngaged());
        return true;
    }
}
