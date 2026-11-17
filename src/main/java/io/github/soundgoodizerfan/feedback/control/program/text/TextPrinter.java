// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.soundgoodizerfan.feedback.control.data.Reduction;
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
        if (!graph.variables().isEmpty())
            out.append('\n');
        for (ProgramNode n : graph.nodes())
            out.append(name(n)).append(" = ").append(expr(graph, n)).append('\n');
        return out.toString();
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

    private static String expr(ProgramGraph g, ProgramNode node) {
        return switch (node) {
            case SensorNode s -> {
                List<String> args = new ArrayList<>();
                if (s.signal() != null)
                    args.add(word(TextWords.signal(s.signal())));
                if (s.kind() != null)
                    args.add(word(TextWords.kind(s.kind())));
                if (s.color() >= 0)
                    args.add(TextWords.color(s.color()));
                if (s.reduction() != Reduction.AVERAGE)
                    args.add(TextWords.reduction(s.reduction()));
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
            case ActuatorNode a -> "actuate(" + operand(g, a.input()) + (a.kind() != null ? ", " + word(TextWords.kind(a.kind())) : "")
                    + (a.color() >= 0 ? ", " + TextWords.color(a.color()) : "") + (a.channel() != 0 ? ", " + a.channel() : "") + ")";
            case WriteNode w -> "write(" + word(w.variable()) + ", " + operand(g, w.valueInput()) + ", " + operand(g, w.triggerInput()) + ")";
        };
    }
}
