// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Lines tied between two {@link LineHost}s (pitons, winches) and anyone who takes hold of one. A line is two block entities that name each
 * other; this keeps a server-side index of the lines (by their primary end) so a player is tested against the lines of their dimension, not
 * against the world, and runs the climb.
 *
 * <ul>
 * <li><b>Taking hold.</b> A body that is off the ground within {@link ModularTuning#FIXED_LINE_REACH} of a line takes hold of it; sneaking
 * drops off (and it cannot be retaken for {@link ModularTuning#FIXED_LINE_REGRAB_TICKS}). Holding, a body hangs on the line: no fall.
 * <li><b>Climbing.</b> Forward moves along the line the way the body looks, back the other way, nothing holds still. A winch at an end
 * draws what hangs on the line toward it by {@link LineHost#reel()}, on top of the climb.
 * <li><b>Load.</b> Everyone hanging on a line is its load, in {@code BodyMass}. Over the line's rating ({@link GrappleHook#lineRating}) the
 * line strains and parts at 1, and a line a winch is straining at its limit ({@link LineHost#stalled}) takes the load several times over;
 * over what the block behind a piton holds ({@link GrappleHook#holdingFigure}) the piton works loose. Both are wear by misuse: a load under
 * the rating costs nothing however long it hangs.
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FixedLines {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LIVE = new HashMap<>();
    /** Per primary end: the load hung on it this tick. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> LOAD = new HashMap<>();
    /** Primary ends that had a load last tick, so a line left empty tells its ends. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();
    private static final Map<UUID, BlockPos> HELD = new HashMap<>();
    private static final Map<UUID, Long> DROPPED = new HashMap<>();

    private FixedLines() {
    }

    // --- the index ------------------------------------------------------------------------------

    /** Keeps the index of primary ends current: called by a host when it loads or its line changes. */
    public static void index(Level level, LineHost host) {
        Set<BlockPos> set = LIVE.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (host.primary())
            set.add(host.getBlockPos());
        else
            set.remove(host.getBlockPos());
    }

    /** Takes a host out of the index when it unloads. */
    public static void drop(Level level, BlockPos pos) {
        Set<BlockPos> set = LIVE.get(level.dimension());
        if (set != null)
            set.remove(pos);
    }

    /** The host at {@code pos}, or null. */
    public static LineHost host(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof LineHost host ? host : null;
    }

    /** The two ends of the line fixed at {@code pos}, or null when that block holds none. */
    public static Vec3[] ends(Level level, BlockPos pos) {
        LineHost a = host(level, pos);
        if (a == null || a.partner() == null)
            return null;
        LineHost b = host(level, a.partner());
        return b == null ? null : new Vec3[] { a.eye(), b.eye() };
    }

    // --- tying and cutting ----------------------------------------------------------------------

    /** The longest span a grapple with a longest line of {@code maxRope} ties. */
    public static float span(float maxRope) {
        return maxRope * ModularTuning.FIXED_LINE_SPAN;
    }

    /**
     * Ties a line of {@code material} between two hosts, cutting whatever either already held.
     *
     * @return the blocks of line paid out, or -1 when either is not a host
     */
    public static float tie(Level level, BlockPos a, BlockPos b, ResourceLocation material) {
        LineHost ha = host(level, a), hb = host(level, b);
        if (ha == null || hb == null || a.equals(b))
            return -1f;
        cut(level, a);
        cut(level, b);
        ha.end().link(b, material);
        hb.end().link(a, material);
        ha.lineChanged();
        hb.lineChanged();
        return (float) ha.eye().distanceTo(hb.eye());
    }

    /** Cuts the line fixed at {@code pos}, both ends. */
    public static void cut(Level level, BlockPos pos) {
        LineHost here = host(level, pos);
        if (here == null || here.partner() == null)
            return;
        BlockPos other = here.partner();
        here.end().unlink();
        here.lineChanged();
        LineHost there = host(level, other);
        if (there != null && pos.equals(there.partner())) {
            there.end().unlink();
            there.lineChanged();
        }
    }

    // --- the climb ------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        UUID id = player.getUUID();
        Set<BlockPos> set = LIVE.get(player.level().dimension());
        if (set == null || set.isEmpty()) {
            HELD.remove(id);
            return;
        }
        Level level = player.level();
        Vec3 body = player.position().add(0, player.getBbHeight() * 0.6, 0);
        BlockPos held = HELD.get(id);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        Vec3[] bestEnds = null;
        for (BlockPos primary : set) {
            Vec3[] ends = ends(level, primary);
            if (ends == null)
                continue;
            double d = distance(body, ends[0], ends[1]);
            if (d < bestDistance || primary.equals(held) && d <= ModularTuning.FIXED_LINE_REACH * 1.5) {
                best = primary;
                bestDistance = d;
                bestEnds = ends;
                if (primary.equals(held))
                    break;
            }
        }
        boolean flying = player.isSpectator() || player.getAbilities().flying || player.isFallFlying() || player.isPassenger();
        if (held != null) {
            if (best == null || !best.equals(held) || bestDistance > ModularTuning.FIXED_LINE_REACH * 1.5 || flying || player.isShiftKeyDown()) {
                if (player.isShiftKeyDown())
                    DROPPED.put(id, level.getGameTime());
                HELD.remove(id);
                return;
            }
        } else {
            if (best == null || bestDistance > ModularTuning.FIXED_LINE_REACH || flying || player.onGround() || player.isShiftKeyDown()
                    || level.getGameTime() - DROPPED.getOrDefault(id, -1000L) < ModularTuning.FIXED_LINE_REGRAB_TICKS)
                return;
            HELD.put(id, best);
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.1f);
        }
        LineHost a = host(level, best);
        LineHost b = a == null ? null : host(level, a.partner());
        double haul = a == null || b == null ? 0 : b.reel() - a.reel();
        climb(player, bestEnds[0], bestEnds[1], body, haul);
        LOAD.computeIfAbsent(level.dimension(), k -> new HashMap<>()).merge(best, BodyMass.of(player), Float::sum);
    }

    /** {@code haul} is the winches' draw along the line, from the first end toward the second (blocks per tick). */
    private static void climb(ServerPlayer player, Vec3 a, Vec3 b, Vec3 body, double haul) {
        Vec3 along = b.subtract(a);
        double length = along.length();
        if (length < 1.0e-4)
            return;
        Vec3 dir = along.scale(1 / length);
        double t = Math.max(0, Math.min(1, body.subtract(a).dot(dir) / length));
        Vec3 nearest = a.add(along.scale(t));
        // The way forward is the end the body looks along the line towards; a look square across the line goes uphill.
        double look = player.getLookAngle().dot(dir);
        double sign = Math.abs(look) > 0.15 ? Math.signum(look) : (dir.y >= 0 ? 1 : -1);
        double speed = player.zza * ModularTuning.FIXED_LINE_SPEED * sign + haul;
        if (t <= 0 && speed < 0 || t >= 1 && speed > 0)
            speed = 0;
        Vec3 pull = nearest.subtract(body).scale(0.5);
        player.setDeltaMovement(dir.scale(speed).add(pull));
        player.fallDistance = 0f;
        player.hurtMarked = true;
    }

    private static double distance(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double len2 = ab.lengthSqr();
        double t = len2 < 1.0e-9 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / len2));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    /** Load turns to strain once a tick, after every player on a line has been counted. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        Map<BlockPos, Float> loads = LOAD.remove(level.dimension());
        Set<BlockPos> before = LOADED.get(level.dimension());
        if (before != null)
            for (BlockPos primary : before)
                if (loads == null || !loads.containsKey(primary))
                    tell(level, primary, 0f);
        if (loads == null) {
            LOADED.remove(level.dimension());
            return;
        }
        LOADED.put(level.dimension(), new HashSet<>(loads.keySet()));
        for (Map.Entry<BlockPos, Float> e : loads.entrySet())
            bear(level, e.getKey(), e.getValue());
    }

    /** Tells both ends the mass now hanging on their line (a winch turns it into a load on its shaft). */
    private static void tell(ServerLevel level, BlockPos primary, float mass) {
        LineHost a = host(level, primary);
        if (a == null)
            return;
        a.hung(mass);
        LineHost b = a.partner() == null ? null : host(level, a.partner());
        if (b != null)
            b.hung(mass);
    }

    private static void bear(ServerLevel level, BlockPos primary, float mass) {
        LineHost a = host(level, primary);
        if (a == null || a.partner() == null)
            return;
        LineHost b = host(level, a.partner());
        if (b == null)
            return;
        tell(level, primary, mass);
        // A winch at its limit is straining the line, not turning: the load comes on hard, as a hook's does on a stalled reel.
        float load = mass * (a.stalled() || b.stalled() ? ModularTuning.GRAPPLE_STALL_LOAD : 1f);
        LineEnd line = a.end();
        float rating = GrappleHook.lineRating(line.line(), level.isRainingAt(primary.above()));
        if (load > rating) {
            line.setStrain(line.strain() + (load / rating - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK);
            a.lineChanged();
            if (line.strain() >= 1f) {
                level.playSound(null, primary, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1f, 1f);
                cut(level, primary);
                return;
            }
        }
        // Each end holds by the block it is fixed to; a winch is bolted fast and holds anything.
        for (LineHost end : new LineHost[] { a, b }) {
            BlockPos behind = end.behind();
            if (behind == null)
                continue;
            float hold = GrappleHook.holdingFigure(level.getBlockState(behind), level, behind);
            if (load <= hold)
                continue;
            end.end().setHoldStrain(end.end().holdStrain() + (load / hold - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK);
            if (end.end().holdStrain() >= 1f) {
                level.playSound(null, end.getBlockPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                level.destroyBlock(end.getBlockPos(), true);
                return;
            }
        }
    }

    /** Breaking a host cuts its line: called from the block, before the entity goes. */
    public static void onBroken(Level level, BlockPos pos) {
        cut(level, pos);
    }
}
