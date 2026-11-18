// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity;
import io.github.soundgoodizerfan.feedback.item.ArcWelderItem;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the workpiece lying on the anvil, and the head dropping onto it.
 * <p>
 * The workpiece is not decoration. Beat 1 ships no measuring tool because a part-worked ingot is meant to be
 * readable by eye, and that requires the player be able to see it sitting in the machine rather
 * than having to pull it out to find out what it has become.
 * <p>
 * The moving part ({@code models/block/mechanical_hammer_head.json}: a hammer head block and striking face on a bronze guide rod) is a separate
 * standalone model over the block's static body, drawn only while a head is seated (with none the machine does nothing and wears nothing).
 * The head is modelled in grey and tinted by the seated head's wearing material ({@link MaterialColors}); the rod keeps its own colour. Its height is a pure function of the driving crank
 * linkage's angle, so it moves exactly as fast as the crank turns, slows with the shaft's inertia and stops when it stops, with no second
 * counter to drift from the one the linkage already owns. Each landed stroke sends a block event and the hammer marks the linkage angle at
 * impact, so the bottom of the drop lines up with the blow and its sound. A short throw drops less far than a long one.
 * <p>
 * Design from Create (MIT, mc1.21.1/dev 4148fd8): MechanicalPressRenderer / PressVisual / PressingBehaviour.getRenderedHeadOffset. Taken: the
 * moving head is a separate registered partial model drawn over a static body, offset by a 0..1 value lerped with the partial tick, falling
 * on a cube law (slow then fast) and returning linearly. Rejected: its machine-owned running-tick counter (the phase here comes from the crank
 * linkage's network angle), Flywheel instancing for a single head, recipe-owned cycle timing, and every Create asset (all rights reserved; none used).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class MechanicalHammerRenderer implements BlockEntityRenderer<MechanicalHammerBlockEntity> {

    /** Top of the anvil slab, which is three pixels tall, plus a hair so it does not z-fight. */
    private static final float ANVIL_SURFACE = 3.05f / 16f;

    private static final ModelResourceLocation HEAD = ModelResourceLocation.standalone(Feedback.id("block/mechanical_hammer_head"));

    /** How far the head travels from its rest (striking face at y=6) toward the anvil (y=3), in blocks, for each throw. */
    private static final float DROP_LONG = 3f / 16f, DROP_SHORT = 2f / 16f;

    /** Fraction of a stroke over which the head rises after impact, and where it starts falling again; the rest is rest at the top. */
    private static final float RISE_END = 0.30f, FALL_START = 0.65f;

    /** Strokes per degree of linkage rotation: the server counts strokes per tick per rpm, the node turns degrees per tick per rpm. */
    private static final float STROKES_PER_DEGREE = MachineTuning.STROKES_PER_RPM_PER_TICK / RotationNode.DEGREES_PER_TICK_PER_RPM;

    private final ItemRenderer itemRenderer;

    public MechanicalHammerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(HEAD);
    }

    @Override
    public void render(MechanicalHammerBlockEntity hammer, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!hammer.getHead().isEmpty())
            drawHead(hammer, partialTick, pose, buffers, light, overlay);

        ItemStack workpiece = hammer.getWorkpiece();
        if (workpiece.isEmpty())
            return;

        pose.pushPose();
        pose.translate(0.5f, ANVIL_SURFACE, 0.5f);
        pose.mulPose(Axis.XP.rotationDegrees(90));   // lay it flat instead of standing it upright
        pose.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(workpiece, ItemDisplayContext.FIXED, light, overlay, pose, buffers,
                hammer.getLevel(), 0);
        pose.popPose();
    }

    private static void drawHead(MechanicalHammerBlockEntity hammer, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        float drop = 0;
        CrankLinkageBlockEntity linkage = CrankLinkageBlockEntity.driving(hammer.getLevel(), hammer.getBlockPos());
        if (linkage != null) {
            float travelled = linkage.getVisualAngle(partialTick) - hammer.getImpactAngle();
            if (linkage.getRpm().value() < 0)
                travelled = -travelled;
            float phase = Mth.positiveModulo(travelled * STROKES_PER_DEGREE, 1f);
            float depth = linkage.getThrow() == Throw.SHORT ? DROP_SHORT : DROP_LONG;
            drop = depth * offset(phase);
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(HEAD);
        pose.pushPose();
        pose.translate(0f, -drop, 0f);
        // Only the faces the model marks as tinted take this colour (the head); the guide rod stays bronze.
        ResourceLocation material = ArcWelderItem.wearMaterial(hammer.getLevel(), hammer.getHead().get());
        int rgb = material == null ? 0xB0B0B0 : MaterialColors.of(material);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderTypeHelper.getEntityRenderType(RenderType.solid(), false)),
                null, model, (rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }

    /** 0 at rest (top), 1 at impact. Phase 0 is the instant a stroke lands: it rises, rests, then falls on a cube law to land again at phase 1. */
    private static float offset(float phase) {
        if (phase < RISE_END)
            return 1f - phase / RISE_END;
        if (phase < FALL_START)
            return 0f;
        float t = (phase - FALL_START) / (1f - FALL_START);
        return t * t * t;
    }
}
