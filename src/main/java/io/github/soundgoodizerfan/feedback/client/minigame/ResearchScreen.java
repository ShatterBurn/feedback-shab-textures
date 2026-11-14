// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.modular.treatise.ResearchTheme;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The research game: a few samples of stuff lie on the bench and a volume's own property (hardness for one, density for another) is to be
 * found out by trying them against each other. Pick two samples to compare them: the bench says which is more, or that it cannot tell (two
 * figures within {@link ModularTuning#RESEARCH_NOISE} of each other read the same). There are only as many comparisons as there are samples,
 * so the order is partly a deduction and partly a guess. Then set the samples in a row, lowest first, and commit: the score is the share of
 * pairs placed the right way round, and it is the quality of the notes of the treatise written. The figures are read from the material table the
 * client holds; the server decides nothing about the play but what it costs. Esc before committing sends nothing and keeps the samples.
 */
final class ResearchScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.WOOD;
    private static final int W = 250, H = 190;

    private final Screen parent;
    private final int containerId;
    private final ResearchTheme.Theme theme;
    private final List<String> names = new ArrayList<>();
    private final List<Double> figures = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private final List<Integer> order = new ArrayList<>();
    private int tests, picked = -1, px, py;
    private boolean ranking, sent;
    private Button compareMode, rankMode, clear, commit;

    ResearchScreen(Screen parent, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        String[] halves = session.data().split(";", 2);
        int index = halves.length > 0 ? parseInt(halves[0]) : 0;
        this.theme = ResearchTheme.of(index);
        var level = Minecraft.getInstance().level;
        var materials = level == null ? java.util.Map.<ResourceLocation, Material>of() : ClientMaterials.on(level);
        if (halves.length > 1)
            for (String id : halves[1].split(",")) {
                ResourceLocation rl = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath("feedback", id);
                Material m = rl == null ? null : materials.get(rl);
                if (m == null)
                    continue;
                var fig = ResearchTheme.figure(theme, m);
                if (fig.isEmpty())
                    continue;
                names.add(Component.translatable("material." + rl.getNamespace() + "." + rl.getPath()).getString());
                figures.add(fig.getAsDouble());
            }
        this.tests = names.size();
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        compareMode = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.compare"), b -> mode(false), px + 8, py + 150, 56, 16));
        rankMode = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.rank"), b -> mode(true), px + 68, py + 150, 56, 16));
        clear = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.clear"), b -> order.clear(), px + 128, py + 150, 52, 16));
        commit = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.commit"), b -> commit(), px + 184, py + 150, 58, 16));
        for (int i = 0; i < names.size(); i++) {
            int n = i;
            addRenderableWidget(GuiStyle.button(SKIN, Component.literal((char) ('A' + i) + "  " + shorten(names.get(i))), b -> sample(n),
                    px + 8 + (i % 2) * 118, py + 50 + (i / 2) * 20, 114, 18));
        }
        mode(false);
    }

    private static String shorten(String s) {
        return s.length() > 14 ? s.substring(0, 13) + "." : s;
    }

    private void mode(boolean rank) {
        ranking = rank;
        picked = -1;
        compareMode.active = rank;
        rankMode.active = !rank;
    }

    private void sample(int i) {
        if (ranking) {
            if (!order.contains(i))
                order.add(i);
            return;
        }
        if (tests <= 0)
            return;
        if (picked < 0 || picked == i) {
            picked = picked == i ? -1 : i;
            return;
        }
        double a = figures.get(picked), b = figures.get(i);
        double big = Math.max(Math.abs(a), Math.abs(b));
        String l = (char) ('A' + picked) + "", r = (char) ('A' + i) + "";
        if (big > 0 && Math.abs(a - b) / big < ModularTuning.RESEARCH_NOISE)
            log.add(l + " and " + r + ": " + Component.translatable("gui.feedback.research.same").getString());
        else
            log.add((a > b ? l : r) + " " + Component.translatable("gui.feedback.research.more").getString() + " " + (a > b ? r : l));
        tests--;
        picked = -1;
    }

    private void commit() {
        if (sent || order.size() < names.size())
            return;
        int pairs = 0, right = 0;
        for (int i = 0; i < order.size(); i++)
            for (int j = i + 1; j < order.size(); j++) {
                pairs++;
                if (figures.get(order.get(i)) <= figures.get(order.get(j)))
                    right++;
            }
        sent = true;
        MinigameScreens.report(containerId, pairs == 0 ? 0f : right / (float) pairs);
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        SKIN.panel(g, px, py, W, H);
        g.drawString(font, title, px + 8, py + 8, SKIN.ink(), false);
        g.drawString(font, Component.translatable(theme.school().itemKey(theme.volume())), px + 8, py + 20, SKIN.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.research.goal", Component.translatable("gui.feedback.research.property." + theme.key())), px + 8, py + 34, SKIN.ink(), false);
        int y = py + 50 + ((names.size() + 1) / 2) * 20 + 4;
        g.drawString(font, Component.translatable("gui.feedback.research.tests", tests), px + 8, y, tests > 0 ? SKIN.accent() : SKIN.warn(), false);
        if (!ranking && picked >= 0)
            g.drawString(font, Component.translatable("gui.feedback.research.against", String.valueOf((char) ('A' + picked))), px + 90, y, SKIN.ink(), false);
        for (int i = 0; i < log.size(); i++)
            g.drawString(font, log.get(i), px + 8, y + 12 + i * 10, SKIN.ink(), false);
        StringBuilder row = new StringBuilder();
        for (int i : order)
            row.append((char) ('A' + i)).append(' ');
        g.drawString(font, Component.translatable("gui.feedback.research.order").getString() + " " + row, px + 8, py + 138, SKIN.ink(), false);
        commit.active = order.size() >= names.size();
        for (var widget : renderables)
            widget.render(g, mx, my, partial);
    }

    /** No vanilla blur (it would blur what is drawn before the widgets): a plain dim behind the panel. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x88000000);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
