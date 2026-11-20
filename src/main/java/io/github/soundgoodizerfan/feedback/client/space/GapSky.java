// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Gap's sky, drawn in place of vanilla's: a hard sun, a dense starfield with a milky band, the Overworld's atmosphere as a
 * limb glow below the horizon, and the destination planets at fixed bearings. Everything is plain coloured geometry on a sphere
 * around the camera (no textures, no shader of ours), so it costs a few thousand vertices a frame. The explored map plane
 * ({@link GapMapView}) is drawn over the lower glow afterwards, at the chunks' real coordinates.
 * <p>
 * shortcut: the mesh is rebuilt every frame through the Tesselator, upload it once into a VertexBuffer if the sky ever shows up in a profile.
 */
final class GapSky {
    /** The sphere the sky is painted on, blocks from the camera; depth test is off, so only the draw order matters. */
    private static final float R = 100f;
    private static final int STARS = 2600, BAND_STARS = 3400;
    private static final long SEED = 0x6A9C0DEL;

    /** The sun: a bearing, and so also the direction every planet is lit from. */
    private static final Vector3f SUN = dir(40f, 35f);

    /** A destination at a fixed bearing: angular size is {@code radius / R}. Colours are the two ends of the cloud bands. */
    private record Planet(float yaw, float pitch, float radius, int bandA, int bandB, float bands, float rim) {
    }

    private static final Planet[] PLANETS = {
            // Acid-cloud world: ochre and sulphur, thick hazy rim.
            new Planet(205f, 18f, 7f, 0xFFD9A441, 0xFF8F5B22, 5f, 0.5f),
            // Frozen moon: pale blue-white, no cloud, a faint rim.
            new Planet(112f, 27f, 4f, 0xFFDCE9F5, 0xFFA9C4DE, 0f, 0.2f),
            // Crystal world: violet and teal.
            new Planet(305f, 41f, 3f, 0xFF8A5CD6, 0xFF3FB6B0, 0f, 0.3f),
            // The End's gas giant: low on the horizon, banded, not large.
            new Planet(265f, 4f, 5.5f, 0xFFC87A3C, 0xFFEBD0A0, 9f, 0.25f),
    };

    private static float[] stars; // x, y, z, size, r, g, b, a per star

    private GapSky() {
    }

    /** Draws the whole sky. {@code view} is the camera's rotation, so directions here are fixed in the world. */
    static void render(Matrix4f view) {
        if (stars == null)
            stars = buildStars();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Light that adds: atmosphere, stars, the sun's glow.
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        BufferBuilder add = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        limb(add, view);
        stars(add, view);
        glow(add, view, SUN, 55f, 0xFFFFD9A0, 0.55f);
        BufferUploader.drawWithShader(add.buildOrThrow());

        // Solid things over it: the planets, then the sun's disc.
        RenderSystem.defaultBlendFunc();
        BufferBuilder solid = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Planet p : PLANETS)
            planet(solid, view, p);
        disc(solid, view, SUN, 3.6f, 0xFFFFFFF0, 0xFFFFF2C8);
        BufferUploader.drawWithShader(solid.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    // ---- the Overworld below ----

    /** Atmosphere seen from above: a bright blue band at the horizon, thinning to nothing straight down, a trace above it. */
    private static void limb(BufferBuilder b, Matrix4f m) {
        float[] pitch = { 7f, 0f, -6f, -18f, -40f, -90f };
        float[] alpha = { 0f, 0.55f, 0.42f, 0.18f, 0.06f, 0.03f };
        int segs = 64;
        for (int i = 0; i < pitch.length - 1; i++)
            for (int s = 0; s < segs; s++) {
                float ya = s * 360f / segs, yb = (s + 1) * 360f / segs;
                vert(b, m, dir(ya, pitch[i]), tint(alpha[i], i == 1 ? 0xFFB5DBFF : 0xFF4C8FEA));
                vert(b, m, dir(yb, pitch[i]), tint(alpha[i], i == 1 ? 0xFFB5DBFF : 0xFF4C8FEA));
                vert(b, m, dir(yb, pitch[i + 1]), tint(alpha[i + 1], 0xFF3A78D8));
                vert(b, m, dir(ya, pitch[i + 1]), tint(alpha[i + 1], 0xFF3A78D8));
            }
    }

    // ---- stars ----

    private static float[] buildStars() {
        Random rnd = new Random(SEED);
        float[] out = new float[(STARS + BAND_STARS) * 8];
        Vector3f n = new Vector3f(0.35f, 0.8f, 0.5f).normalize();
        Vector3f e1 = new Vector3f(n).cross(0f, 1f, 0f).normalize();
        Vector3f e2 = new Vector3f(n).cross(e1).normalize();
        int[] tints = { 0xFFB4C8FF, 0xFFFFFFFF, 0xFFFFF0D2, 0xFFFFC8A0 };
        for (int i = 0; i < STARS + BAND_STARS; i++) {
            Vector3f d;
            float b;
            if (i < STARS) {
                d = new Vector3f(rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 2 - 1);
                if (d.lengthSquared() > 1f || d.lengthSquared() < 1e-4f) {
                    i--;
                    continue;
                }
                d.normalize();
                b = (float) (Math.pow(rnd.nextFloat(), 3) * 0.8 + 0.2);
            } else {
                // A great circle, thickened by a bell: the galaxy seen edge on, many faint stars.
                double t = rnd.nextDouble() * Math.PI * 2;
                d = new Vector3f(e1).mul((float) Math.cos(t)).add(new Vector3f(e2).mul((float) Math.sin(t)))
                        .add(new Vector3f(n).mul((float) (rnd.nextGaussian() * 0.11))).normalize();
                b = 0.15f + rnd.nextFloat() * 0.3f;
            }
            int c = tints[rnd.nextInt(tints.length)];
            int o = i * 8;
            out[o] = d.x;
            out[o + 1] = d.y;
            out[o + 2] = d.z;
            out[o + 3] = 0.15f + 0.4f * b;
            out[o + 4] = (c >> 16 & 255) / 255f;
            out[o + 5] = (c >> 8 & 255) / 255f;
            out[o + 6] = (c & 255) / 255f;
            out[o + 7] = Math.min(1f, b);
        }
        return out;
    }

    private static void stars(BufferBuilder b, Matrix4f m) {
        for (int o = 0; o < stars.length; o += 8) {
            Vector3f d = new Vector3f(stars[o], stars[o + 1], stars[o + 2]);
            float s = stars[o + 3];
            int c = argb(stars[o + 7], stars[o + 4], stars[o + 5], stars[o + 6]);
            Vector3f r = side(d);
            Vector3f u = new Vector3f(d).cross(r);
            quadPoint(b, m, d, r, u, s, c);
        }
    }

    private static void quadPoint(BufferBuilder b, Matrix4f m, Vector3f d, Vector3f r, Vector3f u, float s, int c) {
        float cx = d.x * R, cy = d.y * R, cz = d.z * R;
        b.addVertex(m, cx - r.x * s - u.x * s, cy - r.y * s - u.y * s, cz - r.z * s - u.z * s).setColor(c);
        b.addVertex(m, cx + r.x * s - u.x * s, cy + r.y * s - u.y * s, cz + r.z * s - u.z * s).setColor(c);
        b.addVertex(m, cx + r.x * s + u.x * s, cy + r.y * s + u.y * s, cz + r.z * s + u.z * s).setColor(c);
        b.addVertex(m, cx - r.x * s + u.x * s, cy - r.y * s + u.y * s, cz - r.z * s + u.z * s).setColor(c);
    }

    // ---- sun and planets ----

    /** A soft additive halo: full {@code alpha} at the centre, none at {@code radius}. */
    private static void glow(BufferBuilder b, Matrix4f m, Vector3f c, float radius, int colour, float alpha) {
        fan(b, m, c, radius, 10, (x, y, r) -> tint(alpha * (1f - r) * (1f - r), colour));
    }

    private static void disc(BufferBuilder b, Matrix4f m, Vector3f c, float radius, int centre, int edge) {
        fan(b, m, c, radius, 4, (x, y, r) -> lerp(centre, edge, r));
    }

    private static void planet(BufferBuilder b, Matrix4f m, Planet p) {
        Vector3f c = dir(p.yaw, p.pitch);
        Vector3f r = side(c);
        Vector3f u = new Vector3f(c).cross(r);
        // A dim halo first, for the atmosphere.
        fan(b, m, c, p.radius * 1.5f, 8, (x, y, rad) -> {
            float k = rad * 1.5f;
            return k < 1f ? 0 : tint(p.rim * (1f - (k - 1f) / 0.5f) * 0.35f, lerp(p.bandA, p.bandB, 0.3f));
        });
        fan(b, m, c, p.radius, 10, (x, y, rad) -> {
            float z = (float) Math.sqrt(Math.max(0f, 1f - x * x - y * y));
            // Normal of the sphere point, in world space; the camera sits at the origin looking at the planet.
            Vector3f n = new Vector3f(r).mul(x).add(new Vector3f(u).mul(y)).sub(new Vector3f(c).mul(z));
            float lit = 0.05f + 0.95f * Math.max(0f, n.dot(SUN));
            float band = p.bands > 0f
                    ? 0.5f + 0.5f * (float) Math.sin(y * p.bands * 1.7 + 1.3 * Math.sin(y * p.bands * 4.1 + x * 2.0))
                    : 0.5f + 0.5f * (float) Math.sin(x * 6.0 + 2.0 * Math.sin(y * 5.0)) * (float) Math.cos(y * 4.0);
            int col = lerp(p.bandA, p.bandB, band);
            return argb(1f, (col >> 16 & 255) / 255f * lit, (col >> 8 & 255) / 255f * lit, (col & 255) / 255f * lit);
        });
    }

    // ---- geometry ----

    private interface Shade {
        /** Colour at a point of the disc: {@code x}, {@code y} in -1..1, {@code r} the distance from the centre, 0..1. */
        int at(float x, float y, float r);
    }

    /** A disc facing the camera at bearing {@code c}, angular size {@code radius / R}, as rings of quads. */
    private static void fan(BufferBuilder b, Matrix4f m, Vector3f c, float radius, int rings, Shade shade) {
        Vector3f r = side(c);
        Vector3f u = new Vector3f(c).cross(r);
        int segs = 32;
        for (int i = 0; i < rings; i++) {
            float r0 = (float) i / rings, r1 = (float) (i + 1) / rings;
            for (int s = 0; s < segs; s++) {
                double a0 = s * Math.PI * 2 / segs, a1 = (s + 1) * Math.PI * 2 / segs;
                ring(b, m, c, r, u, radius, r0, a0, shade);
                ring(b, m, c, r, u, radius, r0, a1, shade);
                ring(b, m, c, r, u, radius, r1, a1, shade);
                ring(b, m, c, r, u, radius, r1, a0, shade);
            }
        }
    }

    private static void ring(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f r, Vector3f u, float radius, float rad, double a,
            Shade shade) {
        float x = (float) Math.cos(a) * rad, y = (float) Math.sin(a) * rad;
        float px = c.x * R + (r.x * x + u.x * y) * radius;
        float py = c.y * R + (r.y * x + u.y * y) * radius;
        float pz = c.z * R + (r.z * x + u.z * y) * radius;
        b.addVertex(m, px, py, pz).setColor(shade.at(x, y, rad));
    }

    private static void vert(BufferBuilder b, Matrix4f m, Vector3f d, int colour) {
        b.addVertex(m, d.x * R, d.y * R, d.z * R).setColor(colour);
    }

    /** A unit vector: {@code yaw} turns about the vertical, {@code pitch} 0 is the horizon, 90 straight up. */
    private static Vector3f dir(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vector3f((float) (Math.cos(p) * Math.sin(y)), (float) Math.sin(p), (float) (-Math.cos(p) * Math.cos(y)));
    }

    /** A unit vector at right angles to {@code d} and level (any vector when {@code d} is vertical). */
    private static Vector3f side(Vector3f d) {
        Vector3f r = new Vector3f(d).cross(0f, 1f, 0f);
        return r.lengthSquared() < 1e-6f ? new Vector3f(1f, 0f, 0f) : r.normalize();
    }

    private static int argb(float a, float r, float g, float b) {
        return (Math.round(Math.min(1f, a) * 255) << 24) | (Math.round(Math.min(1f, r) * 255) << 16)
                | (Math.round(Math.min(1f, g) * 255) << 8) | Math.round(Math.min(1f, b) * 255);
    }

    /** {@code colour}'s rgb at {@code alpha}. */
    private static int tint(float alpha, int colour) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (colour & 0xFFFFFF);
    }

    private static int lerp(int a, int b, float t) {
        int out = 0xFF000000;
        for (int sh = 16; sh >= 0; sh -= 8)
            out |= Math.round((a >> sh & 255) * (1f - t) + (b >> sh & 255) * t) << sh;
        return out;
    }
}
