// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The Wither draws on a core: a lit beacon within {@link #RANGE} blocks of it feeds it, by the beacon's pyramid level. The player builds the
 * arena, so the player picks the difficulty; there is no slider.
 *
 * <ul>
 * <li><b>Feed.</b> Regeneration and Strength of amplifier {@code level - 1}, and Speed one lower, refreshed while it draws.
 * <li><b>Shield.</b> While the beam is lit the Wither takes {@link #SHIELD_PER_LEVEL} less damage per pyramid level (at most
 * {@link #SHIELD_MAX}); cover or break the beam and the shield is gone.
 * <li><b>Volleys.</b> It looses an extra volley of skulls at its target every {@code VOLLEY_TICKS / level} ticks.
 * </ul>
 *
 * A beacon is told by its block entity class, its pyramid by the {@code beacon_base_blocks} tag, never by block ids. The strongest beacon in
 * reach counts; two do not add.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WitherCore {

    private WitherCore() {
    }

    /** STAND-INS: blocks a beacon reaches the Wither, share of damage turned per pyramid level and its cap, ticks of an extra volley at level 1, and how often the nearby beacons are looked up. */
    private static final double RANGE = 20;
    private static final float SHIELD_PER_LEVEL = 0.15f, SHIELD_MAX = 0.6f;
    private static final int VOLLEY_TICKS = 160, LOOKUP_TICKS = 20, EFFECT_TICKS = 60;

    /** The pyramid level of the strongest lit beacon within reach of {@code wither}, or 0. */
    public static int level(WitherBoss wither) {
        if (!(wither.level() instanceof ServerLevel level))
            return 0;
        int best = 0;
        int cx = wither.blockPosition().getX() >> 4, cz = wither.blockPosition().getZ() >> 4;
        int reach = (int) Math.ceil(RANGE / 16);
        for (int x = cx - reach; x <= cx + reach; x++)
            for (int z = cz - reach; z <= cz + reach; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null)
                    continue;
                for (var be : chunk.getBlockEntities().values())
                    if (be instanceof BeaconBlockEntity beacon && !beacon.getBeamSections().isEmpty() && be.getBlockPos().distSqr(wither.blockPosition()) <= RANGE * RANGE)
                        best = Math.max(best, pyramid(level, be.getBlockPos()));
            }
        return best;
    }

    /** Layers of beacon base under {@code beacon}, up to four, as the game counts them. */
    private static int pyramid(ServerLevel level, BlockPos beacon) {
        int layers = 0;
        for (int l = 1; l <= 4; l++) {
            boolean full = true;
            for (int x = -l; x <= l && full; x++)
                for (int z = -l; z <= l; z++)
                    if (!level.getBlockState(beacon.offset(x, -l, z)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                        full = false;
                        break;
                    }
            if (!full)
                break;
            layers = l;
        }
        return layers;
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide || wither.isDeadOrDying() || wither.tickCount % LOOKUP_TICKS != 0)
            return;
        int level = level(wither);
        if (level <= 0)
            return;
        int amp = level - 1;
        wither.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EFFECT_TICKS, amp, true, false));
        wither.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_TICKS, amp, true, false));
        if (amp > 0)
            wither.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, amp - 1, true, false));
        LivingEntity target = wither.getTarget();
        if (target != null && wither.tickCount % Math.max(LOOKUP_TICKS, VOLLEY_TICKS / level / LOOKUP_TICKS * LOOKUP_TICKS) == 0)
            wither.performRangedAttack(target, 1f);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither) || wither.level().isClientSide)
            return;
        int level = level(wither);
        if (level > 0)
            event.setAmount(event.getAmount() * (1f - Math.min(SHIELD_MAX, level * SHIELD_PER_LEVEL)));
    }
}
