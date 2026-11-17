// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;

/**
 * The internal disks of every Computer, keyed by a UUID the block entity (and the item it drops as) carries and nothing else, so a
 * Computer's files never ride in a chunk or an item and cannot book-ban a player or a region. Saved with the overworld.
 * <p>
 * A UUID is allocated lazily, on a Computer's first use after placement. A loaded Computer claims its UUID in an in-memory map
 * ({@link #claim}); a UUID already claimed by another loaded Computer (a creative pick, a copied block entity, a cloned item) makes the
 * later one fork to a new UUID with a copy of the files, never an alias. A placed store whose chunk is loaded and which no Computer has
 * claimed for a while is deleted: its block is gone however that happened (explosion, {@code /setblock}, a piston). A carried store (the
 * Computer is an item somewhere nothing can see) is never deleted automatically; only {@code /feedback computers} removes it.
 * <p>
 * Design from CC: Tweaked (MPL-2.0, design only): an id-keyed external store with only the id in the block, the id carried on the item,
 * lazy allocation, one shared media abstraction. Rejected there: an integer counter and {@code ids.json}, directory-backed mounts, byte
 * and config capacity, alias-on-duplicate ids, Lua filesystem mounting and the id in tooltips. See {@code THIRD-PARTY-LICENSES.md}.
 */
public final class ComputerStore extends SavedData {

    private static final String NAME = "feedback_computers", ENTRIES = "Entries";

    /** One Computer's internal disk and where it was last seen. */
    public static final class Entry {
        private final List<ComputerFile> files = new ArrayList<>();
        @Nullable
        ResourceKey<Level> dimension;
        long chunk;
        boolean carried;
        long carriedSince, lastSeen;

        public List<ComputerFile> files() {
            return files;
        }

        public boolean carried() {
            return carried;
        }

        public long lastSeen() {
            return lastSeen;
        }

        public int bytes() {
            int total = 0;
            for (ComputerFile f : files)
                total += f.bytes();
            return total;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    /** The Computers loaded now, by disk id. Never saved. */
    private static final Map<UUID, ComputerBlockEntity> CLAIMED = new HashMap<>();

    public static ComputerStore get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ComputerStore::new, ComputerStore::load), NAME);
    }

    /** A fresh disk. */
    public UUID allocate() {
        UUID id = UUID.randomUUID();
        entries.put(id, new Entry());
        setDirty();
        return id;
    }

    @Nullable
    public Entry find(UUID id) {
        return entries.get(id);
    }

    /** A new disk with a copy of {@code from}'s files. */
    public UUID fork(UUID from) {
        UUID id = allocate();
        Entry source = entries.get(from);
        if (source != null)
            for (ComputerFile f : source.files)
                entries.get(id).files.add(new ComputerFile(f.kind(), f.name(), f.data().copy()));
        return id;
    }

    /** Whether {@code file} can be added to {@code id}'s internal disk: room, and the per-file cap. */
    public boolean put(UUID id, ComputerFile file) {
        Entry entry = entries.get(id);
        if (entry == null || entry.files.size() >= ControlTuning.INTERNAL_DISK_FILES || !file.fits())
            return false;
        entry.files.add(file);
        setDirty();
        return true;
    }

    public boolean replace(UUID id, int index, ComputerFile file) {
        Entry entry = entries.get(id);
        if (entry == null || index < 0 || index >= entry.files.size() || !file.fits())
            return false;
        entry.files.set(index, file);
        setDirty();
        return true;
    }

    public boolean remove(UUID id, int index) {
        Entry entry = entries.get(id);
        if (entry == null || index < 0 || index >= entry.files.size())
            return false;
        entry.files.remove(index);
        setDirty();
        return true;
    }

    // --- claiming ------------------------------------------------------------------------------------------------------------

    /**
     * A Computer that has loaded claims {@code id} at its position. Returns the id it should use: its own, or, when another loaded
     * Computer already holds it, a fork.
     */
    public UUID claim(UUID id, ComputerBlockEntity computer) {
        ComputerBlockEntity holder = CLAIMED.get(id);
        if (holder != null && holder != computer && !holder.isRemoved())
            id = fork(id);
        Entry entry = entries.computeIfAbsent(id, k -> new Entry());
        CLAIMED.put(id, computer);
        entry.carried = false;
        touch(entry, computer);
        setDirty();
        return id;
    }

    /** The Computer unloaded or was removed: it no longer holds its id. */
    public static void release(UUID id, ComputerBlockEntity computer) {
        CLAIMED.remove(id, computer);
    }

    /** The server stopped: no Computer is loaded any more. */
    public static void clearClaims() {
        CLAIMED.clear();
    }

    /** The Computer was broken and its id went with the item it dropped as: nothing in the world holds the disk now. */
    public void carry(UUID id, long now) {
        Entry entry = entries.get(id);
        if (entry == null)
            return;
        entry.carried = true;
        entry.carriedSince = now;
        entry.dimension = null;
        setDirty();
    }

    /** Records where a loaded Computer is, and that it is there now. */
    public void touch(UUID id, ComputerBlockEntity computer) {
        Entry entry = entries.get(id);
        if (entry != null)
            touch(entry, computer);
    }

    private void touch(Entry entry, ComputerBlockEntity computer) {
        Level level = computer.getLevel();
        if (level == null)
            return;
        ResourceKey<Level> dim = level.dimension();
        long chunk = new ChunkPos(computer.getBlockPos()).toLong();
        if (entry.dimension != dim || entry.chunk != chunk || level.getGameTime() - entry.lastSeen > 100)
            setDirty();
        entry.dimension = dim;
        entry.chunk = chunk;
        entry.lastSeen = level.getGameTime();
    }

    /**
     * Deletes every placed store whose chunk is loaded, which no Computer holds and which none has touched for {@link
     * ControlTuning#COMPUTER_ORPHAN_TICKS}: the block is gone. A carried store is never touched here. Called every few seconds.
     */
    public int sweep(MinecraftServer server) {
        int removed = 0;
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entry entry = e.getValue();
            if (entry.carried || entry.dimension == null || CLAIMED.containsKey(e.getKey()))
                continue;
            ServerLevel level = server.getLevel(entry.dimension);
            if (level == null || !level.hasChunk(ChunkPos.getX(entry.chunk), ChunkPos.getZ(entry.chunk)))
                continue;
            if (level.getGameTime() - entry.lastSeen < ControlTuning.COMPUTER_ORPHAN_TICKS)
                continue;
            it.remove();
            removed++;
        }
        if (removed > 0)
            setDirty();
        return removed;
    }

    // --- for the op command ----------------------------------------------------------------------------------------------------

    public Map<UUID, Entry> all() {
        return java.util.Collections.unmodifiableMap(entries);
    }

    public boolean isClaimed(UUID id) {
        return CLAIMED.containsKey(id);
    }

    public boolean purge(UUID id) {
        boolean had = entries.remove(id) != null;
        if (had)
            setDirty();
        return had;
    }

    // --- persistence --------------------------------------------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        entries.forEach((id, entry) -> {
            CompoundTag one = new CompoundTag();
            one.putUUID("Id", id);
            ListTag files = new ListTag();
            for (ComputerFile f : entry.files)
                files.add(f.save());
            one.put("Files", files);
            if (entry.dimension != null) {
                one.putString("Dim", entry.dimension.location().toString());
                one.putLong("Chunk", entry.chunk);
            }
            one.putBoolean("Carried", entry.carried);
            one.putLong("CarriedSince", entry.carriedSince);
            one.putLong("LastSeen", entry.lastSeen);
            list.add(one);
        });
        tag.put(ENTRIES, list);
        return tag;
    }

    private static ComputerStore load(CompoundTag tag, HolderLookup.Provider registries) {
        ComputerStore store = new ComputerStore();
        for (Tag t : tag.getList(ENTRIES, Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag) t;
            Entry entry = new Entry();
            for (Tag f : one.getList("Files", Tag.TAG_COMPOUND)) {
                ComputerFile file = ComputerFile.load((CompoundTag) f, registries);
                if (file != null)
                    entry.files.add(file);
            }
            if (one.contains("Dim")) {
                ResourceLocation dim = ResourceLocation.tryParse(one.getString("Dim"));
                if (dim != null) {
                    entry.dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dim);
                    entry.chunk = one.getLong("Chunk");
                }
            }
            entry.carried = one.getBoolean("Carried");
            entry.carriedSince = one.getLong("CarriedSince");
            entry.lastSeen = one.getLong("LastSeen");
            store.entries.put(one.getUUID("Id"), entry);
        }
        return store;
    }
}
