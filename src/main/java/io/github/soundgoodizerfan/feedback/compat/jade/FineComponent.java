// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

/**
 * A finespace cell names the mote under the cursor, not "Finespace": the block the voxel is made of (a wedge's skin, a hosted machine's
 * own block). The voxel is the one the ray hit ({@link MicroCellBlockEntity#pick}), all on the client where the cell is synced.
 */
public class FineComponent implements IBlockComponentProvider {

    public static final FineComponent INSTANCE = new FineComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof MicroCellBlockEntity cell))
            return;
        var hit = accessor.getHitResult();
        int i = cell.pick(hit.getLocation(), hit.getDirection());
        if (i < 0)
            return;
        BlockState voxel = cell.skin(i) != null ? cell.skin(i) : cell.get(i);
        if (voxel == null)
            return;
        tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(Component.translatable("feedback.jade.mote", voxel.getBlock().getName())));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("finespace");
    }
}
