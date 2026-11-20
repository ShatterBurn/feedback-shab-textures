// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

/**
 * A craft that touches down hard hurts the people aboard (docs/specs/feedback_space_spec.md 6). Every physics step a body's velocity is
 * compared with the last step's; a drop in one step that no engine makes is a hit. Its full-size speed change is turned into the fall
 * that would stop a person as fast ({@code dv^2 / 2g}) and every living thing aboard takes fall damage for the part over
 * {@link SpaceTuning#IMPACT_FREE_FALL_BLOCKS}. The physics thread only notes the hit; the damage is done on the level's own tick.
 * <p>
 * shortcut: blocks at the contact are not broken by the hit yet; upgrade with a contact-point source from Sable.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Impact {
    private static final double G = 9.81;

    private record Hit(ResourceKey<Level> level, UUID body, double fall) {
    }

    /** A body's velocity and scale as of the last step: a crossing rewrites both, which is not a hit. */
    private record Step(Vector3d velocity, double scale) {
    }

    private static final Map<UUID, Step> LAST = new ConcurrentHashMap<>();
    private static final Queue<Hit> HITS = new ConcurrentLinkedQueue<>();

    private Impact() {
    }

    @SubscribeEvent
    public static void onPhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            Vector3d v = event.getPhysicsSystem().getPhysicsHandle(body).getLinearVelocity(new Vector3d());
            double scale = BodyScale.of(body);
            Step was = LAST.put(body.getUniqueId(), new Step(v, scale));
            if (was == null || was.scale() != scale)
                continue;
            double dv = v.distance(was.velocity()) / scale;
            if (dv < SpaceTuning.IMPACT_MIN_SPEED_CHANGE)
                continue;
            HITS.add(new Hit(level.dimension(), body.getUniqueId(), dv * dv / (2 * G)));
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || HITS.isEmpty())
            return;
        for (Hit hit : HITS) {
            if (!hit.level().equals(level.dimension()))
                continue;
            HITS.remove(hit);
            var container = ServerSubLevelContainer.getContainer(level);
            if (container == null || !(container.getSubLevel(hit.body()) instanceof ServerSubLevel body))
                continue;
            float damage = (float) Math.ceil(hit.fall() - SpaceTuning.IMPACT_FREE_FALL_BLOCKS);
            if (damage <= 0)
                continue;
            var b = body.boundingBox();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()).inflate(1)))
                e.hurt(level.damageSources().fall(), damage);
        }
    }

    @SubscribeEvent
    public static void onStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        LAST.clear();
        HITS.clear();
    }
}
