// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Which placed blocks wear a glaze (a coat of the taint-proofing glaze): a block position and the id of the block that stood there when it was
 * glazed. A glaze is a coat on that block and not on the place, so it counts only while the same block is still there; a replaced block is bare
 * and its stale entry is dropped the next time anyone asks. One saved record per dimension.
 */
public class Coats extends SavedData {

    private final Map<Long, String> coated = new HashMap<>();

    public static Coats get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(Coats::new, Coats::load), "feedback_coats");
    }

    /** Whether the block at {@code pos} wears a glaze. */
    public boolean has(BlockPos pos, BlockState state) {
        String id = coated.get(pos.asLong());
        if (id == null)
            return false;
        if (id.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))
            return true;
        coated.remove(pos.asLong());
        setDirty();
        return false;
    }

    /** Glazes the block; false when it already wears one. */
    public boolean coat(BlockPos pos, BlockState state) {
        if (has(pos, state))
            return false;
        coated.put(pos.asLong(), BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        coated.forEach((pos, id) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("Pos", pos);
            e.putString("Block", id);
            list.add(e);
        });
        tag.put("Coats", list);
        return tag;
    }

    private static Coats load(CompoundTag tag, HolderLookup.Provider registries) {
        Coats out = new Coats();
        for (Tag t : tag.getList("Coats", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            out.coated.put(e.getLong("Pos"), e.getString("Block"));
        }
        return out;
    }
}
