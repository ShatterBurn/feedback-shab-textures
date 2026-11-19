// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkStore;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws every {@link DataNode} the client has loaded, and the links between them, while a Data Connector is held in either
 * hand. There is no per-block hook: a block that is a data node is found here, so it shows a node the moment it exists.
 * <p>
 * A node is a small socket: a dim shell around a bright core, tinted by the color its Controller gave its link (a node nobody
 * has linked is a cool grey-blue and pulses faster, asking to be wired), and a spark runs along each link. See
 * {@link DeferredDataRenderer} for why this shows through walls.
 */
public final class DataNodeRenderer {

    private static final int UNLINKED = 0xFF7FA6C4, LINKED = 0xFF66CCFF;
    private static final double LINE_THICKNESS = 0.03125;
    /** How far from the player a node is drawn, in blocks. */
    private static final double RANGE = 64;
    /** The scan of loaded chunks is redone this often, in ticks, not every frame. */
    private static final int RESCAN_TICKS = 10;

    private static final List<DataNode> nodes = new ArrayList<>();
    private static final Set<DataNodeRef> scanned = new java.util.HashSet<>();
    private static long scannedAt = Long.MIN_VALUE;
    private static int scannedEpoch = -1;

    private DataNodeRenderer() {
    }

    /** Whether the player holds a Data Connector in either hand. */
    public static boolean holding(net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem().getItem() instanceof DataConnectorItem || player.getOffhandItem().getItem() instanceof DataConnectorItem;
    }

    /** Queues this frame's nodes and links; called once a frame by {@link DataLinkFrame}. */
    static void queue(Minecraft mc, float partial) {
        if (mc.player == null || mc.level == null || !holding(mc.player)) {
            nodes.clear();
            // now - Long.MIN_VALUE overflows negative, so only the epoch can force the rescan on the next hold.
            scannedEpoch = -1;
            return;
        }
        long now = mc.level.getGameTime();
        if (now - scannedAt >= RESCAN_TICKS || scannedEpoch != DataLinkStore.epoch()) {
            scan(mc);
            scannedAt = now;
            scannedEpoch = DataLinkStore.epoch();
        }
        double time = (now + partial) / 20.0;
        Vec3 eye = mc.player.getEyePosition(partial);
        DeferredDataRenderer renderer = DeferredDataRenderer.get();
        for (DataNode node : nodes)
            scanned.add(node.getNodeRef());
        for (DataNode node : nodes) {
            if (!node.isNodeValid())
                continue;
            BlockPos pos = node.getNodePos();
            if (eye.distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE)
                continue;
            drawNode(renderer, node, time);
        }
    }

    private static void scan(Minecraft mc) {
        nodes.clear();
        scanned.clear();
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
    }

    private static void drawNode(DeferredDataRenderer renderer, DataNode node, double time) {
        BlockPos pos = node.getNodePos();
        AABB box = node.getNodeBox();
        DataNodeRef self = node.getNodeRef();
        Set<DataNodeRef> links = node.getNodeLinks();
        boolean linked = !links.isEmpty();
        int tint = DataLinkStore.tintOf(self);
        int colour = tint >= 0 ? dye(tint) : linked ? LINKED : UNLINKED;
        // Slow breathing when wired, a restless blink when nothing has linked it.
        float pulse = linked ? 0.65f + 0.2f * (float) Math.sin(time * 2.0) : 0.45f + 0.35f * (float) Math.sin(time * 6.0 + pos.asLong());
        AABB core = box.deflate(box.getXsize() * 0.28);

        renderer.deferDraw((pose, consumer) -> {
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            Matrix4f m = pose.last().pose();
            renderer.drawColouredBox(m, consumer, box, colour, Mth.clamp(pulse * 0.6f, 0.15f, 0.9f));
            renderer.drawColouredBox(m, consumer, core, brighter(colour), 0.95f);
            pose.popPose();
        });

        Vec3 from = Vec3.atLowerCornerOf(pos).add(box.getCenter());
        for (DataNodeRef other : links) {
            // Each link is stored on both ends; draw it from whichever end sorts first, or from this end when the other is out of view.
            if (self.compareTo(other) >= 0 && scanned.contains(other))
                continue;
            Vec3 to = Vec3.atLowerCornerOf(other.pos()).add(DataNode.boxFor(other.side()).getCenter());
            int lineColour = tint >= 0 ? colour : tintOfEnd(other, colour);
            renderer.deferDraw((pose, consumer) -> drawLine(renderer, pose, consumer, from, to, lineColour));
            // A spark running from one end to the other.
            double t = (time * 0.6 + (self.pos().asLong() & 7) / 8.0) % 1.0;
            Vec3 spark = from.lerp(to, t);
            AABB dot = new AABB(spark, spark).inflate(0.06);
            renderer.deferDraw((pose, consumer) -> renderer.drawColouredBox(pose.last().pose(), consumer, dot, brighter(lineColour), 1f));
        }
    }

    private static int tintOfEnd(DataNodeRef other, int fallback) {
        int tint = DataLinkStore.tintOf(other);
        return tint >= 0 ? dye(tint) : fallback;
    }

    private static int dye(int id) {
        return 0xFF000000 | DyeColor.byId(id).getTextureDiffuseColor();
    }

    private static int brighter(int argb) {
        int r = Math.min(255, ((argb >> 16 & 255) + 255) / 2), g = Math.min(255, ((argb >> 8 & 255) + 255) / 2), b = Math.min(255, ((argb & 255) + 255) / 2);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** A line of any colour between two world points, for the connector's rubber-band wire too. */
    public static void drawLine(DeferredDataRenderer renderer, PoseStack pose, VertexConsumer consumer,
                                Vec3 from, Vec3 to, int colour) {
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        Vec3 delta = to.subtract(from);
        double yaw = Math.atan2(-delta.z, delta.x) + Math.PI;
        double pitch = Math.atan2(delta.horizontalDistance(), delta.y) + Mth.HALF_PI;
        pose.mulPose(Axis.YP.rotation((float) yaw));
        pose.mulPose(Axis.ZP.rotation((float) pitch));
        Matrix4f matrix = pose.last().pose();
        AABB box = new AABB(0, -LINE_THICKNESS, -LINE_THICKNESS, delta.length(), LINE_THICKNESS, LINE_THICKNESS);
        renderer.drawColouredBox(matrix, consumer, box, colour, 0.8f);
        pose.popPose();
    }
}
