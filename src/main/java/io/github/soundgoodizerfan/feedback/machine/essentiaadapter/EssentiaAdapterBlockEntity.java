// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.essentiaadapter;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeEssentia;
import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The one-way fitting from a Thaumaturge essentia store to a Feedback gas. Beside a store it draws a little essentia a second of the aspects it
 * knows and gives, for each unit, {@link #MB_PER_ESSENTIA} mB of the gas of that aspect (and the heat that gas keeps) into its own tank; an aspect
 * it has no gas for is left in the store untouched. It never puts anything back, so there is no way from the gas into essentia.
 *
 * <p>It fails by spilling. A tank that cannot hold what was drawn loses the rest, and what is lost goes into the chunk as flux
 * ({@link #SPILL_FLUX_PER_ESSENTIA} each), so a full tank on a store that keeps feeding it fouls the aura. Reads and moves nothing without the mod.
 * Figures are invented.
 */
public class EssentiaAdapterBlockEntity extends BlockEntity {

    private static final int TANK_MB = 4000, PER_SECOND = 4, MB_PER_ESSENTIA = 40;
    private static final float SPILL_FLUX_PER_ESSENTIA = 0.1f;

    /** An aspect's gas and the temperature (Tu) it leaves at: aer is plain air, aqua steam, ignis air gone hot. */
    private record Gas(java.util.function.Supplier<Fluid> fluid, float tu) {
    }

    private static final Map<String, Gas> GAS = Map.of(
            "aer", new Gas(FFluids.AIR::get, 20f),
            "aqua", new Gas(FFluids.STEAM::get, 120f),
            "ignis", new Gas(FFluids.AIR::get, 600f));

    private final HeatedTank tank = new HeatedTank(TANK_MB, this::setChanged);

    /** A view that gives gas out and takes nothing in. */
    private final IFluidHandler port = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int t) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int t) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int t, FluidStack s) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int max, FluidAction action) {
            return tank.drain(max, action);
        }
    };

    public EssentiaAdapterBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ESSENTIA_ADAPTER.get(), pos, state);
    }

    public IFluidHandler port() {
        return port;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % 20 != 0 || !ThaumaturgeEssentia.present())
            return;
        for (Direction d : Direction.values()) {
            var drawn = ThaumaturgeEssentia.drawAspect(server, worldPosition.relative(d), d.getOpposite(), PER_SECOND, GAS::containsKey);
            if (drawn == null)
                continue;
            Gas gas = GAS.get(drawn.aspect());
            FluidStack out = FluidHeat.stamp(new FluidStack(gas.fluid().get(), drawn.amount() * MB_PER_ESSENTIA), gas.tu());
            int taken = tank.fill(out, IFluidHandler.FluidAction.EXECUTE);
            int spilledEssentia = (out.getAmount() - taken) / MB_PER_ESSENTIA;
            if (spilledEssentia > 0)
                ThaumaturgeFlux.add(server, worldPosition, spilledEssentia * SPILL_FLUX_PER_ESSENTIA);
            return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag);
    }
}
