// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

/**
 * The apps' research game's rules, pure: a terminal dump of junk with the plan's words hidden in it, one of which, drawn from the seed, is the answer. Picking a word
 * answers with its likeness (letters that match the goal in the same place) and spends a try; a bracket pair on one row is a hack that either removes a
 * dud word or refills the tries. The dump, the tries and the hacks all come from the plan, the seed and the ink, so the client plays it and the server
 * replays the picks the client sent over the same three and believes nothing the client says about its own result.
 *
 * <p>Shape taken from the terminal hacking game in Fallout (words in a dump, likeness, brackets); no code or assets were taken.
 */
public final class AppHack {

    /** The dump: {@link #COLS} columns of {@link #ROWS} rows, each row {@link #WIDTH} characters. */
    public static final int COLS = 2, ROWS = 16, WIDTH = 12, LINES = COLS * ROWS;
    /** The tries a run has with no more ink than the base, and the most ink can buy. */
    public static final int TRIES_MIN = 3, TRIES_MAX = 6;
    /** The ink above the base that buys one bracket pair. */
    public static final int INK_PER_PAIR = 2;
    /** The most picks a result may hold; a result with more is cut off there. */
    public static final int MAX_MOVES = 24;
    /** One hack in this many refills the tries and does not remove a dud. */
    public static final int REFILL_ONE_IN = 4;
    private static final String JUNK = "!@#$%^&*-_+=;:,.?/|~'\"";
    private static final String OPEN = "([{<", CLOSE = ")]}>";

    /** What a pick or a hack did. */
    public enum Result {
        IGNORED, WRONG, FOUND, OUT, REMOVED, REFILLED
    }

    public final String[] words;
    public final int answer, triesStart, pairs;
    /** The dump, {@link #LINES} rows of {@link #WIDTH} characters. */
    public final char[] dump = new char[LINES * WIDTH];
    /** Where each word sits: its row and the column it starts at. */
    public final int[] wordRow, wordCol;
    /** Where each bracket pair sits: its row and the columns of its opening and closing bracket. */
    public final int[] pairRow, pairOpen, pairClose;
    public final boolean[] removed, picked, pairUsed;
    public int tries, likeness = -1, hacksUsed;
    public boolean found;
    private final Random hackRng;

    /**
     * @param ink the run's ink: the tries are 3 plus what is above the base ink (never above {@link #TRIES_MAX}), one bracket pair per
     *            {@link #INK_PER_PAIR} of it
     */
    public AppHack(ResearchPlan plan, int seed, int ink) {
        words = plan.words.toArray(new String[0]);
        // The answer is any word of the pool, drawn from the seed, so it is not the app's own name by rule.
        answer = new Random(seed ^ 0x51ED270BL).nextInt(Math.max(1, words.length));
        int extra = Math.max(0, ink - ModularTuning.RESEARCH_BASE_INK);
        tries = triesStart = Math.min(TRIES_MAX, TRIES_MIN + extra);
        Random rng = new Random(seed);
        hackRng = new Random(seed ^ 0x9E3779B9L);
        int n = words.length;
        pairs = Math.min(extra / INK_PER_PAIR, LINES - n);
        removed = new boolean[n];
        picked = new boolean[n];
        pairUsed = new boolean[pairs];
        wordRow = new int[n];
        wordCol = new int[n];
        pairRow = new int[pairs];
        pairOpen = new int[pairs];
        pairClose = new int[pairs];
        for (int i = 0; i < dump.length; i++)
            dump[i] = JUNK.charAt(rng.nextInt(JUNK.length()));
        List<Integer> rows = new ArrayList<>(LINES);
        for (int r = 0; r < LINES; r++)
            rows.add(r);
        Collections.shuffle(rows, rng);
        for (int i = 0; i < n; i++) {
            int len = Math.min(words[i].length(), WIDTH);
            wordRow[i] = rows.get(i);
            wordCol[i] = rng.nextInt(WIDTH - len + 1);
            for (int c = 0; c < len; c++)
                dump[wordRow[i] * WIDTH + wordCol[i] + c] = words[i].charAt(c);
        }
        for (int p = 0; p < pairs; p++) {
            int kind = rng.nextInt(OPEN.length());
            int open = rng.nextInt(WIDTH - 3), close = open + 3 + rng.nextInt(WIDTH - open - 3);
            pairRow[p] = rows.get(n + p);
            pairOpen[p] = open;
            pairClose[p] = close;
            dump[pairRow[p] * WIDTH + open] = OPEN.charAt(kind);
            dump[pairRow[p] * WIDTH + close] = CLOSE.charAt(kind);
        }
    }

    /** How many letters of {@code word} match {@code goal} in the same place. */
    public static int likeness(String word, String goal) {
        int same = 0;
        for (int i = 0; i < Math.min(word.length(), goal.length()); i++)
            if (word.charAt(i) == goal.charAt(i))
                same++;
        return same;
    }

    /** Whether the run is over: the answer found, or the tries spent. */
    public boolean over() {
        return found || tries <= 0;
    }

    /** Picks the word {@code i}: it is the answer, or it spends a try and says its likeness ({@link #likeness}). */
    public Result pick(int i) {
        if (over() || i < 0 || i >= words.length || removed[i] || picked[i])
            return Result.IGNORED;
        picked[i] = true;
        likeness = likeness(words[i], words[answer]);
        if (i == answer) {
            found = true;
            return Result.FOUND;
        }
        tries--;
        return tries <= 0 ? Result.OUT : Result.WRONG;
    }

    /** Hacks the bracket pair {@code p}: one hack in {@link #REFILL_ONE_IN} refills the tries, the rest remove a dud (the tries are refilled when no dud is left). */
    public Result hack(int p) {
        if (over() || p < 0 || p >= pairs || pairUsed[p])
            return Result.IGNORED;
        pairUsed[p] = true;
        hacksUsed++;
        List<Integer> duds = new ArrayList<>();
        for (int i = 0; i < words.length; i++)
            if (i != answer && !removed[i] && !picked[i])
                duds.add(i);
        if (duds.isEmpty() || hackRng.nextInt(REFILL_ONE_IN) == 0) {
            tries = triesStart;
            return Result.REFILLED;
        }
        removed[duds.get(hackRng.nextInt(duds.size()))] = true;
        return Result.REMOVED;
    }

    /** The notes' grade, 0 to 1: the answer found, the tries left over and the hacks unused. */
    public float grade() {
        if (!found)
            return 0.05f;
        float unused = pairs == 0 ? 1f : 1f - hacksUsed / (float) pairs;
        return 0.4f + 0.4f * tries / triesStart + 0.2f * unused;
    }

    /** Replays a result's picks ({@code w<i>} a word, {@code b<i>} a bracket pair) over the plan, seed and ink; the run stops at its end or a bad move. */
    public static AppHack replay(ResearchPlan plan, int seed, int ink, List<String> moves) {
        AppHack game = new AppHack(plan, seed, ink);
        for (String move : moves) {
            if (move.length() < 2 || game.over())
                break;
            int i;
            try {
                i = Integer.parseInt(move.substring(1));
            } catch (NumberFormatException e) {
                break;
            }
            if (move.charAt(0) == 'w')
                game.pick(i);
            else if (move.charAt(0) == 'b')
                game.hack(i);
            else
                break;
        }
        return game;
    }
}
