// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

/**
 * The treatise research game's rules, pure: the plan's concepts are cards (the start and the goal are not dealt) and two cards link when they share a
 * trait. A hand of {@link #HAND} cards is dealt; a play is a row of up to {@link #MAX_PLAY} of them, judged as a set. The cards that share a trait with
 * another card of the row score; the most cards sharing one trait name the combo (pair to five), which has base chips and a multiplier, each scoring
 * card adds chips, a row of three or more that links card to card in order adds a chain bonus, and a card carrying a trait of a held {@link Joker}
 * or of the plan's goal adds to the multiplier. A card that shares nothing with the rest of a row of two or more burns and strains the specimen; a
 * lone card is a safe single. The run is won when the score reaches the target; it is lost when the hands are spent first. Hands and discards are
 * limited, there is no clock. The client plays it; the server replays the plays over the same plan, seed, ink, tier and jokers ({@link #replay}) and
 * believes nothing the client says about its own score.
 *
 * <p>Tier (the volume's number) sets the target score and how many cards of each deal are guaranteed a partner. Ink above the base buys discards
 * and hands.
 */
public final class TreatiseHand {

    public static final int HAND = 5, MAX_PLAY = 5, HANDS = 4, DISCARDS = 2;
    /** The most moves a result may hold; a result with more is cut off there. */
    public static final int MAX_MOVES = 24;
    /** One ink above the base buys a discard, this many buy a hand. */
    public static final int INK_PER_HAND = 3;
    /** A combo by the most cards sharing one trait (index 0 is unused, 1 is a lone card): base chips and base multiplier. */
    public static final int[] COMBO_CHIPS = {0, 5, 10, 30, 60, 100}, COMBO_MULT = {0, 1, 2, 3, 4, 5};
    /** Chips each scoring card adds. */
    public static final int CARD_CHIPS = 10;
    /** A row of three or more whose cards link in the order played adds these. */
    public static final int CHAIN_CHIPS = 10, CHAIN_MULT = 1;
    /** Multiplier a scoring card adds for each held joker it shares a trait with, and for sharing one with the goal. */
    public static final int JOKER_MULT = 2, GOAL_MULT = 1;
    /** Target score: this, plus this per tier above the first. */
    public static final int TARGET_BASE = 160, TARGET_STEP = 50;
    /** Cards of a deal that are guaranteed a partner in tier 1, falling by one per tier. */
    public static final int GUARANTEE = 2;

    /** A held volume of another school: the traits its school lends a card. */
    public record Joker(String school, int volume, Set<String> traits) {
    }

    /**
     * A row judged: the cards that scored and the ones that burned, in row order, the combo's size (1 for a lone card, 0 when nothing scored),
     * whether the chain bonus applied, the chips and the multiplier, and what the row is worth.
     */
    public record Row(List<Integer> scoring, List<Integer> burned, int combo, boolean chain, int chips, int mult) {
        public static final Row NONE = new Row(List.of(), List.of(), 0, false, 0, 0);

        public int total() {
            return chips * mult;
        }
    }

    /** What a move did. */
    public enum Result {
        IGNORED, SCORED, BROKEN, REACHED, DISCARDED
    }

    public final ResearchPlan plan;
    /** The dealt concepts: a card is its index here. */
    public final String[] cards;
    public final List<Joker> jokers;
    public final Set<String> goalTraits;
    public final int tier, handsStart, discardsStart, target;
    public final List<Integer> hand = new ArrayList<>();
    public int hands, discards, score, bad, links;
    public boolean reached;
    /** The last play, for the screen's tally (the server grades by {@link #score}). */
    public Row last = Row.NONE;
    private final int guarantee;
    private final Random rng;
    private final ArrayDeque<Integer> pile = new ArrayDeque<>();
    private final List<Integer> spent = new ArrayList<>();

    public TreatiseHand(ResearchPlan plan, int seed, int ink, int tier, List<Joker> jokers) {
        this.plan = plan;
        this.tier = Math.max(1, tier);
        this.jokers = List.copyOf(jokers);
        goalTraits = plan.traits.getOrDefault(plan.goal, Set.of());
        List<String> dealt = new ArrayList<>();
        for (String id : plan.labels.keySet())
            if (!id.equals(plan.start) && !id.equals(plan.goal))
                dealt.add(id);
        cards = dealt.toArray(new String[0]);
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
        deal();
    }

    /** Cards left in the draw pile (the spent ones return on a reshuffle and are not counted). */
    public int pileSize() {
        return pile.size();
    }

    /** Whether the run is over: the target reached, or the hands spent. */
    public boolean over() {
        return reached || hands <= 0;
    }

    /** Whether cards {@code a} and {@code b} share a trait. */
    public boolean links(int a, int b) {
        return plan.related(cards[a], cards[b]);
    }

    /** How many jokers share a trait with card {@code c}. */
    public int jokersOn(int c) {
        int n = 0;
        for (Joker j : jokers)
            for (String t : plan.traits.get(cards[c]))
                if (j.traits().contains(t)) {
                    n++;
                    break;
                }
        return n;
    }

    /** Whether card {@code c} carries a trait of the goal. */
    public boolean onGoal(int c) {
        for (String t : plan.traits.get(cards[c]))
            if (goalTraits.contains(t))
                return true;
        return false;
    }

    private boolean partnered(int c, List<Integer> in) {
        for (int o : in)
            if (o != c && links(c, o))
                return true;
        return false;
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
        // a low tier's deal always holds cards with partners: a card that links to nothing in the hand is swapped for one that does, which goes under the pile
        int have = 0;
        for (int c : hand)
            if (partnered(c, hand))
                have++;
        for (int h = 0; have < guarantee && h < hand.size(); h++) {
            int out = hand.get(h);
            if (partnered(out, hand))
                continue;
            Integer pick = null;
            for (int c : pile) {
                for (int o : hand)
                    if (o != out && links(c, o)) {
                        pick = c;
                        break;
                    }
                if (pick != null)
                    break;
            }
            if (pick == null)
                break;
            pile.remove(pick);
            pile.addLast(out);
            hand.set(h, pick);
            have = 0;
            for (int c : hand)
                if (partnered(c, hand))
                    have++;
        }
    }

    /** What the row {@code ids} would score, with nothing changed: the screen shows it as the player builds the row. */
    public Row preview(List<Integer> ids) {
        List<Integer> scoring = new ArrayList<>(), burned = new ArrayList<>();
        for (int c : ids)
            if (ids.size() == 1 || partnered(c, ids))
                scoring.add(c);
            else
                burned.add(c);
        if (scoring.isEmpty())
            return new Row(scoring, burned, 0, false, 0, 0);
        // the combo: the most cards of the row that carry one trait
        int best = 1;
        for (int c : scoring)
            for (String t : plan.traits.get(cards[c])) {
                int n = 0;
                for (int o : scoring)
                    if (plan.traits.get(cards[o]).contains(t))
                        n++;
                best = Math.max(best, n);
            }
        int combo = Math.min(best, COMBO_CHIPS.length - 1);
        boolean chain = ids.size() >= 3 && burned.isEmpty();
        for (int i = 1; chain && i < ids.size(); i++)
            chain = links(ids.get(i - 1), ids.get(i));
        int chips = COMBO_CHIPS[combo] + CARD_CHIPS * scoring.size() + (chain ? CHAIN_CHIPS : 0);
        int mult = COMBO_MULT[combo] + (chain ? CHAIN_MULT : 0);
        for (int c : scoring)
            mult += JOKER_MULT * jokersOn(c) + (onGoal(c) ? GOAL_MULT : 0);
        return new Row(scoring, burned, combo, chain, chips, mult);
    }

    /** Plays the cards {@code ids} (indices into {@link #cards}, all in the hand, no repeats) as a row. */
    public Result play(List<Integer> ids) {
        if (over() || ids.isEmpty() || ids.size() > MAX_PLAY || !valid(ids))
            return Result.IGNORED;
        hands--;
        last = preview(ids);
        for (int c : ids)
            hand.remove((Integer) c);
        spent.addAll(last.burned());
        spent.addAll(last.scoring());
        score += last.total();
        links += last.scoring().size();
        if (!last.burned().isEmpty())
            bad++;
        if (score >= target)
            reached = true;
        if (!over())
            deal();
        return reached ? Result.REACHED : !last.burned().isEmpty() ? Result.BROKEN : Result.SCORED;
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
            return 0.05f + 0.2f * Math.max(0f, Math.min(1f, score / (float) target));
        float score01 = Math.min(1f, score / (1.5f * target));
        float left = 0.5f * hands / handsStart + 0.5f * discards / discardsStart;
        return Math.max(0f, Math.min(1f, 0.35f + 0.3f * score01 + 0.15f * left + (answeredRight ? 0.2f : 0f)));
    }

    /** How hard the specimen was pushed, 0 to 100: each bad play, scaled down by how forgiving the specimen's material is ({@code forgiving} 0 to 1). */
    public int strain(float forgiving) {
        return Math.max(0, Math.min(100, Math.round(bad * 20f * (1f - 0.5f * Math.max(0f, Math.min(1f, forgiving))))));
    }

    /** The jokers as the one string the open payload carries: {@code school:volume=trait,trait;school:volume=trait}. */
    public static String encode(List<Joker> jokers) {
        StringBuilder out = new StringBuilder();
        for (Joker j : jokers)
            out.append(out.length() == 0 ? "" : ";").append(j.school()).append(':').append(j.volume()).append('=').append(String.join(",", j.traits()));
        return out.toString();
    }

    /** The jokers {@link #encode} wrote; a malformed one is left out. */
    public static List<Joker> parse(String text) {
        List<Joker> out = new ArrayList<>();
        for (String part : text.split(";")) {
            int eq = part.indexOf('='), colon = part.indexOf(':');
            if (eq < 0 || colon < 0 || colon > eq)
                continue;
            try {
                out.add(new Joker(part.substring(0, colon), Integer.parseInt(part.substring(colon + 1, eq)),
                        new LinkedHashSet<>(List.of(part.substring(eq + 1).split(",")))));
            } catch (NumberFormatException e) {
                // left out
            }
        }
        return out;
    }

    /**
     * Replays a result's moves ({@code p<id>,<id>..} a play, {@code d<id>,<id>..} a discard) over the plan, seed, ink, tier and jokers; the run stops at
     * its end or a malformed move.
     */
    public static TreatiseHand replay(ResearchPlan plan, int seed, int ink, int tier, List<Joker> jokers, List<String> moves) {
        TreatiseHand game = new TreatiseHand(plan, seed, ink, tier, jokers);
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
