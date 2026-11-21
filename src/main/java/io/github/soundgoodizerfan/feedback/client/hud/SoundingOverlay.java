// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.hud;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Outlines the ore a sounding pulse found, through walls, until its time runs out. */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SoundingOverlay {

    private static List<BlockPos> found = List.of();
    private static long until;

    private SoundingOverlay() {
    }

    public static void show(List<BlockPos> positions, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        found = positions;
        // Each ore is outlined in the colour of the material it is an ore of; an ore with no material of its own keeps the pulse's amber.
        colours = new int[positions.size()];
        for (int i = 0; i < colours.length; i++)
            colours[i] = io.github.soundgoodizerfan.feedback.client.item.FormTint.oreColour(mc.level.getBlockState(positions.get(i))).orElse(AMBER);
        until = mc.level.getGameTime() + ticks;
    }

    private static final int AMBER = 0xFFD933;
    private static int[] colours = new int[0];

    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mc.level == null || found.isEmpty())
            return;
        if (mc.level.getGameTime() > until) {
            found = List.of();
            return;
        }

        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2f);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        for (int i = 0; i < found.size() && i < colours.length; i++) {
            int c = colours[i];
            LevelRenderer.renderLineBox(pose, builder, new AABB(found.get(i)).inflate(0.002), (c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, 1f);
        }
        MeshData data = builder.build();
        if (data != null)
            BufferUploader.drawWithShader(data);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
