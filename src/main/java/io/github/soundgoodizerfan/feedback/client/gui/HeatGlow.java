// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.bench.BenchFire;
import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Light a screen throws from the heat of the thing opened: a bloom behind a slot holding glowing stock, and the fire's flicker.
 * Stateless draw helpers; colour is the bench fire's ({@link BenchFire#colour}), so a bar glows here as it does on the anvil.
 * Nothing draws below the faint red band.
 */
public final class HeatGlow {

    private HeatGlow() {
    }

    /** The fire's brightness multiplier, about 0.85 to 1.15, two summed sines; 1 under reduce motion. */
    public static float pulse() {
        if (ClientSettings.reduceMotion())
            return 1f;
        float t = GuiStyle.time();
        return 1f + 0.09f * Mth.sin(t * 0.55f) + 0.06f * Mth.sin(t * 1.17f + 1.3f);
    }

    /** A bloom behind the 16x16 item at (x, y), in the glow colour of a body at {@code tu}; nothing while it is too cool to glow. */
    public static void slot(GuiGraphics g, int x, int y, float tu) {
        if (Incandescence.light(tu) <= 0)
            return;
        float heat = Mth.clamp(tu / ThermalTuning.MAX_VISIBLE_TU.value(), 0.15f, 1f) * pulse();
        int rgb = BenchFire.colour(tu);
        for (int k = 0; k < 3; k++) { // three rings growing outward, fainter each
            int a = (int) Mth.clamp(0x58 * heat / (k + 1), 0, 0xFF);
            g.fill(x - 1 - k, y - 1 - k, x + 17 + k, y + 17 + k, a << 24 | rgb);
        }
    }
}
