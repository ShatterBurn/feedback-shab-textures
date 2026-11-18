// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock;
import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlockEntity;

import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a clutch as three parts, because only half of it turns with the output: a static guard cage ({@code clutch_housing}), the driving
 * half (input shaft and fixed disc, always at the node's angle) and the driven half (output shaft and pressure plate). Engaged, the plate
 * sits against the disc and both halves turn together. Free, the driven half slides three sixteenths toward the output and turns with
 * whatever is downstream, coasting on its own momentum or standing still, which is what {@link ClutchBlock} says a clutch does: it stops
 * driving, it does not stop anything.
 * <p>
 * The block is {@code ENTITYBLOCK_ANIMATED}, so this draws all three parts; it is not in Flywheel's visualizers because one instance
 * cannot turn its halves independently, and one clutch is three cheap model draws.
 * <p>
 * Design from Create (MIT, mc1.21.1/dev 4148fd8): SplitShaftVisual. Taken: draw each half of a split shaft at its own face's rotation.
 * Rejected: its redstone POWERED model swap and its shaft-half models, the Flywheel instancing, and every Create asset (all rights reserved;
 * none used).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClutchRenderer implements BlockEntityRenderer<ClutchBlockEntity> {

    private static final ModelResourceLocation HOUSING = part("clutch_housing"), DRIVING = part("clutch_driving"), DRIVEN = part("clutch_driven");

    /** How far the driven half slides toward the output when the clutch is free, in blocks. */
    private static final float FREE_GAP = 3f / 16f;

    public ClutchRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(Feedback.id("block/" + name));
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(HOUSING);
        event.register(DRIVING);
        event.register(DRIVEN);
    }

    @Override
    public void render(ClutchBlockEntity clutch, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = clutch.getBlockState();
        Direction facing = state.getValue(ClutchBlock.FACING);
        boolean engaged = state.getValue(ClutchBlock.ENGAGED);
        float angle = clutch.getVisualAngle(partialTick);
        float drivenAngle = angle;
        if (!engaged) {
            BlockEntity downstream = clutch.getLevel() == null ? null : clutch.getLevel().getBlockEntity(clutch.getBlockPos().relative(facing));
            drivenAngle = downstream instanceof RotationNode node ? node.getVisualAngle(partialTick) : 0f;
        }
        // The models face north (output toward -Z); turn them about the block's centre so the output points along the facing.
        // Their +Z then points along the positive axis for a negative facing and against it for a positive one.
        float sign = facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1f : -1f;
        pose.pushPose();
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(new Quaternionf().rotationTo(0, 0, -1, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        pose.translate(-0.5f, -0.5f, -0.5f);
        draw(pose, buffers, HOUSING, 0f, 0f, light, overlay);
        draw(pose, buffers, DRIVING, sign * angle, 0f, light, overlay);
        draw(pose, buffers, DRIVEN, sign * drivenAngle, engaged ? 0f : -FREE_GAP, light, overlay);
        pose.popPose();
    }

    /** One part, turned {@code spin} degrees about the block's Z axis through its centre and slid {@code slide} along Z. */
    private static void draw(PoseStack pose, MultiBufferSource buffers, ModelResourceLocation id, float spin, float slide, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        pose.pushPose();
        pose.translate(0f, 0f, slide);
        pose.translate(0.5f, 0.5f, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.translate(-0.5f, -0.5f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, model, 1f, 1f, 1f, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
