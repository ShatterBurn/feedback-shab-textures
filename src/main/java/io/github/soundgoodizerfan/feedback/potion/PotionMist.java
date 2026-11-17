// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Potion mist: what an atomiser leaves where its stream stops. A cloud holds the millilitres of potion that hung there and thins as it
 * doses what stands in it, a light share of a bottle every {@link PotionTuning#MIST_EVERY} ticks ({@link PotionSpray#body}), until the
 * potion is spent or it has hung {@link PotionTuning#MIST_TICKS}. It is the same act as a stream's, at a fraction of the flow.
 * <p>
 * shortcut: held in memory, not saved (a reload clears the clouds); at most {@link PotionTuning#MIST_MAX_CLOUDS} a dimension, the oldest
 * dropped first; upgrade if a cloud must survive an unload.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class PotionMist {

    private PotionMist() {}

    private static final class Cloud {
        final Vec3 at;
        final FluidStack fluid;
        final long until;
        int mb;

        Cloud(Vec3 at, FluidStack fluid, long until) {
            this.at = at;
            this.fluid = fluid;
            this.until = until;
            this.mb = fluid.getAmount();
        }
    }

    private static final Map<ResourceKey<Level>, List<Cloud>> CLOUDS = new HashMap<>();

    /** Leaves {@code fluid} (its amount is the potion that hangs) as a cloud at {@code at}. */
    public static void add(ServerLevel level, Vec3 at, FluidStack fluid) {
        var clouds = CLOUDS.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        if (clouds.size() >= PotionTuning.MIST_MAX_CLOUDS)
            clouds.remove(0);
        clouds.add(new Cloud(at, fluid, level.getGameTime() + PotionTuning.MIST_TICKS));
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % PotionTuning.MIST_EVERY != 0)
            return;
        var clouds = CLOUDS.get(level.dimension());
        if (clouds == null || clouds.isEmpty())
            return;
        long now = level.getGameTime();
        clouds.removeIf(c -> c.until <= now || c.mb <= 0);
        for (Cloud c : clouds) {
            float r = PotionTuning.MIST_RADIUS;
            for (LivingEntity body : level.getEntitiesOfClass(LivingEntity.class, new AABB(c.at, c.at).inflate(r)))
                if (body.isAlive() && body.position().add(0, body.getBbHeight() * 0.5, 0).distanceToSqr(c.at) <= r * r)
                    PotionSpray.body(level, null, body, c.fluid, Math.min(c.mb, PotionTuning.MIST_DOSE_MB));
            c.mb -= PotionTuning.MIST_DOSE_MB;
            int rgb = PotionSpray.colour(c.fluid);
            level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | rgb), c.at.x, c.at.y, c.at.z, 3, r * 0.4, r * 0.3, r * 0.4, 0.0);
        }
    }
}
