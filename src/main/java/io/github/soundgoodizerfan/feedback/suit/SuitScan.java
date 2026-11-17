// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The suit's SCAN pulse. It lights every creature in reach (the glow the pulse always gave) and takes a reading of what it saw. The held circuit
 * shapes both: its gain lengthens the reach ({@link SuitModules#power}), its noise makes the pulse miss some of what is in reach and smears the
 * distance it reads out. With an endernet upgrade fitted to the suit (a pair id on its {@link SuitState}) the reading is also posted under that
 * pair, where the Data Port at the other end hands it to a program as typed channels ({@link #CHANNELS}), paying teleportant and bandwidth per
 * signal as any endernet crossing does. The reading is the last scan only, and goes stale after {@link SuitTuning#SCAN_SHARE_TICKS}.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = io.github.soundgoodizerfan.feedback.Feedback.MOD_ID)
public final class SuitScan {

    /** What a shared scan carries, in channel order (the Data Port's sense channels while a suit is on the pair). */
    public static final String[] CHANNELS = {"count", "hostile", "nearest", "radius"};

    /** One scan's reading: where the wearer stood and what the pulse made of what it saw. */
    public record Reading(ResourceKey<Level> dimension, Vec3 at, long tick, int count, int hostile, float nearest, float radius) {

        public float channel(int i) {
            return switch (i) {
                case 0 -> count;
                case 1 -> hostile;
                case 2 -> nearest;
                default -> radius;
            };
        }
    }

    private static final Map<UUID, Reading> SHARED = new HashMap<>();

    private SuitScan() {
    }

    /** A thing a pulse may act on: alive, not the wearer, not a pet the wearer owns. */
    private static boolean target(ServerPlayer p, LivingEntity e) {
        return e != p && e.isAlive() && !(e instanceof OwnableEntity o && o.getOwnerUUID() != null && o.getOwnerUUID().equals(p.getUUID()));
    }

    /** Fires the scan at the wearer's feet with the module's {@code power}, the held circuit's figures read from {@code mods}. */
    static void fire(ServerPlayer p, float power, float[] mods) {
        var level = p.serverLevel();
        double r = SuitTuning.SCAN_RADIUS * (0.5 + 0.5 * power);
        float noise = SuitModules.noise(mods);
        int count = 0, hostile = 0;
        double nearest = Double.MAX_VALUE;
        List<LivingEntity> in = level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> target(p, x));
        for (LivingEntity e : in) {
            double d = Math.sqrt(p.distanceToSqr(e));
            if (d > r || p.getRandom().nextFloat() < SuitTuning.SCAN_MISS * noise)
                continue;
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, SuitTuning.SCAN_GLOW_TICKS, 0, true, false, false));
            count++;
            if (e instanceof Enemy)
                hostile++;
            nearest = Math.min(nearest, d);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.8f);
        var pair = ExoSuit.state(p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)).pair();
        if (pair.isPresent()) {
            float seen = nearest == Double.MAX_VALUE ? 0f
                    : (float) (nearest * (1f + (p.getRandom().nextFloat() * 2f - 1f) * SuitTuning.SCAN_JITTER * noise));
            SHARED.put(pair.get(), new Reading(level.dimension(), p.position(), level.getGameTime(), count, hostile, Math.max(0f, seen), (float) r));
        }
    }

    /** The last scan shared under {@code pair}, or null when none is fresh. */
    @Nullable
    public static Reading get(UUID pair, long gameTime) {
        Reading r = SHARED.get(pair);
        return r == null || gameTime - r.tick() > SuitTuning.SCAN_SHARE_TICKS ? null : r;
    }

    /** Forgets every shared scan when a server stops. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onStop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        SHARED.clear();
    }
}
