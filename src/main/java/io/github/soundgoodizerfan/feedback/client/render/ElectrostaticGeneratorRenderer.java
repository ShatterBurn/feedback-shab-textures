// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.electrostatic.ElectrostaticGeneratorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the electrostatic generator's wheel (disc, hub, crank and knob, {@code models/block/electrostatic_generator_wheel.json})
 * turning about its centre while a pulse runs. The base, pillars and balls are the block's own static model. The wheel keeps the
 * block's metal tint, taken from the block colour handler.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ElectrostaticGeneratorRenderer implements BlockEntityRenderer<ElectrostaticGeneratorBlockEntity> {

    private static final ModelResourceLocation WHEEL =
            ModelResourceLocation.standalone(Feedback.id("block/electrostatic_generator_wheel"));

    public ElectrostaticGeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(WHEEL);
    }

    @Override
    public void render(ElectrostaticGeneratorBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = be.getBlockState();
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(WHEEL);
        int colour = be.getLevel() == null ? -1
                : Minecraft.getInstance().getBlockColors().getColor(state, be.getLevel(), be.getBlockPos(), 0);
        float r = colour == -1 ? 1f : (colour >> 16 & 255) / 255f;
        float g = colour == -1 ? 1f : (colour >> 8 & 255) / 255f;
        float b = colour == -1 ? 1f : (colour & 255) / 255f;
        pose.pushPose();
        // Turns about the disc's centre line (8, 8 sixteenths) on Z; positive reads clockwise from the crank side.
        pose.translate(0.5f, 0.5f, 0f);
        pose.mulPose(Axis.ZP.rotationDegrees(be.spinAngle(partialTick)));
        pose.translate(-0.5f, -0.5f, 0f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, model, r, g, b, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
