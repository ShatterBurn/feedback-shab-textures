// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.endernet;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.ControlTuning;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Endernet layer 2, the channel bus. A channel is a number the player tunes on a Data Port or on a Computer's endernet upgrade; every node
 * tuned to it, in one dimension, shares it. The channel keeps one bandwidth allowance for everything on it ({@link ControlTuning#ENDERNET_CHANNEL_SIGNALS_PER_MIN}
 * a minute, refilled from the game clock): a port's reads and writes and a Computer's packets all draw on it, so a busy channel starves its own
 * listeners. It also keeps {@link ControlTuning#BUS_SLOTS} signal slots a program writes with an Actuator card on a tuned Data Port and any other
 * port tuned the same hears with a Sensor card: there is no address on a bus, so anyone tuned to the channel hears it (eavesdropping is the
 * design). A slot's value goes stale after {@link ControlTuning#BUS_STALE_TICKS}. State is in memory only: a restart is a quiet band.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class EndernetBand {

    private record Key(ResourceKey<Level> dimension, int channel) {
    }

    /** What a slot last carried: the value, when, and where it was written from. */
    public record Posted(float value, long tick, Vec3 from) {
    }

    private static final class Band {
        float allowance = ControlTuning.ENDERNET_CHANNEL_SIGNALS_PER_MIN;
        long at;
        final Posted[] slots = new Posted[ControlTuning.BUS_SLOTS];
    }

    private static final Map<Key, Band> BANDS = new HashMap<>();

    private EndernetBand() {
    }

    private static Band band(Level level, int channel) {
        return BANDS.computeIfAbsent(new Key(level.dimension(), channel), k -> new Band());
    }

    /** Takes {@code units} signals of the channel's bandwidth; false (taking nothing) when the channel is saturated. */
    public static boolean take(Level level, int channel, float units) {
        Band b = band(level, channel);
        long now = level.getGameTime();
        float cap = ControlTuning.ENDERNET_CHANNEL_SIGNALS_PER_MIN;
        b.allowance = Math.min(cap, b.allowance + cap * Math.max(0L, now - b.at) / 1200f);
        b.at = now;
        if (b.allowance < units)
            return false;
        b.allowance -= units;
        return true;
    }

    public static void post(Level level, int channel, int slot, float value, Vec3 from) {
        if (slot >= 0 && slot < ControlTuning.BUS_SLOTS)
            band(level, channel).slots[slot] = new Posted(value, level.getGameTime(), from);
    }

    /** What {@code slot} carries now, or null when nothing was written or it is stale. */
    @Nullable
    public static Posted read(Level level, int channel, int slot) {
        if (slot < 0 || slot >= ControlTuning.BUS_SLOTS)
            return null;
        Posted p = band(level, channel).slots[slot];
        return p == null || level.getGameTime() - p.tick() > ControlTuning.BUS_STALE_TICKS ? null : p;
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        BANDS.clear();
    }
}
