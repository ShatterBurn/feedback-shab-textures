// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ComparatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
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
import io.github.soundgoodizerfan.feedback.control.program.text.TextAst.*;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Token;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

/**
 * The compile pipeline of a program text: tolerant lexer, typed syntax tree, lowering to a {@link ProgramGraph}, then the graph's own checks
 * ({@code findEditError} at edit time and per wire, {@code findError} at print time) and the lints. The text is a <em>view</em>: the graph
 * stays the one stored form, a text that does not compile yields no graph, and the nodes keep the ids the text names them with ({@code s3},
 * {@code c5}...) so an editor's layout survives a round trip.
 *
 * <p>The text is declarative single assignment: every {@code name = expression} is one card, order does not matter, a name read twice is one
 * wire fanning out, and a cycle is an error unless it passes through a delay. Types are inferred from the producers (a delay takes its input's;
 * {@code as number} or {@code as boolean} says it where nothing feeds it). A number literal stands only where the card has a literal default
 * (the right side of a comparison or arithmetic) or is a delay's length; anywhere else it is an error. The text never names an item or a
 * block: a sensor or actuator names its {@link DataNodeRef} as the graph does, and the server still checks every reference it did not grant.
 *
 * <p>Design from Super Factory Manager (MPL-2.0, design only, no code copied): taken, the pipeline shape (tolerant lexer, typed tree, lowering,
 * the graph's own error check, then lints with line and column), grammar-driven completion ({@link TextCompletion}) and a canonical
 * pretty-printer ({@link TextPrinter}) so the graph stays the source of truth; rejected, imperative if/else and sequential IO, resource and
 * tag routing, the {@code every} trigger, path-simulation linting and config caps (caps stay {@link ControllerLimits}). See THIRD-PARTY-LICENSES.md.
 */
public final class TextCompiler {

    /** What a compile gives: the graph (null when something blocks) and every finding, in text order. */
    public record Result(ProgramGraph graph, List<Diag> diags) {
        public boolean ok() {
            return graph != null;
        }
    }

    private static final Pattern GENERATED = Pattern.compile("([a-z])(\\d+)");

    private final ControllerLimits limits;
    private final List<Diag> diags = new ArrayList<>();
    private final Map<String, VarDecl> vars = new LinkedHashMap<>();
    private final Map<String, LinkDecl> links = new HashMap<>();
    private final Map<String, Assign> named = new LinkedHashMap<>();
    private final Map<String, Integer> idOf = new HashMap<>();
    private final Map<Integer, Token> where = new HashMap<>();
    private final Map<String, ValueType> typeCache = new HashMap<>();
    private final Set<String> typing = new HashSet<>();
    private final List<ProgramNode> nodes = new ArrayList<>();
    private final Set<Integer> used = new HashSet<>();
    private int nextId;
    /** Variables read, written; names referenced; nodes consumed. */
    private final Set<String> read = new HashSet<>(), written = new HashSet<>();
    private final Set<Integer> consumed = new HashSet<>();

    private TextCompiler(ControllerLimits limits) {
        this.limits = limits;
    }

    public static Result compile(String text, ControllerLimits limits) {
        return new TextCompiler(limits).run(text);
    }

    private Result run(String text) {
        TextParser.Result parsed = TextParser.parse(TextLexer.lex(text));
        diags.addAll(parsed.diags());
        List<Assign> assigns = new ArrayList<>();
        // 1. declarations
        for (Stmt s : parsed.stmts()) {
            switch (s) {
                case VarDecl v -> {
                    if (vars.containsKey(v.name()) || !ProgramVariable.validName(v.name()))
                        err(v.at(), vars.containsKey(v.name()) ? Diag.ERROR + "duplicate_variable" : Diag.ERROR + "bad_variable_name", null);
                    else
                        vars.put(v.name(), v);
                }
                case LinkDecl l -> {
                    if (links.containsKey(l.name()) || named.containsKey(l.name()))
                        err(l.at(), Diag.TEXT + "duplicate_name", l.name());
                    else
                        links.put(l.name(), l);
                }
                case Assign a -> {
                    if (a.name() != null) {
                        if (TextParser.isKeyword(a.name()))
                            err(a.at(), Diag.TEXT + "reserved_name", a.name());
                        else if (named.containsKey(a.name()) || links.containsKey(a.name()))
                            err(a.at(), Diag.TEXT + "duplicate_name", a.name());
                        else
                            named.put(a.name(), a);
                    }
                    assigns.add(a);
                }
            }
        }
        // 2. ids: a generated-looking name (s3, c5) keeps its number when free, so a layout survives; the rest take fresh ones
        int max = 0;
        Map<Assign, Integer> preferred = new HashMap<>();
        for (Assign a : assigns) {
            if (a.name() == null)
                continue;
            Matcher m = GENERATED.matcher(a.name());
            if (m.matches() && m.group(2).length() <= 6) {
                int id = Integer.parseInt(m.group(2));
                preferred.put(a, id);
                max = Math.max(max, id);
            }
        }
        nextId = max + 1;
        Map<Assign, Integer> ids = new HashMap<>();
        for (Assign a : assigns) {
            int id = preferred.getOrDefault(a, -1);
            if (id < 0 || !used.add(id))
                id = fresh();
            ids.put(a, id);
            if (a.name() != null)
                idOf.put(a.name(), id);
            where.put(id, a.at());
        }
        // 3. lowering
        for (Assign a : assigns)
            lowerTop(a, ids.get(a));
        if (hasError())
            return done(null);
        ProgramGraph graph = new ProgramGraph(List.copyOf(nodes), vars.values().stream().map(v -> new ProgramVariable(v.name(), v.type())).toList());
        // 4. the graph's own checks, wire by wire so each lands on its line
        for (ProgramNode node : graph.nodes()) {
            List<Integer> ports = node.ports();
            for (int slot = 0; slot < ports.size(); slot++) {
                Optional<ProgramNode> from = graph.find(ports.get(slot));
                if (from.isPresent()) {
                    Optional<String> e = graph.wireError(node, slot, from.get());
                    if (e.isPresent())
                        err(where.getOrDefault(node.id(), null), Diag.ERROR + e.get(), null);
                }
            }
        }
        if (!hasError()) {
            Optional<String> edit = graph.findEditError(limits);
            if (edit.isPresent())
                diags.add(new Diag(Diag.Severity.ERROR, 1, 1, 1, Diag.ERROR + edit.get(), null));
        }
        if (!hasError())
            cycle(graph);
        if (hasError())
            return done(null);
        // 5. print time, and lints
        graph.findError(limits).ifPresent(e -> diags.add(new Diag(Diag.Severity.PRINT, 1, 1, 1, Diag.ERROR + e, null)));
        lint(graph);
        return done(graph);
    }

    private Result done(ProgramGraph graph) {
        diags.sort(java.util.Comparator.comparingInt(Diag::line).thenComparingInt(Diag::col));
        return new Result(graph, List.copyOf(new java.util.LinkedHashSet<>(diags)));
    }

    private boolean hasError() {
        return diags.stream().anyMatch(Diag::blocks);
    }

    private int fresh() {
        while (used.contains(nextId))
            nextId++;
        used.add(nextId);
        return nextId++;
    }

    private void err(Token at, String key, String arg) {
        add(Diag.Severity.ERROR, at, key, arg);
    }

    private void warn(Token at, String key, String arg) {
        add(Diag.Severity.WARN, at, key, arg);
    }

    private void add(Diag.Severity severity, Token at, String key, String arg) {
        diags.add(at == null ? new Diag(severity, 1, 1, 1, key, arg)
                : new Diag(severity, at.line(), at.col(), Math.max(1, at.end() - at.start()), key, arg));
    }

    // --- lowering -------------------------------------------------------------------------------

    private void lowerTop(Assign a, int id) {
        Expr e = a.expr();
        switch (e) {
            case Num n -> err(n.at(), Diag.TEXT + "literal_here", null);
            case Name n -> {
                // a name that is only another name: an alias of that card
                Integer to = idOf.get(n.name());
                if (to == null)
                    err(n.at(), Diag.TEXT + "unknown_name", n.name());
                else if (a.name() != null)
                    idOf.put(a.name(), to);
                else
                    err(n.at(), Diag.TEXT + "not_a_card", null);
                used.remove(id);
            }
            case Call c -> build(c, id);
            case Bin b -> build(b, id);
            case Not n -> build(n, id);
            default -> err(e.at(), Diag.TEXT + "not_a_card", null);
        }
    }

    /** The id of the card {@code e} makes or names; an inline expression becomes a card of its own. */
    private int value(Expr e) {
        switch (e) {
            case Name n -> {
                Integer id = idOf.get(n.name());
                if (id == null) {
                    err(n.at(), links.containsKey(n.name()) ? Diag.TEXT + "link_here" : Diag.TEXT + "unknown_name", n.name());
                    return ProgramNode.UNWIRED;
                }
                consumed.add(id);
                return id;
            }
            case Unwired u -> {
                warn(u.at(), Diag.TEXT + "warn_unwired", null);
                return ProgramNode.UNWIRED;
            }
            case Num n -> {
                err(n.at(), Diag.TEXT + "literal_here", null);
                return ProgramNode.UNWIRED;
            }
            case Call c -> {
                int id = fresh();
                where.put(id, c.at());
                if (build(c, id))
                    consumed.add(id);
                return id;
            }
            case Bin b -> {
                int id = fresh();
                where.put(id, b.at());
                build(b, id);
                consumed.add(id);
                return id;
            }
            case Not n -> {
                int id = fresh();
                where.put(id, n.at());
                build(n, id);
                consumed.add(id);
                return id;
            }
            default -> {
                err(e.at(), Diag.TEXT + "not_a_card", null);
                return ProgramNode.UNWIRED;
            }
        }
    }

    /** A right-hand operand: a card, or a literal where the card has a default. {@code literal[0]} receives it; the id is UNWIRED for a literal. */
    private int rightValue(Expr e, float[] literal) {
        if (e instanceof Num n) {
            if (n.unit() != Unit.NONE)
                err(n.at(), Diag.TEXT + "unit_here", null);
            literal[0] = n.value();
            return ProgramNode.UNWIRED;
        }
        return value(e);
    }

    private boolean build(Expr e, int id) {
        switch (e) {
            case Bin b -> {
                switch (b.op()) {
                    case ">", "<" -> {
                        float[] lit = {0f};
                        int l = value(b.left()), r = rightValue(b.right(), lit);
                        nodes.add(new ComparatorNode(id, b.op().equals(">") ? ComparatorNode.Compare.GREATER : ComparatorNode.Compare.LESS, l, r, lit[0]));
                    }
                    case "+", "-", "*", "/" -> {
                        float[] lit = {0f};
                        int l = value(b.left()), r = rightValue(b.right(), lit);
                        if (b.op().equals("/") && r == ProgramNode.UNWIRED && lit[0] == 0f)
                            warn(b.right().at(), Diag.TEXT + "warn_zero_divisor", null);
                        MathNode.Op op = switch (b.op()) {
                            case "+" -> MathNode.Op.ADD;
                            case "-" -> MathNode.Op.SUBTRACT;
                            case "*" -> MathNode.Op.MULTIPLY;
                            default -> MathNode.Op.DIVIDE;
                        };
                        nodes.add(new MathNode(id, op, l, r, lit[0]));
                    }
                    default -> nodes.add(new LogicNode(id, b.op().equals("and") ? LogicNode.Gate.AND : LogicNode.Gate.OR, value(b.left()), value(b.right())));
                }
                return true;
            }
            case Not n -> {
                nodes.add(new LogicNode(id, LogicNode.Gate.NOT, value(n.operand()), ProgramNode.UNWIRED));
                return true;
            }
            case Call c -> {
                return call(c, id);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean call(Call c, int id) {
        List<Expr> a = c.args();
        switch (c.fn()) {
            case "sensor" -> {
                if (a.isEmpty() || a.size() > 3) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                DataNodeRef target = target(a.get(0));
                Quantity q = null;
                if (a.size() > 1) {
                    if (a.get(1) instanceof Name qn)
                        q = quantity(qn);
                    else
                        err(a.get(1).at(), Diag.TEXT + "bad_quantity", null);
                }
                int channel = a.size() > 2 ? whole(a.get(2)) : 0;
                nodes.add(new SensorNode(id, target, q, channel));
                return true;
            }
            case "actuate" -> {
                if (a.size() < 2 || a.size() > 3) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                int in = value(a.get(0));
                nodes.add(new ActuatorNode(id, in, target(a.get(1)), a.size() > 2 ? whole(a.get(2)) : 0));
                return true;
            }
            case "read" -> {
                if (a.size() != 1) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                String v = variable(a.get(0));
                if (v != null)
                    read.add(v);
                nodes.add(new ReadNode(id, v == null ? "" : v));
                return true;
            }
            case "write" -> {
                if (a.size() != 3) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                String v = variable(a.get(0));
                if (v != null)
                    written.add(v);
                nodes.add(new WriteNode(id, v == null ? "" : v, value(a.get(1)), value(a.get(2))));
                return true;
            }
            case "min", "max" -> {
                if (a.size() != 2) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                float[] lit = {0f};
                int l = value(a.get(0)), r = rightValue(a.get(1), lit);
                nodes.add(new MathNode(id, c.fn().equals("min") ? MathNode.Op.MIN : MathNode.Op.MAX, l, r, lit[0]));
                return true;
            }
            case "delay" -> {
                if (a.size() != 2) {
                    err(c.at(), Diag.TEXT + "bad_arg_count", c.fn());
                    return false;
                }
                ValueType type = c.as() != null ? c.as() : typeOf(a.get(0));
                if (type == null) {
                    err(c.at(), Diag.TEXT + "type_unknown", null);
                    type = ValueType.NUMBER;
                }
                int in = value(a.get(0));
                int ticks = ControlTuning.DELAY_NAMED_TICKS[0], countFrom = ProgramNode.UNWIRED;
                if (a.get(1) instanceof Num n) {
                    if (n.unit() == Unit.NONE) {
                        err(n.at(), Diag.TEXT + "unit_needed", null);
                    } else {
                        float t = n.value() * n.unit().ticks;
                        if (t != Math.rint(t) || t < 1 || t > ControlTuning.DELAY_MAX_TICKS)
                            err(n.at(), Diag.ERROR + "bad_delay", null);
                        else
                            ticks = (int) t;
                    }
                } else {
                    countFrom = value(a.get(1));
                }
                nodes.add(new DelayNode(id, in, type, ticks, countFrom));
                return true;
            }
            default -> {
                err(c.at(), Diag.TEXT + "unknown_call", c.fn());
                return false;
            }
        }
    }

    @javax.annotation.Nullable
    private DataNodeRef target(Expr e) {
        switch (e) {
            case Unlinked u -> {
                return null;
            }
            case Ref r -> {
                return r.ref();
            }
            case Name n -> {
                LinkDecl l = links.get(n.name());
                if (l == null) {
                    err(n.at(), Diag.TEXT + "unknown_link", n.name());
                    return null;
                }
                return l.target() instanceof Ref r ? r.ref() : null;
            }
            default -> {
                err(e.at(), Diag.TEXT + "expected", "a link");
                return null;
            }
        }
    }

    private Quantity quantity(Name n) {
        for (Quantity q : Quantity.values())
            if (q.getSerializedName().equalsIgnoreCase(n.name()))
                return q;
        err(n.at(), Diag.TEXT + "bad_quantity", n.name());
        return null;
    }

    private int whole(Expr e) {
        if (e instanceof Num n && n.unit() == Unit.NONE && n.value() == Math.rint(n.value()) && n.value() >= 0)
            return (int) n.value();
        err(e.at(), Diag.TEXT + "expected", "a whole number");
        return 0;
    }

    /** A variable named by a word or a quoted name, which must be declared. */
    @javax.annotation.Nullable
    private String variable(Expr e) {
        String name = e instanceof Name n ? n.name() : e instanceof Str s ? s.text() : null;
        if (name == null) {
            err(e.at(), Diag.TEXT + "expected", "a variable name");
            return null;
        }
        if (!vars.containsKey(name)) {
            err(e.at(), Diag.ERROR + "unknown_variable", null);
            return null;
        }
        return name;
    }

    // --- types ----------------------------------------------------------------------------------

    /** The type a value has, from its producer; null where it cannot be told (an unwired input, or a delay that only feeds itself). */
    private ValueType typeOf(Expr e) {
        return switch (e) {
            case Num n -> ValueType.NUMBER;
            case Not n -> ValueType.BOOLEAN;
            case Bin b -> switch (b.op()) {
                case "+", "-", "*", "/" -> ValueType.NUMBER;
                default -> ValueType.BOOLEAN;
            };
            case Name n -> {
                Assign a = named.get(n.name());
                if (a == null) {
                    // an alias resolved earlier points at a card by id only; look it up through the node list
                    Integer id = idOf.get(n.name());
                    yield id == null ? null : nodes.stream().filter(x -> x.id() == id).findFirst().map(this::typeOfNode).orElse(null);
                }
                if (typeCache.containsKey(n.name()))
                    yield typeCache.get(n.name());
                if (!typing.add(n.name()))
                    yield null;
                ValueType t = typeOf(a.expr());
                typing.remove(n.name());
                typeCache.put(n.name(), t);
                yield t;
            }
            case Call c -> switch (c.fn()) {
                case "min", "max" -> ValueType.NUMBER;
                case "read" -> c.args().size() == 1 && c.args().get(0) instanceof Name v && vars.containsKey(v.name()) ? vars.get(v.name()).type()
                        : c.args().size() == 1 && c.args().get(0) instanceof Str s && vars.containsKey(s.text()) ? vars.get(s.text()).type() : null;
                case "sensor" -> c.args().size() > 1 && c.args().get(1) instanceof Name q && q.name().equalsIgnoreCase(Quantity.STATE.getSerializedName())
                        ? ValueType.BOOLEAN : ValueType.NUMBER;
                case "delay" -> c.as() != null ? c.as() : c.args().isEmpty() ? null : typeOf(c.args().get(0));
                default -> null;
            };
            default -> null;
        };
    }

    private ValueType typeOfNode(ProgramNode n) {
        return switch (n) {
            case ComparatorNode x -> ValueType.BOOLEAN;
            case LogicNode x -> ValueType.BOOLEAN;
            case MathNode x -> ValueType.NUMBER;
            case SensorNode x -> x.quantity() == Quantity.STATE ? ValueType.BOOLEAN : ValueType.NUMBER;
            case DelayNode x -> x.type();
            case ReadNode x -> vars.containsKey(x.variable()) ? vars.get(x.variable()).type() : null;
            default -> null;
        };
    }

    // --- cycles and lints -----------------------------------------------------------------------

    /** A cycle is legal only through a delay's data input; the first card found on one is reported at its line. */
    private void cycle(ProgramGraph graph) {
        if (io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator.topologicalOrder(graph).isPresent())
            return;
        Map<Integer, List<Integer>> deps = new HashMap<>();
        for (ProgramNode n : graph.nodes())
            deps.put(n.id(), n instanceof DelayNode d ? (d.countWired() ? List.of(d.ticksInput()) : List.of()) : n.inputs());
        for (ProgramNode n : graph.nodes()) {
            Set<Integer> seen = new HashSet<>();
            java.util.ArrayDeque<Integer> work = new java.util.ArrayDeque<>(deps.getOrDefault(n.id(), List.of()));
            while (!work.isEmpty()) {
                int at = work.poll();
                if (at == n.id()) {
                    err(where.get(n.id()), Diag.ERROR + "cycle", null);
                    return;
                }
                if (seen.add(at))
                    work.addAll(deps.getOrDefault(at, List.of()));
            }
        }
        diags.add(new Diag(Diag.Severity.ERROR, 1, 1, 1, Diag.ERROR + "cycle", null));
    }

    private void lint(ProgramGraph graph) {
        for (VarDecl v : vars.values()) {
            boolean r = read.contains(v.name()), w = written.contains(v.name());
            if (!r && !w)
                warn(v.at(), Diag.TEXT + "warn_unused_variable", v.name());
            else if (w && !r)
                warn(v.at(), Diag.TEXT + "warn_never_read", v.name());
            else if (r && !w)
                warn(v.at(), Diag.TEXT + "warn_never_written", v.name());
        }
        for (ProgramNode n : graph.nodes()) {
            boolean terminal = n instanceof ActuatorNode || n instanceof WriteNode;
            if (!terminal && !consumed.contains(n.id()) && !feeds(graph, n.id()))
                warn(where.get(n.id()), Diag.TEXT + "warn_dead_value", null);
        }
    }

    private static boolean feeds(ProgramGraph graph, int id) {
        for (ProgramNode n : graph.nodes())
            if (n.ports().contains(id))
                return true;
        return false;
    }
}
