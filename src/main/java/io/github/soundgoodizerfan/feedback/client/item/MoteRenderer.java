// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MoteItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * A mote drawn as a small cube of the block it was cut from, in a slot, the hand, a frame and on the ground; the item model's display entries
 * place it. The cube is drawn at half a block so a mote stays readable in a slot; its true size is a quarter of a block.
 */
public final class MoteRenderer extends BlockEntityWithoutLevelRenderer {

    private static final float SCALE = 0.5f;
    private static MoteRenderer instance;

    private MoteRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static final class Extension implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (instance == null)
                instance = new MoteRenderer();
            return instance;
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var state = MoteItem.state(stack);
        if (state == null)
            return;
        pose.pushPose();
        pose.translate(0.5f - SCALE / 2, 0.5f - SCALE / 2, 0.5f - SCALE / 2);
        pose.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
        pose.popPose();
    }
}
