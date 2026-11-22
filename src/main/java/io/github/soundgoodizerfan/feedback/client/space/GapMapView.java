// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.space.MapData;
import io.github.soundgoodizerfan.feedback.space.SpaceTravel;
import io.github.soundgoodizerfan.feedback.space.SpaceTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Gap's view below the craft: a flat plane of the Overworld as explored, one pixel a chunk, drawn after the sky so the stars
 * stay. The client asks the server for the square around the craft every {@code MAP_REQUEST_INTERVAL_TICKS} while in the Gap, and
 * draws the last answer where its chunks really are, so the plane holds still as the craft moves over it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GapMapView {
    private static final ResourceLocation TEXTURE = Feedback.id("gap_map");
    private static DynamicTexture texture;
    private static int x0, z0, size;
    private static long lastAsk;

    private GapMapView() {
    }

    /** A tile from the server. */
    public static void accept(int tx0, int tz0, int tsize, byte[] colours) {
        if (tsize <= 0 || colours.length < tsize * tsize)
            return;
        if (texture == null || size != tsize) {
            if (texture != null)
                texture.close();
            texture = new DynamicTexture(tsize, tsize, false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        }
        NativeImage image = texture.getPixels();
        for (int z = 0; z < tsize; z++)
            for (int x = 0; x < tsize; x++) {
                int argb = MapData.argb(colours[z * tsize + x]);
                // NativeImage is ABGR.
                image.setPixelRGBA(x, z, (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF));
            }
        texture.upload();
        x0 = tx0;
        z0 = tz0;
        size = tsize;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !mc.level.dimension().equals(SpaceTravel.GAP))
            return;
        long now = mc.level.getGameTime();
        if (now - lastAsk >= SpaceTuning.MAP_REQUEST_INTERVAL_TICKS) {
            lastAsk = now;
            PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.GapMapPayloads.Request(
                    mc.player.blockPosition().getX() >> 4, mc.player.blockPosition().getZ() >> 4));
        }
        if (texture == null || size <= 0)
            return;
        var cam = event.getCamera().getPosition();
        float xa = (float) (x0 * 16 - cam.x), xb = (float) ((x0 + size) * 16 - cam.x);
        float za = (float) (z0 * 16 - cam.z), zb = (float) ((z0 + size) * 16 - cam.z);
        // The plane lies further off the higher the craft is, so the same square of the world shows smaller and coarser from up there.
        float climb = (float) ((cam.y - SpaceTuning.ENTRY_Y) / SpaceTuning.MAP_CLIMB_PER_DEPTH);
        float y = -SpaceTuning.MAP_DEPTH_BLOCKS * net.minecraft.util.Mth.clamp(1f + climb, SpaceTuning.MAP_DEPTH_MIN_SHARE, SpaceTuning.MAP_DEPTH_MAX_SHARE);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        var m = event.getModelViewMatrix();
        buffer.addVertex(m, xa, y, zb).setUv(0f, 1f);
        buffer.addVertex(m, xb, y, zb).setUv(1f, 1f);
        buffer.addVertex(m, xb, y, za).setUv(1f, 0f);
        buffer.addVertex(m, xa, y, za).setUv(0f, 0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
