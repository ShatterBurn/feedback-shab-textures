// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The how-to window of a research game: a small sheet over the play area with a title, {@value #STEPS} short steps and a close line, shown the first
 * time the game is opened in a game session (a static set of game keys, nothing saved) and brought back by a "?" button. Each game writes its own
 * words in its own voice under {@code <prefix>.title}, {@code .1} to {@code .3} and {@code .close}, and passes its own colours to {@link #draw}, so
 * the window is the screen's own sheet and not the skin's. The lines are wrapped once in {@link #init}; nothing is allocated in the draw path.
 *
 * <p>A screen holds one, calls {@link #init} from its {@code init}, lets {@link #click} eat the first click, adds {@link #button} and calls
 * {@link #draw} after its widgets.
 */
final class Tutorial {

    static final int STEPS = 3;
    private static final Set<String> SEEN = new HashSet<>();

    private final String key, prefix;
    private final int lineHeight;
    private boolean showing;
    private List<FormattedCharSequence> lines = List.of();
    private int height;

    /** @param key which game (shown once per session per key); @param prefix the lang prefix of the text; @param lineHeight the line pitch in pixels */
    Tutorial(String key, String prefix, int lineHeight) {
        this.key = key;
        this.prefix = prefix;
        this.lineHeight = lineHeight;
        this.showing = !SEEN.contains(key);
    }

    /** Wraps the steps to {@code wrap} pixels; called from the screen's {@code init}, which reruns on resize. */
    void init(Font font, int wrap) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (int i = 1; i <= STEPS; i++)
            out.addAll(font.split(Component.translatable(prefix + "." + i), wrap));
        lines = out;
        height = 36 + out.size() * lineHeight + 8;
    }

    boolean showing() {
        return showing;
    }

    /** A click while the window is up closes it and is eaten; returns whether it was. */
    boolean click() {
        if (!showing)
            return false;
        showing = false;
        SEEN.add(key);
        return true;
    }

    /** The "?" button that brings the window back. */
    Button button(GuiSkin skin, int x, int y) {
        return GuiStyle.button(skin, Component.literal("?"), b -> showing = true, x, y, 18, 14);
    }

    /** Draws the window at {@code x}, {@code y}, {@code w} wide, in the game's own colours; draws nothing when it is not showing. */
    void draw(GuiGraphics g, Font font, int x, int y, int w, int fill, int edge, int title, int ink, int dim) {
        if (!showing)
            return;
        g.fill(x, y, x + w, y + height, fill);
        g.renderOutline(x, y, w, height, edge);
        g.drawString(font, Component.translatable(prefix + ".title"), x + 12, y + 8, title, false);
        for (int i = 0; i < lines.size(); i++)
            g.drawString(font, lines.get(i), x + 12, y + 24 + i * lineHeight, ink, false);
        g.drawString(font, Component.translatable(prefix + ".close"), x + 12, y + height - 12, dim, false);
    }
}
