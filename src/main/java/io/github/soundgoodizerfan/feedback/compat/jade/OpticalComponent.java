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

/** An optical terminal's state and its fibre's cleanliness, by words. */
public class OpticalComponent implements IBlockComponentProvider {

    public static final OpticalComponent INSTANCE = new OpticalComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(OpticalServerData.STATE))
            tooltip.add(Component.translatable("feedback.jade.optical.state." + data.getInt(OpticalServerData.STATE)));
        if (data.contains(OpticalServerData.LINK))
            tooltip.add(Component.translatable("feedback.jade.optical.link." + data.getInt(OpticalServerData.LINK)));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("optical");
    }
}
