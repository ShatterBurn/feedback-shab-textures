// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.diagram;

import io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramDataPayload;
import io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramRequestPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The newest figures the server sent for the sensor a diagram (screen or desktop window) is showing.
 * One slot: only one diagram is read at a time, and a figure for another sensor is dropped.
 */
public final class DiagramFeed {

    private static DiagramDataPayload latest;
    private static long at;

    private DiagramFeed() {
    }

    public static void accept(DiagramDataPayload data) {
        latest = data;
        at = System.currentTimeMillis();
    }

    /** The figures for {@code sensor}, or null if none have come (or they are for another sensor). */
    public static DiagramDataPayload latest(BlockPos sensor) {
        return latest != null && latest.sensor().equals(sensor) ? latest : null;
    }

    /** Milliseconds since the last figures arrived; a diagram says "no reading" when this is long. */
    public static long age() {
        return System.currentTimeMillis() - at;
    }

    public static void request(BlockPos sensor, boolean assembly) {
        PacketDistributor.sendToServer(new DiagramRequestPayload(sensor, assembly));
    }
}
