// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

/**
 * The treatise research game's rules, pure: the plan's concepts are cards (the start and the goal are not dealt), two cards link when they share a
 * trait, and a chain grows from the plan's start. A hand of {@link #HAND} cards is dealt; a play is a row of up to {@link #MAX_PLAY} of them, each
 * linking to the one before (the first to the chain's end), and every card that links scores. A card that does not link burns, and so do the cards
 * after it, the hand is spent and the specimen is strained. A card that shares a trait with the goal reaches it and ends the chain. Hands and
 * discards are limited, there is no clock. The client plays it; the server replays the plays over the same plan, seed, ink and tier ({@link #replay})
 * and believes nothing the client says about its own score.
 *
 * <p>Tier (the volume's number) sets the target score and how many linking cards each deal is guaranteed to hold. Ink above the base buys discards
 * and hands.
 */
public final class TreatiseHand {

    public static final int HAND = 5, MAX_PLAY = 5, HANDS = 4, DISCARDS = 2;
    /** The most moves a result may hold; a result with more is cut off there. */
    public static final int MAX_MOVES = 24;
    /** One ink above the base buys a discard, this many buy a hand. */
    public static final int INK_PER_HAND = 3;
    public static final int CARD_POINTS = 10, GOAL_BONUS = 100;
    /** Target score: this, plus this per tier above the first. */
    public static final int TARGET_BASE = 100, TARGET_STEP = 30;
    /** Linking cards each deal is guaranteed to hold in tier 1, falling by one per tier. */
    public static final int GUARANTEE = 2;

    /** What a move did. */
    public enum Result {
        IGNORED, LINKED, BROKEN, REACHED, DISCARDED
    }

    public final ResearchPlan plan;
    /** The dealt concepts: a card is its index here. */
    public final String[] cards;
    public final int tier, handsStart, discardsStart, target;
    /** The chain, from the plan's start; its last concept is what the next play must link to. */
    public final List<String> chain = new ArrayList<>();
    public final List<Integer> hand = new ArrayList<>();
    public final boolean[] inChain;
    public int hands, discards, score, bad, links;
    public boolean reached;
    private final int guarantee;
    private final Random rng;
    private final ArrayDeque<Integer> pile = new ArrayDeque<>();
    private final List<Integer> spent = new ArrayList<>();

    public TreatiseHand(ResearchPlan plan, int seed, int ink, int tier) {
        this.plan = plan;
        this.tier = Math.max(1, tier);
        List<String> dealt = new ArrayList<>();
        for (String id : plan.labels.keySet())
            if (!id.equals(plan.start) && !id.equals(plan.goal))
                dealt.add(id);
        cards = dealt.toArray(new String[0]);
        inChain = new boolean[cards.length];
        int extra = Math.max(0, ink - ModularTuning.RESEARCH_BASE_INK);
        hands = handsStart = HANDS + extra / INK_PER_HAND;
        discards = discardsStart = DISCARDS + extra;
        target = TARGET_BASE + TARGET_STEP * (this.tier - 1);
        guarantee = Math.max(0, GUARANTEE - (this.tier - 1));
        rng = new Random(seed);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cards.length; i++)
            order.add(i);
        Collections.shuffle(order, rng);
        pile.addAll(order);
        chain.add(plan.start);
        deal();
    }

    /** The concept the next play must link to. */
    public String end() {
        return chain.get(chain.size() - 1);
    }

    /** Whether the run is over: the goal reached, or the hands spent. */
    public boolean over() {
        return reached || hands <= 0;
    }

    /** Whether card {@code i} links to the concept {@code to}. */
    public boolean links(int i, String to) {
        return plan.related(cards[i], to);
    }

    private void deal() {
        while (hand.size() < HAND) {
            if (pile.isEmpty()) {
                if (spent.isEmpty())
                    break;
                Collections.shuffle(spent, rng);
                pile.addAll(spent);
                spent.clear();
            }
            hand.add(pile.poll());
        }
        // a low tier's deal always holds linking cards: a card that links to nothing is swapped for one that does, which goes under the pile
        int have = 0;
        for (int c : hand)
            if (links(c, end()))
                have++;
        for (int h = 0; have < guarantee && h < hand.size(); h++) {
            if (links(hand.get(h), end()))
                continue;
            Integer pick = null;
            for (int c : pile)
                if (links(c, end())) {
                    pick = c;
                    break;
                }
            if (pick == null)
                break;
            pile.remove(pick);
            pile.addLast(hand.get(h));
            hand.set(h, pick);
            have++;
        }
    }

    /** Plays the cards {@code ids} (indices into {@link #cards}, all in the hand, no repeats) as a row. */
    public Result play(List<Integer> ids) {
        if (over() || ids.isEmpty() || ids.size() > MAX_PLAY || !valid(ids))
            return Result.IGNORED;
        hands--;
        float total = 0;
        int linked = 0;
        boolean broke = false;
        for (int i = 0; i < ids.size(); i++) {
            int c = ids.get(i);
            hand.remove((Integer) c);
            List<String> same = plan.shared(cards[c], end());
            if (broke || same.isEmpty()) {
                broke = true;
                spent.add(c);
                continue;
            }
            total += CARD_POINTS * same.size() * Math.min(3f, 1f + 0.25f * (chain.size() - 1));
            chain.add(cards[c]);
            inChain[c] = true;
            linked++;
            links++;
            if (plan.related(cards[c], plan.goal)) {
                reached = true;
                score += GOAL_BONUS;
                // the cards after the one that reached the goal stay in the hand
                for (int k = i + 1; k < ids.size(); k++)
                    hand.add(ids.get(k));
                break;
            }
        }
        score += Math.round(total * linked);
        if (broke)
            bad++;
        if (!over())
            deal();
        return reached ? Result.REACHED : broke ? Result.BROKEN : Result.LINKED;
    }

    /** Throws the cards {@code ids} back: they return on the next reshuffle and a fresh hand is dealt. Costs a discard, not a hand. */
    public Result discard(List<Integer> ids) {
        if (over() || discards <= 0 || ids.isEmpty() || ids.size() > HAND || !valid(ids))
            return Result.IGNORED;
        discards--;
        for (int c : ids) {
            hand.remove((Integer) c);
            spent.add(c);
        }
        deal();
        return Result.DISCARDED;
    }

    private boolean valid(List<Integer> ids) {
        for (int i = 0; i < ids.size(); i++)
            if (!hand.contains(ids.get(i)) || ids.indexOf(ids.get(i)) != i)
                return false;
        return true;
    }

    /** The notes' quality, 0 to 1, given whether the closing question was answered right. */
    public float quality(boolean answeredRight) {
        if (!reached)
            return 0.05f + 0.2f * Math.max(0f, Math.min(1f, links / (float) Math.max(1, cards.length / 2)));
        float score01 = Math.min(1f, score / (1.5f * target));
        float left = 0.5f * hands / handsStart + 0.5f * discards / discardsStart;
        return Math.max(0f, Math.min(1f, 0.35f + 0.3f * score01 + 0.15f * left + (answeredRight ? 0.2f : 0f)));
    }

    /** How hard the specimen was pushed, 0 to 100: each bad play, scaled down by how forgiving the specimen's material is ({@code forgiving} 0 to 1). */
    public int strain(float forgiving) {
        return Math.max(0, Math.min(100, Math.round(bad * 20f * (1f - 0.5f * Math.max(0f, Math.min(1f, forgiving))))));
    }

    /**
     * Replays a result's moves ({@code p<id>,<id>..} a play, {@code d<id>,<id>..} a discard) over the plan, seed, ink and tier; the run stops at its end
     * or a malformed move.
     */
    public static TreatiseHand replay(ResearchPlan plan, int seed, int ink, int tier, List<String> moves) {
        TreatiseHand game = new TreatiseHand(plan, seed, ink, tier);
        for (String move : moves) {
            if (move.length() < 2 || game.over())
                break;
            List<Integer> ids = new ArrayList<>();
            try {
                for (String part : move.substring(1).split(","))
                    ids.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                break;
            }
            if (move.charAt(0) == 'p')
                game.play(ids);
            else if (move.charAt(0) == 'd')
                game.discard(ids);
            else
                break;
        }
        return game;
    }
}
