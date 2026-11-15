// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.util.ArrayList;

/**
 * The chunks robots hold loaded through a chunk-loading part: one ticket per robot, over the chunk it stands in, held only while it runs.
 * All robots together may hold {@link RobotConfig#LOADED_CHUNKS} chunks; a robot past that halts when its chunk unloads like any other.
 * Tickets are not kept across a restart (a robot takes its ticket again when it next runs), so a robot that was lost holds nothing.
 *
 * <p>Prior art: PneumaticCraft: Repressurized's {@code DynamicChunkLoader} (GPL-3.0), design only: an opt-in part, a force ticket that
 * follows the entity from chunk to chunk and is released on its death, and a drain on its power. Rejected: tying it to an owner being online.
 */
public final class RobotTickets {

    private RobotTickets() {
    }

    private static int held;

    public static final TicketController CONTROLLER = new TicketController(Feedback.id("robots"), (level, helper) -> {
        for (var owner : new ArrayList<>(helper.getEntityTickets().keySet()))
            helper.removeAllTickets(owner);
    });

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** A new server: no tickets are held. */
    public static void reset() {
        held = 0;
    }

    /** Takes a ticket over a chunk for {@code robot}; false if the cap is reached. */
    public static boolean hold(ServerLevel level, Entity robot, int chunkX, int chunkZ) {
        if (held >= RobotConfig.LOADED_CHUNKS.get())
            return false;
        CONTROLLER.forceChunk(level, robot, chunkX, chunkZ, true, true);
        held++;
        return true;
    }

    public static void release(ServerLevel level, Entity robot, int chunkX, int chunkZ) {
        CONTROLLER.forceChunk(level, robot, chunkX, chunkZ, false, true);
        held = Math.max(0, held - 1);
    }
}
