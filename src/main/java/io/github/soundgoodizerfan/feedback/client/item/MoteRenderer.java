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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * A mote drawn as a small cube of the block it was cut from, on all its faces: the item model is vanilla's block display ({@code
 * models/item/mote.json}), this draws the carried block's own model at a quarter of a block's width and a little over (6 px of 16),
 * so a stone mote is grey and a log mote shows bark and rings. The block is the {@code feedback:mote} component ({@link MoteItem#state}).
 */
public final class MoteRenderer extends BlockEntityWithoutLevelRenderer {

    /** A mote is 4 texels of a block's 16 on a side (a quarter), drawn a little larger so it reads in a slot. */
    private static final float SCALE = 6f / 16f;

    private static MoteRenderer instance;

    private MoteRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Lets the mote item draw through this renderer. */
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
        BlockState state = MoteItem.state(stack);
        if (state == null)
            return;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, overlay);
        pose.popPose();
    }
}
