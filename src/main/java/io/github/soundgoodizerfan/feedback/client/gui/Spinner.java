// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A GUI sprite turning about its centre at a real speed, for any screen or skin: a paddle, a rake, a fan, a gear, a flywheel. One
 * instance per spinning thing keeps its angle, which advances by the time between draws (the GUI clock, so it holds still while
 * paused) times the RPM given, so the speed can change every frame and the sprite never jumps. Fast turning smears: above a speed it
 * is drawn again behind itself at trailing angles with falling opacity, more copies and a longer trail the faster it goes, so the
 * blur is made of the sprite's own pixels. Under reduce motion it stands at a fixed angle with no smear. Draw only; nothing is
 * allocated per frame. Sprites should be even-sized with their centre at the exact middle and a roughly round silhouette.
 */
public final class Spinner {

    /** Degrees a turn at 1 RPM advances per game tick (360 / 60 s / 20 ticks). */
    private static final float DEG_PER_TICK_PER_RPM = 0.3f;
    /** Degrees a tick below which no smear is drawn, and per extra trailing copy above it. */
    private static final float BLUR_FROM = 14f, BLUR_STEP = 14f;
    private static final int MAX_GHOSTS = 3;
    /** Opacity of the first trailing copy, falling by half each copy. */
    private static final float GHOST_ALPHA = 0.35f;

    private float angle;
    private float lastTime = Float.NaN;

    /** The angle in degrees now (positive turns clockwise on screen). */
    public float angle() {
        return angle;
    }

    /**
     * Draw {@code sprite} {@code w} by {@code h} centred on ({@code cx}, {@code cy}), turned at {@code rpm} (negative turns the other
     * way). Call once per frame.
     */
    public void draw(GuiGraphics g, ResourceLocation sprite, int cx, int cy, int w, int h, float rpm) {
        float now = GuiStyle.time();
        float dt = Float.isNaN(lastTime) ? 0f : Mth.clamp(now - lastTime, 0f, 4f);
        lastTime = now;
        if (ClientSettings.reduceMotion()) {
            blit(g, sprite, cx, cy, w, h, 30f, 1f);
            return;
        }
        float perTick = rpm * DEG_PER_TICK_PER_RPM;
        angle = (angle + perTick * dt) % 360f;
        float speed = Math.abs(perTick);
        int ghosts = speed < BLUR_FROM ? 0 : Math.min(MAX_GHOSTS, 1 + (int) ((speed - BLUR_FROM) / BLUR_STEP));
        float trail = Math.min(speed, 40f); // the smear trails behind the turn by about a tick of travel, capped so it stays a tail and not a haze
        for (int k = ghosts; k >= 1; k--) { // oldest copy first so the newest sits on top
            float a = GHOST_ALPHA * (float) Math.pow(0.5, k - 1); // the copy nearest the sprite is the strongest
            blit(g, sprite, cx, cy, w, h, angle - Math.signum(perTick) * trail * k / ghosts, a);
        }
        blit(g, sprite, cx, cy, w, h, angle, 1f);
    }

    /** Draw {@code sprite} turned {@code degrees} about its centre at {@code alpha}, with no state: a fixed pose. */
    public static void blit(GuiGraphics g, ResourceLocation sprite, int cx, int cy, int w, int h, float degrees, float alpha) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
        if (alpha < 1f) {
            RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, alpha);
        }
        g.blitSprite(sprite, -w / 2, -h / 2, w, h);
        if (alpha < 1f)
            g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
    }
}
