// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Takes {@code air} from a pipe and, once a second, lets it into the cabin on any open side. Does nothing outside a
 * Sable body or with no open cell beside it. It ticks only to move air, not to wear.
 */
public class AirValveBlockEntity extends BlockEntity {

    private final FluidTank tank = new FluidTank(SpaceTuning.AIR_VALVE_TANK_MB, s -> s.getFluid() == FFluids.AIR.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public AirValveBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.AIR_VALVE.get(), pos, state);
    }

    /** The pipe-facing handler: air in. */
    public IFluidHandler handler() {
        return tank;
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server) || tank.isEmpty() || server.getGameTime() % SpaceTuning.CABIN_RESCAN_TICKS != 0)
            return;
        if (!(SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body))
            return;
        for (Direction d : Direction.values()) {
            var region = Cabin.scan(server, body, worldPosition.relative(d));
            if (region == null || region.holes() > 0 && region.cells() >= SpaceTuning.CABIN_CELL_CAP)
                continue;
            int took = Cabin.add(region, tank.getFluidAmount());
            if (took > 0)
                tank.drain(took, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
