// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.thermoelectric;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A thermoelectric generator: two legs of different metals, a hot side and a cold side, and a trickle of LV between them. The voltage a degree
 * of difference drives is the gap between the metals' Seebeck figures, so the pair is the design: copper against constantan makes a good pair,
 * the same metal twice makes nothing. The temperature difference is the hot side's (whatever {@link HeatSource} reaches the block) against the
 * cold side's (a cryocooler's cold head, water or ice touching it, else the air), and the legs hold only some share of it (a stubby leg
 * conducts the heat straight through). Power goes with the square of the voltage: twice the difference, four times the current, up to what the
 * legs' section can carry. Past the weaker leg's service temperature the legs fail and it supplies nothing.
 *
 * <p>Nothing here is stored but the figures: the reading is recomputed every {@link #REFRESH_TICKS} ticks, not every tick.
 */
public class ThermoelectricGeneratorBlockEntity extends ElectricNode {

    /** How often the temperatures are read again. A generator that sits and runs costs one read a second. */
    private static final int REFRESH_TICKS = 20;
    /** A thermocouple's voltage (microvolts) at which the legs deliver all their section can carry; under it current falls with its square. */
    private static final float FULL_EMF = 20000f;
    /** What water against the block holds the cold side to, and what ice does, in Tu. */
    private static final float WATER_TU = 10f, ICE_TU = 0f;

    private final FigureStore figures = new FigureStore();
    private long readAt = Long.MIN_VALUE;
    private float amps;

    public ThermoelectricGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.THERMOELECTRIC_GENERATOR.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public VoltageBand sourceBand() {
        return figures.get("Gap", 0f) > 0f && read() > 0f ? VoltageBand.LV : null;
    }

    @Override
    public float sourceAmps() {
        return read();
    }

    /** The current the legs give now, from the hot and cold sides, read again when it is stale. */
    private float read() {
        if (level == null || level.isClientSide)
            return 0f;
        long now = level.getGameTime();
        if (now - readAt < REFRESH_TICKS)
            return amps;
        readAt = now;
        float hot = HeatSource.at(level, this).value();
        float service = figures.get("ServiceTu", 0f);
        if (service > 0f && hot > service) {
            amps = 0f;
            return 0f;
        }
        float dt = Math.max(0f, hot - cold()) * figures.get("Hold", 1f);
        float emf = figures.get("Gap", 0f) * dt;
        float share = Math.min(1f, (emf / FULL_EMF) * (emf / FULL_EMF));
        amps = figures.get("Supply", 0f) * share;
        return amps;
    }

    /** The cold side: a cryocooler's cold head, else the coldest of water or ice touching the block, else the air. */
    private float cold() {
        float cold = Math.min(HeatSource.coldAt(level, this).value(), AmbientTable.at(level).value());
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            if (level.getFluidState(at).is(FluidTags.WATER))
                cold = Math.min(cold, WATER_TU);
            if (level.getBlockState(at).is(BlockTags.ICE))
                cold = Math.min(cold, ICE_TU);
        }
        return cold;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }
}
