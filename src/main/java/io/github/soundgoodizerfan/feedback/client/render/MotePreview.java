// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroPlacer;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MoteItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The outline of the voxel a held mote would go in: the same target {@link MicroPlacer#voxelInFront} picks on a click, drawn every frame
 * without changing anything. Prior art: Chisels & Bits' placement preview, design only (a wireframe of the target, the world untouched).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class MotePreview {

    private static final int N = MicroCellBlockEntity.N;

    private MotePreview() {
    }

    @SubscribeEvent
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof MoteItem)
                && !(mc.player.getOffhandItem().getItem() instanceof MoteItem))
            return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos v = MicroPlacer.voxelInFront(hit.getLocation(), hit.getDirection());
        AABB box = new AABB(v.getX() / (double) N, v.getY() / (double) N, v.getZ() / (double) N,
                (v.getX() + 1) / (double) N, (v.getY() + 1) / (double) N, (v.getZ() + 1) / (double) N);
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(event.getPoseStack(), buffers.getBuffer(RenderType.lines()),
                box.move(-camera.x, -camera.y, -camera.z).inflate(0.002), 1f, 1f, 1f, 0.8f);
        buffers.endBatch(RenderType.lines());
    }
}
