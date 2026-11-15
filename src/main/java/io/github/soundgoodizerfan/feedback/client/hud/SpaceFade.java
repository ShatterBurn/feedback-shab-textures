// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.hud;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.space.SpaceTuning;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Hides the dimension swap of a crossing: the screen starts covered and clears. Going up it is black, as the air is
 * gone; coming down it is a hot white-orange that cools, as the air thickens. Not a screen, so no skin: a fill over
 * everything, shorter with reduce-motion on.
 */
public final class SpaceFade {
    private static long start = -1;
    private static boolean up;

    private SpaceFade() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("space_fade"), SpaceFade::render);
    }

    public static void start(boolean goingUp) {
        up = goingUp;
        start = System.currentTimeMillis();
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        if (start < 0)
            return;
        float ms = SpaceTuning.FADE_TICKS * 50f * (ClientSettings.reduceMotion() ? 0.33f : 1f);
        float t = (System.currentTimeMillis() - start) / ms;
        if (t >= 1f) {
            start = -1;
            return;
        }
        int alpha = Mth.clamp(Math.round((1f - t) * (1f - t) * 255), 0, 255);
        // Down: white-orange that reddens as it clears. Up: black.
        int rgb = up ? 0x000000 : (0xFF << 16) | (Mth.clamp(Math.round(220 - 120 * t), 0, 255) << 8) | Mth.clamp(Math.round(190 - 150 * t), 0, 255);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | rgb);
    }
}
