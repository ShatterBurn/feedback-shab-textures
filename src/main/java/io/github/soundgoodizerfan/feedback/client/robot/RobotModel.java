// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.robot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.robot.RobotSpec;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A robot built from boxes, one set per part, each tinted by its own part's material on the shared grey storage-block sprite
 * (the sprite every metal block is tinted on). The chassis is the body; the drive is wheels, legs or a rotor by its variant; the head
 * sits on top with its tool at the front; the sensor is a probe, a pair of rangefinder eyes or an inertial plate by its variant; a
 * loader is a hopper on the back and a radio a mast with a ball. Absent parts draw nothing.
 *
 * <p>Model space is pixels, y up, the front toward -z, standing on y 0 and centred on x and z. Every face samples the sprite at one texel
 * per model pixel from its corner, so a face is never stretched and a texel is a pixel everywhere (the faces are at most 16 pixels long).
 * Used by the entity renderer and by the item, so a held robot and a standing one are the same thing.
 */
public final class RobotModel {

    private static final ResourceLocation SPRITE = Feedback.id("textures/block/form/block.png");

    private static final int UNTINTED = 0xFFFFFFFF;

    private RobotModel() {
    }

    /** Draws the design held in {@code stack} at the pose's origin; {@code spin} turns a rotor, degrees. Nothing for a stack without a design. */
    public static void draw(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float spin) {
        ModularDesign design = ModularStacks.design(stack);
        if (design == null)
            return;
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(SPRITE));
        pose.pushPose();
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        Brush b = new Brush(out, pose, light, overlay);

        int chassis = colour(design, "C"), head = colour(design, "T");
        ModularDesign.Part driveGear = design.part("D");
        String drive = driveGear == null || driveGear.variant().isEmpty() ? RobotSpec.WHEELS : driveGear.variant();
        int driveColour = colour(design, "D");

        // chassis: the body, a lip round its top and a belly plate
        b.box(chassis, -5, 4, -5, 5, 10, 5);
        b.box(chassis, -5.5f, 9, -5.5f, 5.5f, 10, 5.5f);
        b.box(chassis, -3, 3, -3, 3, 4, 3);

        switch (drive) {
            case RobotSpec.LEGS -> {
                // four jointed legs: thigh out from the body, shin down, foot
                for (int sx = -1; sx <= 1; sx += 2)
                    for (int sz = -1; sz <= 1; sz += 2) {
                        float x = sx * 5.5f, z = sz * 3.5f;
                        float k = x + sx * 1.5f;
                        b.box(driveColour, Math.min(x, k) - 0.5f, 5, z - 1, Math.max(x, k) + 0.5f, 7, z + 1);
                        b.box(driveColour, k - 1, 1, z - 1, k + 1, 7, z + 1);
                        b.box(driveColour, k - 1.5f, 0, z - 1.5f, k + 1.5f, 1, z + 1.5f);
                    }
            }
            case RobotSpec.ROTOR -> {
                // skids under the body and a mast with two blades on top, turning
                for (int sx = -1; sx <= 1; sx += 2) {
                    b.box(driveColour, sx * 3.5f - 0.5f, 1, -5, sx * 3.5f + 0.5f, 4, -4);
                    b.box(driveColour, sx * 3.5f - 0.5f, 1, 4, sx * 3.5f + 0.5f, 4, 5);
                    b.box(driveColour, sx * 3.5f - 0.5f, 0, -6, sx * 3.5f + 0.5f, 1, 6);
                }
                b.box(driveColour, -1, 15, -1, 1, 16.5f, 1);
                pose.pushPose();
                pose.translate(0f, 16.5f, 0f);
                pose.mulPose(Axis.YP.rotationDegrees(spin));
                b.box(driveColour, -7, 0, -1, 7, 0.5f, 1);
                b.box(driveColour, -1, 0.5f, -7, 1, 1, 7);
                pose.popPose();
            }
            default -> {
                // four wheels, a pixel-wide tyre band round each, and axles between
                for (int sx = -1; sx <= 1; sx += 2)
                    for (int sz = -1; sz <= 1; sz += 2) {
                        float x = sx * 6f, z = sz * 3.5f;
                        b.box(driveColour, x - 1.5f, 0, z - 2, x + 1.5f, 4, z + 2);
                        b.box(driveColour, x - 1.75f, 0.5f, z - 2.25f, x + 1.75f, 3.5f, z + 2.25f);
                    }
                b.box(driveColour, -6, 1.5f, -3.5f, 6, 2.5f, -3f);
                b.box(driveColour, -6, 1.5f, 3f, 6, 2.5f, 3.5f);
            }
        }

        // head: a block over the body, its tool a jaw at the front
        b.box(head, -3, 10, -3, 3, 15, 3);
        b.box(head, -1.5f, 10.5f, -7, 1.5f, 12.5f, -3);
        b.box(head, -2.5f, 10.5f, -6, -1.5f, 11.5f, -4);
        b.box(head, 1.5f, 10.5f, -6, 2.5f, 11.5f, -4);

        ModularDesign.Part sensor = design.part("S");
        if (sensor != null) {
            int c = colour(design, "S");
            switch (sensor.variant()) {
                case RobotSpec.RANGEFINDER -> {
                    b.box(c, -2.5f, 12.5f, -3.75f, -0.5f, 14.5f, -3f);
                    b.box(c, 0.5f, 12.5f, -3.75f, 2.5f, 14.5f, -3f);
                    b.box(c, -3, 14.5f, -3.5f, 3, 15.25f, -2);
                }
                case RobotSpec.INERTIAL -> {
                    b.box(c, -2, 15, -2, 2, 16, 2);
                    b.box(c, -0.5f, 16, -0.5f, 0.5f, 16.75f, 0.5f);
                }
                default -> {
                    // a probe: a thin rod reaching ahead of the head
                    b.box(c, -0.5f, 13, -7, 0.5f, 14, -3);
                    b.box(c, -1, 12.75f, -8, 1, 14.25f, -7);
                }
            }
        }
        if (design.part("L") != null) {
            int c = colour(design, "L");
            b.box(c, -3.5f, 10, 5, 3.5f, 13, 7);
            b.box(c, -2.5f, 13, 5.25f, 2.5f, 14, 6.75f);
        }
        if (design.part("R") != null) {
            int c = colour(design, "R");
            b.box(c, 3, 10, 2.5f, 4.5f, 17, 4);
            b.box(c, 2.5f, 17, 2, 5, 19, 4.5f);
        }
        pose.popPose();
    }

    private static int colour(ModularDesign design, String role) {
        ModularDesign.Part part = design.part(role);
        return part == null ? UNTINTED : 0xFF000000 | MaterialColors.of(part.material());
    }

    /** Draws boxes in the pose it was made with. */
    private static final class Brush {

        private final VertexConsumer out;
        private final PoseStack pose;
        private final int light, overlay;

        Brush(VertexConsumer out, PoseStack pose, int light, int overlay) {
            this.out = out;
            this.pose = pose;
            this.light = light;
            this.overlay = overlay;
        }

        void box(int argb, float x0, float y0, float z0, float x1, float y1, float z1) {
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            PoseStack.Pose p = pose.last();
            quad(p, argb, 0, 1, 0, dx, dz, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
            quad(p, argb, 0, -1, 0, dx, dz, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
            quad(p, argb, 0, 0, -1, dx, dy, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
            quad(p, argb, 0, 0, 1, dx, dy, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
            quad(p, argb, -1, 0, 0, dz, dy, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
            quad(p, argb, 1, 0, 0, dz, dy, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        }

        /** One face: corners in order from its lower left, a texel to the pixel, so its {@code across} and {@code up} spans are its sprite share. */
        private void quad(PoseStack.Pose p, int argb, float nx, float ny, float nz, float across, float up, float... c) {
            float u = Math.min(across, 16f) / 16f, v = Math.min(up, 16f) / 16f;
            float[] us = {0f, u, u, 0f}, vs = {v, v, 0f, 0f};
            for (int i = 0; i < 4; i++)
                out.addVertex(p, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(argb).setUv(us[i], vs[i]).setOverlay(overlay)
                        .setLight(light).setNormal(p, nx, ny, nz);
        }
    }
}
