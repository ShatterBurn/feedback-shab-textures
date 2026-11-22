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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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
 * Lines tied between two pitons and anyone who takes hold of one. A line is two {@link PitonBlockEntity} ends that name each other; this
 * keeps a server-side index of the lines (by their primary end) so a player is tested against the lines of their dimension, not against
 * the world, and runs the climb.
 *
 * <ul>
 * <li><b>Taking hold.</b> A body that is off the ground within {@link ModularTuning#FIXED_LINE_REACH} of a line takes hold of it; sneaking
 * drops off (and it cannot be retaken for {@link ModularTuning#FIXED_LINE_REGRAB_TICKS}). Holding, a body hangs on the line: no fall.
 * <li><b>Climbing.</b> Forward moves along the line the way the body looks, back the other way, nothing holds still. The pay-out is the
 * line itself: it runs where it was tied, so the span is the route.
 * <li><b>Load.</b> Everyone hanging on a line is its load, in {@code BodyMass}. Over the line's rating ({@link GrappleHook#lineRating}) the
 * line strains and parts at 1; over what the block behind a piton holds ({@link GrappleHook#holdingFigure}) the piton works loose. Both are
 * wear by misuse: a load under the rating costs nothing however long it hangs.
 * </ul>
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FixedLines {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LIVE = new HashMap<>();
    /** Per primary end: the load hung on it this tick. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Float>> LOAD = new HashMap<>();
    private static final Map<UUID, BlockPos> HELD = new HashMap<>();
    private static final Map<UUID, Long> DROPPED = new HashMap<>();

    private FixedLines() {
    }

    // --- the index ------------------------------------------------------------------------------

    static void index(Level level, PitonBlockEntity piton) {
        Set<BlockPos> set = LIVE.computeIfAbsent(level.dimension(), k -> new HashSet<>());
        if (piton.primary())
            set.add(piton.getBlockPos());
        else
            set.remove(piton.getBlockPos());
    }

    static void drop(Level level, BlockPos pos) {
        Set<BlockPos> set = LIVE.get(level.dimension());
        if (set != null)
            set.remove(pos);
    }

    /** The two ends of the line fixed at {@code pos}, or null when that piton holds none. */
    public static Vec3[] ends(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PitonBlockEntity a) || a.partner() == null || !(level.getBlockEntity(a.partner()) instanceof PitonBlockEntity))
            return null;
        return new Vec3[] { PitonBlock.eye(level.getBlockState(pos), pos), PitonBlock.eye(level.getBlockState(a.partner()), a.partner()) };
    }

    // --- tying and cutting ----------------------------------------------------------------------

    /** The longest span a grapple with a longest line of {@code maxRope} ties. */
    public static float span(float maxRope) {
        return maxRope * ModularTuning.FIXED_LINE_SPAN;
    }

    /**
     * Ties a line of {@code material} between two pitons, cutting whatever either already held.
     *
     * @return the blocks of line paid out, or -1 when either is not a piton
     */
    public static float tie(Level level, BlockPos a, BlockPos b, ResourceLocation material) {
        if (!(level.getBlockEntity(a) instanceof PitonBlockEntity ea) || !(level.getBlockEntity(b) instanceof PitonBlockEntity eb) || a.equals(b))
            return -1f;
        cut(level, a);
        cut(level, b);
        ea.link(b, material);
        eb.link(a, material);
        return (float) PitonBlock.eye(level.getBlockState(a), a).distanceTo(PitonBlock.eye(level.getBlockState(b), b));
    }

    /** Cuts the line fixed at {@code pos}, both ends. */
    public static void cut(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PitonBlockEntity here) || here.partner() == null)
            return;
        BlockPos other = here.partner();
        here.unlink();
        if (level.getBlockEntity(other) instanceof PitonBlockEntity there && pos.equals(there.partner()))
            there.unlink();
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
        // The nearest line to the body.
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
        climb(player, bestEnds[0], bestEnds[1], body);
        LOAD.computeIfAbsent(level.dimension(), k -> new HashMap<>()).merge(best, io.github.soundgoodizerfan.feedback.combat.tether.BodyMass.of(player), Float::sum);
    }

    private static void climb(ServerPlayer player, Vec3 a, Vec3 b, Vec3 body) {
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
        double speed = player.zza * ModularTuning.FIXED_LINE_SPEED * sign;
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
        if (loads == null)
            return;
        for (Map.Entry<BlockPos, Float> e : loads.entrySet())
            bear(level, e.getKey(), e.getValue());
    }

    private static void bear(ServerLevel level, BlockPos primary, float load) {
        if (!(level.getBlockEntity(primary) instanceof PitonBlockEntity a) || a.partner() == null)
            return;
        float rating = GrappleHook.lineRating(a.line(), level.isRainingAt(primary.above()));
        if (load > rating) {
            a.setStrain(a.strain() + (load / rating - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK);
            if (a.strain() >= 1f) {
                level.playSound(null, primary, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1f, 1f);
                cut(level, primary);
                return;
            }
        }
        // Each end holds by the block it is driven into.
        for (BlockPos end : new BlockPos[] { primary, a.partner() }) {
            if (!(level.getBlockEntity(end) instanceof PitonBlockEntity piton))
                continue;
            BlockState state = level.getBlockState(end);
            BlockPos behind = end.relative(state.getValue(PitonBlock.FACING).getOpposite());
            float hold = GrappleHook.holdingFigure(level.getBlockState(behind), level, behind);
            if (load <= hold)
                continue;
            piton.setHoldStrain(piton.holdStrain() + (load / hold - 1f) * ModularTuning.GRAPPLE_STRAIN_PER_TICK);
            if (piton.holdStrain() >= 1f) {
                level.playSound(null, end, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
                level.destroyBlock(end, true);
                return;
            }
        }
    }

    /** Breaking a piton cuts its line: called from the block, before the entity goes. */
    public static void onBroken(Level level, BlockPos pos) {
        cut(level, pos);
    }

    /** Whether {@code block} is a piton. */
    public static boolean isPiton(Block block) {
        return block instanceof PitonBlock;
    }
}
