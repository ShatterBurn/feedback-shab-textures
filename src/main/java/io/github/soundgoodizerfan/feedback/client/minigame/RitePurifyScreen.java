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
import io.github.soundgoodizerfan.feedback.modular.research.RitePurify;
import io.github.soundgoodizerfan.feedback.net.ResearchOpenPayload;
import io.github.soundgoodizerfan.feedback.net.ResearchResultPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The rite research game ({@link RitePurify}) on the enchanting table's mystic skin: a five by five grid of murk to cleanse, a tray of three
 * pieces, no clock. Drag a tray piece onto the grid and let go to set it down; a full row or column clears, and murk goes only where the line also holds
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
    /** The how-to window's backing, a deep violet (the window is its own sheet, in its own colour). */
    /** The tile atlas: 13 px tiles, columns by element, rows piece / murk / anchor, then 8 px tray tiles; board tiles are drawn at twice the size. */
    private static final ResourceLocation TILES = ResourceLocation.fromNamespaceAndPath("feedback", "textures/gui/rite_tiles.png");
    private static final int TW = 78, TH = 47, TILE = 13;
    private static final int VEIL = 0xF0140A26;
    private static final int[] COLOUR = {0xFFE8542A, 0xFF3C8CE8, 0xFF9A7240, 0xFFCFE6EE, 0xFFFFE070, 0xFF6B44A8};

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final RitePurify game;
    private final List<String> moves = new ArrayList<>();
    private final Specks specks = new Specks(160);
    private final byte[] before = new byte[RitePurify.SIZE * RitePurify.SIZE];
    private final String[] initial = new String[RitePurify.ELEMENTS];
    private final Juice juice = new Juice();
    /** When each cell was last filled and last cleared (motion clock ticks), and the colour a cleared cell fades out in. */
    private final float[] placedAt = new float[RitePurify.SIZE * RitePurify.SIZE], clearedAt = new float[RitePurify.SIZE * RitePurify.SIZE];
    private final int[] clearedColour = new int[RitePurify.SIZE * RitePurify.SIZE];
    /** Placements in a row that cleansed murk: the cleanse chime climbs with it. */
    private int px, py, selected, streak;
    /** The tray slot held on the pointer (-1 for none), when it was lifted, and the flight of a refused piece back to its slot. */
    private int dragSlot = -1, backSlot = -1;
    private float grabAt, backAt;
    private double backX, backY;
    private boolean sent, stuck;
    private final Tutorial tutorial = new Tutorial("rite", "gui.feedback.research.rite.tutorial", 9);
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
        java.util.Arrays.fill(placedAt, Float.NEGATIVE_INFINITY);
        java.util.Arrays.fill(clearedAt, Float.NEGATIVE_INFINITY);
    }

    @Override
    protected void init() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        opening.start();
        helpLines = font.split(Component.translatable("gui.feedback.research.rite.help"), W - 28);
        tutorial.init(font, W - 40 - 24);
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
        addRenderableWidget(tutorial.button(SKIN, px + W - 28, py + 6));
    }

    private void redraw() {
        if (!game.redraw())
            return;
        moves.add("d");
        GuiSkin.play(FSounds.RITE_PLACE.get(), 0.6f);
        selected = 0;
        stuck = !game.canPlay();
        if (game.over())
            ended();
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
        if (!game.over() && tutorial.click())
            return true;
        if (super.mouseClicked(mx, my, button))
            return true;
        if (game.over())
            return true;
        int slot = slotAt(mx, my);
        if (slot >= 0 && game.tray[slot] != null) {
            selected = slot;
            dragSlot = slot;
            backSlot = -1;
            grabAt = GuiStyle.time();
            GuiSkin.play(FSounds.RITE_PLACE.get(), 1.5f, 0.4f);
        }
        return true;
    }

    /** Letting go sets the held piece on the grid cell it hangs over, or sends it back to its slot. */
    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragSlot < 0 || button != 0)
            return super.mouseReleased(mx, my, button);
        int slot = dragSlot;
        dragSlot = -1;
        if (!opening.settled() || game.over() || game.tray[slot] == null)
            return true;
        int[] o = snap(slot, mx, my);
        if (game.fits(slot, o[0], o[1])) {
            selected = slot;
            place(o[0], o[1]);
            return true;
        }
        int n = RitePurify.SIZE;
        if (mx >= px + BX && mx < px + BX + n * CELL && my >= py + BY && my < py + BY + n * CELL) {
            GuiSkin.play(FSounds.RITE_STUCK.get(), 1.4f, 0.5f);
            juice.shake(3);
        }
        if (ClientSettings.reduceMotion())
            return true;
        backSlot = slot;
        backX = mx;
        backY = my;
        backAt = GuiStyle.time();
        return true;
    }

    private static int shapeW(int shape) {
        int w = 0;
        for (int[] o : RitePurify.SHAPES[shape])
            w = Math.max(w, o[0] + 1);
        return w;
    }

    private static int shapeH(int shape) {
        int h = 0;
        for (int[] o : RitePurify.SHAPES[shape])
            h = Math.max(h, o[1] + 1);
        return h;
    }

    /** The grid cell the held piece's top-left would land on: the piece hangs centred on the pointer and snaps to the nearest cell. */
    private int[] snap(int slot, double mx, double my) {
        int shape = game.tray[slot].shape();
        return new int[] {(int) Math.round((mx - shapeW(shape) * CELL / 2.0 - px - BX) / CELL),
                (int) Math.round((my - shapeH(shape) * CELL / 2.0 - py - BY) / CELL)};
    }

    private void place(int x, int y) {
        if (!game.fits(selected, x, y)) {
            GuiSkin.play(FSounds.RITE_STUCK.get(), 1.4f, 0.5f);
            juice.shake(3);
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
        float now = GuiStyle.time();
        if (sameGrid) {
            for (int c = 0; c < before.length; c++) {
                if (before[c] == RitePurify.EMPTY && game.kind[c] != RitePurify.EMPTY)
                    placedAt[c] = now;
                else if (before[c] != RitePurify.EMPTY && game.kind[c] == RitePurify.EMPTY)
                    spark(c, face[c], before[c] == RitePurify.MURK);
            }
        } else {
            // the grid was renewed: sparks from every murk cell the last stage held
            for (int c = 0; c < before.length; c++)
                if (before[c] == RitePurify.MURK)
                    spark(c, face[c], true);
        }
        if (out.cleansed() > 0) {
            int power = Math.min(4, out.cleansed());
            GuiSkin.play(FSounds.RITE_CLEANSE.get(), Juice.climb(0.9f + 0.08f * power, streak));
            juice.flash(0xFFF2CC, 6 + 2 * power, 5 + 3 * power);
            if (out.cleansed() > 1)
                juice.shake(1 + Math.min(2, out.cleansed() / 2));
            streak++;
        } else {
            streak = 0;
            if (out.lines() > 0) {
                GuiSkin.play(FSounds.RITE_CLEAR.get(), 1f);
                juice.flash(0xFFF2CC, 5, 4);
            }
        }
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
            juice.shake(5);
            juice.flash(0xFF4040, 8, 6);
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
        clearedAt[cell] = GuiStyle.time();
        clearedColour[cell] = murk ? shade(colour, 0.6f) : colour;
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
        if (backSlot >= 0 && GuiStyle.time() - backAt >= 8)
            backSlot = -1;
        specks.tick(0.04f);
        juice.tick();
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
        int dx = juice.dx(), dy = juice.dy();
        opening.render(g, width, height, px, py, W, H, () -> {
            SKIN.panel(g, px, py, W, H);
            g.drawString(font, Component.translatable("gui.feedback.research.rite.header",
                    Component.translatable("gui.feedback.research.rite.element." + RitePurify.ELEMENT_NAMES[game.params.aspect()])), px + 14, py + 10, SKIN.ink(), false);
            drawStatus(g);
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            drawBoard(g, mx, my);
            g.pose().popPose();
            drawTray(g, mx, my);
            specks.draw(g, partial);
            for (var widget : renderables)
                widget.render(g, mx, my, partial);
            drawCarried(g, mx, my);
            if (game.over())
                GuiStyle.stamp(g, Component.literal(letter()), px + TX + 42, py + H - 90, System.currentTimeMillis() - endedAt, game.taken ? 0xFFB02E : 0x5CFF7A);
            else if (stuck)
                g.drawString(font, Component.translatable("gui.feedback.research.rite.stuck"), px + BX, py + 188, SKIN.warn(), false);
            for (int i = 0; i < helpLines.size(); i++)
                g.drawString(font, helpLines.get(i), px + 14, py + H - 22 + i * 9, SKIN.dim(), false);
            if (!game.over())
                tutorial.draw(g, font, px + 20, py + 28, W - 40, VEIL, SKIN.accent(), SKIN.accent(), SKIN.ink(), SKIN.dim());
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
        float clock = GuiStyle.time();
        boolean still = ClientSettings.reduceMotion();
        for (int c = 0; c < n * n; c++) {
            int x = px + BX + (c % n) * CELL, y = py + BY + (c / n) * CELL;
            g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x22FFFFFF);
            byte kind = game.kind[c];
            if (kind == RitePurify.EMPTY) {
                // a cleared cell shrinks away in the colour it had
                float gone = (clock - clearedAt[c]) / 8f;
                if (!still && gone >= 0 && gone < 1) {
                    int shrink = 1 + (int) (gone * (CELL / 2 - 2));
                    g.fill(x + shrink, y + shrink, x + CELL - shrink, y + CELL - shrink, ((int) (200 * (1 - gone)) << 24) | (clearedColour[c] & 0xFFFFFF));
                }
                continue;
            }
            // a piece just set down drops in: a bright halo that settles into its cell
            float settle = (clock - placedAt[c]) / 6f;
            if (!still && settle >= 0 && settle < 1) {
                int pop = (int) (4 * (1 - GuiStyle.easeOut(settle)));
                g.fill(x + 1 - pop, y + 1 - pop, x + CELL - 1 + pop, y + CELL - 1 + pop, 0x66FFFFFF);
            }
            int e = game.element[c], colour = COLOUR[e];
            if (kind == RitePurify.MURK) {
                tile(g, 1, e, x, y);
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, shade(colour, 1f), false);
            } else if (kind == RitePurify.ANCHOR) {
                tile(g, 2, e, x, y);
                float pulse = 0.6f + 0.4f * (float) Math.sin(time / 380.0 + c);
                g.renderOutline(x + 1, y + 1, CELL - 2, CELL - 2, withAlpha(SKIN.accent(), pulse));
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, 0xFF1A1030, false);
            } else {
                tile(g, 0, e, x, y);
                g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, 0xFF1A1030, false);
            }
        }
        juice.draw(g, px + BX, py + BY, px + BX + n * CELL, py + BY + n * CELL);
        // the held piece's landing: its cells, and any row or column it would fill
        if (dragSlot >= 0 && opening.settled() && !game.over() && game.tray[dragSlot] != null) {
            int[] at = snap(dragSlot, mx, my);
            int hx = at[0], hy = at[1];
            boolean ok = game.fits(dragSlot, hx, hy);
            var piece = game.tray[dragSlot];
            int[][] shape = RitePurify.SHAPES[piece.shape()];
            int colour = ok ? withAlpha(COLOUR[piece.element()], 0.55f) : 0x66FF4040;
            float beat = still ? 0.8f : 0.55f + 0.35f * (float) Math.sin(time / 140.0);
            if (ok) {
                boolean[] full = new boolean[n * n];
                for (int c = 0; c < n * n; c++)
                    full[c] = game.kind[c] != RitePurify.EMPTY;
                for (int[] o : shape)
                    full[(hy + o[1]) * n + hx + o[0]] = true;
                for (int r = 0; r < n; r++) {
                    boolean row = true, col = true;
                    for (int k = 0; k < n; k++) {
                        row &= full[r * n + k];
                        col &= full[k * n + r];
                    }
                    if (row) {
                        g.fill(px + BX, py + BY + r * CELL, px + BX + n * CELL, py + BY + (r + 1) * CELL, withAlpha(0xFFF2CC, 0.22f * beat));
                        g.renderOutline(px + BX, py + BY + r * CELL, n * CELL, CELL, withAlpha(SKIN.accent(), beat));
                    }
                    if (col) {
                        g.fill(px + BX + r * CELL, py + BY, px + BX + (r + 1) * CELL, py + BY + n * CELL, withAlpha(0xFFF2CC, 0.22f * beat));
                        g.renderOutline(px + BX + r * CELL, py + BY, CELL, n * CELL, withAlpha(SKIN.accent(), beat));
                    }
                }
            }
            for (int[] o : shape) {
                int gx = hx + o[0], gy = hy + o[1];
                if (gx < 0 || gy < 0 || gx >= n || gy >= n)
                    continue;
                int x = px + BX + gx * CELL, y = py + BY + gy * CELL;
                g.fill(x + 2, y + 2, x + CELL - 2, y + CELL - 2, colour);
                g.renderOutline(x + 2, y + 2, CELL - 4, CELL - 4, withAlpha(ok ? 0xFF5CFF7A : 0xFFFF4040, beat));
            }
        }
    }

    /** One 13 px tile of the atlas (row 0 piece, 1 murk, 2 anchor) of element {@code e}, drawn at twice the size. */
    private static void tile(GuiGraphics g, int row, int e, int x, int y) {
        g.blit(TILES, x, y, CELL, CELL, e * TILE, row * TILE, TILE, TILE, TW, TH);
    }

    /** A tray piece at full size centred on {@code cx, cy}, scaled from the tray's size, with a shadow and a slight tilt: held on the pointer or flying home. */
    private void drawHeld(GuiGraphics g, int slot, double cx, double cy, float scale, float tilt) {
        var piece = game.tray[slot];
        if (piece == null)
            return;
        int shape = piece.shape(), e = piece.element();
        float ox = -shapeW(shape) * CELL / 2f, oy = -shapeH(shape) * CELL / 2f;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 250);
        g.pose().scale(scale, scale, 1);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tilt));
        for (int[] o : RitePurify.SHAPES[shape])
            g.fill((int) (ox + o[0] * CELL) + 3, (int) (oy + o[1] * CELL) + 4, (int) (ox + (o[0] + 1) * CELL) + 3, (int) (oy + (o[1] + 1) * CELL) + 4, 0x55000000);
        for (int[] o : RitePurify.SHAPES[shape]) {
            int x = (int) (ox + o[0] * CELL), y = (int) (oy + o[1] * CELL);
            tile(g, 0, e, x, y);
            g.drawString(font, initial[e], x + (CELL - font.width(initial[e])) / 2, y + 9, 0xFF1A1030, false);
        }
        g.pose().popPose();
    }

    /** Draws the piece on the pointer, or the one a refused drop sends back to its slot. */
    private void drawCarried(GuiGraphics g, int mx, int my) {
        boolean still = ClientSettings.reduceMotion();
        float tray = MINI / (float) CELL, clock = GuiStyle.time();
        if (dragSlot >= 0 && !game.over()) {
            float k = still ? 1 : GuiStyle.easeOut(Math.min(1f, (clock - grabAt) / 5f));
            drawHeld(g, dragSlot, mx, my, tray + (1 - tray) * k, still ? 0 : 3 * k);
        } else if (backSlot >= 0 && !still) {
            float t = GuiStyle.easeOut(Math.min(1f, (clock - backAt) / 8f));
            int slotX = px + TX + 6 + 2 * MINI, slotY = py + TY + backSlot * (SLOT + SLOT_GAP) + SLOT / 2;
            drawHeld(g, backSlot, backX + (slotX - backX) * t, backY + (slotY - backY) * t, 1 + (tray - 1) * t, 3 * (1 - t));
        }
    }

    private void drawTray(GuiGraphics g, int mx, int my) {
        for (int s = 0; s < RitePurify.TRAY; s++) {
            int x = px + TX, y = py + TY + s * (SLOT + SLOT_GAP);
            boolean live = game.tray[s] != null && s != dragSlot && s != backSlot;
            SKIN.plaque(g, x, y, SLOT + 40, SLOT);
            if (s == dragSlot && !game.over())
                g.renderOutline(x - 1, y - 1, SLOT + 42, SLOT + 2, SKIN.accent());
            if (!live)
                continue;
            var piece = game.tray[s];
            for (int[] o : RitePurify.SHAPES[piece.shape()]) {
                int cx = x + 6 + o[0] * MINI, cy = y + 3 + o[1] * MINI;
                g.blit(TILES, cx, cy, MINI, MINI, piece.element() * MINI, 3 * TILE, MINI, MINI, TW, TH);
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
