// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;
import io.github.soundgoodizerfan.feedback.modular.research.AppHack;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchPlan;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;
import io.github.soundgoodizerfan.feedback.net.ResearchResultPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The apps' research game ({@link AppHack}) on a phosphor terminal: a column of hexadecimal addresses beside a dump of junk with words hidden in
 * it. Click a word to try it (the terminal answers with its likeness and a try is spent); click a bracket pair to hack it (a dud word is dropped,
 * or the tries refill). Find the goal word and the app is written; run out of tries and the terminal locks. What is sent is what was done (the
 * picks in order); the server replays them ({@link AppHack#replay}) and the grade shown here is only the stamp. Esc before the end sends nothing.
 *
 * <p>The dump is drawn a character at a time on a fixed pitch so a click finds its character; the glyph widths are measured once in
 * {@code init} and nothing is allocated in the draw path (the log is shifted when a pick happens, not when it is drawn).
 */
final class AppHackScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.TERMINAL;
    private static final int W = 380, H = 214, CW = 6, RH = 9, ADDR_W = 30, COL_X = 12, COL_DX = 118, TOP = 34, STATUS_X = 248, STATUS_W = 120, LOG_LINES = 12;
    /** The terminal glass colour the highlighted slab's text is cut out in. */
    private static final int GLASS = 0xFF020804;
    private static final String[] GLYPH = new String[128];

    static {
        for (int c = 32; c < 127; c++)
            GLYPH[c] = String.valueOf((char) c);
    }

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final ResearchPlan plan;
    private final AppHack game;
    private final List<String> moves = new ArrayList<>();
    /** What each dump cell belongs to: a word's index, 1000 plus a bracket pair's index, or -1 for junk. */
    private final int[] owner = new int[AppHack.LINES * AppHack.WIDTH];
    private final String[] address = new String[AppHack.LINES];
    private final String[] log = new String[LOG_LINES];
    private final int[] glyphWidth = new int[128];
    private final Specks specks = new Specks(80);
    private final Component helpLine, triesLine, grantedLine, lockedLine;
    private List<net.minecraft.util.FormattedCharSequence> helpLines = List.of();
    private final String header;
    private int px, py, hover = -1;
    private boolean sent;
    private long endedAt;

    AppHackScreen(Screen parent, ResearchOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.plan = ResearchPlan.parse("play", session.plan());
        this.game = new AppHack(plan, session.seed(), session.ink());
        java.util.Arrays.fill(owner, -1);
        for (int i = 0; i < game.words.length; i++)
            for (int c = 0; c < game.words[i].length(); c++)
                owner[game.wordRow[i] * AppHack.WIDTH + game.wordCol[i] + c] = i;
        for (int p = 0; p < game.pairs; p++)
            for (int c = game.pairOpen[p]; c <= game.pairClose[p]; c++)
                owner[game.pairRow[p] * AppHack.WIDTH + c] = 1000 + p;
        for (int r = 0; r < AppHack.LINES; r++)
            address[r] = String.format("%04X", 0xF000 + r * AppHack.WIDTH);
        helpLine = Component.translatable("gui.feedback.research.app.help");
        triesLine = Component.translatable("gui.feedback.research.app.tries");
        grantedLine = Component.translatable("gui.feedback.research.app.granted");
        lockedLine = Component.translatable("gui.feedback.research.app.locked");
        header = Component.translatable("gui.feedback.research.app.header", plan.title).getString().toUpperCase(java.util.Locale.ROOT);
        push(Component.translatable("gui.feedback.research.app.ready").getString());
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        for (int c = 32; c < 127; c++)
            glyphWidth[c] = font.width(GLYPH[c]);
        helpLines = font.split(helpLine, W - 24);
        clearWidgets();
        if (game.over())
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.research.app.finish"), b -> finish(), px + STATUS_X, py + H - 52, STATUS_W, 18, null));
    }

    /** Adds a line to the terminal's log, the oldest scrolling off the top. */
    private void push(String line) {
        System.arraycopy(log, 1, log, 0, LOG_LINES - 1);
        log[LOG_LINES - 1] = line.toUpperCase(java.util.Locale.ROOT);
    }

    /** The dump cell under the pointer, or -1. */
    private int cellAt(double mx, double my) {
        int row = (int) Math.floor((my - py - TOP) / RH);
        if (row < 0 || row >= AppHack.ROWS)
            return -1;
        for (int col = 0; col < AppHack.COLS; col++) {
            int ch = (int) Math.floor((mx - px - COL_X - col * COL_DX - ADDR_W) / CW);
            if (ch >= 0 && ch < AppHack.WIDTH)
                return (col * AppHack.ROWS + row) * AppHack.WIDTH + ch;
        }
        return -1;
    }

    /** Whether the word or bracket pair a cell belongs to can still be clicked. */
    private boolean live(int o) {
        return o >= 0 && !game.over() && (o < 1000 ? !game.removed[o] && !game.picked[o] : !game.pairUsed[o - 1000]);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled() || button != 0)
            return !opening.settled() || super.mouseClicked(mx, my, button);
        if (super.mouseClicked(mx, my, button))
            return true;
        int cell = cellAt(mx, my);
        int o = cell < 0 ? -1 : owner[cell];
        if (!live(o))
            return true;
        if (o < 1000)
            pick(o);
        else
            hack(o - 1000);
        return true;
    }

    private void pick(int i) {
        var result = game.pick(i);
        if (result == AppHack.Result.IGNORED)
            return;
        moves.add("w" + i);
        push("> " + game.words[i]);
        GuiSkin.play(FSounds.APP_KEY.get(), 1f);
        if (result == AppHack.Result.FOUND) {
            push(grantedLine.getString());
            GuiSkin.play(FSounds.APP_FOUND.get(), 1f);
            specks.burst(px + 130, py + 100, 40, 2.2f, 0.7f, SKIN.ink(), 30, 3);
        } else {
            push(Component.translatable("gui.feedback.research.app.likeness", game.likeness).getString());
            push(Component.translatable("gui.feedback.research.app.denied").getString());
            GuiSkin.play(FSounds.APP_DENY.get(), 1f);
            if (result == AppHack.Result.OUT)
                push(lockedLine.getString());
        }
        afterMove();
    }

    private void hack(int p) {
        var result = game.hack(p);
        if (result == AppHack.Result.IGNORED)
            return;
        moves.add("b" + p);
        push("> HACK");
        push(Component.translatable(result == AppHack.Result.REFILLED ? "gui.feedback.research.app.refilled" : "gui.feedback.research.app.removed").getString());
        GuiSkin.play(FSounds.APP_HACK.get(), result == AppHack.Result.REFILLED ? 1.25f : 1f);
        afterMove();
    }

    private void afterMove() {
        if (game.over()) {
            endedAt = System.currentTimeMillis();
            rebuildWidgets();
        }
    }

    private void finish() {
        if (sent)
            return;
        sent = true;
        PacketDistributor.sendToServer(new ResearchResultPayload(containerId, moves, -1));
        opening.close();
    }

    @Override
    public void tick() {
        specks.tick(0.05f);
    }

    private String letter() {
        float q = game.grade();
        return q >= 0.9f ? "A" : q >= 0.7f ? "B" : q >= 0.5f ? "C" : q >= 0.25f ? "D" : "F";
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            minecraft.setScreen(parent);
            return;
        }
        opening.render(g, width, height, px, py, W, H, () -> {
            SKIN.panel(g, px, py, W, H);
            g.drawString(font, header, px + 12, py + 8, SKIN.ink(), false);
            drawTries(g);
            drawDump(g, mx, my);
            drawLog(g);
            specks.draw(g, partial);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
        });
    }

    private void drawTries(GuiGraphics g) {
        g.drawString(font, triesLine, px + 12, py + 20, SKIN.dim(), false);
        int x0 = px + 12 + font.width(triesLine) + 6;
        for (int i = 0; i < game.triesStart; i++) {
            int x = x0 + i * 9;
            if (i < game.tries)
                g.fill(x, py + 20, x + 7, py + 27, SKIN.ink());
            else
                g.renderOutline(x, py + 20, 7, 7, SKIN.dim());
        }
    }

    private void drawDump(GuiGraphics g, int mx, int my) {
        int hoverOwner = opening.settled() ? ownerAt(mx, my) : -1;
        hover = live(hoverOwner) ? hoverOwner : -1;
        for (int r = 0; r < AppHack.LINES; r++) {
            int col = r / AppHack.ROWS, row = r % AppHack.ROWS;
            int x0 = px + COL_X + col * COL_DX, y = py + TOP + row * RH;
            g.drawString(font, address[r], x0, y, SKIN.dim(), false);
            for (int c = 0; c < AppHack.WIDTH; c++) {
                int cell = r * AppHack.WIDTH + c, o = owner[cell];
                char ch = dump(cell, o);
                int x = x0 + ADDR_W + c * CW;
                boolean lit = o >= 0 && o == hover;
                if (lit)
                    g.fill(x, y - 1, x + CW, y + RH - 1, SKIN.ink());
                int colour = lit ? GLASS : o >= 0 && o < 1000 && game.picked[o] ? SKIN.dim() : SKIN.ink();
                g.drawString(font, GLYPH[ch], x + (CW - glyphWidth[ch]) / 2, y, colour, false);
            }
        }
    }

    /** The character a cell shows: a removed word and a used bracket pair read as dots. */
    private char dump(int cell, int o) {
        if (o >= 0 && (o < 1000 ? game.removed[o] : game.pairUsed[o - 1000]))
            return '.';
        return game.dump[cell];
    }

    private int ownerAt(int mx, int my) {
        int cell = cellAt(mx, my);
        return cell < 0 ? -1 : owner[cell];
    }

    private void drawLog(GuiGraphics g) {
        int x = px + STATUS_X, y0 = py + TOP;
        int last = -1;
        for (int i = 0; i < LOG_LINES; i++) {
            if (log[i] == null)
                continue;
            g.drawString(font, log[i], x, y0 + i * RH, i == LOG_LINES - 1 ? SKIN.ink() : SKIN.dim(), false);
            last = i;
        }
        if (game.over()) {
            GuiStyle.stamp(g, Component.literal(letter()), px + STATUS_X + STATUS_W / 2f, py + H - 80, System.currentTimeMillis() - endedAt,
                    game.found ? 0x5CFF7A : 0xFFB02E);
            return;
        }
        // the blinking cursor sits after the newest line
        if (System.currentTimeMillis() / 450 % 2 == 0) {
            int cx = x + (last < 0 ? 0 : font.width(log[last]) + 2), cy = y0 + Math.max(last, 0) * RH;
            g.fill(cx, cy, cx + 5, cy + 7, SKIN.ink());
        }
        for (int i = 0; i < helpLines.size(); i++)
            g.drawString(font, helpLines.get(i), px + 12, py + H - 28 + i * 9, SKIN.dim(), false);
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
