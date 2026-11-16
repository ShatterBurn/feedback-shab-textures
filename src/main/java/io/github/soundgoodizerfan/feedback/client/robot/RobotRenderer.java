// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.robot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.robot.RobotEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A robot is drawn by {@link RobotModel} from its design: each part in its own material. It turns with the robot's heading and stands on the ground. */
public class RobotRenderer extends EntityRenderer<RobotEntity> {

    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(RobotEntity robot, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffer, int light) {
        ItemStack design = robot.design();
        if (design.isEmpty())
            return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - entityYaw));
        // the model is up to 15 pixels tall at 16 to the block; 0.75 fits it in the entity's box
        pose.scale(0.75f, 0.75f, 0.75f);
        float spin = robot.spec().flies() ? (robot.tickCount + partial) * 60f : 0f;
        RobotModel.draw(design, pose, buffer, light, OverlayTexture.NO_OVERLAY, spin);
        pose.popPose();
        super.render(robot, entityYaw, partial, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RobotEntity robot) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
