// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

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

    /** Every few ticks a slice of the saved links is checked against the world: a node whose block is gone is forgotten. */
    @SubscribeEvent
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % 20 == 0)
            DataLinkStore.sweep(level);
    }

    /** Every few seconds the internal disks of Computers that are gone are forgotten (control/computer/ComputerStore). */
    @SubscribeEvent
    private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 == 0)
            io.github.soundgoodizerfan.feedback.control.computer.ComputerStore.get(event.getServer()).sweep(event.getServer());
    }

    @SubscribeEvent
    private static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        io.github.soundgoodizerfan.feedback.control.computer.ComputerStore.clearClaims();
    }

    /** The client forgets a chunk's links when the chunk leaves view, and all of them when its level goes. */
    @SubscribeEvent
    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide())
            DataLinkStore.forgetClientChunk(event.getChunk().getPos());
    }

    @SubscribeEvent
    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide())
            DataLinkStore.clearClient();
    }
}
