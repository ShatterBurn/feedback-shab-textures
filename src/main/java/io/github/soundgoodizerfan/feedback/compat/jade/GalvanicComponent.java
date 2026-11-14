// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** A galvanic cell's anode by words (spent, nearly spent, half, plenty); the share needs a gauge. */
public class GalvanicComponent implements IBlockComponentProvider {

    public static final GalvanicComponent INSTANCE = new GalvanicComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag t = accessor.getServerData().getCompound(GalvanicServerData.CELL);
        if (t.isEmpty())
            return;
        float anode = t.getFloat("Anode");
        tooltip.add(Component.translatable(anode <= 0f ? "feedback.jade.galvanic.spent" : anode < 0.15f ? "feedback.jade.galvanic.low"
                : anode < 0.6f ? "feedback.jade.galvanic.half" : "feedback.jade.galvanic.plenty"));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("galvanic");
    }
}
