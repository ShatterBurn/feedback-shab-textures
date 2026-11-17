// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.net.DataLinksPayload;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Every data link in a dimension, saved with the level and mirrored to the clients watching its chunk. A node
 * never stores its own links: {@link DataNode#getNodeLinks()} reads here, keyed by {@link DataNodeRef}, so a
 * block that is a data node persists and syncs its links without a line of its own. The client copy is only
 * ever read (drawing, the Programmer); the server's is the one mutated.
 */
public final class DataLinkStore extends SavedData {

    private static final String NAME = "feedback_data_links", LINKS = "Links", NODE = "Node", TO = "To", COLORS = "Colors", COLOR = "C";

    private final Map<DataNodeRef, Set<DataNodeRef>> links = new HashMap<>();
    private final Map<Long, Set<DataNodeRef>> byChunk = new HashMap<>();
    /** Link colors: a Controller's ref, then each node bound to it and the color it was given. */
    private final Map<DataNodeRef, Map<DataNodeRef, Integer>> colors = new HashMap<>();

    /** The client's mirror, filled by {@link DataLinksPayload}; one level at a time on a client. */
    private static final DataLinkStore CLIENT = new DataLinkStore();

    /** Bumped on every change anywhere, so a Controller can tell its bindings may be stale. */
    private static volatile int epoch;

    private static final Set<DataNodeRef> NONE = java.util.Collections.emptySet();

    public static int epoch() {
        return epoch;
    }

    private static DataLinkStore of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(DataLinkStore::new, DataLinkStore::load), NAME);
    }

    /** The live link set of {@code ref}: mutate it only on the server, and call {@link #changed} after. */
    public static Set<DataNodeRef> linksOf(Level level, DataNodeRef ref) {
        if (level == null)
            return NONE;
        if (level instanceof ServerLevel server)
            return of(server).links.computeIfAbsent(ref, r -> {
                of(server).byChunk.computeIfAbsent(new ChunkPos(r.pos()).toLong(), c -> new HashSet<>()).add(r);
                return new HashSet<>();
            });
        return CLIENT.links.getOrDefault(ref, NONE);
    }

    /** Marks {@code ref}'s links edited: saved, and sent to the players watching its chunk. */
    public static void changed(Level level, DataNodeRef ref) {
        epoch++;
        if (!(level instanceof ServerLevel server))
            return;
        DataLinkStore store = of(server);
        store.setDirty();
        PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(ref.pos()),
                DataLinksPayload.of(List.of(ref), store.links, store.colors));
    }

    /** Sends a player every link in a chunk it has just started to watch. */
    public static void sendChunk(net.minecraft.server.level.ServerPlayer player, ServerLevel level, ChunkPos chunk) {
        DataLinkStore store = of(level);
        Set<DataNodeRef> refs = store.byChunk.get(chunk.toLong());
        if (refs != null && !refs.isEmpty())
            PacketDistributor.sendToPlayer(player, DataLinksPayload.of(new ArrayList<>(refs), store.links, store.colors));
    }

    /**
     * Blocks were carried to new positions (the Lift): every node and link end at an old position follows it, so a vehicle
     * keeps its wiring when it is lifted or lowered.
     */
    public static void moved(ServerLevel level, Map<net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> moves) {
        DataLinkStore store = of(level);
        Map<DataNodeRef, Set<DataNodeRef>> moved = new HashMap<>();
        boolean any = false;
        for (Map.Entry<DataNodeRef, Set<DataNodeRef>> entry : store.links.entrySet()) {
            DataNodeRef key = follow(entry.getKey(), moves);
            Set<DataNodeRef> to = new HashSet<>();
            for (DataNodeRef other : entry.getValue())
                to.add(follow(other, moves));
            any |= !key.equals(entry.getKey()) || !to.equals(entry.getValue());
            moved.put(key, to);
        }
        Map<DataNodeRef, Map<DataNodeRef, Integer>> recoloured = new HashMap<>();
        for (Map.Entry<DataNodeRef, Map<DataNodeRef, Integer>> entry : store.colors.entrySet()) {
            Map<DataNodeRef, Integer> tints = new HashMap<>();
            entry.getValue().forEach((n, c) -> tints.put(follow(n, moves), c));
            DataNodeRef key = follow(entry.getKey(), moves);
            any |= !key.equals(entry.getKey());
            recoloured.put(key, tints);
        }
        if (!any)
            return;
        store.colors.clear();
        store.colors.putAll(recoloured);
        store.links.clear();
        store.links.putAll(moved);
        store.byChunk.clear();
        for (DataNodeRef ref : moved.keySet())
            store.byChunk.computeIfAbsent(new ChunkPos(ref.pos()).toLong(), c -> new HashSet<>()).add(ref);
        store.setDirty();
        epoch++;
    }

    private static DataNodeRef follow(DataNodeRef ref, Map<net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> moves) {
        net.minecraft.core.BlockPos to = moves.get(ref.pos());
        return to == null ? ref : new DataNodeRef(to, ref.side(), ref.voxel(), ref.local());
    }

    /** The color {@code controller} gave its link to {@code node}, or -1 for none. */
    public static int colorOf(Level level, DataNodeRef controller, DataNodeRef node) {
        if (level == null)
            return -1;
        Map<DataNodeRef, Integer> mine = (level instanceof ServerLevel server ? of(server) : CLIENT).colors.get(controller);
        return mine == null ? -1 : mine.getOrDefault(node, -1);
    }

    /** Sets (or, with -1, clears) the color {@code controller} gives its link to {@code node}; sent to the players watching. */
    public static void setColor(Level level, DataNodeRef controller, DataNodeRef node, int color) {
        if (!(level instanceof ServerLevel server))
            return;
        DataLinkStore store = of(server);
        Map<DataNodeRef, Integer> mine = store.colors.computeIfAbsent(controller, c -> new HashMap<>());
        boolean differs = color < 0 ? mine.remove(node) != null : !Integer.valueOf(color).equals(mine.put(node, color));
        if (mine.isEmpty())
            store.colors.remove(controller);
        if (differs)
            changed(level, controller);
    }

    /** Client: the color some Controller gave its link to {@code node} (for tinting the node), or -1. */
    public static int tintOf(DataNodeRef node) {
        for (Map<DataNodeRef, Integer> tints : CLIENT.colors.values()) {
            Integer color = tints.get(node);
            if (color != null)
                return color;
        }
        return -1;
    }

    /** Client: a node's links as the server last said. */
    public static void receive(DataLinksPayload payload) {
        epoch++;
        for (DataLinksPayload.Entry entry : payload.entries()) {
            if (entry.links().isEmpty())
                CLIENT.links.remove(entry.node());
            else
                CLIENT.links.put(entry.node(), new HashSet<>(entry.links()));
            if (entry.tints().isEmpty())
                CLIENT.colors.remove(entry.node());
            else {
                Map<DataNodeRef, Integer> tints = new HashMap<>();
                entry.tints().forEach(t -> tints.put(t.node(), t.color()));
                CLIENT.colors.put(entry.node(), tints);
            }
        }
    }

    public static void clearClient() {
        CLIENT.links.clear();
        CLIENT.colors.clear();
        epoch++;
    }

    /** Every node that has links in the chunk, for drawing; the client's copy. */
    public static Iterable<Map.Entry<DataNodeRef, Set<DataNodeRef>>> clientEntries() {
        return CLIENT.links.entrySet();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<DataNodeRef, Set<DataNodeRef>> entry : links.entrySet()) {
            if (entry.getValue().isEmpty())
                continue;
            CompoundTag one = new CompoundTag();
            one.put(NODE, entry.getKey().toTag());
            ListTag to = new ListTag();
            for (DataNodeRef other : entry.getValue())
                to.add(other.toTag());
            one.put(TO, to);
            list.add(one);
        }
        tag.put(LINKS, list);
        ListTag tinted = new ListTag();
        for (Map.Entry<DataNodeRef, Map<DataNodeRef, Integer>> entry : colors.entrySet())
            for (Map.Entry<DataNodeRef, Integer> tint : entry.getValue().entrySet()) {
                CompoundTag one = new CompoundTag();
                one.put(NODE, entry.getKey().toTag());
                one.put(TO, tint.getKey().toTag());
                one.putInt(COLOR, tint.getValue());
                tinted.add(one);
            }
        tag.put(COLORS, tinted);
        return tag;
    }

    private static DataLinkStore load(CompoundTag tag, HolderLookup.Provider registries) {
        DataLinkStore store = new DataLinkStore();
        for (Tag t : tag.getList(LINKS, Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) t;
            DataNodeRef node = DataNodeRef.fromTag(one.getCompound(NODE));
            Set<DataNodeRef> to = new HashSet<>();
            for (Tag other : one.getList(TO, Tag.TAG_COMPOUND))
                to.add(DataNodeRef.fromTag((CompoundTag) other));
            store.links.put(node, to);
            store.byChunk.computeIfAbsent(new ChunkPos(node.pos()).toLong(), c -> new HashSet<>()).add(node);
        }
        for (Tag t : tag.getList(COLORS, Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) t;
            store.colors.computeIfAbsent(DataNodeRef.fromTag(one.getCompound(NODE)), c -> new HashMap<>())
                    .put(DataNodeRef.fromTag(one.getCompound(TO)), one.getInt(COLOR));
        }
        return store;
    }
}
