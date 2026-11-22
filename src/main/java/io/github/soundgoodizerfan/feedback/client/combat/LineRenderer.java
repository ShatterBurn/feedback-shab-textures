// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.combat.tether.FixedLines;
import io.github.soundgoodizerfan.feedback.combat.tether.LineHost;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the fixed line from a host (piton, winch) to the one it is tied to, from the primary end only so each line is drawn once. The line
 * hangs a little (more the longer it is, less the nearer it is to parting): a fixed line is tied taut, so its sag is small.
 */
public class LineRenderer<T extends BlockEntity & LineHost> implements BlockEntityRenderer<T> {

    public LineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T host, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (!host.primary() || host.getLevel() == null)
            return;
        LineHost other = FixedLines.host(host.getLevel(), host.partner());
        if (other == null)
            return;
        Vec3 a = host.eye();
        Vec3 offset = other.eye().subtract(a);
        float sag = (float) Math.min(0.6, offset.length() * 0.02 * (1.0 - Math.min(1f, host.end().strain())));
        pose.pushPose();
        pose.translate(a.x - host.getBlockPos().getX(), a.y - host.getBlockPos().getY(), a.z - host.getBlockPos().getZ());
        TetherRopes.strip(buffer.getBuffer(RenderType.lineStrip()), pose.last(), (float) offset.x, (float) offset.y, (float) offset.z, sag);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(T host) {
        BlockEntity be = host;
        return host.partner() == null ? new AABB(be.getBlockPos()) : new AABB(be.getBlockPos()).minmax(new AABB(host.partner())).inflate(1);
    }

    @Override
    public boolean shouldRenderOffScreen(T host) {
        return true;
    }
}
