// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.Map;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Takes the ore out of vanilla's noise-carved iron and copper veins as a chunk generates. Ore exists only in Feedback's own veins, and the
 * noise router makes these two with no placed feature to remove, so the ore and raw-ore blocks of the vein are turned into the vein's own
 * filler (copper's granite, iron's tuff); the filler rock stays. Only a newly generated chunk is touched, so a block a player placed is never swapped.
 */
public final class VanillaVeinStrip {

    private static final Map<Block, Block> STRIP = Map.of(
            Blocks.COPPER_ORE, Blocks.GRANITE,
            Blocks.RAW_COPPER_BLOCK, Blocks.GRANITE,
            Blocks.DEEPSLATE_IRON_ORE, Blocks.TUFF,
            Blocks.RAW_IRON_BLOCK, Blocks.TUFF);

    private VanillaVeinStrip() {}

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getChunk() instanceof LevelChunk chunk))
            return;
        for (int i = 0; i < chunk.getSectionsCount(); i++) {
            var section = chunk.getSection(i);
            if (section.hasOnlyAir() || !section.maybeHas(state -> STRIP.containsKey(state.getBlock())))
                continue;
            boolean changed = false;
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++) {
                        Block filler = STRIP.get(section.getBlockState(x, y, z).getBlock());
                        if (filler != null) {
                            // Straight into the section: chunk.setBlockState here (inside the load event, before the chunk is
                            // registered) pokes the light engine and heightmaps, and can stall the chunk promotion. Ore and
                            // filler are both opaque full blocks, so nothing they feed needs an update.
                            section.setBlockState(x, y, z, filler.defaultBlockState(), false);
                            changed = true;
                        }
                    }
            if (changed)
                chunk.setUnsaved(true);
        }
    }
}
