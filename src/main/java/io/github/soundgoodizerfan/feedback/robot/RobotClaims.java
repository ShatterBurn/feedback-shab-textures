// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The block a robot is breaking is claimed for the time it takes, so a fleet spreads over a face instead of every robot digging the same
 * block. A claim lapses on its own after its ticks, so a robot that is lost holds nothing. Server memory only, never saved.
 *
 * <p>Prior art: PneumaticCraft: Repressurized's {@code DroneClaimManager} (GPL-3.0), design only: a per-dimension map of claimed positions
 * with a timeout. Rewritten here keyed by an owner id so a robot may renew its own claim.
 */
public final class RobotClaims {

    private RobotClaims() {
    }

    private record Claim(long owner, long until) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Claim>> CLAIMS = new HashMap<>();

    /** Claims {@code pos} for {@code owner} until {@code until} (game time); false if another owner holds it. */
    public static boolean claim(Level level, BlockPos pos, long owner, long until) {
        Map<Long, Claim> held = CLAIMS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        long key = pos.asLong();
        Claim there = held.get(key);
        long now = level.getGameTime();
        if (there != null && there.owner != owner && there.until > now)
            return false;
        held.put(key, new Claim(owner, until));
        if (held.size() > 512)
            held.values().removeIf(c -> c.until <= now);
        return true;
    }

    public static void release(Level level, BlockPos pos, long owner) {
        Map<Long, Claim> held = CLAIMS.get(level.dimension());
        if (held != null)
            held.computeIfPresent(pos.asLong(), (k, c) -> c.owner == owner ? null : c);
    }
}
