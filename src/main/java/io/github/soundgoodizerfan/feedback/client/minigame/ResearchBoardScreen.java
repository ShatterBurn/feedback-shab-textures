// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchPlan;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The research board: a researcher who only knows the old way has to link what they know to a new idea. A hex board holds the concept they own
 * (the start) and, across it, the one they are after (the goal); a hand of concepts lies beside it. Pick a concept, then a free hex beside the
 * chain, and it is tried: a concept that really belongs with its neighbour joins the chain with a rising note, one that does not fizzles, costs
 * a point of ink and strains the specimen. A few joins in a row give an ink back. Reach the goal and the page replays what was found, line by
 * line, and asks the one thing the chain does not say; then a stamp grades the notes. Out of ink first and the notes are scrawl. The result is
 * one {@code ResearchResultPayload}; Esc before the end sends nothing and spends nothing.
 *
 * <p>Inspired by the shape of Thaumcraft's research table (a hex board you link concepts across); no code or assets were taken.
 */
final class ResearchBoardScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.WOOD;
    private static final int W = 350, H = 250, RADIUS = 3, HEX = 14, BOARD_X = 120, BOARD_Y = 138;
    private static final int PLAY = 0, REPLAY = 1, ASK = 2, END = 3;
    private static final int GREEN = 0xFF5DBB63, RED = 0xFFD2452F, GOLD = 0xFFE8B83A, BLOCKED = 0xFF3B2F22, FREE = 0xFFB89A6A, FREE_HI = 0xFFD8BC88, CHAIN = 0xFF8A6A3A;

    private final Screen parent;
    private final int containerId, inkStart;
    private final ResearchPlan plan;
    private final List<String> hand = new ArrayList<>();
    private final Map<Long, String> placed = new HashMap<>();
    private final Map<Long, Long> placedAt = new HashMap<>();
    private final Map<Long, Long> from = new HashMap<>();
    private final Set<Long> blocked = new HashSet<>();
    private final Specks specks = new Specks(160);
    private final long startCell, goalCell;
    private int ink, fizzles, streak, bestStreak, links, phase = PLAY, asked = -1, shownChars;
    private float shakeAt = Float.NEGATIVE_INFINITY, phaseAt;
    private String held;
    private boolean reached, correct, sent;
    private long endedAt;
    private int px, py;

    ResearchBoardScreen(Screen parent, ResearchOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.plan = ResearchPlan.parse("play", session.plan());
        this.ink = this.inkStart = Math.max(3, session.ink());
        this.startCell = key(-RADIUS, 1);
        this.goalCell = key(RADIUS, -1);
        placed.put(startCell, plan.start);
        placedAt.put(startCell, 0L);
        for (String id : plan.labels.keySet())
            if (!id.equals(plan.start) && !id.equals(plan.goal))
                hand.add(id);
        RandomSource rng = RandomSource.create(session.seed());
        // Rubble in the way: a handful of cells the chain must go round, never beside the start or the goal.
        List<Long> cells = new ArrayList<>();
        for (int q = -RADIUS; q <= RADIUS; q++)
            for (int r = -RADIUS; r <= RADIUS; r++)
                if (Math.abs(q + r) <= RADIUS && distance(q, r, -RADIUS, 1) > 1 && distance(q, r, RADIUS, -1) > 1)
                    cells.add(key(q, r));
        java.util.Collections.shuffle(cells, new java.util.Random(session.seed()));
        for (int i = 0; i < 6 && i < cells.size(); i++)
            blocked.add(cells.get(i));
    }

    private static long key(int q, int r) {
        return ((long) q << 32) | (r & 0xFFFFFFFFL);
    }

    private static int q(long k) {
        return (int) (k >> 32);
    }

    private static int r(long k) {
        return (int) k;
    }

    private static int distance(int q1, int r1, int q2, int r2) {
        return (Math.abs(q1 - q2) + Math.abs(r1 - r2) + Math.abs(q1 + r1 - q2 - r2)) / 2;
    }

    private static final int[][] DIRS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    private List<Long> neighbours(long c) {
        List<Long> out = new ArrayList<>(6);
        for (int[] d : DIRS) {
            int nq = q(c) + d[0], nr = r(c) + d[1];
            if (Math.abs(nq) <= RADIUS && Math.abs(nr) <= RADIUS && Math.abs(nq + nr) <= RADIUS)
                out.add(key(nq, nr));
        }
        return out;
    }

    private int cx(long c) {
        return px + BOARD_X + Math.round(HEX * 1.732f * (q(c) + r(c) / 2f));
    }

    private int cy(long c) {
        return py + BOARD_Y + Math.round(HEX * 1.5f * r(c));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        phaseAt = GuiStyle.time();
        rebuild();
    }

    /** The buttons of the phase: the answers while asking, the close button at the end. */
    private void rebuild() {
        clearWidgets();
        if (phase == ASK)
            for (int i = 0; i < plan.options.size(); i++) {
                int n = i;
                addRenderableWidget(GuiStyle.button(SKIN, Component.literal(plan.options.get(i)), b -> answer(n), px + 24, py + 150 + i * 22, W - 48, 18));
            }
        if (phase == END)
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.research.finish"), b -> finish(), px + W / 2 - 40, py + H - 30, 80, 18, null));
    }

    private void ui(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void to(int next) {
        phase = next;
        phaseAt = GuiStyle.time();
        shownChars = 0;
        rebuild();
    }

    // --- play -----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase == PLAY && button == 0) {
            // a concept in the hand
            for (int i = 0; i < hand.size(); i++) {
                int x = px + 232 + (i % 2) * 54, y = py + 52 + (i / 2) * 22;
                if (mx >= x && mx < x + 52 && my >= y && my < y + 20) {
                    String id = hand.get(i);
                    if (placed.containsValue(id))
                        return true;
                    held = id.equals(held) ? null : id;
                    ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.4f);
                    return true;
                }
            }
            long cell = hexAt(mx, my);
            if (cell != Long.MIN_VALUE && held != null)
                place(cell);
            return true;
        }
        if (phase == REPLAY && button == 0) {
            if (shownChars < replayLength())
                shownChars = replayLength();
            else
                to(ASK);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private long hexAt(double mx, double my) {
        for (int q = -RADIUS; q <= RADIUS; q++)
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (Math.abs(q + r) > RADIUS)
                    continue;
                long c = key(q, r);
                double dx = mx - cx(c), dy = my - cy(c);
                if (dx * dx + dy * dy < (HEX * 0.86) * (HEX * 0.86))
                    return c;
            }
        return Long.MIN_VALUE;
    }

    /** Tries the held concept in {@code cell}: it must be free and beside the chain, and really belong with the chain cell it sits beside. */
    private void place(long cell) {
        if (placed.containsKey(cell) || blocked.contains(cell) || cell == goalCell && !adjacentTo(cell, plan.goal))
            return;
        long hold = Long.MIN_VALUE;
        boolean beside = false;
        for (long n : neighbours(cell))
            if (placed.containsKey(n)) {
                beside = true;
                if (plan.related(held, placed.get(n)))
                    hold = n;
            }
        boolean touchesGoal = neighbours(cell).contains(goalCell);
        if (!beside)
            return;
        ink--;
        float cxp = cx(cell), cyp = cy(cell);
        if (hold == Long.MIN_VALUE) {
            fizzles++;
            streak = 0;
            shakeAt = GuiStyle.time();
            specks.burst(cxp, cyp, 14, 1.8f, 0.4f, RED, 18, 2);
            ui(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.9f);
            ui(SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.4f, 0.4f);
        } else {
            placed.put(cell, held);
            placedAt.put(cell, (long) GuiStyle.time());
            from.put(cell, hold);
            links++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
            specks.burst(cxp, cyp, 10 + streak * 3, 1.5f, 0.6f, GOLD, 22, 2);
            ui(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f + 0.09f * links, 0.8f);
            if (streak % 3 == 0) {
                ink++;
                specks.burst(px + 40, py + 30, 12, 1.2f, 0.3f, GREEN, 20, 2);
                ui(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.5f);
            }
            if (plan.related(held, plan.goal) && touchesGoal) {
                reached = true;
                placed.put(goalCell, plan.goal);
                placedAt.put(goalCell, (long) GuiStyle.time());
                from.put(goalCell, cell);
                specks.burst(cx(goalCell), cy(goalCell), 40, 2.4f, 0.8f, GOLD, 30, 3);
                ui(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
                held = null;
                to(REPLAY);
                return;
            }
        }
        held = null;
        if (ink <= 0 && !reached) {
            ui(SoundEvents.ANVIL_LAND, 0.6f, 0.4f);
            to(END);
            endedAt = System.currentTimeMillis();
        }
    }

    private boolean adjacentTo(long cell, String concept) {
        for (long n : neighbours(cell))
            if (concept.equals(placed.get(n)))
                return true;
        return false;
    }

    // --- replay and question ----------------------------------------------------------------

    private String replayText() {
        return String.join("\n", plan.replay);
    }

    private int replayLength() {
        return replayText().length();
    }

    private void answer(int n) {
        correct = n == plan.answer;
        asked = n;
        ui(correct ? SoundEvents.PLAYER_LEVELUP : SoundEvents.VILLAGER_NO, correct ? 1.3f : 0.8f, 0.6f);
        endedAt = System.currentTimeMillis();
        to(END);
    }

    @Override
    public void tick() {
        specks.tick(0.05f);
        if (phase == REPLAY && shownChars < replayLength()) {
            shownChars = Math.min(replayLength(), shownChars + 2);
            if (shownChars % 4 == 0)
                ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f + (shownChars % 3) * 0.1f, 0.12f);
        }
    }

    // --- result -----------------------------------------------------------------------------

    private float quality() {
        if (!reached)
            return 0.05f + 0.2f * Mth.clamp(links / (float) Math.max(1, plan.labels.size() / 2), 0f, 1f);
        float left = Mth.clamp(ink / (float) inkStart, 0f, 1f);
        return Mth.clamp(0.35f + 0.35f * left + 0.1f * Math.min(1f, bestStreak / 4f) + (correct ? 0.2f : 0f), 0f, 1f);
    }

    private int strain() {
        return Mth.clamp(Math.round(fizzles * 140f / inkStart), 0, 100) ;
    }

    private String grade() {
        float q = quality();
        return q >= 0.9f ? "A" : q >= 0.7f ? "B" : q >= 0.5f ? "C" : q >= 0.25f ? "D" : "F";
    }

    private void finish() {
        if (sent)
            return;
        sent = true;
        PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.ResearchResultPayload(containerId, Math.round(quality() * 100f), strain()));
        Minecraft.getInstance().setScreen(parent);
    }

    // --- drawing ----------------------------------------------------------------------------

    private static void hex(GuiGraphics g, int cx, int cy, int size, int colour) {
        for (int dy = -size; dy <= size; dy++) {
            float half = Math.abs(dy) <= size / 2f ? size * 0.866f : (size - Math.abs(dy)) * 1.732f;
            g.fill(Math.round(cx - half), cy + dy, Math.round(cx + half), cy + dy + 1, colour);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x99000000);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g, mx, my, partial);
        int shake = GuiStyle.shake(shakeAt, 3);
        g.pose().pushPose();
        g.pose().translate(shake, shake == 0 ? 0 : -shake / 2f, 0);
        SKIN.panel(g, px, py, W, H);
        g.drawString(font, Component.literal(plan.title), px + 12, py + 8, SKIN.ink(), false);
        if (phase == PLAY)
            drawPlay(g, mx, my, partial);
        else if (phase == REPLAY || phase == ASK)
            drawPage(g);
        else
            drawEnd(g);
        specks.draw(g, partial);
        for (var widget : renderables)
            widget.render(g, mx, my, partial);
        g.pose().popPose();
    }

    private void drawPlay(GuiGraphics g, int mx, int my, float partial) {
        g.drawString(font, Component.translatable("gui.feedback.research.link", plan.labels.get(plan.start), plan.labels.get(plan.goal)), px + 12, py + 20, SKIN.dim(), false);
        // ink pips and the streak
        for (int i = 0; i < inkStart + 3; i++) {
            int x = px + 12 + i * 8;
            boolean full = i < ink;
            if (i < ink || i < inkStart)
                g.fill(x, py + 32, x + 6, py + 38, full ? GOLD : 0xFF4A3B26);
        }
        if (streak >= 2)
            g.drawString(font, Component.translatable("gui.feedback.research.streak", streak), px + 12, py + 42, GOLD, false);
        // links
        for (var e : from.entrySet())
            GuiStyle.line(g, cx(e.getKey()), cy(e.getKey()), cx(e.getValue()), cy(e.getValue()), CHAIN | 0xFF000000);
        long hover = hexAt(mx, my);
        for (int q = -RADIUS; q <= RADIUS; q++)
            for (int r = -RADIUS; r <= RADIUS; r++) {
                if (Math.abs(q + r) > RADIUS)
                    continue;
                long c = key(q, r);
                int x = cx(c), y = cy(c);
                String concept = placed.get(c);
                if (blocked.contains(c)) {
                    hex(g, x, y, HEX - 1, BLOCKED);
                    continue;
                }
                boolean usable = held != null && concept == null && anyPlacedNeighbour(c);
                hex(g, x, y, HEX - 1, concept != null ? 0xFF6B4E2A : usable ? (hover == c ? FREE_HI : FREE) : 0xFF8C7650);
                if (concept != null || c == goalCell) {
                    long at = placedAt.getOrDefault(c, -1L);
                    float age = at < 0 ? 99 : GuiStyle.time() - at;
                    int size = Math.round(HEX - 3 + (age < 6 ? (6 - age) * 0.8f : 0));
                    hex(g, x, y, size, concept == null ? 0xFF5A4A33 : c == startCell ? 0xFF3C6F8F : c == goalCell ? GREEN : 0xFFD6B26A);
                    String label = concept != null ? plan.labels.get(concept) : "?";
                    String shown = font.plainSubstrByWidth(label, 36);
                    g.drawString(font, shown, x - font.width(shown) / 2, y - 3, concept == null ? SKIN.dim() : 0xFF22160A, false);
                }
            }
        g.drawString(font, Component.translatable("gui.feedback.research.hand"), px + 232, py + 40, SKIN.dim(), false);
        for (int i = 0; i < hand.size(); i++) {
            int x = px + 232 + (i % 2) * 54, y = py + 52 + (i / 2) * 22;
            String id = hand.get(i);
            boolean used = placed.containsValue(id);
            int col = used ? 0xFF4A3B26 : id.equals(held) ? GOLD : 0xFF8C7650;
            g.fill(x, y, x + 52, y + 20, col);
            g.renderOutline(x, y, 52, 20, 0xFF2A1D10);
            String label = plan.labels.get(id);
            var lines = font.split(Component.literal(label), 48);
            for (int l = 0; l < Math.min(2, lines.size()); l++)
                g.drawString(font, lines.get(l), x + 3, y + 2 + l * 9, used ? SKIN.dim() : 0xFF22160A, false);
        }
        g.drawString(font, Component.translatable("gui.feedback.research.help"), px + 12, py + H - 14, SKIN.dim(), false);
        if (held != null) {
            int y = py + 52 + ((hand.size() + 1) / 2) * 22 + 6;
            g.drawString(font, Component.translatable("gui.feedback.research.knows", plan.labels.get(held)), px + 232, y, SKIN.ink(), false);
            int line = 0;
            for (String other : plan.labels.keySet())
                if (!other.equals(held) && plan.knows(held, other) && line < 4)
                    g.drawString(font, Component.literal("- " + plan.labels.get(other)), px + 232, y + 10 + 9 * line++, SKIN.dim(), false);
        }
    }

    private boolean anyPlacedNeighbour(long c) {
        for (long n : neighbours(c))
            if (placed.containsKey(n))
                return true;
        return false;
    }

    /** The replay: what the chain found, as handwriting that comes in line by line. */
    private void drawPage(GuiGraphics g) {
        g.fill(px + 14, py + 24, px + W - 14, py + 136, 0xFFEDE3C4);
        g.renderOutline(px + 14, py + 24, W - 28, 112, 0xFF8A6A3A);
        String text = replayText();
        String shown = text.substring(0, Math.min(shownChars, text.length()));
        int y = py + 32;
        for (String paragraph : shown.split("\n", -1)) {
            for (var line : font.split(Component.literal(paragraph), W - 56)) {
                g.drawString(font, line, px + 24, y, 0xFF1F2A5C, false);
                y += 10;
            }
            y += 4;
        }
        if (phase == ASK) {
            g.drawString(font, Component.literal(plan.question), px + 24, py + 138, SKIN.ink(), false);
        } else if (shownChars >= replayLength())
            g.drawString(font, Component.translatable("gui.feedback.research.click"), px + 24, py + 126, 0xFF6C6C70, false);
    }

    private void drawEnd(GuiGraphics g) {
        g.fill(px + 14, py + 24, px + W - 14, py + 150, 0xFFEDE3C4);
        g.renderOutline(px + 14, py + 24, W - 28, 126, 0xFF8A6A3A);
        int y = py + 34;
        g.drawString(font, Component.literal(plan.title), px + 24, y, 0xFF1F2A5C, false);
        y += 16;
        g.drawString(font, Component.translatable(reached ? "gui.feedback.research.found" : "gui.feedback.research.lost"), px + 24, y, 0xFF1F2A5C, false);
        y += 14;
        g.drawString(font, Component.translatable("gui.feedback.research.tally", links, fizzles, ink), px + 24, y, 0xFF1F2A5C, false);
        y += 12;
        if (reached)
            g.drawString(font, Component.translatable(correct ? "gui.feedback.research.right" : "gui.feedback.research.wrong", asked >= 0 ? plan.options.get(plan.answer) : ""), px + 24, y, correct ? 0xFF2E7D32 : 0xFFA33A24, false);
        GuiStyle.stamp(g, Component.literal(grade()), px + W - 70, py + 70, System.currentTimeMillis() - endedAt, quality() >= 0.5f ? 0x2E7D32 : 0xA33A24);
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
