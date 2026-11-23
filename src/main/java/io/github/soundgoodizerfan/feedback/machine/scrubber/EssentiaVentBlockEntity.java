// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.scrubber;

import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeEssentia;
import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A relief valve with its own gauge for Thaumaturge's essentia stores. Beside a reservoir (or any essentia store) it reads how much is held
 * once a second; past {@link MachineTuning#ESSENTIA_VENT_ABOVE} it opens and bleeds essentia out of it, and it stays open, coasting, until the store
 * has fallen below {@link MachineTuning#ESSENTIA_VENT_RESEAT} before it reseats. What it bleeds is not lost: vented essentia goes into the chunk as
 * flux, so unless a scrubbing bed takes it the world pays for the relief. Reads and moves nothing without the mod.
 */
public class EssentiaVentBlockEntity extends BlockEntity {

    private boolean open;

    public EssentiaVentBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ESSENTIA_VENT.get(), pos, state);
    }

    public boolean open() {
        return open;
    }

    /** The most essentia held by any store this valve touches, -1 when it touches none. */
    public int gauge() {
        if (level == null)
            return -1;
        int most = -1;
        for (Direction d : Direction.values())
            most = Math.max(most, ThaumaturgeEssentia.stored(level, worldPosition.relative(d), d.getOpposite()));
        return most;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0 || !ThaumaturgeEssentia.present())
            return;
        int held = gauge();
        boolean was = open;
        if (held >= MachineTuning.ESSENTIA_VENT_ABOVE)
            open = true;
        else if (held < MachineTuning.ESSENTIA_VENT_RESEAT)
            open = false;
        if (open) {
            int bled = 0;
            for (Direction d : Direction.values()) {
                bled = ThaumaturgeEssentia.draw(server, worldPosition.relative(d), d.getOpposite(), MachineTuning.ESSENTIA_VENT_PER_SECOND);
                if (bled > 0)
                    break;
            }
            if (bled > 0)
                ThaumaturgeFlux.add(server, worldPosition, bled * MachineTuning.ESSENTIA_VENT_FLUX_PER_ESSENTIA);
            server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f);
            server.sendParticles(ParticleTypes.CLOUD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.01);
        }
        if (open != was)
            setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Open", open);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        open = tag.getBoolean("Open");
    }
}
