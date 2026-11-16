// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.diagram;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Cause;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Contraption Diagram, opened by using a Flight Sensor: the body it sits on drawn as a blueprint with
 * the forces acting on it. The legend toggles each cause; the buttons set merged or separate arrows, the
 * centre of mass, the body alone or the whole joined assembly, and the angle. The page is
 * {@link DiagramPage}; this screen is its frame ({@link GuiSkin#BLUEPRINT}, a plain screen like
 * {@code CreativeSourceScreen}).
 */
public class DiagramScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.BLUEPRINT;
    private static final int LEGEND = 126, PAD = 10;

    private final Opening opening = new Opening(SKIN);
    private final DiagramPage page;

    public DiagramScreen(BlockPos sensor) {
        super(Component.translatable("screen.feedback.diagram"));
        this.page = new DiagramPage(sensor);
    }

    private int side() {
        return Math.max(128, Math.min(BodyRender.SIZE, height - 70));
    }

    private int w() {
        return PAD * 3 + LEGEND + side();
    }

    private int h() {
        return side() + 52;
    }

    private int left() {
        return (width - w()) / 2;
    }

    private int top() {
        return (height - h()) / 2;
    }

    @Override
    protected void init() {
        opening.start();
        int x = left() + PAD, y = top() + 24;
        for (Cause c : Cause.values()) {
            var box = GuiStyle.checkbox(SKIN, Component.translatable(c.key()), () -> page.shown.contains(c), b -> {
                if (!page.shown.remove(c))
                    page.shown.add(c);
            }, x, y, LEGEND);
            addRenderableWidget(box);
            y += 16;
        }
        y += 6;
        addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.diagram.merge"), () -> page.merge,
                b -> page.merge = !page.merge, x, y, LEGEND));
        addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.diagram.centre"), () -> page.centre,
                b -> page.centre = !page.centre, x, y + 18, LEGEND));
        addRenderableWidget(GuiStyle.toggle(SKIN, Component.translatable("screen.feedback.diagram.assembly"), () -> page.assembly,
                b -> page.assembly = !page.assembly, x, y + 36, LEGEND));
        int px = left() + PAD * 2 + LEGEND, by = top() + 24 + side() + 6;
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("<"), b -> page.turn(-0.3f, 0), px, by, 18, 14));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal(">"), b -> page.turn(0.3f, 0), px + 20, by, 18, 14));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("^"), b -> page.turn(0, -0.2f), px + 40, by, 18, 14));
        addRenderableWidget(GuiStyle.button(SKIN, Component.literal("v"), b -> page.turn(0, 0.2f), px + 60, by, 18, 14));
    }

    @Override
    public void tick() {
        page.tick();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        Component[] tip = new Component[1];
        opening.render(g, width, height, x, y, w(), h(), 0f, () -> {
            SKIN.panel(g, x, y, w(), h());
            g.drawString(font, title, x + PAD, y + 10, SKIN.ink(), false);
            int ly = y + 24;
            for (Cause c : Cause.values()) {
                g.fill(x + PAD - 4, ly + 3, x + PAD, ly + 11, 0xFF000000 | c.colour());
                ly += 16;
            }
            // The sensor's own readings under the toggles, as it reads them (quantised, signed); live with the diagram's refresh.
            var data = page.data();
            if (data != null && data.onBody() && !data.flight().isEmpty()) {
                int fy = y + 24 + Cause.values().length * 16 + 6 + 3 * 18 + 8;
                g.drawString(font, Component.translatable("screen.feedback.diagram.flight"), x + PAD, fy, SKIN.dim(), false);
                for (Component line : data.flight())
                    for (var part : font.split(line, LEGEND + 4)) {
                        fy += 10;
                        g.drawString(font, part, x + PAD, fy, SKIN.ink(), false);
                    }
            }
            tip[0] = page.draw(g, SKIN, font, x + PAD * 2 + LEGEND, y + 24, side(), mx, my, partial);
            g.drawString(font, page.massText(), x + PAD * 2 + LEGEND + 88, y + 24 + side() + 9, SKIN.ink(), false);
            super.render(g, mx, my, partial);
        });
        if (tip[0] != null && opening.settled())
            g.renderTooltip(font, tip[0], mx, my);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public void removed() {
        page.close();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return !opening.settled() || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        return opening.closing() || super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
