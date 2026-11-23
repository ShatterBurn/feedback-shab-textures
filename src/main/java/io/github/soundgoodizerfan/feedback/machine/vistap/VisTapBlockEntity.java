// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vistap;

import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;
import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeVis;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electricity from a Thaumaturge vis relay network: a source that draws centivis from the network and gives it back as current.
 * Without Thaumaturge, or with no linked relay in range, it has nothing to give.
 *
 * <p>It draws (as a consumer: {@code VisRelayHelper.drainCentivis}) while its buffer has room, and every centivis becomes
 * {@link ElectricMachineTuning#VIS_TAP_EU_PER_CENTIVIS} Eu of the buffer, so the conversion is the tap's own rate, not Thaumaturge's flat
 * one. The line draws the buffer down at the band it is offered at ({@link ElectricMachineTuning#VIS_TAP_BAND}), up to
 * {@link ElectricMachineTuning#VIS_TAP_AMPS} amps. When the load stops the buffer fills and the tap keeps being offered vis: it cannot
 * stop drawing what the network pushes at it, so the surplus backs up as flux in the chunk ({@link ElectricMachineTuning#VIS_TAP_BACKFLOW}
 * a second): the tap fails by fouling the aura, not by switching off, and a load that stays connected keeps it clean.
 */
public class VisTapBlockEntity extends ElectricNode {

    private static final String BUFFER = "Buffer";

    private float buffer;

    public VisTapBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VIS_TAP.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public VoltageBand sourceBand() {
        return buffer >= ElectricRules.packetEu(ElectricMachineTuning.VIS_TAP_BAND) ? ElectricMachineTuning.VIS_TAP_BAND : null;
    }

    @Override
    public float sourceAmps() {
        float packet = ElectricRules.packetEu(ElectricMachineTuning.VIS_TAP_BAND);
        return Math.min(ElectricMachineTuning.VIS_TAP_AMPS, buffer / packet);
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || !ThaumaturgeVis.present())
            return;
        // What the line took this tick leaves the buffer.
        float drawn = getAmps() * ElectricRules.packetEu(ElectricMachineTuning.VIS_TAP_BAND);
        if (drawn > 0f) {
            buffer = Math.max(0f, buffer - drawn);
            setChanged();
        }
        if (level.getGameTime() % ElectricMachineTuning.VIS_TAP_DRAW_EVERY != 0)
            return;
        int offered = ThaumaturgeVis.drainCentivis(server, worldPosition, ElectricMachineTuning.VIS_TAP_PRIMAL,
                ElectricMachineTuning.VIS_TAP_CENTIVIS_PER_DRAW, true);
        if (offered <= 0)
            return;
        if (buffer + offered * ElectricMachineTuning.VIS_TAP_EU_PER_CENTIVIS <= ElectricMachineTuning.VIS_TAP_CAPACITY_EU) {
            int taken = ThaumaturgeVis.drainCentivis(server, worldPosition, ElectricMachineTuning.VIS_TAP_PRIMAL,
                    ElectricMachineTuning.VIS_TAP_CENTIVIS_PER_DRAW, false);
            buffer += taken * ElectricMachineTuning.VIS_TAP_EU_PER_CENTIVIS;
            setChanged();
        } else {
            // Full: the network still pushes, and what cannot be held fouls the chunk.
            ThaumaturgeFlux.add(level, worldPosition, ElectricMachineTuning.VIS_TAP_BACKFLOW * ElectricMachineTuning.VIS_TAP_DRAW_EVERY / 20f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(BUFFER, buffer);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer = tag.getFloat(BUFFER);
    }
}
