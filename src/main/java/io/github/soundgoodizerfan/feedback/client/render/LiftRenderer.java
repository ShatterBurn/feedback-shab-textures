// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The Lift's moving top. The block's own model ({@code lift}) is the housing, the cylinder's base and the console; the platform with its brass
 * brackets ({@code lift_top}) is drawn here, always, {@code height} blocks above where it sits at rest, so it rides up with the vehicle, and a
 * bronze ram ({@code lift_column}, one block long) is drawn between the cylinder and the platform's underside. Client only: nothing is placed
 * in the world (the structure is levitated and extra colliders would destabilise it); {@code LiftBlockEntity} sends {@code height} in its
 * update tag, and the vehicle is pinned at exactly that height above the lift's top, so the platform meets its underside.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LiftRenderer implements BlockEntityRenderer<LiftBlockEntity> {

    private static final ModelResourceLocation TOP = ModelResourceLocation.standalone(Feedback.id("block/lift_top"));
    private static final ModelResourceLocation RAM = ModelResourceLocation.standalone(Feedback.id("block/lift_column"));
    /** The cylinder's top, where the ram starts: 12 of 16 up the block. */
    private static final float CYLINDER_TOP = 12f / 16f;

    public LiftRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(TOP);
        event.register(RAM);
    }

    @Override
    public void render(LiftBlockEntity lift, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (lift.getLevel() == null)
            return;
        int height = lift.height();
        var manager = Minecraft.getInstance().getModelManager();
        VertexConsumer consumer = buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false));
        BakedModel ram = manager.getModel(RAM);
        for (int k = 0; k < height; k++)
            draw(pose, consumer, ram, CYLINDER_TOP + k, LevelRenderer.getLightColor(lift.getLevel(), lift.getBlockPos().above(k + 1)));
        draw(pose, consumer, manager.getModel(TOP), height,
                height == 0 ? light : LevelRenderer.getLightColor(lift.getLevel(), lift.getBlockPos().above(height)));
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, BakedModel model, float up, int light) {
        pose.pushPose();
        pose.translate(0f, up, 0f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model,
                1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** The platform rises above the block, so the culling box does too. */
    @Override
    public AABB getRenderBoundingBox(LiftBlockEntity lift) {
        return new AABB(lift.getBlockPos()).expandTowards(0, lift.height() + 1, 0);
    }
}
