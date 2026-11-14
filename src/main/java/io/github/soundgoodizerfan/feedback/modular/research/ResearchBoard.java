// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The research board's rules, pure: a hex grid of radius {@link #RADIUS} with the concept the researcher owns at one end and the one they are
 * after at the other, a few rubble hexes laid by the seed, and the ink to spend. The client plays it and shows the player what happens; the server
 * replays the placements the client sent over the same plan, seed and ink, so what was graded is what was played and nothing the client says
 * about its own score is believed.
 */
public final class ResearchBoard {

    public static final int RADIUS = 3;
    /** Rubble hexes laid by the seed; the ink bonus a streak of this many joins in a row gives back. */
    public static final int RUBBLE = 6, STREAK_REFUND = 3;
    /** The most placements a run may hold; a result with more is cut off there. */
    public static final int MAX_MOVES = 64;

    /** What a placement did. */
    public enum Result {
        IGNORED, FIZZLE, JOINED, REACHED
    }

    public static final int[][] DIRS = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};

    public final ResearchPlan plan;
    public final int inkStart;
    public final long startCell = key(-RADIUS, 1), goalCell = key(RADIUS, -1);
    public final Map<Long, String> placed = new HashMap<>();
    public final Map<Long, Long> from = new HashMap<>();
    public final Set<Long> blocked = new HashSet<>();
    public int ink, fizzles, links, streak, bestStreak;
    public boolean reached;

    public ResearchBoard(ResearchPlan plan, int seed, int ink) {
        this.plan = plan;
        this.ink = this.inkStart = Math.max(3, ink);
        placed.put(startCell, plan.start);
        List<Long> cells = new ArrayList<>();
        for (int q = -RADIUS; q <= RADIUS; q++)
            for (int r = -RADIUS; r <= RADIUS; r++)
                if (Math.abs(q + r) <= RADIUS && distance(q, r, q(startCell), r(startCell)) > 1 && distance(q, r, q(goalCell), r(goalCell)) > 1)
                    cells.add(key(q, r));
        java.util.Collections.shuffle(cells, new java.util.Random(seed));
        for (int i = 0; i < RUBBLE && i < cells.size(); i++)
            blocked.add(cells.get(i));
    }

    public static long key(int q, int r) {
        return ((long) q << 32) | (r & 0xFFFFFFFFL);
    }

    public static int q(long k) {
        return (int) (k >> 32);
    }

    public static int r(long k) {
        return (int) k;
    }

    public static int distance(int q1, int r1, int q2, int r2) {
        return (Math.abs(q1 - q2) + Math.abs(r1 - r2) + Math.abs(q1 + r1 - q2 - r2)) / 2;
    }

    public static boolean inside(int q, int r) {
        return Math.abs(q) <= RADIUS && Math.abs(r) <= RADIUS && Math.abs(q + r) <= RADIUS;
    }

    public List<Long> neighbours(long c) {
        List<Long> out = new ArrayList<>(6);
        for (int[] d : DIRS)
            if (inside(q(c) + d[0], r(c) + d[1]))
                out.add(key(q(c) + d[0], r(c) + d[1]));
        return out;
    }

    /** Whether the run is over: the goal reached, or the ink spent. */
    public boolean over() {
        return reached || ink <= 0;
    }

    /** Whether a concept may be tried in {@code cell}: free, not rubble, not the goal, beside the chain, and the concept not already on the board. */
    public boolean canTry(String concept, long cell) {
        if (over() || !plan.labels.containsKey(concept) || concept.equals(plan.start) || concept.equals(plan.goal) || placed.containsValue(concept))
            return false;
        if (placed.containsKey(cell) || blocked.contains(cell) || cell == goalCell || !inside(q(cell), r(cell)))
            return false;
        return anyPlacedNeighbour(cell);
    }

    public boolean anyPlacedNeighbour(long cell) {
        for (long n : neighbours(cell))
            if (placed.containsKey(n))
                return true;
        return false;
    }

    /** Tries {@code concept} in {@code cell}: it joins the chain if it really belongs with a chain cell it touches, else it fizzles; either costs ink. */
    public Result place(String concept, long cell) {
        if (!canTry(concept, cell))
            return Result.IGNORED;
        long hold = Long.MIN_VALUE;
        for (long n : neighbours(cell))
            if (placed.containsKey(n) && plan.related(concept, placed.get(n)))
                hold = n;
        ink--;
        if (hold == Long.MIN_VALUE) {
            fizzles++;
            streak = 0;
            return Result.FIZZLE;
        }
        placed.put(cell, concept);
        from.put(cell, hold);
        links++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        if (streak % STREAK_REFUND == 0)
            ink++;
        if (neighbours(cell).contains(goalCell) && plan.related(concept, plan.goal)) {
            reached = true;
            placed.put(goalCell, plan.goal);
            from.put(goalCell, cell);
            return Result.REACHED;
        }
        return Result.JOINED;
    }

    /** The notes' quality, 0 to 1, given whether the question after the replay was answered right. */
    public float quality(boolean answeredRight) {
        if (!reached)
            return 0.05f + 0.2f * Math.max(0f, Math.min(1f, links / (float) Math.max(1, plan.labels.size() / 2)));
        float left = Math.max(0f, Math.min(1f, ink / (float) inkStart));
        return Math.max(0f, Math.min(1f, 0.35f + 0.35f * left + 0.1f * Math.min(1f, bestStreak / 4f) + (answeredRight ? 0.2f : 0f)));
    }

    /** How hard the specimen was pushed, 0 to 100: the share of the ink lost to fizzles, scaled down by how forgiving the specimen's material is ({@code forgiving} 0 to 1). */
    public int strain(float forgiving) {
        return Math.max(0, Math.min(100, Math.round(fizzles * 140f / inkStart * (1f - 0.5f * Math.max(0f, Math.min(1f, forgiving))))));
    }
}
