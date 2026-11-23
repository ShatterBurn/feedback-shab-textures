// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.thermoelectric;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A thermocouple probe: two legs of different metals joined at a tip on the face it looks at, and the voltage of the junction is the
 * reading, so it needs no power. What it reads is the temperature of the thing at its face (a thermal body's own, else the heat reaching
 * that block), as a fraction of the hottest its weaker leg stands ({@code ServiceTu}); a reading past that saturates. How finely it reads
 * is the Seebeck gap of its two metals: the bigger the gap, the more bands a controller card sees. Nothing is stored but the figures; the
 * reading is taken from the world when a card asks.
 */
public class ThermocoupleProbeBlockEntity extends BlockEntity implements DataNode, ChannelSource {

    /** Seebeck gap (microvolts a kelvin) one band of reading needs; a copper and constantan pair (about 43) reads in about eight. */
    private static final float GAP_PER_BAND = 5f;
    private static final int MIN_BANDS = 2, MAX_BANDS = 16;

    private final FigureStore figures = new FigureStore();

    public ThermocoupleProbeBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.THERMOCOUPLE_PROBE.get(), pos, state);
    }

    private Direction face() {
        return getBlockState().getValue(ThermocoupleProbeBlock.FACING);
    }

    /** The number of bands the pair resolves: more for a wider Seebeck gap, never fewer than two. */
    public int bands() {
        return Math.max(MIN_BANDS, Math.min(MAX_BANDS, Math.round(figures.get("Gap", 0f) / GAP_PER_BAND)));
    }

    @Override
    public float read(int channel) {
        float service = figures.get("ServiceTu", 0f);
        if (level == null || service <= 0f || figures.get("Gap", 0f) <= 0f)
            return 0f;
        BlockPos at = worldPosition.relative(face());
        float temperature = level.getBlockEntity(at) instanceof ThermalBody body ? body.getTemperature().value() : HeatSource.at(level, at).value();
        int bands = bands();
        return Math.round(Math.max(0f, Math.min(1f, temperature / service)) * bands) / (float) bands;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
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
