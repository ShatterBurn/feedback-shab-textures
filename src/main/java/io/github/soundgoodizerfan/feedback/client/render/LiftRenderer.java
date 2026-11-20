// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.LiftBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
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
 * The Lift's column: while the lift stands {@code height} blocks up, its central shaft is drawn as a column from the lift's top face
 * up to the lifted body, with the platform plates riding on top where the vehicle sits. Client only, nothing is placed in the world
 * (the structure is levitated and extra colliders would destabilise it); {@code LiftBlockEntity} sends {@code height} in its update tag.
 * <p>
 * The column is one block-long standalone model ({@code lift_column}) drawn once per block of height, the platform a second
 * ({@code lift_platform}). Not drawn at rest: the block's own model already has its platform and shaft.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LiftRenderer implements BlockEntityRenderer<LiftBlockEntity> {

    private static final ModelResourceLocation COLUMN = ModelResourceLocation.standalone(Feedback.id("block/lift_column"));
    private static final ModelResourceLocation PLATFORM = ModelResourceLocation.standalone(Feedback.id("block/lift_platform"));
    /** The platform plate is four sixteenths thick. */
    private static final float PLATFORM_THICKNESS = 4f / 16f;

    public LiftRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(COLUMN);
        event.register(PLATFORM);
    }

    @Override
    public void render(LiftBlockEntity lift, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int height = lift.height();
        if (height <= 0 || lift.getLevel() == null)
            return;
        var manager = Minecraft.getInstance().getModelManager();
        var consumer = buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false));
        for (int i = 1; i <= height; i++)
            draw(lift, pose, consumer, manager.getModel(COLUMN), i, 0f);
        draw(lift, pose, consumer, manager.getModel(PLATFORM), height, 1f - PLATFORM_THICKNESS);
    }

    /** One model {@code up} blocks above the lift block, and {@code extra} of a block higher, lit by the air it stands in. */
    private static void draw(LiftBlockEntity lift, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer consumer, BakedModel model,
                             int up, float extra) {
        int light = LevelRenderer.getLightColor(lift.getLevel(), lift.getBlockPos().above(up));
        pose.pushPose();
        pose.translate(0f, up + extra, 0f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model,
                1f, 1f, 1f, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** The column rises above the block, so the culling box does too. */
    @Override
    public AABB getRenderBoundingBox(LiftBlockEntity lift) {
        return new AABB(lift.getBlockPos()).expandTowards(0, lift.height() + 1, 0);
    }
}
