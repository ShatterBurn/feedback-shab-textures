// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.blockcamera;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a Block Camera remembers about a lock-on: who it has locked on to and how many blinks are done. It does not tick; it
 * exists so a player's check can find cameras in the chunks around them ({@link BlockCameraWatch}).
 */
public class BlockCameraBlockEntity extends BlockEntity {

    @Nullable
    private UUID target;
    private int blinks;

    public BlockCameraBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.BLOCK_CAMERA.get(), pos, state);
    }

    void lock(UUID player) {
        target = player;
        blinks = 0;
        setChanged();
    }

    void release() {
        target = null;
        blinks = 0;
        setChanged();
    }

    /** Counts one more blink of the lock-on and returns how many have been counted. */
    int blink() {
        blinks++;
        setChanged();
        return blinks;
    }

    @Nullable
    ServerPlayer target(ServerLevel level) {
        return target == null ? null : level.getPlayerByUUID(target) instanceof ServerPlayer p ? p : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null)
            tag.putUUID("Target", target);
        tag.putInt("Blinks", blinks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        target = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        blinks = tag.getInt("Blinks");
    }
}
