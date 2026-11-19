// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * A round tire about an axle: a ring of {@link #SEGMENTS} flat segments (outer tread, inner wall and two sidewalls), tinted by its rubber.
 * Allocates nothing per frame: the ring's angles are tables and each vertex is written straight from them.
 */
public final class TireMesh {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    private static final int SEGMENTS = 24;
    private static final float[] COS = new float[SEGMENTS + 1], SIN = new float[SEGMENTS + 1];

    static {
        for (int k = 0; k <= SEGMENTS; k++) {
            COS[k] = (float) Math.cos(2 * Math.PI * k / SEGMENTS);
            SIN[k] = (float) Math.sin(2 * Math.PI * k / SEGMENTS);
        }
    }

    private TireMesh() {
    }

    /**
     * Draws the tire centred on the origin of {@code pose}, its axle along {@code axis}; {@code radius} is the tread, {@code width} the
     * tread's length along the axle, {@code rgb} the rubber's colour.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, Direction.Axis axis, double radius, double width, int rgb, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        float outer = (float) radius, inner = (float) (radius * 0.55), half = (float) (width / 2);
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        // the ring lies in the plane across the axle: axis X uses (y, z), Y uses (z, x), Z uses (x, y)
        for (int k = 0; k < SEGMENTS; k++) {
            float c0 = COS[k], s0 = SIN[k], c1 = COS[k + 1], s1 = SIN[k + 1];
            float cm = (c0 + c1) / 2, sm = (s0 + s1) / 2;
            // outer tread, then inner wall, then the two sidewalls
            quad(pose, vc, axis, c0, s0, c1, s1, outer, outer, -half, half, cm, sm, 0, r, g, b, light);
            quad(pose, vc, axis, c1, s1, c0, s0, inner, inner, -half, half, -cm, -sm, 0, r, g, b, light);
            quad(pose, vc, axis, c0, s0, c1, s1, inner, outer, half, half, 0, 0, 1, r, g, b, light);
            quad(pose, vc, axis, c1, s1, c0, s0, inner, outer, -half, -half, 0, 0, -1, r, g, b, light);
        }
    }

    /**
     * One quad between two ring angles: the first edge at radius {@code ra} and axle offset {@code sa}, the second at {@code rb} and
     * {@code sb} (for a side wall they differ in radius, for the tread and wall in offset). {@code nu}, {@code nv} is the normal in the ring's
     * plane and {@code na} along the axle.
     */
    private static void quad(PoseStack pose, VertexConsumer vc, Direction.Axis axis, float c0, float s0, float c1, float s1, float ra, float rb,
                             float sa, float sb, float nu, float nv, float na, int r, int g, int b, int light) {
        vertex(pose, vc, axis, c0 * ra, s0 * ra, sa, 0, 0, nu, nv, na, r, g, b, light);
        vertex(pose, vc, axis, c1 * ra, s1 * ra, sa, 1, 0, nu, nv, na, r, g, b, light);
        vertex(pose, vc, axis, c1 * rb, s1 * rb, sb, 1, 1, nu, nv, na, r, g, b, light);
        vertex(pose, vc, axis, c0 * rb, s0 * rb, sb, 0, 1, nu, nv, na, r, g, b, light);
    }

    private static void vertex(PoseStack pose, VertexConsumer vc, Direction.Axis axis, float u, float v, float a, float tu, float tv,
                               float nu, float nv, float na, int r, int g, int b, int light) {
        float x, y, z, nx, ny, nz;
        switch (axis) {
            case X -> { x = a; y = u; z = v; nx = na; ny = nu; nz = nv; }
            case Y -> { x = v; y = a; z = u; nx = nv; ny = na; nz = nu; }
            default -> { x = u; y = v; z = a; nx = nu; ny = nv; nz = na; }
        }
        vc.addVertex(pose.last(), x, y, z).setColor(r, g, b, 255).setUv(tu, tv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose.last(), nx, ny, nz);
    }
}
