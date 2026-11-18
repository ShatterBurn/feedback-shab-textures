// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.robot.RobotArmBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a robot arm's head (turret, arm and gripper, {@code models/block/robot_arm_head.json}) swung round the pedestal to the
 * arm's facing. The base and pedestal are the block's own static model; the blockstate's facing changes at once, so the head's angle
 * eases toward it in {@link RobotArmBlockEntity#visualYaw}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RobotArmRenderer implements BlockEntityRenderer<RobotArmBlockEntity> {

    private static final ModelResourceLocation HEAD = ModelResourceLocation.standalone(Feedback.id("block/robot_arm_head"));

    public RobotArmRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(HEAD);
    }

    @Override
    public void render(RobotArmBlockEntity arm, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(HEAD);
        pose.pushPose();
        // The model faces north; the angle is clockwise from above, and a positive turn about +Y is anticlockwise, so it is negated.
        pose.translate(0.5f, 0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-arm.visualYaw(partialTick)));
        pose.translate(-0.5f, 0f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
