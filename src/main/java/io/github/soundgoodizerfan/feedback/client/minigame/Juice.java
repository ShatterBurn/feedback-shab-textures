// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The feel shared by the research games: a shake, a tinted flash, a flicker and a rising pitch for a streak. One per screen; call {@link #tick} from
 * the screen's {@code tick}. Shake and the flash layers are motion, so both are off under reduce motion (sounds are not, and {@link #climb} stays).
 * Nothing is allocated after construction.
 */
final class Juice {

    private float shakeAt = Float.NEGATIVE_INFINITY;
    private int shakeAmp;
    private int flashLeft, flashPower, flashRgb, flickerLeft, flickerRgb, flickerPower;

    /** Shakes whatever the screen draws offset by {@link #dx} and {@link #dy}. */
    void shake(int amplitude) {
        shakeAt = GuiStyle.time();
        shakeAmp = amplitude;
    }

    int dx() {
        return GuiStyle.shake(shakeAt, shakeAmp);
    }

    int dy() {
        return -dx() / 2;
    }

    /** A tint over the drawn area that fades by {@code power} alpha per tick for {@code ticks} ticks. */
    void flash(int rgb, int ticks, int power) {
        flashLeft = ticks;
        flashPower = power;
        flashRgb = rgb & 0xFFFFFF;
    }

    /** A tint that blinks on and off every other tick, dying out over {@code ticks}. */
    void flicker(int rgb, int ticks, int power) {
        flickerLeft = ticks;
        flickerPower = power;
        flickerRgb = rgb & 0xFFFFFF;
    }

    void tick() {
        if (flashLeft > 0)
            flashLeft--;
        if (flickerLeft > 0)
            flickerLeft--;
    }

    /** Draws the flash and the flicker over a rectangle. */
    void draw(GuiGraphics g, int x0, int y0, int x1, int y1) {
        if (ClientSettings.reduceMotion())
            return;
        if (flashLeft > 0)
            g.fill(x0, y0, x1, y1, Math.min(170, flashLeft * flashPower) << 24 | flashRgb);
        if (flickerLeft > 0 && (flickerLeft / 2) % 2 == 0)
            g.fill(x0, y0, x1, y1, Math.min(170, flickerLeft * flickerPower) << 24 | flickerRgb);
    }

    /** A pitch that climbs with a streak: each step is a little over a semitone, capped so it never squeals. */
    static float climb(float base, int streak) {
        return base * (1f + 0.07f * Math.min(Math.max(streak, 0), 8));
    }

    /** One colour mixed toward another, {@code t} from 0 to 1. */
    static int mix(int from, int to, float t) {
        float u = Math.max(0f, Math.min(1f, t));
        int r = (int) (((from >> 16) & 0xFF) * (1 - u) + ((to >> 16) & 0xFF) * u);
        int gr = (int) (((from >> 8) & 0xFF) * (1 - u) + ((to >> 8) & 0xFF) * u);
        int b = (int) ((from & 0xFF) * (1 - u) + (to & 0xFF) * u);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }
}
