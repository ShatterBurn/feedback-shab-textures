// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.cell.GalvanicCellBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** How much anode a galvanic cell has left, for the Jade line to say in words. */
public class GalvanicServerData implements IServerDataProvider<BlockAccessor> {

    public static final GalvanicServerData INSTANCE = new GalvanicServerData();
    static final String CELL = "GalvanicCell";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof GalvanicCellBlockEntity cell) {
            CompoundTag t = new CompoundTag();
            t.putFloat("Anode", cell.getAnode());
            data.put(CELL, t);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("galvanic");
    }
}
