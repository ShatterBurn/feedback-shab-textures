// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

/** Gives a player the links of a chunk the moment it starts to watch it. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DataLinkEvents {

    private DataLinkEvents() {
    }

    @SubscribeEvent
    private static void onChunkSent(ChunkWatchEvent.Sent event) {
        if (event.getLevel() instanceof ServerLevel level)
            DataLinkStore.sendChunk(event.getPlayer(), level, event.getPos());
    }
}
