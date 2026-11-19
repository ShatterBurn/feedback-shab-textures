// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.vehicle.micro.TireHubBlock;
import io.github.soundgoodizerfan.feedback.vehicle.micro.TireHubBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** The round tire of a tire hub in coarsespace, drawn about its axle beyond the block; a body turned by a bearing turns it with the block. */
public class TireHubRenderer implements BlockEntityRenderer<TireHubBlockEntity> {

    public TireHubRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The tire's colour: the rubber's, or a dark grey for a material that names none. */
    public static int colour(Material tire) {
        return tire.color().orElse(0x2a2a2c);
    }

    @Override
    public void render(TireHubBlockEntity hub, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = hub.getBlockState();
        if (!(state.getBlock() instanceof TireHubBlock block))
            return;
        Material tire = TireHubBlock.tire(state);
        if (tire == null)
            return;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        double radius = block.radius(false);
        TireMesh.draw(pose, buffers, state.getValue(TireHubBlock.AXIS), radius, Math.min(0.5, radius * 0.5), colour(tire), light);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TireHubBlockEntity hub) {
        return new AABB(hub.getBlockPos()).inflate(1.6);
    }
}
