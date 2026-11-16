// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The crossing, with no command: a Sable body that sits in air of density zero for {@link SpaceTuning#HOLD_TICKS}
 * leaves the Overworld for the Gap, and a body that falls below the Gap's floor comes back, and so does a person on no body (see {@link SpaceTravel#dropStrays}). The up condition reads
 * the air table (a place with no air), never a height; the two heights are far enough apart that a body does not
 * bounce between them. Every {@link SpaceTuning#CHECK_INTERVAL} ticks, bodies only. In the Gap a drifting body is
 * woken each physics step, since Sable puts a body slower than 0.15 b/s to sleep and it would stop in free fall.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SpaceTrigger {
    private static final Map<UUID, Integer> HELD = new HashMap<>();

    private SpaceTrigger() {
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        HELD.clear();
        SpaceTravel.forgetStanding();
    }

    /** Ticks {@code body} has sat in thin air (0 when it is not waiting to cross). For the probe command. */
    public static int held(UUID body) {
        return HELD.getOrDefault(body, 0);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        boolean gap = level.dimension().equals(SpaceTravel.GAP);
        if (gap)
            SpaceTravel.dropStrays(level); // every tick: a person in free fall covers the floor in a few checks
        if (level.getGameTime() % SpaceTuning.CHECK_INTERVAL != 0)
            return;
        if (!gap && !level.dimension().equals(Level.OVERWORLD))
            return;
        SpaceTravel.noteStanding(level);
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        var movers = new ArrayList<ServerSubLevel>();
        var thin = new java.util.HashSet<UUID>();
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            double y = body.logicalPose().position().y();
            if (gap) {
                if (y < SpaceTuning.FLOOR_Y)
                    movers.add(body);
            } else if (AirDensityTable.at(level, y) <= 0f) {
                thin.add(body.getUniqueId());
                if (HELD.merge(body.getUniqueId(), SpaceTuning.CHECK_INTERVAL, Integer::sum) >= SpaceTuning.HOLD_TICKS)
                    movers.add(body);
            } else {
                HELD.remove(body.getUniqueId());
            }
        }
        if (!gap) // the Gap's pass has no thin bodies and must not forget the Overworld's
            HELD.keySet().retainAll(thin); // a body that was removed or left the thin air is forgotten
        for (ServerSubLevel body : movers) {
            HELD.remove(body.getUniqueId());
            if (!body.isRemoved()) // a chain moves with its first body
{
                    int n = gap ? SpaceTravel.back(level, body) : SpaceTravel.send(level, body);
                    Feedback.LOGGER.info("Space crossing of body {} at y {}: {} body(ies) moved to {}", body.getUniqueId(),
                            Math.round(body.logicalPose().position().y()), n, gap ? "the Overworld" : "the Gap");
                }
        }
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        if (!level.dimension().equals(SpaceTravel.GAP))
            return;
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        var pipeline = event.getPhysicsSystem().getPipeline();
        for (ServerSubLevel body : container.getAllSubLevels())
            if (!body.isRemoved()
                    && event.getPhysicsSystem().getPhysicsHandle(body).getLinearVelocity(new org.joml.Vector3d()).lengthSquared()
                            > SpaceTuning.DRIFT_MIN_SPEED * SpaceTuning.DRIFT_MIN_SPEED)
                pipeline.wakeUp(body);
    }
}
