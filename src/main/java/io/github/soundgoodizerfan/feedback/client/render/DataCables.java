// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkStore;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The data wire as a thing in the world. Every link between two data nodes is drawn, all the time, as a wire hanging between the
 * two sockets: depth-tested (a wall hides it, as a wall would hide a real wire), sagging with its length, in bare copper or in the
 * dye the link was coloured with. The Data Connector's outlined sockets and sparks ({@link DataNodeRenderer}) remain what it shows
 * while it is held; this is only the wire itself, and it costs a line strip per link within {@link #RANGE} blocks.
 *
 * <p>Links are read the way the connector's overlay reads them (the nodes of the loaded chunks near the player, each link once, from
 * whichever end sorts first) and rescanned every {@link #RESCAN_TICKS} ticks or when the link store changes.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class DataCables {

    private static final double RANGE = 48;
    private static final int RESCAN_TICKS = 20, SEGMENTS = 12, COPPER = 0xFFB87333;
    private static final float SAG_PER_BLOCK = 0.06f, SAG_MAX = 1.5f;

    private record Cable(Vec3 from, Vec3 to, int colour) {
    }

    private static final List<Cable> CABLES = new ArrayList<>();
    private static long scannedAt = Long.MIN_VALUE;
    private static long scannedEpoch = -1;

    private DataCables() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        long now = mc.level.getGameTime();
        if (now - scannedAt >= RESCAN_TICKS || scannedEpoch != DataLinkStore.epoch()) {
            scan(mc);
            scannedAt = now;
            scannedEpoch = DataLinkStore.epoch();
        }
        if (CABLES.isEmpty())
            return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffer.getBuffer(RenderType.lineStrip());
        for (Cable c : CABLES) {
            if (camera.distanceToSqr(c.from()) > RANGE * RANGE)
                continue;
            pose.pushPose();
            pose.translate(c.from().x - camera.x, c.from().y - camera.y, c.from().z - camera.z);
            Vec3 d = c.to().subtract(c.from());
            float sag = Math.min(SAG_MAX, (float) d.length() * SAG_PER_BLOCK);
            strip(lines, pose.last(), (float) d.x, (float) d.y, (float) d.z, sag, c.colour());
            pose.popPose();
        }
        buffer.endBatch(RenderType.lineStrip());
    }

    private static void scan(Minecraft mc) {
        CABLES.clear();
        List<DataNode> nodes = new ArrayList<>();
        ChunkPos at = mc.player.chunkPosition();
        int radius = (int) (RANGE / 16) + 1;
        for (int cx = at.x - radius; cx <= at.x + radius; cx++)
            for (int cz = at.z - radius; cz <= at.z + radius; cz++) {
                var chunk = mc.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null)
                    continue;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof DataNode node)
                        nodes.add(node);
                    if (be instanceof Fittable fittable)
                        for (Direction side : Direction.values())
                            if (fittable.getSidedFitting(side) instanceof DataNode node)
                                nodes.add(node);
                }
            }
        Set<DataNodeRef> scanned = new HashSet<>();
        for (DataNode node : nodes)
            scanned.add(node.getNodeRef());
        for (DataNode node : nodes) {
            if (!node.isNodeValid())
                continue;
            DataNodeRef self = node.getNodeRef();
            Vec3 from = Vec3.atLowerCornerOf(node.getNodePos()).add(DataNode.boxFor(self.side()).getCenter());
            int tint = DataLinkStore.tintOf(self);
            for (DataNodeRef other : node.getNodeLinks()) {
                // Each link is stored on both ends: draw it once, from whichever end sorts first, or from this end when the other is out of view.
                if (self.compareTo(other) >= 0 && scanned.contains(other))
                    continue;
                Vec3 to = Vec3.atLowerCornerOf(other.pos()).add(DataNode.boxFor(other.side()).getCenter());
                int otherTint = DataLinkStore.tintOf(other);
                int dye = tint >= 0 ? tint : otherTint;
                CABLES.add(new Cable(from, to, dye >= 0 ? 0xFF000000 | DyeColor.byId(dye).getTextureDiffuseColor() : COPPER));
            }
        }
    }

    /** A line strip from the pose's origin to the offset, hanging {@code sag} blocks at its middle. */
    private static void strip(VertexConsumer lines, PoseStack.Pose pose, float dx, float dy, float dz, float sag, int colour) {
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = i / (float) SEGMENTS, next = (i + 1) / (float) SEGMENTS;
            float x = dx * t, y = dy * t - sag * 4f * t * (1f - t), z = dz * t;
            float nx = dx * next - x, ny = dy * next - sag * 4f * next * (1f - next) - y, nz = dz * next - z;
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0f) {
                nx /= len;
                ny /= len;
                nz /= len;
            } else {
                nx = 0f;
                ny = 1f;
                nz = 0f;
            }
            lines.addVertex(pose, x, y, z).setColor(colour).setNormal(pose, nx, ny, nz);
        }
    }
}
