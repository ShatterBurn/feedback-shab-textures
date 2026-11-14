// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
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

    private static final int W = 250, H = 190, PAGE = 0xFFEDE3C4, INK = 0xFF1F2A5C, DIM = 0xFF6C6C70, GOOD = 0xFF2E7D32, BAD = 0xFFA33A24;

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

    ResearchScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
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
        compareMode = addRenderableWidget(Button.builder(Component.translatable("gui.feedback.research.compare"), b -> mode(false)).bounds(px + 8, py + 150, 56, 16).build());
        rankMode = addRenderableWidget(Button.builder(Component.translatable("gui.feedback.research.rank"), b -> mode(true)).bounds(px + 68, py + 150, 56, 16).build());
        clear = addRenderableWidget(Button.builder(Component.translatable("gui.feedback.research.clear"), b -> order.clear()).bounds(px + 128, py + 150, 52, 16).build());
        commit = addRenderableWidget(Button.builder(Component.translatable("gui.feedback.research.commit"), b -> commit()).bounds(px + 184, py + 150, 58, 16).build());
        for (int i = 0; i < names.size(); i++) {
            int n = i;
            addRenderableWidget(Button.builder(Component.literal(String.valueOf((char) ('A' + i)) + "  " + shorten(names.get(i))), b -> sample(n))
                    .bounds(px + 8 + (i % 2) * 118, py + 50 + (i / 2) * 20, 114, 18).build());
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
        renderTransparentBackground(g);
        g.fill(px, py, px + W, py + H, PAGE);
        g.renderOutline(px, py, W, H, INK);
        g.drawString(font, title, px + 8, py + 8, INK, false);
        g.drawString(font, Component.translatable(theme.school().itemKey(theme.volume())), px + 8, py + 20, DIM, false);
        g.drawString(font, Component.translatable("gui.feedback.research.goal", Component.translatable("gui.feedback.research.property." + theme.key())), px + 8, py + 34, INK, false);
        int y = py + 50 + ((names.size() + 1) / 2) * 20 + 4;
        g.drawString(font, Component.translatable("gui.feedback.research.tests", tests), px + 8, y, tests > 0 ? GOOD : BAD, false);
        if (!ranking && picked >= 0)
            g.drawString(font, Component.translatable("gui.feedback.research.against", String.valueOf((char) ('A' + picked))), px + 90, y, INK, false);
        for (int i = 0; i < log.size(); i++)
            g.drawString(font, log.get(i), px + 8, y + 12 + i * 10, INK, false);
        StringBuilder row = new StringBuilder();
        for (int i : order)
            row.append((char) ('A' + i)).append(' ');
        g.drawString(font, Component.translatable("gui.feedback.research.order").getString() + " " + row, px + 8, py + 138, INK, false);
        commit.active = order.size() >= names.size();
        super.render(g, mx, my, partial);
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
