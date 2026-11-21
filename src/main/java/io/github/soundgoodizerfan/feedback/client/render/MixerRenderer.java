// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the Mixer's shaft and paddles turning inside the static drum (the block model), at the node's own angle, so the paddles move exactly
 * as fast as the shaft's RPM and coast and stop with it. One model draw per mixer, so it skips Flywheel.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MixerRenderer implements BlockEntityRenderer<MixerBlockEntity> {

    private static final ModelResourceLocation SPINNER = ModelResourceLocation.standalone(Feedback.id("block/mixer_spinner"));

    public MixerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(SPINNER);
    }

    @Override
    public void render(MixerBlockEntity mixer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5f, 0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(mixer.getVisualAngle(partialTick)));
        pose.translate(-0.5f, 0f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, Minecraft.getInstance().getModelManager().getModel(SPINNER), 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(MixerBlockEntity mixer) {
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(mixer.getBlockPos(), mixer.getBlockPos().above());
    }
}
