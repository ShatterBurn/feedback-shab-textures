// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;
import io.github.soundgoodizerfan.feedback.modular.research.RitePurify;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;
import io.github.soundgoodizerfan.feedback.net.ResearchResultPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rite research game ({@link RitePurify}) on the enchanting table's mystic skin: a five by five grid of murk to cleanse, a tray of three
 * pieces, no clock. Click a tray piece, click the grid to set it down; a full row or column clears, and murk goes only where the line also holds
 * its opposite element. Re-draw the tray for one ink, or take the residue. What is sent is what was done (the moves in order); the server replays
 * them ({@link RitePurify#replay}) and the stamp shown here is only a preview. Esc before the end sends nothing.
 *
 * <p>The game's cell arrays are read directly in the draw path; nothing is allocated per frame (a click copies the grid once to find what a move
 * removed, which is where the cleanse sparks go).
 */
final class RitePurifyScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.MYSTIC;
    private static final int W = 300, H = 236, CELL = 26, BX = 18, BY = 50, TX = 196, TY = 50, MINI = 8, SLOT = 38, SLOT_GAP = 6;
    /** Fire, water, earth, air, light, dark: the pieces' faces. */
    private static final int[] COLOUR = {0xFFE8542A, 0xFF3C8CE8, 0xFF9A7240, 0xFFCFE6EE, 0xFFFFE070, 0xFF6B44A8};

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final RitePurify game;
    private final List<String> moves = new ArrayList<>();
    private final Specks specks = new Specks(160);
    private final byte[] before = new byte[RitePurify.SIZE * RitePurify.SIZE];
    private final String[] initial = new String[RitePurify.ELEMENTS];
    private int px, py, selected, flash;
    private boolean sent, stuck;
    private long endedAt;
    private Button redraw, residue;
    private List<net.minecraft.util.FormattedCharSequence> helpLines = List.of();

    RitePurifyScreen(Screen parent, ResearchOpenPayload session) {
        super(Component.translatable("gui.feedback.research.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.game = new RitePurify(RitePurify.Params.decode(session.plan()), session.seed());
        for (int e = 0; e < RitePurify.ELEMENTS; e++) {
            String name = Component.translatable("gui.feedback.research.rite.element." + RitePurify.ELEMENT_NAMES[e]).getString();
            initial[e] = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        }
        stuck = !game.canPlay();
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        helpLines = font.split(Component.translatable("gui.feedback.research.rite.help"), W - 28);
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        if (game.over()) {
            addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.research.rite.finish"), b -> finish(), px + TX, py + H - 54, 84, 18, null));
            return;
        }
        redraw = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.rite.redraw"), b -> redraw(), px + TX, py + 178, 84, 16));
        residue = addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.research.rite.residue"), b -> residue(), px + TX, py + 196, 84, 16));
    }

    private void redraw() {
        if (!game.redraw())
            return;
        moves.add("d");
        GuiSkin.play(FSounds.RITE_PLACE.get(), 0.6f);
        selected = 0;
        stuck = !game.canPlay();
    }

    private void residue() {
        game.takeResidue();
        moves.add("r");
        ended();
    }

    private void ended() {
        endedAt = System.currentTimeMillis();
        rebuild();
    }

    private int cellAt(double mx, double my) {
        int x = (int) Math.floor((mx - px - BX) / CELL), y = (int) Math.floor((my - py - BY) / CELL);
        return x < 0 || y < 0 || x >= RitePurify.SIZE || y >= RitePurify.SIZE ? -1 : y * RitePurify.SIZE + x;
    }

    private int slotAt(double mx, double my) {
        if (mx < px + TX || mx >= px + TX + SLOT + 40)
            return -1;
        int s = (int) Math.floor((my - py - TY) / (double) (SLOT + SLOT_GAP));
        return s < 0 || s >= RitePurify.TRAY || my - py - TY - s * (SLOT + SLOT_GAP) >= SLOT ? -1 : s;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled() || button != 0)
            return !opening.settled() || super.mouseClicked(mx, my, button);
        if (super.mouseClicked(mx, my, button))
            return true;
        if (game.over())
            return true;
        int slot = slotAt(mx, my);
        if (slot >= 0 && game.tray[slot] != null) {
            selected = slot;
            GuiSkin.play(FSounds.RITE_PLACE.get(), 1.5f, 0.4f);
            return true;
        }
        int cell = cellAt(mx, my);
        if (cell >= 0)
            place(cell % RitePurify.SIZE, cell / RitePurify.SIZE);
        return true;
    }

    private void place(int x, int y) {
        if (!game.fits(selected, x, y)) {
            GuiSkin.play(FSounds.RITE_STUCK.get(), 1.4f, 0.5f);
            return;
        }
        System.arraycopy(game.kind, 0, before, 0, before.length);
        int stageBefore = game.stage;
        int[] face = new int[before.length];
        for (int c = 0; c < face.length; c++)
            face[c] = COLOUR[game.element[c]];
        var piece = game.tray[selected];
        var out = game.place(selected, x, y);
        if (!out.placed())
            return;
        moves.add("p" + selected + "@" + x + "," + y);
        boolean sameGrid = game.stage == stageBefore;
        GuiSkin.play(FSounds.RITE_PLACE.get(), 0.9f + 0.05f * piece.cells());
        if (sameGrid) {
            for (int c = 0; c < before.length; c++)
                if (before[c] != RitePurify.EMPTY && game.kind[c] == RitePurify.EMPTY)
                    spark(c, face[c], before[c] == RitePurify.MURK);
        } else {
            // the grid was renewed: sparks from every murk cell the last stage held
            for (int c = 0; c < before.length; c++)
                if (before[c] == RitePurify.MURK)
                    spark(c, face[c], true);
        }
        if (out.cleansed() > 0) {
            GuiSkin.play(FSounds.RITE_CLEANSE.get(), 0.9f + 0.08f * Math.min(4, out.cleansed()));
            flash = 8;
        } else if (out.lines() > 0)
            GuiSkin.play(FSounds.RITE_CLEAR.get(), 1f);
        selected = nextSlot();
        // a run as long as the server will replay is ended there, as if the residue were taken
        if (!game.over() && moves.size() >= RitePurify.MAX_MOVES - 1) {
            game.takeResidue();
            moves.add("r");
        }
        if (game.over())
            ended();
        else if (!game.canPlay() && !stuck) {
            GuiSkin.play(FSounds.RITE_STUCK.get(), 0.8f);
            stuck = true;
        } else
            stuck = !game.canPlay();
    }

    /** The first tray slot still holding a piece, so the selection never rests on an empty one. */
    private int nextSlot() {
        for (int i = 0; i < RitePurify.TRAY; i++)
            if (game.tray[(selected + i) % RitePurify.TRAY] != null)
                return (selected + i) % RitePurify.TRAY;
        return 0;
    }

    private void spark(int cell, int colour, boolean murk) {
        float cx = px + BX + (cell % RitePurify.SIZE) * CELL + CELL / 2f, cy = py + BY + (cell / RitePurify.SIZE) * CELL + CELL / 2f;
        specks.burst(cx, cy, murk ? 14 : 6, murk ? 1.6f : 0.9f, murk ? 0.9f : 0.3f, murk ? SKIN.accent() : colour, 24, 2);
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
        specks.tick(0.04f);
        if (flash > 0)
            flash--;
    }

    private String letter() {
        float q = game.quality();
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
            g.drawString(font, Component.translatable("gui.feedback.research.rite.header",
                    Component.translatable("gui.feedback.research.rite.element." + RitePurify.ELEMENT_NAMES[game.params.aspect()])), px + 14, py + 10, SKIN.ink(), false);
            drawStatus(g);
            drawBoard(g, mx, my);
            drawTray(g, mx, my);
            specks.draw(g, partial);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
            if (game.over())
                GuiStyle.stamp(g, Component.literal(letter()), px + TX + 42, py + H - 90, System.currentTimeMillis() - endedAt, game.taken ? 0xFFB02E : 0x5CFF7A);
            else if (stuck)
                g.drawString(font, Component.translatable("gui.feedback.research.rite.stuck"), px + BX, py + 188, SKIN.warn(), false);
            for (int i = 0; i < helpLines.size(); i++)
                g.drawString(font, helpLines.get(i), px + 14, py + H - 22 + i * 9, SKIN.dim(), false);
        });
    }

    private void drawStatus(GuiGraphics g) {
        g.drawString(font, Component.translatable("gui.feedback.research.rite.stage", Math.min(game.stage + 1, game.params.stages()), game.params.stages()),
                px + 14, py + 24, SKIN.dim(), false);
        Component inkLabel = Component.translatable("gui.feedback.research.rite.ink");
        g.drawString(font, inkLabel, px + 14, py + 35, SKIN.dim(), false);
        int x0 = px + 14 + font.width(inkLabel) + 6;
        for (int i = 0; i < Math.min(game.params.ink(), 18); i++) {
            int x = x0 + i * 7;
            if (i < game.ink)
                g.fill(x, py + 35, x + 5, py + 42, SKIN.accent());
            else
                g.renderOutline(x, py + 35, 5, 7, SKIN.dim());
        }
        g.drawString(font, Component.translatable("gui.feedback.research.rite.murk", game.murkLeft), px + TX, py + 24, SKIN.ink(), false);
        g.drawString(font, Component.translatable("gui.feedback.research.rite.pieces", game.pieces), px + TX, py + 35, SKIN.dim(), false);
    }

    private void drawBoard(GuiGraphics g, int mx, int my) {
        int n = RitePurify.SIZE;
        SKIN.inset(g, px + BX - 4, py + BY - 4, n * CELL + 8, n * CELL + 8);
        long time = System.currentTimeMillis();
        for (int c = 0; c < n * n; c++) {
            int x = px + BX + (c % n) * CELL, y = py + BY + (c / n) * CELL;
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x22FFFFFF);
            byte kind = game.kind[c];
            if (kind == RitePurify.EMPTY)
                continue;
            int e = game.element[c], colour = COLOUR[e];
            if (kind == RitePurify.MURK) {
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, shade(colour, 0.38f));
                for (int d = 4; d < CELL - 4; d += 6) {
                    g.fill(x + d, y + 3, x + d + 1, y + CELL - 3, shade(colour, 0.55f));
                }
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, shade(colour, 1f), false);
            } else if (kind == RitePurify.ANCHOR) {
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, colour);
                float pulse = 0.6f + 0.4f * (float) Math.sin(time / 380.0 + c);
                g.renderOutline(x + 1, y + 1, CELL - 2, CELL - 2, withAlpha(SKIN.accent(), pulse));
                g.renderOutline(x + 3, y + 3, CELL - 6, CELL - 6, 0xFF1A1030);
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, 0xFF1A1030, false);
            } else {
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, colour);
                g.fill(x + 1, y + 1, x + CELL - 1, y + 3, 0x55FFFFFF);
                g.fill(x + 1, y + CELL - 3, x + CELL - 1, y + CELL - 1, 0x44000000);
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, 0xFF1A1030, false);
            }
        }
        if (flash > 0)
            g.fill(px + BX, py + BY, px + BX + n * CELL, py + BY + n * CELL, (flash * 6) << 24 | 0xFFF2CC);
        // the ghost of the selected piece under the pointer
        int cell = opening.settled() && !game.over() ? cellAt(mx, my) : -1;
        if (cell >= 0 && game.tray[selected] != null) {
            int hx = cell % n, hy = cell / n;
            boolean ok = game.fits(selected, hx, hy);
            int colour = ok ? withAlpha(COLOUR[game.tray[selected].element()], 0.55f) : 0x66FF4040;
            for (int[] o : RitePurify.SHAPES[game.tray[selected].shape()]) {
                int gx = hx + o[0], gy = hy + o[1];
                if (gx >= n || gy >= n)
                    continue;
                int x = px + BX + gx * CELL, y = py + BY + gy * CELL;
                g.fill(x + 2, y + 2, x + CELL - 2, y + CELL - 2, colour);
            }
        }
    }

    private void drawTray(GuiGraphics g, int mx, int my) {
        for (int s = 0; s < RitePurify.TRAY; s++) {
            int x = px + TX, y = py + TY + s * (SLOT + SLOT_GAP);
            boolean live = game.tray[s] != null;
            SKIN.plaque(g, x, y, SLOT + 40, SLOT);
            if (!live)
                continue;
            if (s == selected && !game.over())
                g.renderOutline(x - 1, y - 1, SLOT + 42, SLOT + 2, SKIN.accent());
            var piece = game.tray[s];
            int colour = COLOUR[piece.element()];
            for (int[] o : RitePurify.SHAPES[piece.shape()]) {
                int cx = x + 6 + o[0] * MINI, cy = y + 3 + o[1] * MINI;
                g.fill(cx, cy, cx + MINI - 1, cy + MINI - 1, colour);
            }
            g.drawString(font, Component.translatable("gui.feedback.research.rite.element." + RitePurify.ELEMENT_NAMES[piece.element()]), x + 6 + 4 * MINI + 4, y + 15, SKIN.dim(), false);
        }
    }

    private static int shade(int argb, float f) {
        int r = (int) (((argb >> 16) & 0xFF) * f), gr = (int) (((argb >> 8) & 0xFF) * f), b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, gr) << 8 | Math.min(255, b);
    }

    private static int withAlpha(int argb, float a) {
        return ((int) (Math.max(0f, Math.min(1f, a)) * 255) << 24) | (argb & 0xFFFFFF);
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
