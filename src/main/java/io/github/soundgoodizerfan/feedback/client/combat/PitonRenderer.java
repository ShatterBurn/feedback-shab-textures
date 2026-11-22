// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.combat.tether.PitonBlock;
import io.github.soundgoodizerfan.feedback.combat.tether.PitonBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the fixed line from a piton to the one it is tied to, from the primary end only so each line is drawn once. The line hangs a little
 * (more the longer it is, less the nearer it is to parting): a fixed line is tied taut, so its sag is small.
 */
public class PitonRenderer implements BlockEntityRenderer<PitonBlockEntity> {

    public PitonRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PitonBlockEntity piton, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (!piton.primary() || piton.getLevel() == null)
            return;
        var level = piton.getLevel();
        Vec3 a = PitonBlock.eye(piton.getBlockState(), piton.getBlockPos());
        Vec3 b = PitonBlock.eye(level.getBlockState(piton.partner()), piton.partner());
        Vec3 offset = b.subtract(a);
        float sag = (float) Math.min(0.6, offset.length() * 0.02 * (1.0 - Math.min(1f, piton.strain())));
        pose.pushPose();
        pose.translate(a.x - piton.getBlockPos().getX(), a.y - piton.getBlockPos().getY(), a.z - piton.getBlockPos().getZ());
        TetherRopes.strip(buffer.getBuffer(RenderType.lineStrip()), pose.last(), (float) offset.x, (float) offset.y, (float) offset.z, sag);
        pose.popPose();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PitonBlockEntity piton) {
        return piton.lineBounds();
    }

    @Override
    public boolean shouldRenderOffScreen(PitonBlockEntity piton) {
        return true;
    }
}
