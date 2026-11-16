// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.robot;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** The robot item drawn as the robot it stands as ({@link RobotModel}), in the hand, a slot, a frame and on the ground; the model's display entries place it. */
public final class RobotItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static RobotItemRenderer instance;

    private RobotItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static final class Extension implements IClientItemExtensions {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (instance == null)
                instance = new RobotItemRenderer();
            return instance;
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        // centred on the cell, the model's 15 pixels of height about the middle of it
        pose.translate(0.5f, 0.5f - 7.5f / 16f, 0.5f);
        float spin = (System.currentTimeMillis() % 6000L) * 0.06f;
        RobotModel.draw(stack, pose, buffers, light, overlay, spin);
        pose.popPose();
    }
}
