// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The rite research game: purify a grid. Pure rules, run by the client to play and by the server to replay (what the client sends is the moves,
 * never a score). Nothing here touches the world or a screen.
 *
 * <p>A stage is a {@value #SIZE} by {@value #SIZE} grid holding <b>murk</b> (cells of an element: the unrefined effect) and a few <b>anchors</b>
 * (cells of the effect's aspect, which are never cleared and count as filled). The tray holds {@value #TRAY} pieces of one element each, placed
 * anywhere they fit and never rotated. A full row or column clears: every piece in it is removed, and a murk cell in it is cleansed only when the
 * line also holds that murk's <b>opposite</b> element (any cell of it: a piece, an anchor or other murk). A line without the right opposite clears
 * the player's own pieces and leaves the murk. A stage is done when no murk is left; a rite is {@code stages} of them, each grid thicker.
 * No clock: the tray can be re-drawn for one ink, and the player can take the residue at any time, which grades what was cleansed.
 *
 * <p>Elements are three opposing pairs: fire and water, earth and air, light and dark ({@link #opposite}). The figures that vary a run are in
 * {@link Params}: the aspect and stage count (the effect's), how thick the murk is and how awkward the pieces are (the offering's), and an element
 * the tray leans toward (the reagent's). Everything random comes from one {@link Random} seeded once and consumed in play order, so a replay of the
 * same moves over the same params and seed reaches the same grid.
 */
public final class RitePurify {

    public static final int SIZE = 5, TRAY = 3, ELEMENTS = 6, MAX_STAGES = 3;
    /** The most moves a run is replayed for; a longer list is cut. */
    public static final int MAX_MOVES = 160;
    public static final String[] ELEMENT_NAMES = {"fire", "water", "earth", "air", "light", "dark"};

    /** What a cell holds. */
    public static final byte EMPTY = 0, PIECE = 1, MURK = 2, ANCHOR = 3;

    /**
     * The shapes of tray pieces as {@code {x, y}} cell offsets, never rotated. The first {@link #TIDY} are the tidy ones (dot, bars, square); the
     * rest are the awkward ones (corners, tees, steps).
     */
    public static final int[][][] SHAPES = {
            {{0, 0}},
            {{0, 0}, {1, 0}}, {{0, 0}, {0, 1}},
            {{0, 0}, {1, 0}, {2, 0}}, {{0, 0}, {0, 1}, {0, 2}},
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}}, {{0, 0}, {0, 1}, {0, 2}, {0, 3}},
            // awkward: corners of three, then the four-cell tee, step and hook shapes
            {{0, 0}, {1, 0}, {0, 1}}, {{0, 0}, {1, 0}, {1, 1}}, {{0, 0}, {0, 1}, {1, 1}}, {{1, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {1, 0}, {2, 0}, {1, 1}}, {{1, 0}, {0, 1}, {1, 1}, {2, 1}},
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}}, {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {0, 1}, {0, 2}, {1, 2}}, {{1, 0}, {1, 1}, {1, 2}, {0, 2}}
    };
    public static final int TIDY = 8;

    /**
     * The figures of one run. {@code aspect} is the element the rite refines to and {@code stages} how many grids; {@code thick} 0 to 1 how much murk a
     * grid starts with and {@code awkward} 0 to 1 how often the tray deals the awkward shapes; {@code reagent} whether a reagent leans the tray toward the element
     * that cleanses the thickest murk; {@code ink} what re-draws are paid from.
     */
    public record Params(int aspect, int stages, float thick, float awkward, boolean reagent, int ink) {

        public Params {
            aspect = Math.floorMod(aspect, ELEMENTS);
            stages = Math.max(1, Math.min(MAX_STAGES, stages));
            // held to the hundredth, which is all the wire form carries, so the server's figures and the client's decoded ones are the same floats
            thick = Math.round(Math.max(0f, Math.min(1f, thick)) * 100f) / 100f;
            awkward = Math.round(Math.max(0f, Math.min(1f, awkward)) * 100f) / 100f;
            ink = Math.max(0, ink);
        }

        /** The wire form, one token string the open payload carries. */
        public String encode() {
            return "rite;" + aspect + ";" + stages + ";" + Math.round(thick * 100f) + ";" + Math.round(awkward * 100f) + ";" + (reagent ? 1 : 0) + ";" + ink;
        }

        /** Whether {@code s} is an encoded run (a plan's json never begins so). */
        public static boolean is(String s) {
            return s != null && s.startsWith("rite;");
        }

        public static Params decode(String s) {
            String[] t = s.split(";");
            return new Params(Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]) / 100f, Integer.parseInt(t[4]) / 100f,
                    Integer.parseInt(t[5]) != 0, Integer.parseInt(t[6]));
        }
    }

    /** A piece in the tray: a shape index and an element. */
    public record Piece(int shape, int element) {
        public int cells() {
            return SHAPES[shape].length;
        }
    }

    public final Params params;
    private final Random random;
    public final byte[] kind = new byte[SIZE * SIZE];
    public final byte[] element = new byte[SIZE * SIZE];
    public final Piece[] tray = new Piece[TRAY];
    public int stage;
    /** Murk the current stage began with, and how many cells of it are still there. */
    public int murkStart, murkLeft;
    public int ink;
    public int pieces, redraws;
    public boolean taken;
    private int par;
    private float finishedShare;

    public RitePurify(Params params, int seed) {
        this.params = params;
        this.ink = params.ink();
        this.random = new Random(seed * 0x9E3779B97F4A7C15L + 0x51ED);
        startStage();
        fillTray();
    }

    public static int opposite(int element) {
        return element ^ 1;
    }

    /** Whether the whole rite is done: every stage cleansed, or the residue taken. */
    public boolean over() {
        return taken || stage >= params.stages();
    }

    // --- the grid ---------------------------------------------------------------------------------

    private void startStage() {
        java.util.Arrays.fill(kind, EMPTY);
        java.util.Arrays.fill(element, (byte) 0);
        // the anchors: two cells of the aspect, the third in later grids
        int anchors = stage >= 2 ? 3 : 2;
        for (int i = 0; i < anchors; i++) {
            int c = freeCell();
            kind[c] = ANCHOR;
            element[c] = (byte) params.aspect();
        }
        int base = 4 + 2 * stage;
        int murk = Math.max(2, Math.min(11, Math.round(base * (0.55f + 0.9f * params.thick()))));
        int placed = 0, tries = 0;
        while (placed < murk && tries++ < 400) {
            int c = random.nextInt(SIZE * SIZE);
            if (kind[c] != EMPTY)
                continue;
            int e = random.nextInt(ELEMENTS - 1);
            if (e >= params.aspect())
                e++;
            kind[c] = MURK;
            element[c] = (byte) e;
            if (!guaranteed()) {
                kind[c] = EMPTY;
                continue;
            }
            placed++;
        }
        murkStart = murkLeft = placed;
        par += placed * 2 + 2;
    }

    private int freeCell() {
        int c;
        do {
            c = random.nextInt(SIZE * SIZE);
        } while (kind[c] != EMPTY);
        return c;
    }

    /**
     * The generator's guarantee: every murk cell sits in a row or column that can hold its opposite (it already does, or has an empty cell to put it
     * in), and no line is nothing but murk and anchors (such a line would clear on its own and never need a piece).
     */
    private boolean guaranteed() {
        for (int i = 0; i < SIZE; i++) {
            boolean rowFixed = true, colFixed = true;
            for (int j = 0; j < SIZE; j++) {
                rowFixed &= kind[i * SIZE + j] >= MURK;
                colFixed &= kind[j * SIZE + i] >= MURK;
            }
            if (rowFixed || colFixed)
                return false;
        }
        for (int c = 0; c < SIZE * SIZE; c++)
            if (kind[c] == MURK && !clearable(c))
                return false;
        return true;
    }

    private boolean clearable(int c) {
        int x = c % SIZE, y = c / SIZE, opp = opposite(element[c]);
        boolean row = false, col = false;
        for (int i = 0; i < SIZE; i++) {
            int r = y * SIZE + i, k = i * SIZE + x;
            row |= kind[r] == EMPTY || kind[r] != PIECE && element[r] == opp;
            col |= kind[k] == EMPTY || kind[k] != PIECE && element[k] == opp;
        }
        return row || col;
    }

    // --- the tray ---------------------------------------------------------------------------------

    private void fillTray() {
        for (int i = 0; i < TRAY; i++)
            if (tray[i] == null)
                tray[i] = draw();
    }

    private Piece draw() {
        boolean awkward = random.nextFloat() < 0.15f + 0.7f * params.awkward();
        int shape = awkward ? TIDY + random.nextInt(SHAPES.length - TIDY) : random.nextInt(TIDY);
        int e;
        float roll = random.nextFloat();
        int worst = params.reagent() && roll < 0.3f ? thickest() : -1;
        if (worst >= 0)
            e = opposite(worst);
        else if (roll < 0.65f && murkLeft > 0)
            e = opposite(nthMurk(random.nextInt(murkLeft)));
        else
            e = random.nextInt(ELEMENTS);
        return new Piece(shape, e);
    }

    private int nthMurk(int n) {
        for (int c = 0; c < SIZE * SIZE; c++)
            if (kind[c] == MURK && n-- == 0)
                return element[c];
        return 0;
    }

    /** The element murk is thickest in (what a reagent leans the tray against): the one with the most cells left, lowest id on a tie; -1 when none. */
    public int thickest() {
        int[] count = new int[ELEMENTS];
        for (int c = 0; c < SIZE * SIZE; c++)
            if (kind[c] == MURK)
                count[element[c]]++;
        int best = -1;
        for (int e = 0; e < ELEMENTS; e++)
            if (count[e] > 0 && (best < 0 || count[e] > count[best]))
                best = e;
        return best;
    }

    // --- moves ------------------------------------------------------------------------------------

    /** Whether tray piece {@code slot} fits with its top-left cell at {@code x, y}. */
    public boolean fits(int slot, int x, int y) {
        if (over() || slot < 0 || slot >= TRAY || tray[slot] == null)
            return false;
        for (int[] o : SHAPES[tray[slot].shape()]) {
            int cx = x + o[0], cy = y + o[1];
            if (cx < 0 || cy < 0 || cx >= SIZE || cy >= SIZE || kind[cy * SIZE + cx] != EMPTY)
                return false;
        }
        return true;
    }

    /** Whether any tray piece fits anywhere. */
    public boolean canPlay() {
        for (int s = 0; s < TRAY; s++)
            for (int y = 0; y < SIZE; y++)
                for (int x = 0; x < SIZE; x++)
                    if (fits(s, x, y))
                        return true;
        return false;
    }

    /** What a placement did: the cells of murk cleansed and of the player's pieces swept away by the lines it completed, and whether the stage or rite ended. */
    public record Outcome(boolean placed, int lines, int cleansed, int swept, boolean stageDone) {
        static final Outcome NONE = new Outcome(false, 0, 0, 0, false);
    }

    /** Places tray piece {@code slot} at {@code x, y}; completed rows and columns clear; the tray slot is dealt again. */
    public Outcome place(int slot, int x, int y) {
        if (!fits(slot, x, y))
            return Outcome.NONE;
        Piece p = tray[slot];
        for (int[] o : SHAPES[p.shape()]) {
            int c = (y + o[1]) * SIZE + x + o[0];
            kind[c] = PIECE;
            element[c] = (byte) p.element();
        }
        pieces++;
        tray[slot] = null;
        // lines completed by this placement, judged on the grid as it stands, then cleared together
        boolean[] rows = new boolean[SIZE], cols = new boolean[SIZE];
        int lines = 0;
        for (int i = 0; i < SIZE; i++) {
            rows[i] = full(i, true);
            cols[i] = full(i, false);
            lines += (rows[i] ? 1 : 0) + (cols[i] ? 1 : 0);
        }
        int cleansed = 0, swept = 0;
        boolean[] remove = new boolean[SIZE * SIZE];
        for (int c = 0; c < SIZE * SIZE; c++) {
            if (kind[c] == EMPTY || kind[c] == ANCHOR)
                continue;
            int cx = c % SIZE, cy = c / SIZE;
            boolean inRow = rows[cy], inCol = cols[cx];
            if (!inRow && !inCol)
                continue;
            if (kind[c] == PIECE)
                remove[c] = true;
            else if (inRow && holds(cy, true, opposite(element[c])) || inCol && holds(cx, false, opposite(element[c])))
                remove[c] = true;
        }
        for (int c = 0; c < SIZE * SIZE; c++)
            if (remove[c]) {
                if (kind[c] == MURK) {
                    cleansed++;
                    murkLeft--;
                } else
                    swept++;
                kind[c] = EMPTY;
            }
        boolean done = murkLeft <= 0;
        if (done) {
            finishedShare += 1f;
            stage++;
            if (stage < params.stages()) {
                startStage();
                java.util.Arrays.fill(tray, null);
            }
        }
        fillTray();
        return new Outcome(true, lines, cleansed, swept, done);
    }

    private boolean full(int i, boolean row) {
        for (int j = 0; j < SIZE; j++)
            if (kind[row ? i * SIZE + j : j * SIZE + i] == EMPTY)
                return false;
        return true;
    }

    private boolean holds(int i, boolean row, int e) {
        for (int j = 0; j < SIZE; j++) {
            int c = row ? i * SIZE + j : j * SIZE + i;
            if (kind[c] != EMPTY && element[c] == e)
                return true;
        }
        return false;
    }

    /** Re-draws the whole tray for one ink; false when there is no ink left. */
    public boolean redraw() {
        if (over() || ink <= 0)
            return false;
        ink--;
        redraws++;
        java.util.Arrays.fill(tray, null);
        fillTray();
        return true;
    }

    /** Ends the rite with the grid as it is. */
    public void takeResidue() {
        if (!over())
            taken = true;
    }

    // --- grade ------------------------------------------------------------------------------------

    /**
     * The notes' quality, 0 to 1: how pure the grids came out (the stages cleansed, and the share of the one in hand), times a little for how few
     * pieces it took against a par and how few re-draws. Nothing cleansed grades nothing.
     */
    public float quality() {
        float current = stage < params.stages() ? 1f - murkLeft / (float) Math.max(1, murkStart) : 0f;
        float purity = Math.min(1f, (finishedShare + current) / params.stages());
        float economy = Math.max(0f, Math.min(1f, par / (float) Math.max(1, pieces * 1.0f)) - 0.05f * redraws);
        return Math.max(0f, Math.min(1f, purity * (0.7f + 0.3f * economy)));
    }

    // --- replay -----------------------------------------------------------------------------------

    /**
     * Plays {@code moves} over a fresh run: {@code p<slot>@<x>,<y>} places a piece, {@code d} re-draws the tray, {@code r} takes the residue. A move
     * that cannot be made is ignored.
     */
    public static RitePurify replay(Params params, int seed, List<String> moves) {
        RitePurify game = new RitePurify(params, seed);
        int n = 0;
        for (String move : moves) {
            if (game.over() || n++ >= MAX_MOVES)
                break;
            try {
                if (move.equals("d"))
                    game.redraw();
                else if (move.equals("r"))
                    game.takeResidue();
                else if (move.startsWith("p")) {
                    int at = move.indexOf('@'), comma = move.indexOf(',', at + 1);
                    if (at > 0 && comma > 0)
                        game.place(Integer.parseInt(move.substring(1, at)), Integer.parseInt(move.substring(at + 1, comma)),
                                Integer.parseInt(move.substring(comma + 1)));
                }
            } catch (NumberFormatException e) {
                break;
            }
        }
        return game;
    }

    /** The par, for tests and the screen's hint. */
    public int par() {
        return par;
    }

    /** All the cells of {@code shape} placed at {@code x, y}, for drawing a ghost. */
    public static List<int[]> cells(int shape, int x, int y) {
        List<int[]> out = new ArrayList<>();
        for (int[] o : SHAPES[shape])
            out.add(new int[] {x + o[0], y + o[1]});
        return out;
    }
}
