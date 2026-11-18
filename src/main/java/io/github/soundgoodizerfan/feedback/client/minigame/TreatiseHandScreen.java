// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
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
    private final Juice juice = new Juice();
    /** Cards in the air: a played card flies to its place in the chain, a card that did not link burns where it lay, a discard drops away. */
    private static final int FLIGHTS = 16, JOIN = 0, BURN = 1, DISCARD = 2;
    private static final float FLY_TICKS = 8f;
    private final String[] flyId = new String[FLIGHTS];
    private final float[] flyFromX = new float[FLIGHTS], flyFromY = new float[FLIGHTS], flyToX = new float[FLIGHTS], flyToY = new float[FLIGHTS], flyAt = new float[FLIGHTS];
    private final int[] flyKind = new int[FLIGHTS], flyOrder = new int[FLIGHTS];
    private final boolean[] flyLanded = new boolean[FLIGHTS];
    private int flyNext;
    /** When each card of the plan was dealt into the hand (it is hidden until then); the chain chips from {@code revealFrom} show as their card lands. */
    private float[] dealtAt;
    private int revealFrom = Integer.MAX_VALUE;
    private float revealAt;
    private boolean correct, sent;
    private final Tutorial tutorial = new Tutorial("hand", "gui.feedback.research.hand.tutorial", 10);
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
        tutorial.init(font, W - 80 - 24);
        if (dealtAt == null) {
            // the opening hand is dealt out one card at a time
            dealtAt = new float[game.cards.length];
            java.util.Arrays.fill(dealtAt, Float.NEGATIVE_INFINITY);
            deal(List.of(), GuiStyle.time() + 4);
        }
        rebuild();
    }

    /** Sets the cards that are in the hand now and were not in {@code before} to fly in one after another from {@code start}. */
    private void deal(List<Integer> before, float start) {
        if (ClientSettings.reduceMotion())
            return;
        int n = 0;
        for (int card : game.hand)
            if (!before.contains(card))
                dealtAt[card] = start + 2 * n++;
    }

    private int chipX(int index, int from) {
        return px + 12 + (from > 0 ? 10 : 0) + (index - from) * (CHIP_W + 3);
    }

    private void launch(String id, float fromX, float fromY, float toX, float toY, float at, int kind, int order) {
        int f = flyNext;
        flyNext = (flyNext + 1) % FLIGHTS;
        flyId[f] = id;
        flyFromX[f] = fromX;
        flyFromY[f] = fromY;
        flyToX[f] = toX;
        flyToY[f] = toY;
        flyAt[f] = ClientSettings.reduceMotion() ? at - 3 * FLY_TICKS : at;
        flyKind[f] = kind;
        flyOrder[f] = order;
        flyLanded[f] = false;
    }

    /** The buttons of the phase: Play and Discard while playing, the answers while asking, the close button at the end. */
    private void rebuild() {
        clearWidgets();
        if (phase == PLAY) {
            addRenderableWidget(GuiStyle.primary(SKIN, playLabel, b -> play(), px + 12, py + H - 34, 96, 18, null));
            addRenderableWidget(GuiStyle.button(SKIN, discardLabel, b -> discard(), px + 114, py + H - 34, 96, 18));
            addRenderableWidget(tutorial.button(SKIN, px + W - 30, py + 6));
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
        float clock = GuiStyle.time();
        for (int s = 0; s < game.hand.size(); s++) {
            if (clock < dealtAt[game.hand.get(s)] + FLY_TICKS)
                continue;
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
        if (phase == PLAY && tutorial.click())
            return true;
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
        List<Integer> handBefore = new ArrayList<>(game.hand);
        float[] fromX = new float[row.size()];
        for (int k = 0; k < fromX.length; k++)
            fromX[k] = cardX(game.hand.indexOf(row.get(k)));
        int chainBefore = game.chain.size();
        var result = game.play(row);
        if (result == TreatiseHand.Result.IGNORED)
            return;
        moves.add(encode('p', row));
        picked.clear();
        hoverStamp = -1;
        float clock = GuiStyle.time();
        float x = px + W / 2f, y = py + CHAIN_Y + CHIP_H / 2f;
        // the cards that linked fly to their chips and chime as each lands; the rest burn where they lay
        int joined = Math.max(0, Math.min(row.size(), game.chain.size() - chainBefore));
        int from = Math.max(0, game.chain.size() - CHAIN_SHOWN);
        revealFrom = ClientSettings.reduceMotion() ? Integer.MAX_VALUE : chainBefore;
        revealAt = clock;
        for (int k = 0; k < row.size(); k++) {
            String id = game.cards[row.get(k)];
            float fy = py + HAND_Y - 6;
            if (k < joined)
                launch(id, fromX[k], fy, chipX(Math.max(from, chainBefore + k), from), py + CHAIN_Y, clock + 2 * k, JOIN, k);
            else
                launch(id, fromX[k], fy, fromX[k], fy - 10, clock, BURN, 0);
        }
        deal(handBefore, clock + FLY_TICKS);
        switch (result) {
            case BROKEN -> {
                messageText = burned;
                messageUntil = System.currentTimeMillis() + 4500;
                juice.shake(3);
                juice.flash(0xD2452F, 6, 8);
                specks.burst(x, py + HAND_Y + 20, 14, 1.8f, 0.4f, RED, 18, 2);
                GuiSkin.play(FSounds.CARD_BURN.get(), 1f);
            }
            case REACHED -> {
                specks.burst(x, y, 40, 2.4f, 0.8f, GOLD, 30, 3);
                juice.flash(0xE8B83A, 8, 8);
                GuiSkin.play(FSounds.PAGE_FOUND.get(), 1f);
            }
            default -> {
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
        List<Integer> handBefore = new ArrayList<>(game.hand);
        float[] fromX = new float[out.size()];
        for (int k = 0; k < fromX.length; k++)
            fromX[k] = cardX(game.hand.indexOf(out.get(k)));
        if (game.discard(out) == TreatiseHand.Result.IGNORED)
            return;
        float clock = GuiStyle.time();
        for (int k = 0; k < out.size(); k++)
            launch(game.cards[out.get(k)], fromX[k], py + HAND_Y - 6, fromX[k] + 20, py + HAND_Y + 70, clock + k, DISCARD, 0);
        deal(handBefore, clock + FLY_TICKS);
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
        if (correct) {
            specks.burst(px + W - 70, py + 70, 40, 2.4f, 0.8f, GOLD, 30, 3);
            juice.flash(0xE8B83A, 6, 8);
        } else {
            juice.shake(4);
            juice.flash(0xD2452F, 7, 9);
            specks.burst(px + W - 70, py + 70, 24, 2f, 0.5f, RED, 22, 2);
            GuiSkin.play(FSounds.CARD_BURN.get(), 0.8f, 0.6f);
        }
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
        juice.tick();
        float clock = GuiStyle.time();
        for (int f = 0; f < FLIGHTS; f++) {
            if (flyId[f] == null)
                continue;
            float age = clock - flyAt[f];
            if (flyKind[f] == JOIN && !flyLanded[f] && age >= FLY_TICKS) {
                flyLanded[f] = true;
                GuiSkin.play(FSounds.CARD_PLAY.get(), Juice.climb(1f, flyOrder[f]));
                specks.burst(flyToX[f] + CHIP_W / 2f, flyToY[f] + CHIP_H / 2f, 8, 1.2f, 0.5f, GOLD, 18, 2);
            }
            if (age > FLY_TICKS + 2)
                flyId[f] = null;
        }
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
        int shakeX = juice.dx(), shakeY = juice.dy();
        opening.render(g, width, height, px, py, W, H, () -> {
            g.pose().pushPose();
            g.pose().translate(shakeX, shakeY, 0);
            SKIN.panel(g, px, py, W, H);
            g.drawString(font, plan.title, px + 12, py + 8, SKIN.ink(), false);
            if (phase == PLAY)
                drawPlay(g, mx, my);
            else if (phase == REPLAY || phase == ASK)
                drawPage(g);
            else
                drawEnd(g);
            specks.draw(g, partial);
            juice.draw(g, px + 3, py + 3, px + W - 3, py + H - 3);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
            if (phase == PLAY)
                tutorial.draw(g, font, px + 40, py + 50, W - 80, PAGE, CHAIN, PAGE_INK, PAGE_INK, 0xFF6C6C70);
            g.pose().popPose();
        });
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
        float clock = GuiStyle.time();
        boolean still = ClientSettings.reduceMotion();
        // how far along the chain is to the goal: the newest chip glows brighter and thicker as it nears
        float near = game.target > 0 ? Math.min(1f, game.score / (float) game.target) : 0f;
        for (int i = from; i < total; i++, x += CHIP_W + 3) {
            if (i >= revealFrom && clock < revealAt + 2 * (i - revealFrom) + FLY_TICKS) {
                g.renderOutline(x, py + CHAIN_Y, CHIP_W, CHIP_H, CHAIN);
                continue;
            }
            chip(g, x, game.chain.get(i), i == 0 ? START : 0xFFD6B26A);
            if (i == total - 1 && total > 1) {
                float pulse = still ? 1f : 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 220.0);
                int glow = ((int) (255 * pulse) << 24) | (Juice.mix(GOLD, 0xFFFFFFFF, near) & 0xFFFFFF);
                int thick = near > 0.8f ? 3 : near > 0.5f ? 2 : 1;
                for (int t = 0; t < thick; t++)
                    g.renderOutline(x - t, py + CHAIN_Y - t, CHIP_W + 2 * t, CHIP_H + 2 * t, glow);
            }
        }
        chip(g, px + W - 12 - CHIP_W, plan.goal, GREEN);
        updateHover(mx, my);
        int slots = game.hand.size();
        for (int s = 0; s < slots; s++) {
            int card = game.hand.get(s), order = picked.indexOf(card);
            float age = clock - dealtAt[card];
            if (age < 0)
                continue;
            int cx = cardX(s), cy = py + HAND_Y - (order >= 0 ? 6 : 0);
            if (age < FLY_TICKS) {
                // a newly dealt card slides in from the deck, above and to the right
                float left = 1 - GuiStyle.easeOut(age / FLY_TICKS);
                cx += (int) (left * 60);
                cy -= (int) (left * 36);
            }
            drawCard(g, cx, cy, game.cards[card], order);
        }
        for (int f = 0; f < FLIGHTS; f++)
            if (flyId[f] != null)
                drawFlight(g, f, clock);
        if (System.currentTimeMillis() < messageUntil)
            g.drawString(font, messageText, px + 12, py + H - 56, RED, false);
        else if (!hoverText.isEmpty())
            g.drawString(font, hoverText, px + 12, py + H - 56, GOLD, false);
        g.drawString(font, helpLine, px + 220, py + H - 30, SKIN.dim(), false);
    }

    private static final String[] ORDER = {"1", "2", "3", "4", "5"};

    /** One concept card at a position; {@code order} is its place in the row being built (0 first) or -1 when it is not picked. */
    private void drawCard(GuiGraphics g, int cx, int cy, String id, int order) {
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

    /** A card in the air: a played card shrinks to a chip as it flies to the chain, a burning one reddens and rises, a discard drops and shrinks. */
    private void drawFlight(GuiGraphics g, int f, float clock) {
        float age = clock - flyAt[f];
        float t = GuiStyle.easeOut(age / FLY_TICKS);
        if (age < 0)
            t = 0;
        float scale = flyKind[f] == JOIN ? 1 - t * (1 - CHIP_W / (float) CARD_W) : flyKind[f] == DISCARD ? 1 - 0.5f * t : 1f;
        float x = flyFromX[f] + (flyToX[f] - flyFromX[f]) * t, y = flyFromY[f] + (flyToY[f] - flyFromY[f]) * t;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        drawCard(g, 0, 0, flyId[f], flyKind[f] == JOIN ? 0 : -1);
        if (flyKind[f] == BURN)
            g.fill(0, 0, CARD_W, CARD_H, ((int) (170 * t) << 24) | 0xD2452F);
        g.pose().popPose();
    }

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
