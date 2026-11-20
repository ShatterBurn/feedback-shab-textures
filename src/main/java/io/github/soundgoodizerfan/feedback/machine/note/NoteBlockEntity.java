// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.note;

import io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * What a note block was built from: its body's pitch and up to {@link NoteTuning#VOICES} noisemakers, each an instrument ({@code _i0..3}, -1 for
 * none), a loudness ({@code _v}) and a pitch shift in octaves ({@code _s}) its material gave. It does nothing on its own: the block reads it
 * when it plays.
 */
public class NoteBlockEntity extends BlockEntity implements FigureHolder {

    private final FigureStore figures = new FigureStore();

    public NoteBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.NOTE_BLOCK.get(), pos, state);
    }

    @Override
    public FigureStore figures() {
        return figures;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
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

    /** The note the body was cut to, or the middle one for a block placed from no design. */
    public int pitch() {
        return Math.round(figures.get("Pitch", NoteTuning.DEFAULT_NOTE));
    }

    /** The instrument of noisemaker {@code i}, or null for none. */
    public NoteBlockInstrument instrument(int i) {
        int o = Math.round(figures.get("_i" + i, -1f));
        return o < 0 || o >= NoteBlockInstrument.values().length ? null : NoteBlockInstrument.values()[o];
    }

    public float volume(int i) {
        return figures.get("_v" + i, NoteTuning.MAX_VOLUME);
    }

    /** Octaves noisemaker {@code i}'s material moves its pitch. */
    public float shift(int i) {
        return figures.get("_s" + i, 0f);
    }

    /** Whether a noisemaker is fitted at all. */
    public boolean any() {
        for (int i = 0; i < NoteTuning.VOICES; i++)
            if (instrument(i) != null)
                return true;
        return false;
    }
}
