// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.energyfitting;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The one place another mod's energy leaves a Feedback line: a placed fitting that takes electricity as a sink and gives Forge Energy
 * (FE) out of its faces, at a loss. No machine of ours has a free FE port; foreign power meets ours only here.
 *
 * <p>While the line is at the fitting's band or above and its buffer has room, it asks for {@link ElectricMachineTuning#ENERGY_FITTING_AMPS}
 * amps; what arrives becomes FE at {@link ElectricMachineTuning#ENERGY_FITTING_FE_PER_EU} per Eu and keeps only
 * {@link ElectricMachineTuning#ENERGY_FITTING_EFFICIENCY} of it (the rest is the fitting's heat, not modelled). Each tick it offers its
 * buffer to the blocks touching it that take FE, and the same buffer can be drawn from by anything that extracts. When the foreign
 * load stops taking, the buffer fills and the fitting stops asking the line for current: back-pressure, not a dead stop (a full buffer
 * wastes nothing). A band over its rating charges it at the usual over-voltage cost, as for any store.
 */
public class EnergyFittingBlockEntity extends ElectricNode {

    private static final String STORED = "Stored";

    /** Buffered FE, kept as a float so a small delivery is not lost to rounding. */
    private float stored;

    private final IEnergyStorage view = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int give = Math.min(Math.min(toExtract, (int) stored), ElectricMachineTuning.ENERGY_FITTING_FE_PER_TICK);
            if (give > 0 && !simulate) {
                stored -= give;
                setChanged();
            }
            return Math.max(0, give);
        }

        @Override
        public int getEnergyStored() {
            return (int) stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return ElectricMachineTuning.ENERGY_FITTING_CAPACITY_FE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public EnergyFittingBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ENERGY_FITTING.get(), pos, state);
    }

    public IEnergyStorage storage() {
        return view;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return stored < ElectricMachineTuning.ENERGY_FITTING_CAPACITY_FE
                && ElectricRules.accepts(getBand(), getPacketEu(), ElectricMachineTuning.ENERGY_FITTING_BAND)
                ? ElectricMachineTuning.ENERGY_FITTING_AMPS : 0f;
    }

    public void tickServer() {
        if (level == null)
            return;
        if (getAmps() > 0f) {
            float fe = getAmps() * getPacketEu() * ElectricMachineTuning.ENERGY_FITTING_FE_PER_EU * ElectricMachineTuning.ENERGY_FITTING_EFFICIENCY;
            stored = Math.min(ElectricMachineTuning.ENERGY_FITTING_CAPACITY_FE, stored + fe);
            setChanged();
        }
        if (stored < 1f)
            return;
        // Offer the buffer to whatever touching block takes FE.
        for (Direction side : Direction.values()) {
            IEnergyStorage far = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (far == null || !far.canReceive())
                continue;
            int offered = Math.min((int) stored, ElectricMachineTuning.ENERGY_FITTING_FE_PER_TICK);
            int taken = far.receiveEnergy(offered, false);
            if (taken > 0) {
                stored -= taken;
                setChanged();
            }
            if (stored < 1f)
                return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(STORED, stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getFloat(STORED);
    }
}
