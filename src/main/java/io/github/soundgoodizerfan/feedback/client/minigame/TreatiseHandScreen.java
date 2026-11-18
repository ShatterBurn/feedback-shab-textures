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
import io.github.soundgoodizerfan.feedback.modular.research.ResearchPlan;
import io.github.soundgoodizerfan.feedback.modular.research.TreatiseHand;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;
import io.github.soundgoodizerfan.feedback.net.ResearchResultPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The treatise research game ({@link TreatiseHand}) on a notebook page: the concepts the researcher owns link up in a chain along the top, a hand of
 * concept cards lies below. Click cards to lay them in a row (the numbers say the order), then Play: each card must share a trait with the one before
 * it (the first with the chain's end). Cards that link join the chain and score; the first that does not burns with those after it, spends the hand
 * and strains the specimen. A card that shares a trait with the goal reaches it; the page then replays what was found and asks the one thing the
 * chain does not say, and a stamp grades the notes. Discards throw cards back for a fresh hand. What is sent is what was done (the plays and the
 * discards in order, and the answer); the server replays it and grades that. Esc before the end sends nothing and spends nothing.
 *
 * <p>The chain chips, the cards and the specks are the subject of the screen and draw their own colours; the panel, the page and every button are the
 * skin's. Faces and labels are wrapped once when the screen opens; nothing is allocated in the draw path.
 */
final class TreatiseHandScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.WOOD;
    private static final int W = 400, H = 250, CARD_W = 68, CARD_H = 64, CARD_GAP = 6, CHIP_W = 50, CHIP_H = 30, CHAIN_SHOWN = 6, CHAIN_Y = 62, HAND_Y = 118;
    private static final int PLAY = 0, REPLAY = 1, ASK = 2, END = 3;
    private static final int GREEN = 0xFF5DBB63, RED = 0xFFD2452F, GOLD = 0xFFE8B83A, PAGE = 0xFFEDE3C4, PAGE_INK = 0xFF1F2A5C, EDGE = 0xFF2A1D10, CHAIN = 0xFF8A6A3A,
            CARD = 0xFFD8BC88, CARD_HI = 0xFFEBD4A0, CARD_INK = 0xFF22160A, CARD_DIM = 0xFF4A3B26, START = 0xFF3C6F8F;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final ResearchPlan plan;
    private final TreatiseHand game;
    private final List<Integer> picked = new ArrayList<>();
    private final List<String> moves = new ArrayList<>();
    private final Specks specks = new Specks(120);
    private final Map<String, FormattedCharSequence> chipLabels = new HashMap<>();
    private final Map<String, List<FormattedCharSequence>> faces = new HashMap<>(), chipFaces = new HashMap<>();
    private final Map<String, String> traitText = new HashMap<>();
    private Component goalLine, countLine, helpLine, foundLine, lostLine, tallyLine, answerLine, clickLine, playLabel, discardLabel;
    private List<String> replayLines = List.of();
    private String hoverText = "", messageText = "";
    private long messageUntil;
    private int phase = PLAY, asked = -1, shownChars, replayChars, px, py, hoverCard = -1, hoverStamp = -1;
    private float shakeAt = Float.NEGATIVE_INFINITY;
    private boolean correct, sent;
    /** The how-to window shows until the first click, once per game session; the "?" button brings it back. */
    private static boolean tutorialSeen;
    private boolean tutorial = !tutorialSeen;
    private List<FormattedCharSequence> tutorialLines = List.of();
    private int tutorialH;
    private long endedAt;

    TreatiseHandScreen(Screen parent, ResearchOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.plan = ResearchPlan.parse("play", session.plan());
        this.game = new TreatiseHand(plan, session.seed(), session.ink(), session.tier());
        for (String id : plan.labels.keySet())
            traitText.put(id, traitNames(plan.traits.get(id)));
        goalLine = Component.translatable("gui.feedback.research.hand.goal", plan.labels.get(plan.start), plan.labels.get(plan.goal), traitText.get(plan.goal));
        helpLine = Component.translatable("gui.feedback.research.hand.help");
        clickLine = Component.translatable("gui.feedback.research.click");
        foundLine = Component.translatable("gui.feedback.research.found");
        lostLine = Component.translatable("gui.feedback.research.hand.lost");
        playLabel = Component.translatable("gui.feedback.research.hand.play");
        discardLabel = Component.translatable("gui.feedback.research.hand.discard");
        refreshCount();
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        for (var e : plan.labels.entrySet()) {
            chipLabels.put(e.getKey(), font.split(Component.literal(e.getValue()), CHIP_W - 4).get(0));
            faces.put(e.getKey(), font.split(Component.literal(traitText.get(e.getKey())), (int) ((CARD_W - 8) / 0.75f)));
            chipFaces.put(e.getKey(), font.split(Component.literal(traitText.get(e.getKey())), (int) ((CHIP_W - 4) / 0.6f)));
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : plan.replay)
            for (var piece : font.getSplitter().splitLines(paragraph, W - 56, net.minecraft.network.chat.Style.EMPTY))
                lines.add(piece.getString());
        replayLines = lines;
        List<FormattedCharSequence> how = new ArrayList<>();
        for (int i = 1; i <= 3; i++)
            how.addAll(font.split(Component.translatable("gui.feedback.research.hand.tutorial." + i), W - 80 - 24));
        tutorialLines = how;
        tutorialH = 36 + how.size() * 10 + 8;
        rebuild();
    }

    /** The buttons of the phase: Play and Discard while playing, the answers while asking, the close button at the end. */
    private void rebuild() {
        clearWidgets();
        if (phase == PLAY) {
            addRenderableWidget(GuiStyle.primary(SKIN, playLabel, b -> play(), px + 12, py + H - 34, 96, 18, null));
            addRenderableWidget(GuiStyle.button(SKIN, discardLabel, b -> discard(), px + 114, py + H - 34, 96, 18));
            addRenderableWidget(GuiStyle.button(SKIN, Component.literal("?"), b -> tutorial = true, px + W - 30, py + 6, 18, 14));
        } else if (phase == ASK)
            for (int i = 0; i < plan.options.size(); i++) {
                int n = i;
                addRenderableWidget(GuiStyle.button(SKIN, Component.literal(plan.options.get(i)), b -> answer(n), px + 24, py + 150 + i * 22, W - 48, 18));
            }
        else if (phase == END)
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.research.finish"), b -> finish(), px + W / 2 - 40, py + H - 30, 80, 18, null));
    }

    private void refreshCount() {
        countLine = Component.translatable("gui.feedback.research.hand.count", game.hands, game.discards, game.score, game.target);
    }

    private static String traitNames(java.util.Collection<String> ids) {
        StringBuilder out = new StringBuilder();
        for (String t : ids)
            out.append(out.length() == 0 ? "" : ", ").append(Component.translatable("gui.feedback.research.trait." + t).getString());
        return out.toString();
    }

    private int cardX(int slot) {
        return px + (W - (TreatiseHand.HAND * CARD_W + (TreatiseHand.HAND - 1) * CARD_GAP)) / 2 + slot * (CARD_W + CARD_GAP);
    }

    private int cardAt(double mx, double my) {
        for (int s = 0; s < game.hand.size(); s++) {
            int x = cardX(s), y = py + HAND_Y - (picked.contains(game.hand.get(s)) ? 6 : 0);
            if (mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H)
                return s;
        }
        return -1;
    }

    // --- play -----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled() || button != 0)
            return !opening.settled() || super.mouseClicked(mx, my, button);
        if (tutorial && phase == PLAY) {
            tutorial = false;
            tutorialSeen = true;
            return true;
        }
        if (super.mouseClicked(mx, my, button))
            return true;
        if (phase == PLAY) {
            int slot = cardAt(mx, my);
            if (slot >= 0) {
                Integer card = game.hand.get(slot);
                if (!picked.remove(card) && picked.size() < TreatiseHand.MAX_PLAY)
                    picked.add(card);
                hoverStamp = -1;
                GuiSkin.play(FSounds.CARD_DEAL.get(), 1.5f, 0.35f);
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
        return false;
    }

    private static String encode(char kind, List<Integer> ids) {
        StringBuilder out = new StringBuilder().append(kind);
        for (int i = 0; i < ids.size(); i++)
            out.append(i == 0 ? "" : ",").append(ids.get(i));
        return out.toString();
    }

    private void play() {
        if (phase != PLAY || picked.isEmpty())
            return;
        List<Integer> row = new ArrayList<>(picked);
        String burned = firstBreak(row);
        var result = game.play(row);
        if (result == TreatiseHand.Result.IGNORED)
            return;
        moves.add(encode('p', row));
        picked.clear();
        hoverStamp = -1;
        float x = px + W / 2f, y = py + CHAIN_Y + CHIP_H / 2f;
        switch (result) {
            case BROKEN -> {
                messageText = burned;
                messageUntil = System.currentTimeMillis() + 4500;
                shakeAt = GuiStyle.time();
                specks.burst(x, py + HAND_Y + 20, 14, 1.8f, 0.4f, RED, 18, 2);
                GuiSkin.play(FSounds.CARD_BURN.get(), 1f);
            }
            case REACHED -> {
                specks.burst(x, y, 40, 2.4f, 0.8f, GOLD, 30, 3);
                GuiSkin.play(FSounds.PAGE_FOUND.get(), 1f);
            }
            default -> {
                specks.burst(x, y, 10 + 2 * row.size(), 1.5f, 0.6f, GOLD, 22, 2);
                GuiSkin.play(FSounds.CARD_PLAY.get(), 1f);
            }
        }
        refreshCount();
        if (result == TreatiseHand.Result.REACHED) {
            to(REPLAY);
            return;
        }
        if (result != TreatiseHand.Result.BROKEN)
            GuiSkin.play(FSounds.CARD_DEAL.get(), 1f, 0.5f);
        if (game.over()) {
            endedAt = System.currentTimeMillis();
            to(END);
        }
    }

    /** The line for a play's first card that does not link, for the burn message; empty when every card links. */
    private String firstBreak(List<Integer> row) {
        String prev = game.end();
        for (int c : row) {
            if (!plan.related(game.cards[c], prev))
                return Component.translatable("gui.feedback.research.hand.burned", plan.labels.get(game.cards[c]), plan.labels.get(prev)).getString();
            prev = game.cards[c];
        }
        return "";
    }

    private void discard() {
        if (phase != PLAY || picked.isEmpty() || game.discards <= 0)
            return;
        List<Integer> out = new ArrayList<>(picked);
        if (game.discard(out) == TreatiseHand.Result.IGNORED)
            return;
        moves.add(encode('d', out));
        picked.clear();
        hoverStamp = -1;
        GuiSkin.play(FSounds.CARD_DEAL.get(), 0.9f);
        refreshCount();
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

    private void answer(int n) {
        correct = n == plan.answer;
        asked = n;
        GuiSkin.play(FSounds.CARD_PLAY.get(), correct ? 1.3f : 0.8f);
        endedAt = System.currentTimeMillis();
        to(END);
    }

    private void finish() {
        if (sent)
            return;
        sent = true;
        PacketDistributor.sendToServer(new ResearchResultPayload(containerId, moves, asked));
        opening.close();
    }

    @Override
    public void tick() {
        specks.tick(0.05f);
        if (phase == REPLAY && shownChars < replayChars)
            shownChars = Math.min(replayChars, shownChars + 2);
    }

    private float quality() {
        return game.quality(game.reached && correct);
    }

    private String grade() {
        float q = quality();
        return q >= 0.9f ? "A" : q >= 0.7f ? "B" : q >= 0.5f ? "C" : q >= 0.25f ? "D" : "F";
    }

    // --- drawing ----------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            minecraft.setScreen(parent);
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
            if (tutorial && phase == PLAY)
                drawTutorial(g);
            g.pose().popPose();
        });
    }

    /** The how-to window, a sheet of paper in its own colours over the play area. */
    private void drawTutorial(GuiGraphics g) {
        int x = px + 40, y = py + 50, w = W - 80;
        g.fill(x, y, x + w, y + tutorialH, PAGE);
        g.renderOutline(x, y, w, tutorialH, CHAIN);
        g.drawString(font, Component.translatable("gui.feedback.research.hand.tutorial.title"), x + 12, y + 8, PAGE_INK, false);
        for (int i = 0; i < tutorialLines.size(); i++)
            g.drawString(font, tutorialLines.get(i), x + 12, y + 24 + i * 10, PAGE_INK, false);
        g.drawString(font, clickLine, x + w - 12 - font.width(clickLine), y + tutorialH - 12, 0xFF6C6C70, false);
    }

    private void drawPlay(GuiGraphics g, int mx, int my) {
        g.drawString(font, goalLine, px + 12, py + 20, SKIN.dim(), false);
        g.drawString(font, countLine, px + 12, py + 34, SKIN.ink(), false);
        // the chain: its last few concepts, then the goal
        int total = game.chain.size(), from = Math.max(0, total - CHAIN_SHOWN);
        int x = px + 12;
        if (from > 0) {
            g.drawString(font, "…", x, py + CHAIN_Y + 10, SKIN.dim(), false);
            x += 10;
        }
        for (int i = from; i < total; i++, x += CHIP_W + 3)
            chip(g, x, game.chain.get(i), i == 0 ? START : 0xFFD6B26A);
        chip(g, px + W - 12 - CHIP_W, plan.goal, GREEN);
        updateHover(mx, my);
        int slots = game.hand.size();
        for (int s = 0; s < slots; s++) {
            int card = game.hand.get(s), order = picked.indexOf(card);
            int cx = cardX(s), cy = py + HAND_Y - (order >= 0 ? 6 : 0);
            String id = game.cards[card];
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, order >= 0 ? CARD_HI : CARD);
            g.renderOutline(cx, cy, CARD_W, CARD_H, order >= 0 ? GOLD : EDGE);
            g.drawString(font, chipLabelOf(id), cx + 4, cy + 4, CARD_INK, false);
            List<FormattedCharSequence> lines = faces.get(id);
            g.pose().pushPose();
            g.pose().translate(cx + 4, cy + 18, 0);
            g.pose().scale(0.75f, 0.75f, 1f);
            for (int l = 0; l < lines.size() && l < 4; l++)
                g.drawString(font, lines.get(l), 0, l * 10, CARD_DIM, false);
            g.pose().popPose();
            if (order >= 0) {
                g.fill(cx + CARD_W - 13, cy + 2, cx + CARD_W - 2, cy + 13, GOLD);
                g.drawString(font, ORDER[order], cx + CARD_W - 10, cy + 4, CARD_INK, false);
            }
        }
        if (System.currentTimeMillis() < messageUntil)
            g.drawString(font, messageText, px + 12, py + H - 56, RED, false);
        else if (!hoverText.isEmpty())
            g.drawString(font, hoverText, px + 12, py + H - 56, GOLD, false);
        g.drawString(font, helpLine, px + 220, py + H - 30, SKIN.dim(), false);
    }

    private static final String[] ORDER = {"1", "2", "3", "4", "5"};

    private FormattedCharSequence chipLabelOf(String id) {
        return chipLabels.get(id);
    }

    private void chip(GuiGraphics g, int x, String id, int colour) {
        int y = py + CHAIN_Y;
        g.fill(x, y, x + CHIP_W, y + CHIP_H, colour);
        g.renderOutline(x, y, CHIP_W, CHIP_H, EDGE);
        FormattedCharSequence label = chipLabels.get(id);
        g.drawString(font, label, x + 2, y + 2, CARD_INK, false);
        g.pose().pushPose();
        g.pose().translate(x + 2, y + 13, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        List<FormattedCharSequence> lines = chipFaces.get(id);
        for (int l = 0; l < lines.size() && l < 3; l++)
            g.drawString(font, lines.get(l), 0, l * 9, CARD_DIM, false);
        g.pose().popPose();
    }

    /** Works out what the line under the hand says about the card under the pointer; redone only when the card or the row changed. */
    private void updateHover(int mx, int my) {
        int slot = cardAt(mx, my);
        int stamp = picked.size() * 64 + game.chain.size();
        if (slot == hoverCard && stamp == hoverStamp)
            return;
        hoverCard = slot;
        hoverStamp = stamp;
        hoverText = "";
        if (slot < 0)
            return;
        int card = game.hand.get(slot), at = picked.indexOf(card);
        String before = at > 0 ? game.cards[picked.get(at - 1)] : at == 0 ? game.end() : picked.isEmpty() ? game.end() : game.cards[picked.get(picked.size() - 1)];
        List<String> same = plan.shared(game.cards[card], before);
        hoverText = same.isEmpty()
                ? Component.translatable("gui.feedback.research.hand.shares_none", plan.labels.get(game.cards[card]), plan.labels.get(before)).getString()
                : Component.translatable("gui.feedback.research.hand.shares", plan.labels.get(game.cards[card]), traitNames(same), plan.labels.get(before)).getString();
    }

    /** The replay: what the chain found, as handwriting that comes in line by line (the page is a sheet of paper, drawn in its own colours). */
    private void drawPage(GuiGraphics g) {
        g.fill(px + 14, py + 24, px + W - 14, py + 136, PAGE);
        g.renderOutline(px + 14, py + 24, W - 28, 112, CHAIN);
        int budget = shownChars, y = py + 32;
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
            tallyLine = Component.translatable("gui.feedback.research.hand.tally", game.links, game.bad, game.score, game.target);
            answerLine = game.reached ? Component.translatable(correct ? "gui.feedback.research.right" : "gui.feedback.research.wrong", plan.options.get(plan.answer)) : null;
        }
        int y = py + 34;
        g.drawString(font, plan.title, px + 24, y, PAGE_INK, false);
        g.drawString(font, game.reached ? foundLine : lostLine, px + 24, y + 16, PAGE_INK, false);
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

    /** Esc: the screen closes and nothing is sent (unless the result was already sent with its button). */
    @Override
    public void onClose() {
        opening.close();
    }
}
