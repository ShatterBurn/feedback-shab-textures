// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.robot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.robot.RobotEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * A robot is drawn as its own item model, tinted by its design like the item, so a robot looks like what was built and an identical
 * design shares one baked model with its item (the cache is the item model cache; nothing is rebuilt per frame). It turns with the
 * robot's heading and sits on the ground; the item sprite is a flat stand-in until a form model lands.
 */
public class RobotRenderer extends EntityRenderer<RobotEntity> {

    private final ItemRenderer items;

    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.items = context.getItemRenderer();
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(RobotEntity robot, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        ItemStack design = robot.design();
        if (design.isEmpty())
            return;
        pose.pushPose();
        pose.translate(0.0, 0.45, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(180f - entityYaw));
        pose.scale(1.4f, 1.4f, 1.4f);
        items.renderStatic(design, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffer, robot.level(), robot.getId());
        pose.popPose();
        super.render(robot, entityYaw, partial, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RobotEntity robot) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
