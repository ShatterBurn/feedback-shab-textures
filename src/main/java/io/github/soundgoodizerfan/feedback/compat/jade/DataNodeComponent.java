// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkStore;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataSpec;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * A data node's kind and the color a Controller linked it with. Both are already on the client (block tags and the mirrored link store),
 * so there is no server data; the player reads which card a node answers to without opening the Programmer.
 */
public class DataNodeComponent implements IBlockComponentProvider {

    public static final DataNodeComponent INSTANCE = new DataNodeComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof DataNode node))
            return;
        for (ResourceLocation kind : new java.util.TreeSet<>(DataSpec.kindsOf(accessor.getBlock())))
            tooltip.add(Component.translatable("feedback.jade.data.kind", Component.translatableWithFallback(
                    "feedback.kind." + kind.getNamespace() + "." + kind.getPath().replace(DataSpec.KIND_PREFIX, "").replace('/', '.'),
                    kind.getPath().replace(DataSpec.KIND_PREFIX, ""))));
        int color = DataLinkStore.tintOf(node.getNodeRef());
        if (color >= 0)
            tooltip.add(Component.translatable("feedback.jade.data.link_color",
                    Component.translatable("color.minecraft." + DyeColor.byId(color).getName())));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("data_node");
    }
}
