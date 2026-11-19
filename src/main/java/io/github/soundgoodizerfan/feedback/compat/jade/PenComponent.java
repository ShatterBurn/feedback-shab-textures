// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A creeper pen in words: empty or occupied, charged, how full the reserve is, what it has eaten, and a warning when primed. */
public class PenComponent implements IBlockComponentProvider {

    public static final PenComponent INSTANCE = new PenComponent();

    private static String fill(float f) {
        return f <= 0.001f ? "empty" : f < 0.34f ? "low" : f < 0.75f ? "half" : "full";
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(PenServerData.PEN))
            return;
        if (!data.getBoolean(PenServerData.PEN)) {
            tooltip.add(Component.translatable("jade.feedback.pen.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable(data.getBoolean(PenServerData.CHARGED) ? "jade.feedback.pen.charged" : "jade.feedback.pen.occupied")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jade.feedback.pen.fill." + fill(data.getFloat(PenServerData.FILL))).withStyle(ChatFormatting.GRAY));
        if (data.getBoolean(PenServerData.OXIDISER) || data.getBoolean(PenServerData.FUEL))
            tooltip.add(Component.translatable("jade.feedback.pen.stomach."
                    + (data.getBoolean(PenServerData.OXIDISER) ? (data.getBoolean(PenServerData.FUEL) ? "both" : "oxidiser") : "fuel"))
                    .withStyle(ChatFormatting.GRAY));
        if (data.getBoolean(PenServerData.PRIMED))
            tooltip.add(Component.translatable("jade.feedback.pen.primed").withStyle(ChatFormatting.RED));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("pen");
    }
}
