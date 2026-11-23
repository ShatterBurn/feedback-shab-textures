// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.blockcamera;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Finds a Block Camera for a player with Bad Omen. Only such a player does any work: once a second they look through the
 * block entities of the chunks within reach of them for an idle camera in range. Everyone else, and every camera, costs nothing.
 * The numbers are placeholders for the author to tune.
 */
public final class BlockCameraWatch {

    private BlockCameraWatch() {
    }

    /** Blocks within which a camera notices a player with Bad Omen. */
    public static final int RANGE = 16;
    /** Blocks past which a locked-on camera loses the player. */
    public static final int LOSE_RANGE = 32;
    /** How often, in ticks, a player with Bad Omen looks for a camera. */
    static final int SCAN_TICKS = 20;
    /** Ticks between the lamp lighting and going dark. */
    static final int BLINK_TICKS = 10;
    /** Scheduled ticks in a whole lock-on: the lamp lights and goes dark three times, three seconds. */
    static final int BLINKS = 6;
    /** Ticks a lock-on lasts from first click to report. */
    public static final int LOCK_TICKS = BLINK_TICKS * BLINKS;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % SCAN_TICKS != 0
                || !player.hasEffect(MobEffects.BAD_OMEN))
            return;
        ServerLevel level = player.serverLevel();
        if (!canRaid(level) || player.isSpectator())
            return;
        int reach = (RANGE + 15) >> 4;
        int cx = player.chunkPosition().x, cz = player.chunkPosition().z;
        for (int x = cx - reach; x <= cx + reach; x++)
            for (int z = cz - reach; z <= cz + reach; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null)
                    continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof BlockCameraBlockEntity camera))
                        continue;
                    BlockPos pos = camera.getBlockPos();
                    var state = camera.getBlockState();
                    if (state.getValue(BlockCameraBlock.STAGE) == BlockCameraStage.IDLE && reportable(level, pos, player, RANGE)
                            && state.getBlock() instanceof BlockCameraBlock block) {
                        block.lockOn(level, pos, state, player, camera);
                        return;
                    }
                }
            }
    }

    /** Whether the world lets a raid be raised at all: not on Peaceful, not with raids switched off, and not in a dimension without them. */
    static boolean canRaid(ServerLevel level) {
        return level.getDifficulty() != Difficulty.PEACEFUL && !level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)
                && level.dimensionType().hasRaids();
    }

    /**
     * Whether a camera at {@code pos} can report {@code player} now: within {@code range} blocks, still carrying Bad Omen, and not at a
     * raid that has taken every omen it can (the check vanilla's Bad Omen makes in a village).
     */
    static boolean reportable(ServerLevel level, BlockPos pos, ServerPlayer player, int range) {
        if (!player.isAlive() || player.isSpectator() || !player.hasEffect(MobEffects.BAD_OMEN) || !canRaid(level)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) range * range)
            return false;
        Raid raid = level.getRaidAt(pos);
        return raid == null || raid.getRaidOmenLevel() < raid.getMaxRaidOmenLevel();
    }
}
