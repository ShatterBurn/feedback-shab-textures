// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.programmer.NodePos;

/**
 * A tidy place for the cards a text made that the editor has no position for: a column per step of depth from the readings (a card sits one
 * column right of the deepest card feeding it), a row per card in its column, below whatever already stands there. Cards that keep an
 * id keep their position; only new ones are placed.
 */
public final class TextLayout {

    private static final int COLUMN = 130, ROW = 56, MARGIN = 20;

    private TextLayout() {
    }

    /** Positions for every card of {@code graph} that {@code known} lacks. */
    public static Map<Integer, NodePos> place(ProgramGraph graph, Map<Integer, NodePos> known) {
        Map<Integer, Integer> depth = new HashMap<>();
        for (ProgramNode n : graph.nodes())
            depth.put(n.id(), 0);
        // relax at most one pass per card, so a loop through a delay settles instead of climbing for ever
        for (int pass = 0; pass < graph.nodes().size(); pass++) {
            boolean moved = false;
            for (ProgramNode n : graph.nodes())
                for (int in : n.inputs())
                    if (depth.containsKey(in) && depth.get(in) + 1 > depth.get(n.id()) && depth.get(in) < graph.nodes().size()) {
                        depth.put(n.id(), depth.get(in) + 1);
                        moved = true;
                    }
            if (!moved)
                break;
        }
        int below = MARGIN;
        for (NodePos p : known.values())
            below = Math.max(below, p.y() + ROW);
        boolean anyKnown = graph.nodes().stream().anyMatch(n -> known.containsKey(n.id()));
        Map<Integer, Integer> rows = new HashMap<>();
        Map<Integer, NodePos> out = new HashMap<>();
        for (ProgramNode n : graph.nodes()) {
            if (known.containsKey(n.id()))
                continue;
            int col = depth.get(n.id());
            int row = rows.merge(col, 1, Integer::sum) - 1;
            out.put(n.id(), new NodePos(MARGIN + col * COLUMN, (anyKnown ? below : MARGIN) + row * ROW));
        }
        return out;
    }
}
