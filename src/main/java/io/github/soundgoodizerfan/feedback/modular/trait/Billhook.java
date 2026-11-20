// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.Feedback;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The billhook's hooked cut. A blow on a mob opens a bleed of {@code bleed_damage} a second for
 * {@code bleed_seconds}, credited to the attacker; it does not stack. Cost: the blow itself lands at {@code hit_mult}, so
 * the bleed has to do the work.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Billhook implements ModularTrait {

    private record Bleed(LivingEntity victim, long end, float damage, int stacks, @Nullable UUID cause) {
    }

    private static final Map<UUID, Bleed> BLEEDS = new ConcurrentHashMap<>();

    @Override
    public void onAttack(LivingIncomingDamageEvent event, LivingEntity attacker, ItemStack weapon, Params params) {
        event.setAmount(event.getAmount() * params.get("hit_mult", 0.8f));
        open(event.getEntity(), attacker, params.get("bleed_seconds", 5f), params.get("bleed_damage", 1f));
    }

    /** Opens a bleed on {@code victim}, unless one is already running (claws on a glove use it too); {@code cause} is credited with the damage and a kill, or null for none. */
    public static void open(LivingEntity victim, @Nullable Entity cause, float seconds, float damage) {
        BLEEDS.putIfAbsent(victim.getUUID(), new Bleed(victim, victim.level().getGameTime() + (long) (seconds * FTuning.TICKS_PER_SECOND), damage, 1,
                cause == null ? null : cause.getUUID()));
    }

    /** Whether {@code victim} has a bleed running. */
    public static boolean bleeding(LivingEntity victim) {
        return BLEEDS.containsKey(victim.getUUID());
    }

    /** Adds a stack to a running bleed (up to {@code max}) and runs it {@code seconds} longer from now; false if there is none. */
    public static boolean stack(LivingEntity victim, int max, float seconds) {
        Bleed b = BLEEDS.get(victim.getUUID());
        if (b == null)
            return false;
        BLEEDS.put(victim.getUUID(), new Bleed(victim, Math.max(b.end, victim.level().getGameTime() + (long) (seconds * FTuning.TICKS_PER_SECOND)),
                b.damage, Math.min(max, b.stacks + 1), b.cause));
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (BLEEDS.isEmpty() || event.getServer().getTickCount() % 20 != 0)
            return;
        Iterator<Bleed> it = BLEEDS.values().iterator();
        while (it.hasNext()) {
            Bleed b = it.next();
            if (!b.victim.isAlive() || b.victim.level().getGameTime() >= b.end) {
                BLEEDS.remove(b.victim.getUUID());
                continue;
            }
            // Cause set, direct entity null: a kill is credited, but the hit is no melee blow, so no weapon trait runs on it.
            Entity cause = b.cause == null || !(b.victim.level() instanceof ServerLevel server) ? null : server.getEntity(b.cause);
            DamageSource source = cause == null ? b.victim.damageSources().generic() : new DamageSource(b.victim.damageSources().generic().typeHolder(), null, cause);
            b.victim.hurt(source, b.damage * b.stacks);
        }
    }
}
