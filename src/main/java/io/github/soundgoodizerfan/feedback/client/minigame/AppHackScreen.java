// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
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
    /** What a dying word or bracket pair flickers through before it reads as dots. */
    private static final String SCRAMBLE = "#%&@$?*!<>";
    private static final int TXT_GREEN = 0xFF5CFF7A, TXT_RED = 0xFFFF5A3C, TXT_AMBER = 0xFFFFB02E, RGB_RED = 0xFF3020;

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
    /** One terminal line; a colour of 0 means the screen's own ink. */
    private record Line(String text, int colour) {
    }

    private final Line[] log = new Line[LOG_LINES];
    /** Lines waiting to be typed out, the one being typed and how much of it shows: the terminal answers a few characters a tick. */
    private final java.util.ArrayDeque<Line> pending = new java.util.ArrayDeque<>();
    private Line typing;
    private int typed;
    private final Juice juice = new Juice();
    /** When each word and bracket pair was cut (0 for never), for the flicker before it reads as dots. */
    private final long[] wordDied, pairDied;
    private int foundWord = -1, lostSlot = -1;
    private long triesLostAt, refilledAt;
    private final int[] glyphWidth = new int[128];
    private final Specks specks = new Specks(220);
    private final Component helpLine, triesLine, grantedLine, lockedLine;
    private List<net.minecraft.util.FormattedCharSequence> helpLines = List.of();
    private final String header;
    private int px, py, hover = -1;
    private boolean sent;
    private final Tutorial tutorial = new Tutorial("app", "gui.feedback.research.app.tutorial", 10);
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
        wordDied = new long[game.words.length];
        pairDied = new long[game.pairs];
        helpLine = Component.translatable("gui.feedback.research.app.help");
        triesLine = Component.translatable("gui.feedback.research.app.tries");
        grantedLine = Component.translatable("gui.feedback.research.app.granted");
        lockedLine = Component.translatable("gui.feedback.research.app.locked");
        header = Component.translatable("gui.feedback.research.app.header", plan.title).getString().toUpperCase(java.util.Locale.ROOT);
        append(new Line(Component.translatable("gui.feedback.research.app.ready").getString().toUpperCase(java.util.Locale.ROOT), 0));
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        for (int c = 32; c < 127; c++)
            glyphWidth[c] = font.width(GLYPH[c]);
        helpLines = font.split(helpLine, W - 24);
        tutorial.init(font, W - 80 - 24);
        clearWidgets();
        if (game.over())
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.research.app.finish"), b -> finish(), px + STATUS_X, py + H - 52, STATUS_W, 18, null));
        else
            addRenderableWidget(tutorial.button(SKIN, px + W - 30, py + 6));
    }

    /** Queues a line for the terminal to type out. */
    private void say(String line, int colour) {
        pending.add(new Line(line.toUpperCase(java.util.Locale.ROOT), colour));
    }

    /** Adds a typed line to the terminal's log, the oldest scrolling off the top. */
    private void append(Line line) {
        System.arraycopy(log, 1, log, 0, LOG_LINES - 1);
        log[LOG_LINES - 1] = line;
    }

    /** Where a word's middle is on the screen, for the sparks that mark it. */
    private float wordX(int i) {
        return px + COL_X + (game.wordRow[i] / AppHack.ROWS) * COL_DX + ADDR_W + (game.wordCol[i] + game.words[i].length() / 2f) * CW;
    }

    private float wordY(int i) {
        return py + TOP + (game.wordRow[i] % AppHack.ROWS) * RH + RH / 2f;
    }

    private float pairX(int p) {
        return px + COL_X + (game.pairRow[p] / AppHack.ROWS) * COL_DX + ADDR_W + (game.pairOpen[p] + game.pairClose[p] + 1) / 2f * CW;
    }

    private float pairY(int p) {
        return py + TOP + (game.pairRow[p] % AppHack.ROWS) * RH + RH / 2f;
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
        if (!game.over() && tutorial.click())
            return true;
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
        say("> " + game.words[i], 0);
        GuiSkin.play(FSounds.APP_KEY.get(), 1f);
        float wx = wordX(i), wy = wordY(i);
        if (result == AppHack.Result.FOUND) {
            say(grantedLine.getString(), TXT_GREEN);
            GuiSkin.play(FSounds.APP_FOUND.get(), 1f);
            foundWord = i;
            specks.burst(wx, wy, 60, 2.6f, 1.1f, 0x5CFF7A, 34, 3);
            specks.burst(wx, wy, 24, 1.4f, 0.6f, SKIN.ink(), 26, 2);
            juice.flash(0x5CFF7A, 8, 9);
        } else {
            float close = Math.min(1f, game.likeness / (float) game.words[i].length());
            say(Component.translatable("gui.feedback.research.app.likeness", game.likeness).getString(), Juice.mix(TXT_RED, TXT_GREEN, close));
            say(Component.translatable("gui.feedback.research.app.denied").getString(), TXT_RED);
            GuiSkin.play(FSounds.APP_DENY.get(), 1f);
            juice.shake(3);
            juice.flash(RGB_RED, 6, 9);
            specks.burst(wx, wy, 18, 1.8f, 0.6f, 0xFF5A3C, 20, 2);
            triesLostAt = System.currentTimeMillis();
            lostSlot = game.tries;
            if (result == AppHack.Result.OUT) {
                say(lockedLine.getString(), TXT_RED);
                juice.shake(5);
                juice.flicker(RGB_RED, 18, 9);
            }
        }
        afterMove();
    }

    private void hack(int p) {
        boolean[] was = game.removed.clone();
        var result = game.hack(p);
        if (result == AppHack.Result.IGNORED)
            return;
        long now = System.currentTimeMillis();
        moves.add("b" + p);
        say("> HACK", 0);
        say(Component.translatable(result == AppHack.Result.REFILLED ? "gui.feedback.research.app.refilled" : "gui.feedback.research.app.removed").getString(),
                result == AppHack.Result.REFILLED ? TXT_GREEN : TXT_AMBER);
        GuiSkin.play(FSounds.APP_HACK.get(), result == AppHack.Result.REFILLED ? 1.25f : 1f);
        pairDied[p] = now;
        specks.burst(pairX(p), pairY(p), 16, 1.6f, 0.7f, SKIN.ink(), 22, 2);
        if (result == AppHack.Result.REFILLED) {
            refilledAt = now;
            juice.flash(0x5CFF7A, 6, 6);
        } else
            for (int w = 0; w < was.length; w++)
                if (!was[w] && game.removed[w]) {
                    wordDied[w] = now;
                    specks.burst(wordX(w), wordY(w), 20, 1.6f, 0.7f, 0xFF5A3C, 22, 2);
                }
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
        juice.tick();
        if (typing == null && !pending.isEmpty()) {
            typing = pending.poll();
            typed = 0;
        }
        if (typing != null) {
            typed += 3 + 3 * pending.size();
            if (typed >= typing.text().length()) {
                append(typing);
                typing = null;
            }
        }
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
        int dx = juice.dx(), dy = juice.dy();
        opening.render(g, width, height, px, py, W, H, () -> {
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            SKIN.panel(g, px, py, W, H);
            g.drawString(font, header, px + 12, py + 8, SKIN.ink(), false);
            drawTries(g);
            drawDump(g, mx, my);
            drawGlass(g);
            drawLog(g);
            specks.draw(g, partial);
            juice.draw(g, px + 3, py + 3, px + W - 3, py + H - 3);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
            if (!game.over())
                tutorial.draw(g, font, px + 40, py + 40, W - 80, GLASS, SKIN.ink(), SKIN.ink(), SKIN.ink(), SKIN.dim());
            g.pose().popPose();
        });
    }

    private void drawTries(GuiGraphics g) {
        g.drawString(font, triesLine, px + 12, py + 20, SKIN.dim(), false);
        int x0 = px + 12 + font.width(triesLine) + 6;
        long now = System.currentTimeMillis();
        boolean refilled = now - refilledAt < 500 && refilledAt != 0;
        boolean low = !game.over() && game.tries <= 1 && now / 250 % 2 == 0 && !ClientSettings.reduceMotion();
        for (int i = 0; i < game.triesStart; i++) {
            int x = x0 + i * 9;
            if (i < game.tries) {
                g.fill(x, py + 20, x + 7, py + 27, refilled ? TXT_GREEN : low ? TXT_RED : SKIN.ink());
                continue;
            }
            g.renderOutline(x, py + 20, 7, 7, SKIN.dim());
            long age = now - triesLostAt;
            if (i == lostSlot && age < 400 && !ClientSettings.reduceMotion()) {
                int grow = (int) (age / 80), a = (int) (255 * (1 - age / 400f));
                g.renderOutline(x - grow, py + 20 - grow, 7 + 2 * grow, 7 + 2 * grow, a << 24 | 0xFF5A3C);
                g.fill(x, py + 20, x + 7, py + 27, a << 24 | 0xFF5A3C);
            }
        }
    }

    private void drawDump(GuiGraphics g, int mx, int my) {
        long now = System.currentTimeMillis();
        int hoverOwner = opening.settled() ? ownerAt(mx, my) : -1;
        hover = live(hoverOwner) ? hoverOwner : -1;
        for (int r = 0; r < AppHack.LINES; r++) {
            int col = r / AppHack.ROWS, row = r % AppHack.ROWS;
            int x0 = px + COL_X + col * COL_DX, y = py + TOP + row * RH;
            g.drawString(font, address[r], x0, y, SKIN.dim(), false);
            for (int c = 0; c < AppHack.WIDTH; c++) {
                int cell = r * AppHack.WIDTH + c, o = owner[cell];
                char ch = dump(cell, o, now);
                int x = x0 + ADDR_W + c * CW;
                boolean found = o >= 0 && o == foundWord;
                boolean lit = found || o >= 0 && o == hover;
                if (lit)
                    g.fill(x, y - 1, x + CW, y + RH - 1, found ? TXT_GREEN : SKIN.ink());
                boolean dying = ch != '.' && ch != game.dump[cell];
                int colour = lit ? GLASS : dying ? TXT_RED : o >= 0 && o < 1000 && game.picked[o] ? SKIN.dim() : SKIN.ink();
                g.drawString(font, GLYPH[ch], x + (CW - glyphWidth[ch]) / 2, y, colour, false);
            }
        }
    }

    /** A faint bright band sweeping down the glass, and a red wash once the terminal has locked. */
    private void drawGlass(GuiGraphics g) {
        int x0 = px + COL_X, x1 = px + COL_X + AppHack.COLS * COL_DX, top = py + TOP, bottom = py + TOP + AppHack.ROWS * RH;
        if (game.over() && !game.found)
            g.fill(x0, top, x1, bottom, 0x1CFF3020);
        if (ClientSettings.reduceMotion())
            return;
        int span = AppHack.ROWS * RH + 40, y = top - 20 + (int) (System.currentTimeMillis() / 25 % span);
        int y0 = Math.max(y, top), y1 = Math.min(y + 6, bottom);
        if (y1 > y0)
            g.fill(x0, y0, x1, y1, (SKIN.ink() & 0xFFFFFF) | 0x16000000);
    }

    /** When a word or bracket pair was cut, or 0. */
    private long dyingAt(int o) {
        return o < 1000 ? wordDied[o] : pairDied[o - 1000];
    }

    /** The character a cell shows: a removed word and a used bracket pair flicker through junk for a moment, then read as dots. */
    private char dump(int cell, int o, long now) {
        if (o >= 0 && (o < 1000 ? game.removed[o] : game.pairUsed[o - 1000])) {
            long age = now - dyingAt(o);
            if (dyingAt(o) != 0 && age < 450 && !ClientSettings.reduceMotion())
                return SCRAMBLE.charAt((int) ((cell * 7L + age / 60) % SCRAMBLE.length()));
            return '.';
        }
        return game.dump[cell];
    }

    private int ownerAt(int mx, int my) {
        int cell = cellAt(mx, my);
        return cell < 0 ? -1 : owner[cell];
    }

    private void drawLog(GuiGraphics g) {
        int x = px + STATUS_X, y0 = py + TOP;
        // while a line is being typed the log sits one row higher to make room for it
        int shift = typing != null ? 1 : 0, last = -1;
        for (int i = 0; i < LOG_LINES; i++) {
            Line line = log[i];
            int row = i - shift;
            if (line == null || row < 0)
                continue;
            int colour = line.colour() != 0 ? line.colour() : i == LOG_LINES - 1 && typing == null ? SKIN.ink() : SKIN.dim();
            g.drawString(font, line.text(), x, y0 + row * RH, colour, false);
            last = row;
        }
        int cx = x, cy = y0;
        if (typing != null) {
            String shown = typing.text().substring(0, Math.min(typed, typing.text().length()));
            g.drawString(font, shown, x, y0 + (LOG_LINES - 1) * RH, typing.colour() != 0 ? typing.colour() : SKIN.ink(), false);
            cx = x + font.width(shown) + 2;
            cy = y0 + (LOG_LINES - 1) * RH;
        } else if (last >= 0) {
            cx = x + font.width(log[LOG_LINES - 1].text()) + 2;
            cy = y0 + last * RH;
        }
        if (game.over()) {
            GuiStyle.stamp(g, Component.literal(letter()), px + STATUS_X + STATUS_W / 2f, py + H - 80, System.currentTimeMillis() - endedAt,
                    game.found ? 0x5CFF7A : 0xFFB02E);
            return;
        }
        // the blinking cursor sits after the newest line
        if (System.currentTimeMillis() / 450 % 2 == 0)
            g.fill(cx, cy, cx + 5, cy + 7, SKIN.ink());
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
