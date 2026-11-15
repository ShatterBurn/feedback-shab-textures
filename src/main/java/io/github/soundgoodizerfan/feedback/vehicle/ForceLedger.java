// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * What each force source applied to a body, by cause, so the Contraption Diagram shows what the sim did
 * and cannot drift from it. A source calls {@link #record} beside its own push; the call is one empty-map
 * check until somebody watches the body, so a vehicle nobody is reading costs nothing.
 * <p>
 * Read from Create Simulated's contraption diagram (MIT; design only, no code): per-body recording that
 * is switched on while a viewer asks, sources recording a point force per cause, gravity added by the
 * reader. Taken: opt-in recording, point forces in the body's own frame. Rejected: Sable's force-group
 * registry (Feedback's causes are its own), one-step snapshots (this keeps a one second mean, since a
 * pulsed thruster or a cycling ballast tank would flicker).
 * <p>
 * Impulses are summed per cause and block cell in the body's own plot frame over half-second buckets;
 * the last two buckets make the mean. A force is the summed impulse over the time it covers, which is
 * kN (a block is a metre and Sable's mass unit, the kpg, a tonne), the {@code Fu}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ForceLedger {

    /** What a force comes from. The order is the wire id; the diagram's legend lists them in it. */
    public enum Cause {
        GRAVITY(0x66FF88), DRAG(0xD28BFF), LEVITATION(0x7FD4FF), BUOYANCY(0xFFB04D), BALLAST(0x4DE6D8),
        PROPULSION(0xFF6B6B), WING_LIFT(0xE6E64D);

        private final int colour;

        Cause(int colour) {
            this.colour = colour;
        }

        public int colour() {
            return colour;
        }

        public String key() {
            return "feedback.diagram.cause." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One force at a point of the body's plot frame, in Fu. */
    public record Sample(Cause cause, float x, float y, float z, float fx, float fy, float fz) {
    }

    private static final int BUCKET_TICKS = 10;
    private static final int KEEP_TICKS = 60;

    private static final Map<ServerSubLevel, Tracker> WATCHED = new ConcurrentHashMap<>();

    private ForceLedger() {
    }

    /** Called beside a source's own push: a point and an impulse, both in the body's plot frame. */
    public static void record(ServerSubLevel body, Cause cause, double px, double py, double pz, double ix, double iy, double iz) {
        if (WATCHED.isEmpty())
            return;
        Tracker t = WATCHED.get(body);
        if (t != null)
            t.add(cause, px, py, pz, ix, iy, iz);
    }

    /** Starts or extends recording for {@code body}. */
    public static void watch(ServerSubLevel body) {
        WATCHED.computeIfAbsent(body, Tracker::new).keep = KEEP_TICKS;
    }

    /** The last second's mean forces on {@code body}, empty if it is not watched yet. */
    public static List<Sample> read(ServerSubLevel body) {
        Tracker t = WATCHED.get(body);
        return t == null ? List.of() : t.latest;
    }

    /** Whether somebody is reading {@code body}'s forces: lets a source skip work that only feeds the ledger. */
    public static boolean watching(ServerSubLevel body) {
        return !WATCHED.isEmpty() && WATCHED.containsKey(body);
    }

    @SubscribeEvent
    public static void onServerStopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event) {
        WATCHED.clear();
        io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramService.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (WATCHED.isEmpty())
            return;
        WATCHED.entrySet().removeIf(e -> e.getKey().isRemoved() || e.getValue().tick());
    }

    private static final class Tracker {
        private final ServerSubLevel body;
        volatile int keep = KEEP_TICKS;
        volatile List<Sample> latest = List.of();
        private int age, filled;
        private Map<Long, double[]>[] current = fresh();
        private Map<Long, double[]>[] previous = fresh();

        Tracker(ServerSubLevel body) {
            this.body = body;
        }

        @SuppressWarnings("unchecked")
        private static Map<Long, double[]>[] fresh() {
            Map<Long, double[]>[] maps = new Map[Cause.values().length];
            for (int i = 0; i < maps.length; i++)
                maps[i] = new HashMap<>();
            return maps;
        }

        /** Physics thread. A cell's entry is its summed impulse and the point of its first record. */
        synchronized void add(Cause cause, double px, double py, double pz, double ix, double iy, double iz) {
            long cell = BlockPos.asLong((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
            Map<Long, double[]> cells = current[cause.ordinal()];
            double[] e = cells.get(cell);
            if (e == null)
                cells.put(cell, e = new double[] {0, 0, 0, px, py, pz});
            e[0] += ix;
            e[1] += iy;
            e[2] += iz;
        }

        /** Game thread, once a tick. True when nobody has asked for a while and the tracker can go. */
        boolean tick() {
            if (--keep <= 0)
                return true;
            if (++age >= BUCKET_TICKS) {
                age = 0;
                rotate();
            }
            return false;
        }

        private synchronized void rotate() {
            // Mean over this bucket and the last, in seconds of game time they cover (a tick is 0.05 s).
            filled = Math.min(2, filled + 1);
            double seconds = filled * BUCKET_TICKS * 0.05;
            Map<Long, double[]>[] now = current;
            Map<Long, double[]>[] before = previous;
            List<Sample> out = new ArrayList<>();
            for (Cause c : Cause.values()) {
                Map<Long, double[]> sum = new HashMap<>();
                for (Map<Long, double[]> bucket : List.of(now[c.ordinal()], before[c.ordinal()]))
                    for (var e : bucket.entrySet()) {
                        double[] s = sum.computeIfAbsent(e.getKey(), k -> new double[] {0, 0, 0, e.getValue()[3], e.getValue()[4], e.getValue()[5]});
                        s[0] += e.getValue()[0];
                        s[1] += e.getValue()[1];
                        s[2] += e.getValue()[2];
                    }
                for (double[] s : sum.values()) {
                    float fx = (float) (s[0] / seconds), fy = (float) (s[1] / seconds), fz = (float) (s[2] / seconds);
                    if (fx * fx + fy * fy + fz * fz > 1e-8f)
                        out.add(new Sample(c, (float) s[3], (float) s[4], (float) s[5], fx, fy, fz));
                }
            }
            latest = List.copyOf(out);
            previous = now;
            current = fresh();
        }
    }
}
