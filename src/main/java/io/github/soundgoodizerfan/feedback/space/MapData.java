// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.companion.SableCompanion;
import io.github.soundgoodizerfan.feedback.Feedback;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * What the Overworld looks like from above, one map colour a chunk, for the Shelf's view below the craft. A chunk is sampled once,
 * the first time it loads (sixteen columns on a 4 by 4 grid, the commonest map colour wins), so ground nobody has been near stays
 * unknown and explored land shows. A byte a chunk: the colour id times 4, plus the shade; 0 is unknown. Saved with the Overworld.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class MapData extends SavedData {
    private static final String NAME = "feedback_shelf_map";

    private final Long2ByteOpenHashMap colours = new Long2ByteOpenHashMap();

    public static MapData of(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MapData::new, MapData::load, null), NAME);
    }

    /** The packed colour recorded for chunk ({@code cx}, {@code cz}); 0 where none is. */
    public byte at(int cx, int cz) {
        return colours.get(ChunkPos.asLong(cx, cz));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
                || !(event.getChunk() instanceof LevelChunk chunk))
            return;
        ChunkPos pos = chunk.getPos();
        if (SableCompanion.INSTANCE.getContaining(level, pos) != null) // a body's plot, not ground
            return;
        MapData data = of(level);
        long key = pos.toLong();
        if (data.colours.containsKey(key))
            return;
        int[] count = new int[64];
        var at = new BlockPos.MutableBlockPos();
        for (int x = 2; x < 16; x += 4)
            for (int z = 2; z < 16; z += 4) {
                int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                at.set(pos.getMinBlockX() + x, Math.max(level.getMinBuildHeight(), y - 1), pos.getMinBlockZ() + z);
                count[chunk.getBlockState(at).getMapColor(level, at).id]++;
            }
        int best = 0;
        for (int i = 1; i < count.length; i++)
            if (count[i] > count[best])
                best = i;
        if (best == 0)
            return;
        data.colours.put(key, (byte) (best * 4 + 1));
        data.setDirty();
    }

    /** A {@code size} by {@code size} square of chunks from ({@code x0}, {@code z0}), one packed colour each. */
    public byte[] tile(int x0, int z0, int size) {
        byte[] out = new byte[size * size];
        for (int z = 0; z < size; z++)
            for (int x = 0; x < size; x++)
                out[z * size + x] = at(x0 + x, z0 + z);
        return out;
    }

    /** The ARGB colour of a packed byte; the dark neutral of unexplored ground for 0. */
    public static int argb(byte packed) {
        int v = packed & 0xFF;
        if (v == 0)
            return 0xFF1A1A1E;
        return 0xFF000000 | MapColor.byId(v >> 2).calculateRGBColor(MapColor.Brightness.byId(v & 3));
    }

    // --- saving ---

    private static MapData load(CompoundTag tag, HolderLookup.Provider registries) {
        MapData d = new MapData();
        long[] keys = tag.getLongArray("Chunks");
        byte[] vals = tag.getByteArray("Colours");
        for (int i = 0; i < Math.min(keys.length, vals.length); i++)
            d.colours.put(keys[i], vals[i]);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] keys = new long[colours.size()];
        byte[] vals = new byte[colours.size()];
        int i = 0;
        for (var e : colours.long2ByteEntrySet()) {
            keys[i] = e.getLongKey();
            vals[i++] = e.getByteValue();
        }
        tag.put("Chunks", new LongArrayTag(keys));
        tag.put("Colours", new ByteArrayTag(vals));
        return tag;
    }
}
