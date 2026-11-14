// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchBoard;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchPlan;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The research board: a researcher who only knows the old way has to link what they know to a new idea. A hex board holds the concept they own
 * (the start) and, across it, the one they are after (the goal); a hand of concepts lies beside it. Pick a concept, then a free hex beside the
 * chain, and it is tried ({@link ResearchBoard}): a concept that really belongs with its neighbour joins the chain with a rising note, one that
 * does not fizzles, costs a point of ink and strains the specimen. A few joins in a row give an ink back. Reach the goal and the page replays what
 * was found, line by line, and asks the one thing the chain does not say; then a stamp grades the notes. Out of ink first and the notes are
 * scrawl. What is sent is what was done (the placements and the answer); the server replays it and grades that. Esc before the end sends nothing
 * and spends nothing.
 *
 * <p>The board, its concept chips, the ink pips and the specks are the subject of the screen and draw their own colours; the panel, the page and
 * every button are the skin's. Nothing is allocated in the draw path: the layout, the labels and the replay lines are built when they change.
 *
 * <p>Inspired by the shape of Thaumcraft's research table (a hex board you link concepts across); no code or assets were taken.
 */
final class ResearchBoardScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.WOOD;
    private static final int W = 350, H = 250, HEX = 14, BOARD_X = 120, BOARD_Y = 138, CELLS = 37, CHIP_W = 52, CHIP_H = 20;
    private static final int PLAY = 0, REPLAY = 1, ASK = 2, END = 3;
    private static final int GREEN = 0xFF5DBB63, RED = 0xFFD2452F, GOLD = 0xFFE8B83A, BLOCKED = 0xFF3B2F22, FREE = 0xFFB89A6A, FREE_HI = 0xFFD8BC88, CHAIN = 0xFF8A6A3A,
            RUBBLE_EDGE = 0xFF2A1D10, PAGE = 0xFFEDE3C4, PAGE_INK = 0xFF1F2A5C;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final ResearchBoard board;
    private final ResearchPlan plan;
    private final List<String> hand = new ArrayList<>();
    private final List<String> moves = new ArrayList<>();
    private final Map<Long, Long> placedAt = new HashMap<>();
    private final Specks specks = new Specks(160);
    private final long[] cells = new long[CELLS];
    private final int[] cellX = new int[CELLS], cellY = new int[CELLS];
    private final boolean[] usable = new boolean[CELLS];
    private final Map<String, FormattedCharSequence> chipLabels = new HashMap<>();
    private Component linkLine, handLine, helpLine, clickLine, foundLine, lostLine, tallyLine, answerLine, streakLine;
    private List<String> replayLines = List.of();
    private List<FormattedCharSequence> knownLines = List.of();
    private int phase = PLAY, asked = -1, shownChars, shownLines, px, py, lastStreak = -1;
    private float shakeAt = Float.NEGATIVE_INFINITY;
    private String held;
    private boolean correct, sent;
    private long endedAt;
    private int replayChars;

    ResearchBoardScreen(Screen parent, ResearchOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.plan = ResearchPlan.parse("play", session.plan());
        this.board = new ResearchBoard(plan, session.seed(), session.ink());
        for (String id : plan.labels.keySet())
            if (!id.equals(plan.start) && !id.equals(plan.goal))
                hand.add(id);
        int n = 0;
        for (int q = -ResearchBoard.RADIUS; q <= ResearchBoard.RADIUS; q++)
            for (int r = -ResearchBoard.RADIUS; r <= ResearchBoard.RADIUS; r++)
                if (ResearchBoard.inside(q, r)) {
                    cells[n] = ResearchBoard.key(q, r);
                    cellX[n] = BOARD_X + Math.round(HEX * 1.732f * (q + r / 2f));
                    cellY[n] = BOARD_Y + Math.round(HEX * 1.5f * r);
                    n++;
                }
        placedAt.put(board.startCell, 0L);
        linkLine = Component.translatable("gui.feedback.research.link", plan.labels.get(plan.start), plan.labels.get(plan.goal));
        handLine = Component.translatable("gui.feedback.research.hand");
        helpLine = Component.translatable("gui.feedback.research.help");
        clickLine = Component.translatable("gui.feedback.research.click");
        foundLine = Component.translatable("gui.feedback.research.found");
        lostLine = Component.translatable("gui.feedback.research.lost");
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        for (var e : plan.labels.entrySet())
            chipLabels.put(e.getKey(), font.split(Component.literal(e.getValue()), CHIP_W - 6).get(0));
        List<String> lines = new ArrayList<>();
        for (String paragraph : plan.replay)
            for (var piece : font.getSplitter().splitLines(paragraph, W - 56, net.minecraft.network.chat.Style.EMPTY))
                lines.add(piece.getString());
        replayLines = lines;
        refreshUsable();
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

    /** Which cells the held concept may be tried in, worked out when the held concept or the chain changes. */
    private void refreshUsable() {
        for (int i = 0; i < CELLS; i++)
            usable[i] = held != null && board.canTry(held, cells[i]);
        if (held == null)
            knownLines = List.of();
        else {
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(font.split(Component.translatable("gui.feedback.research.knows", plan.labels.get(held)), 90).get(0));
            for (String other : plan.labels.keySet())
                if (!other.equals(held) && plan.knows(held, other) && lines.size() < 5)
                    lines.add(Component.literal("- " + plan.labels.get(other)).getVisualOrderText());
            knownLines = lines;
        }
    }

    private void ui(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    private void to(int next) {
        phase = next;
        shownChars = 0;
        int total = 0;
        for (String line : replayLines)
            total += line.length();
        replayChars = total;
        rebuild();
    }

    // --- play -----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled() || button != 0)
            return !opening.settled() || super.mouseClicked(mx, my, button);
        if (phase == PLAY) {
            for (int i = 0; i < hand.size(); i++) {
                int x = px + 232 + (i % 2) * 54, y = py + 52 + (i / 2) * 22;
                if (mx >= x && mx < x + CHIP_W && my >= y && my < y + CHIP_H) {
                    String id = hand.get(i);
                    if (board.placed.containsValue(id))
                        return true;
                    held = id.equals(held) ? null : id;
                    refreshUsable();
                    ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f, 0.4f);
                    return true;
                }
            }
            for (int i = 0; i < CELLS; i++) {
                double dx = mx - px - cellX[i], dy = my - py - cellY[i];
                if (dx * dx + dy * dy < (HEX * 0.86) * (HEX * 0.86)) {
                    if (held != null)
                        place(i);
                    return true;
                }
            }
            return true;
        }
        if (phase == REPLAY) {
            if (shownChars < replayChars)
                shownChars = replayChars;
            else
                to(ASK);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    /** Tries the held concept in the {@code i}-th cell. */
    private void place(int i) {
        long cell = cells[i];
        String concept = held;
        var result = board.place(concept, cell);
        if (result == ResearchBoard.Result.IGNORED)
            return;
        moves.add(concept + "@" + ResearchBoard.q(cell) + "," + ResearchBoard.r(cell));
        float x = px + cellX[i], y = py + cellY[i];
        held = null;
        switch (result) {
            case FIZZLE -> {
                shakeAt = GuiStyle.time();
                specks.burst(x, y, 14, 1.8f, 0.4f, RED, 18, 2);
                ui(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, 0.9f);
                ui(SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.4f, 0.4f);
            }
            default -> {
                placedAt.put(cell, (long) GuiStyle.time());
                specks.burst(x, y, 10 + board.streak * 3, 1.5f, 0.6f, GOLD, 22, 2);
                ui(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6f + 0.09f * board.links, 0.8f);
                if (board.streak > 0 && board.streak % ResearchBoard.STREAK_REFUND == 0) {
                    specks.burst(px + 40, py + 30, 12, 1.2f, 0.3f, GREEN, 20, 2);
                    ui(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2f, 0.5f);
                }
                if (result == ResearchBoard.Result.REACHED) {
                    placedAt.put(board.goalCell, (long) GuiStyle.time());
                    specks.burst(px + cellX[CELLS - 1], py + cellY[CELLS - 1], 40, 2.4f, 0.8f, GOLD, 30, 3);
                    ui(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
                    refreshUsable();
                    to(REPLAY);
                    return;
                }
            }
        }
        refreshUsable();
        if (board.ink <= 0) {
            ui(SoundEvents.ANVIL_LAND, 0.6f, 0.4f);
            endedAt = System.currentTimeMillis();
            to(END);
        }
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
        if (phase == REPLAY && shownChars < replayChars) {
            shownChars = Math.min(replayChars, shownChars + 2);
            if (shownChars % 4 == 0)
                ui(SoundEvents.UI_BUTTON_CLICK.value(), 1.8f + (shownChars % 3) * 0.1f, 0.12f);
        }
    }

    // --- result -----------------------------------------------------------------------------------

    private float quality() {
        return board.quality(board.reached && correct);
    }

    private String grade() {
        float q = quality();
        return q >= 0.9f ? "A" : q >= 0.7f ? "B" : q >= 0.5f ? "C" : q >= 0.25f ? "D" : "F";
    }

    private void finish() {
        if (sent)
            return;
        sent = true;
        PacketDistributor.sendToServer(new io.github.soundgoodizerfan.feedback.net.ResearchResultPayload(containerId, moves, asked));
        opening.close();
    }

    // --- drawing ----------------------------------------------------------------------------------

    private static void hex(GuiGraphics g, int cx, int cy, int size, int colour) {
        for (int dy = -size; dy <= size; dy++) {
            float half = Math.abs(dy) <= size / 2f ? size * 0.866f : (size - Math.abs(dy)) * 1.732f;
            g.fill(Math.round(cx - half), cy + dy, Math.round(cx + half), cy + dy + 1, colour);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        int shake = GuiStyle.shake(shakeAt, 3);
        opening.render(g, width, height, px, py, W, H, () -> {
            g.pose().pushPose();
            g.pose().translate(shake, shake == 0 ? 0 : -shake / 2f, 0);
            SKIN.panel(g, px, py, W, H);
            g.drawString(font, plan.title, px + 12, py + 8, SKIN.ink(), false);
            if (phase == PLAY)
                drawPlay(g, mx, my);
            else if (phase == REPLAY || phase == ASK)
                drawPage(g);
            else
                drawEnd(g);
            specks.draw(g, partial);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
            g.pose().popPose();
        });
    }

    private void drawPlay(GuiGraphics g, int mx, int my) {
        g.drawString(font, linkLine, px + 12, py + 20, SKIN.dim(), false);
        for (int i = 0; i < board.inkStart + 3; i++) {
            int x = px + 12 + i * 8;
            if (i < board.ink || i < board.inkStart)
                g.fill(x, py + 32, x + 6, py + 38, i < board.ink ? GOLD : 0xFF4A3B26);
        }
        if (board.streak >= 2) {
            if (lastStreak != board.streak) {
                streakLine = Component.translatable("gui.feedback.research.streak", board.streak);
                lastStreak = board.streak;
            }
            g.drawString(font, streakLine, px + 12, py + 42, GOLD, false);
        }
        for (var e : board.from.entrySet())
            GuiStyle.line(g, px + cellXOf(e.getKey()), py + cellYOf(e.getKey()), px + cellXOf(e.getValue()), py + cellYOf(e.getValue()), CHAIN);
        float now = GuiStyle.time();
        for (int i = 0; i < CELLS; i++) {
            long c = cells[i];
            int x = px + cellX[i], y = py + cellY[i];
            String concept = board.placed.get(c);
            if (board.blocked.contains(c)) {
                hex(g, x, y, HEX - 1, BLOCKED);
                continue;
            }
            double dx = mx - x, dy = my - y;
            boolean hover = dx * dx + dy * dy < (HEX * 0.86) * (HEX * 0.86);
            hex(g, x, y, HEX - 1, concept != null ? 0xFF6B4E2A : usable[i] ? (hover ? FREE_HI : FREE) : 0xFF8C7650);
            if (concept != null || c == board.goalCell) {
                Long at = placedAt.get(c);
                float age = at == null ? 99 : now - at;
                int size = Math.round(HEX - 3 + (age < 6 ? (6 - age) * 0.8f : 0));
                hex(g, x, y, size, concept == null ? 0xFF5A4A33 : c == board.startCell ? 0xFF3C6F8F : c == board.goalCell ? GREEN : 0xFFD6B26A);
                FormattedCharSequence label = concept == null ? null : chipLabels.get(concept);
                if (label != null)
                    g.drawString(font, label, x - font.width(label) / 2, y - 3, 0xFF22160A, false);
                else
                    g.drawString(font, "?", x - 2, y - 3, SKIN.dim(), false);
            }
        }
        g.drawString(font, handLine, px + 232, py + 40, SKIN.dim(), false);
        for (int i = 0; i < hand.size(); i++) {
            int x = px + 232 + (i % 2) * 54, y = py + 52 + (i / 2) * 22;
            String id = hand.get(i);
            boolean used = board.placed.containsValue(id);
            g.fill(x, y, x + CHIP_W, y + CHIP_H, used ? 0xFF4A3B26 : id.equals(held) ? GOLD : 0xFF8C7650);
            g.renderOutline(x, y, CHIP_W, CHIP_H, RUBBLE_EDGE);
            g.drawString(font, chipLabels.get(id), x + 3, y + 6, used ? SKIN.dim() : 0xFF22160A, false);
        }
        int ky = py + 52 + ((hand.size() + 1) / 2) * 22 + 6;
        for (int i = 0; i < knownLines.size(); i++)
            g.drawString(font, knownLines.get(i), px + 232, ky + i * 9, i == 0 ? SKIN.ink() : SKIN.dim(), false);
        g.drawString(font, helpLine, px + 12, py + H - 14, SKIN.dim(), false);
    }

    private int cellXOf(long c) {
        return BOARD_X + Math.round(HEX * 1.732f * (ResearchBoard.q(c) + ResearchBoard.r(c) / 2f));
    }

    private int cellYOf(long c) {
        return BOARD_Y + Math.round(HEX * 1.5f * ResearchBoard.r(c));
    }

    /** The replay: what the chain found, as handwriting that comes in line by line (the page is a sheet of paper, drawn in its own colours). */
    private void drawPage(GuiGraphics g) {
        g.fill(px + 14, py + 24, px + W - 14, py + 136, PAGE);
        g.renderOutline(px + 14, py + 24, W - 28, 112, CHAIN);
        int budget = shownChars;
        int y = py + 32;
        // each line shows as many characters as have been written so far
        for (String line : replayLines) {
            if (budget <= 0)
                break;
            g.drawString(font, budget >= line.length() ? line : line.substring(0, budget), px + 24, y, PAGE_INK, false);
            budget -= line.length();
            y += 10;
        }
        if (phase == ASK)
            g.drawString(font, plan.question, px + 24, py + 138, SKIN.ink(), false);
        else if (shownChars >= replayChars)
            g.drawString(font, clickLine, px + 24, py + 126, 0xFF6C6C70, false);
    }

    private void drawEnd(GuiGraphics g) {
        g.fill(px + 14, py + 24, px + W - 14, py + 150, PAGE);
        g.renderOutline(px + 14, py + 24, W - 28, 126, CHAIN);
        if (tallyLine == null) {
            tallyLine = Component.translatable("gui.feedback.research.tally", board.links, board.fizzles, Math.max(0, board.ink));
            answerLine = board.reached ? Component.translatable(correct ? "gui.feedback.research.right" : "gui.feedback.research.wrong", plan.options.get(plan.answer)) : null;
        }
        int y = py + 34;
        g.drawString(font, plan.title, px + 24, y, PAGE_INK, false);
        g.drawString(font, board.reached ? foundLine : lostLine, px + 24, y + 16, PAGE_INK, false);
        g.drawString(font, tallyLine, px + 24, y + 30, PAGE_INK, false);
        if (answerLine != null)
            g.drawString(font, answerLine, px + 24, y + 42, correct ? 0xFF2E7D32 : 0xFFA33A24, false);
        GuiStyle.stamp(g, Component.literal(grade()), px + W - 70, py + 70, System.currentTimeMillis() - endedAt, quality() >= 0.5f ? 0x2E7D32 : 0xA33A24);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Esc: the screen closes and nothing is sent (unless the notebook was already closed with its button). */
    @Override
    public void onClose() {
        opening.close();
    }
}
