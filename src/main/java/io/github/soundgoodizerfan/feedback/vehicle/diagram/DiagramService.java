// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.diagram;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3d;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.Gravity;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Cause;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Sample;
import io.github.soundgoodizerfan.feedback.vehicle.Superobject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Builds the diagram's figures for a Flight Sensor's body: the ledger's recorded forces, plus the body's
 * weight (gravity is not a source, it is mass times the field, so it is added here from the mass Sable
 * holds). Everything is put in the sensor body's own plot frame, so one drawing holds a joined assembly.
 */
public final class DiagramService {

    private static final double REACH_SQ = 24 * 24;
    private static final int MAX_SAMPLES = 400;
    private static final long ASSEMBLY_KEEP_MS = 2000;

    /** The joined assembly of a body is a scan of every plot, so it is kept two seconds. */
    private record Assembly(Set<ServerSubLevel> bodies, long at) {
    }

    private static final ConcurrentHashMap<UUID, Assembly> ASSEMBLIES = new ConcurrentHashMap<>();

    private DiagramService() {
    }

    /** Drops the kept assemblies; they hold bodies, and so a whole level, past a server stop. */
    public static void clear() {
        ASSEMBLIES.clear();
    }

    /** The body a sensor stands in, or null when it is on the ground. */
    public static ServerSubLevel bodyOf(ServerLevel level, BlockPos pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos) instanceof ServerSubLevel body && !body.isRemoved() ? body : null;
    }

    public static void respond(ServerPlayer player, BlockPos pos, boolean assembly) {
        if (!(player.level() instanceof ServerLevel level) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof FlightSensorBlockEntity))
            return;
        ServerSubLevel root = bodyOf(level, pos);
        Vector3d where = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (root != null)
            root.logicalPose().transformPosition(where);
        if (player.distanceToSqr(where.x, where.y, where.z) > REACH_SQ)
            return;
        if (root == null) {
            PacketDistributor.sendToPlayer(player, new DiagramDataPayload(pos, false, assembly, 0, 0, 0, 0, List.of(), List.of(), List.of()));
            return;
        }
        Set<ServerSubLevel> bodies = assembly ? assemblyOf(level, root) : Set.of(root);
        Pose3dc rootPose = root.logicalPose();
        Vector3d field = Gravity.vector(level);
        List<UUID> ids = new ArrayList<>();
        List<Sample> samples = new ArrayList<>();
        double mass = 0;
        Vector3d com = new Vector3d();
        Vector3d p = new Vector3d(), f = new Vector3d();
        ids.add(root.getUniqueId());
        for (ServerSubLevel b : bodies) {
            if (b != root)
                ids.add(b.getUniqueId());
            ForceLedger.watch(b);
            Pose3dc pose = b.logicalPose();
            double m = b.getMassTracker().getMass();
            var c = b.getMassTracker().getCenterOfMass();
            if (c != null && m > 0) {
                toRoot(pose, rootPose, b == root, new Vector3d(c), p);
                com.fma(m, p);
                mass += m;
                // Weight: mass times the field, down in the world, taken to the root's frame.
                f.set(field).mul(m);
                rootPose.transformNormalInverse(f);
                if (f.lengthSquared() > 0)
                    samples.add(new Sample(Cause.GRAVITY, (float) p.x, (float) p.y, (float) p.z, (float) f.x, (float) f.y, (float) f.z));
            }
            for (Sample s : ForceLedger.read(b)) {
                if (samples.size() >= MAX_SAMPLES)
                    break;
                toRoot(pose, rootPose, b == root, p.set(s.x(), s.y(), s.z()), p);
                Vector3d v = new Vector3d(s.fx(), s.fy(), s.fz());
                if (b != root)
                    rootPose.transformNormalInverse(pose.transformNormal(v));
                samples.add(new Sample(s.cause(), (float) p.x, (float) p.y, (float) p.z, (float) v.x, (float) v.y, (float) v.z));
            }
        }
        if (mass > 0)
            com.div(mass);
        PacketDistributor.sendToPlayer(player, new DiagramDataPayload(pos, true, assembly, (float) (mass * Units.MU_PER_KPG),
                (float) com.x, (float) com.y, (float) com.z, ids, samples,
                ((FlightSensorBlockEntity) level.getBlockEntity(pos)).readings()));
    }

    private static Set<ServerSubLevel> assemblyOf(ServerLevel level, ServerSubLevel root) {
        long now = System.currentTimeMillis();
        Assembly known = ASSEMBLIES.get(root.getUniqueId());
        if (known != null && now - known.at < ASSEMBLY_KEEP_MS && known.bodies.stream().noneMatch(ServerSubLevel::isRemoved))
            return known.bodies;
        Set<ServerSubLevel> found = Superobject.of(level, root);
        ASSEMBLIES.put(root.getUniqueId(), new Assembly(found, now));
        ASSEMBLIES.values().removeIf(a -> now - a.at > 30 * ASSEMBLY_KEEP_MS);
        return found;
    }

    /** A point in {@code from}'s plot frame, put into {@code root}'s; {@code out} may be {@code point}. */
    private static void toRoot(Pose3dc from, Pose3dc root, boolean same, Vector3d point, Vector3d out) {
        if (same) {
            out.set(point);
            return;
        }
        from.transformPosition(point);
        root.transformPositionInverse(point);
        out.set(point);
    }
}
