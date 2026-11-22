// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The air each cabin holds (mB, by {@link Cabin.Key}), kept with the world so a restart does not refill every cabin. One store a server,
 * in the overworld's data storage. A cabin's entry is kept as long as its body and smallest cell name it.
 * shortcut: entries of bodies that no longer exist are never swept (a few bytes each); sweep when bodies are removed if the file grows.
 */
public final class CabinAir extends SavedData {

    private static final String NAME = "feedback_cabin_air";

    private final Map<Cabin.Key, float[]> air = new HashMap<>();

    private static final SavedData.Factory<CabinAir> FACTORY = new SavedData.Factory<>(CabinAir::new, CabinAir::load, null);

    public static CabinAir of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** The cabin's air cell, made with {@code start} mB if it was not there; changes to it are marked with {@link #changed}. */
    public float[] cell(Cabin.Key key, float start) {
        return air.computeIfAbsent(key, k -> {
            setDirty();
            return new float[] {start};
        });
    }

    /** The air in a cell this store gave out changed: it is saved with the world. */
    public void changed() {
        setDirty();
    }

    private static CabinAir load(CompoundTag tag, HolderLookup.Provider registries) {
        CabinAir out = new CabinAir();
        ListTag list = tag.getList("Cabins", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag one = list.getCompound(i);
            out.air.put(new Cabin.Key(one.getUUID("Body"), one.getLong("Anchor")), new float[] {one.getFloat("Air")});
        }
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Cabin.Key, float[]> e : air.entrySet()) {
            CompoundTag one = new CompoundTag();
            UUID body = e.getKey().body();
            one.putUUID("Body", body);
            one.putLong("Anchor", e.getKey().anchor());
            one.putFloat("Air", e.getValue()[0]);
            list.add(one);
        }
        tag.put("Cabins", list);
        return tag;
    }
}
