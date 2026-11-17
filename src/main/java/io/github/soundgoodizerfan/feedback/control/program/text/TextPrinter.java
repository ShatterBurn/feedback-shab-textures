// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ComparatorNode;
import io.github.soundgoodizerfan.feedback.control.program.DelayNode;
import io.github.soundgoodizerfan.feedback.control.program.LogicNode;
import io.github.soundgoodizerfan.feedback.control.program.MathNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.ReadNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.control.program.WriteNode;

/**
 * The canonical text of a graph: the one pretty-printer, so the graph stays the source of truth and the text is its view. Variables first,
 * then one {@code link} per distinct target, then one statement per card in the graph's own order, each named by its kind and id ({@code s3} a
 * sensor, {@code c5} a comparison, {@code g1} a logic gate, {@code m2} arithmetic, {@code d4} a delay, {@code r6} a read, {@code a7} an
 * actuator, {@code w8} a write), which is what lets {@link TextCompiler} give every card back the id it had. Printing is idempotent:
 * compiling a printed graph and printing again gives the same text. An unwired input prints as {@code unwired}.
 */
public final class TextPrinter {

    private static final Pattern PLAIN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private TextPrinter() {
    }

    public static String print(ProgramGraph graph) {
        StringBuilder out = new StringBuilder();
        for (ProgramVariable v : graph.variables())
            out.append("var ").append(word(v.name())).append(": ").append(v.type().getSerializedName()).append('\n');
        Map<DataNodeRef, String> links = new LinkedHashMap<>();
        for (ProgramNode n : graph.nodes()) {
            DataNodeRef t = target(n);
            if (t != null && !t.isLocal() && !links.containsKey(t))
                links.put(t, "l" + (links.size() + 1));
        }
        links.forEach((ref, name) -> out.append("link ").append(name).append(" = ").append(ref(ref)).append('\n'));
        if (!graph.variables().isEmpty() || !links.isEmpty())
            out.append('\n');
        for (ProgramNode n : graph.nodes())
            out.append(name(n)).append(" = ").append(expr(graph, n, links)).append('\n');
        return out.toString();
    }

    private static DataNodeRef target(ProgramNode n) {
        return n instanceof SensorNode s ? s.target() : n instanceof ActuatorNode a ? a.target() : null;
    }

    /** The name a card prints under: its kind and its id. */
    public static String name(ProgramNode n) {
        return prefix(n) + n.id();
    }

    private static String prefix(ProgramNode n) {
        return switch (n) {
            case SensorNode x -> "s";
            case ComparatorNode x -> "c";
            case LogicNode x -> "g";
            case MathNode x -> "m";
            case DelayNode x -> "d";
            case ReadNode x -> "r";
            case ActuatorNode x -> "a";
            case WriteNode x -> "w";
        };
    }

    private static String word(String variable) {
        return PLAIN.matcher(variable).matches() && !TextParser.isKeyword(variable) ? variable : "\"" + variable + "\"";
    }

    private static String ref(DataNodeRef r) {
        if (r.isLocal())
            return "@local " + r.local();
        StringBuilder s = new StringBuilder("@").append(r.pos().getX()).append(' ').append(r.pos().getY()).append(' ').append(r.pos().getZ());
        if (r.side() != null)
            s.append(' ').append(r.side().getName());
        if (r.isVoxel())
            s.append(" voxel ").append(r.voxel());
        return s.toString();
    }

    private static String operand(ProgramGraph g, int id) {
        return g.find(id).map(TextPrinter::name).orElse("unwired");
    }

    private static String number(float v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e9f)
            return String.valueOf((long) v);
        return String.valueOf(v);
    }

    private static String duration(int ticks) {
        if (ticks % 72000 == 0)
            return ticks / 72000 + "h";
        if (ticks % 1200 == 0)
            return ticks / 1200 + "m";
        if (ticks % 20 == 0)
            return ticks / 20 + "s";
        return ticks + "t";
    }

    private static String target(DataNodeRef t, Map<DataNodeRef, String> links) {
        return t == null ? "unlinked" : t.isLocal() ? ref(t) : links.get(t);
    }

    private static String expr(ProgramGraph g, ProgramNode node, Map<DataNodeRef, String> links) {
        return switch (node) {
            case SensorNode s -> {
                List<String> args = new ArrayList<>();
                args.add(target(s.target(), links));
                if (s.quantity() != null || s.channel() != 0)
                    args.add(s.quantity() == null ? "unlinked" : s.quantity().getSerializedName());
                if (s.channel() != 0)
                    args.add(String.valueOf(s.channel()));
                yield "sensor(" + String.join(", ", args) + ")";
            }
            case ComparatorNode c -> operand(g, c.leftInput()) + (c.op() == ComparatorNode.Compare.GREATER ? " > " : " < ")
                    + (c.rightInput() == ProgramNode.UNWIRED ? number(c.literalDefault()) : operand(g, c.rightInput()));
            case LogicNode l -> l.op().unary() ? "not " + operand(g, l.leftInput())
                    : operand(g, l.leftInput()) + (l.op() == LogicNode.Gate.AND ? " and " : " or ") + operand(g, l.rightInput());
            case MathNode m -> {
                String right = m.rightInput() == ProgramNode.UNWIRED ? number(m.literalDefault()) : operand(g, m.rightInput());
                yield switch (m.op()) {
                    case ADD -> operand(g, m.leftInput()) + " + " + right;
                    case SUBTRACT -> operand(g, m.leftInput()) + " - " + right;
                    case MULTIPLY -> operand(g, m.leftInput()) + " * " + right;
                    case DIVIDE -> operand(g, m.leftInput()) + " / " + right;
                    case MIN -> "min(" + operand(g, m.leftInput()) + ", " + right + ")";
                    case MAX -> "max(" + operand(g, m.leftInput()) + ", " + right + ")";
                };
            }
            case DelayNode d -> "delay(" + operand(g, d.input()) + ", " + (d.countWired() ? operand(g, d.ticksInput()) : duration(d.ticks())) + ")"
                    + (g.find(d.input()).isEmpty() ? " as " + d.type().getSerializedName() : "");
            case ReadNode r -> "read(" + word(r.variable()) + ")";
            case ActuatorNode a -> "actuate(" + operand(g, a.input()) + ", " + target(a.target(), links) + (a.channel() != 0 ? ", " + a.channel() : "") + ")";
            case WriteNode w -> "write(" + word(w.variable()) + ", " + operand(g, w.valueInput()) + ", " + operand(g, w.triggerInput()) + ")";
        };
    }
}
