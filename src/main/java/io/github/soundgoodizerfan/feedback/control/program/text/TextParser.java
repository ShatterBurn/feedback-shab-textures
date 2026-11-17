// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.control.program.text.TextAst.*;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Kind;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Token;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Recursive descent over the tokens: statements are {@code var name: type}, {@code link name = @x y z side}, {@code name = expression} and a bare
 * {@code actuate(...)} or {@code write(...)}. Precedence, loosest first: {@code or}, {@code and}, {@code not}, {@code > <}, {@code + -},
 * {@code * /}. A statement that does not parse is reported at its token and skipped to the next line, so the rest still gets its own errors.
 */
public final class TextParser {

    /** Words the language owns, compared case-insensitively. */
    public static final List<String> KEYWORDS = List.of("var", "link", "sensor", "actuate", "write", "read", "delay", "min", "max",
            "and", "or", "not", "as", "unlinked", "unwired", "number", "boolean", "local", "voxel");

    public record Result(List<Stmt> stmts, List<Diag> diags) {
    }

    private static final class Fail extends RuntimeException {
        final Token at;
        final String key, arg;

        Fail(Token at, String key, String arg) {
            super(null, null, false, false);
            this.at = at;
            this.key = key;
            this.arg = arg;
        }
    }

    private final List<Token> tokens;
    private final List<Diag> diags = new ArrayList<>();
    private int pos, depth;

    private TextParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public static Result parse(List<Token> tokens) {
        return new TextParser(tokens).run();
    }

    private Result run() {
        List<Stmt> out = new ArrayList<>();
        while (true) {
            skipBreaks();
            if (peek().kind() == Kind.EOF)
                break;
            try {
                out.add(statement());
                Token t = peek();
                if (t.kind() != Kind.NEWLINE && t.kind() != Kind.EOF)
                    throw new Fail(t, "expected", "end of line");
            } catch (Fail f) {
                diags.add(new Diag(Diag.Severity.ERROR, f.at.line(), f.at.col(), Math.max(1, f.at.end() - f.at.start()), Diag.TEXT + f.key, f.arg));
                depth = 0;
                while (peek().kind() != Kind.NEWLINE && peek().kind() != Kind.EOF)
                    pos++;
            }
        }
        return new Result(out, diags);
    }

    // --- tokens ---------------------------------------------------------------------------------

    private Token peek() {
        while (depth > 0 && tokens.get(pos).kind() == Kind.NEWLINE)
            pos++;
        return tokens.get(pos);
    }

    private Token peek2() {
        int p = pos + 1;
        while (depth > 0 && tokens.get(Math.min(p, tokens.size() - 1)).kind() == Kind.NEWLINE)
            p++;
        return tokens.get(Math.min(p, tokens.size() - 1));
    }

    private Token next() {
        Token t = peek();
        if (t.kind() != Kind.EOF)
            pos++;
        return t;
    }

    private void skipBreaks() {
        while (tokens.get(pos).kind() == Kind.NEWLINE)
            pos++;
    }

    private Token expectSym(String s) {
        Token t = peek();
        if (!t.sym(s))
            throw new Fail(t, t.kind() == Kind.BAD ? "unexpected_char" : "expected", t.kind() == Kind.BAD ? t.text() : s);
        return next();
    }

    private Token expectIdent(String what) {
        Token t = peek();
        if (t.kind() != Kind.IDENT)
            throw new Fail(t, t.kind() == Kind.BAD ? "unexpected_char" : "expected", t.kind() == Kind.BAD ? t.text() : what);
        return next();
    }

    // --- statements -----------------------------------------------------------------------------

    private Stmt statement() {
        Token first = peek();
        if (first.kind() == Kind.BAD)
            throw new Fail(first, "unexpected_char", first.text());
        if (first.word("var")) {
            next();
            Token name = peek();
            if (name.kind() != Kind.IDENT && name.kind() != Kind.STRING)
                throw new Fail(name, "expected", "a variable name");
            next();
            expectSym(":");
            Token type = expectIdent("number or boolean");
            return new VarDecl(name.text(), valueType(type), first);
        }
        if (first.word("link")) {
            next();
            Token name = expectIdent("a link name");
            expectSym("=");
            Expr target = primary();
            if (!(target instanceof Ref) && !(target instanceof Unlinked))
                throw new Fail(target.at(), "expected", "@x y z or unlinked");
            return new LinkDecl(name.text(), target, first);
        }
        if (first.kind() == Kind.IDENT && peek2().sym("=") && !isKeyword(first.text())) {
            next();
            next();
            return new Assign(first.text(), expr(), first);
        }
        if (first.kind() == Kind.IDENT && peek2().sym("=")) {
            // a keyword on the left of '=' is a name that cannot be one
            return new Assign(first.text(), skipAssign(), first);
        }
        return new Assign(null, expr(), first);
    }

    private Expr skipAssign() {
        next();
        next();
        return expr();
    }

    public static boolean isKeyword(String s) {
        for (String k : KEYWORDS)
            if (k.equalsIgnoreCase(s))
                return true;
        return false;
    }

    private static ValueType valueType(Token t) {
        if (t.word("number"))
            return ValueType.NUMBER;
        if (t.word("boolean"))
            return ValueType.BOOLEAN;
        throw new Fail(t, "bad_type", t.text());
    }

    // --- expressions ----------------------------------------------------------------------------

    private Expr expr() {
        Expr left = and();
        while (peek().word("or")) {
            Token op = next();
            left = new Bin("or", left, and(), op);
        }
        return left;
    }

    private Expr and() {
        Expr left = not();
        while (peek().word("and")) {
            Token op = next();
            left = new Bin("and", left, not(), op);
        }
        return left;
    }

    private Expr not() {
        if (peek().word("not")) {
            Token op = next();
            return new Not(not(), op);
        }
        return compare();
    }

    private Expr compare() {
        Expr left = add();
        Token t = peek();
        if (t.sym(">") || t.sym("<")) {
            next();
            return new Bin(t.text(), left, add(), t);
        }
        return left;
    }

    private Expr add() {
        Expr left = mul();
        while (peek().sym("+") || peek().sym("-")) {
            Token op = next();
            left = new Bin(op.text(), left, mul(), op);
        }
        return left;
    }

    private Expr mul() {
        Expr left = primary();
        while (peek().sym("*") || peek().sym("/")) {
            Token op = next();
            left = new Bin(op.text(), left, primary(), op);
        }
        return left;
    }

    private Expr primary() {
        Token t = peek();
        switch (t.kind()) {
            case NUMBER -> {
                next();
                return number(t, false);
            }
            case STRING -> {
                next();
                return new Str(t.text(), t);
            }
            case BAD -> throw new Fail(t, "unexpected_char", t.text());
            case SYM -> {
                if (t.sym("-") && peek2().kind() == Kind.NUMBER) {
                    next();
                    return number(next(), true);
                }
                if (t.sym("(")) {
                    next();
                    depth++;
                    Expr inner = expr();
                    expectSym(")");
                    depth--;
                    return inner;
                }
                if (t.sym("@"))
                    return ref();
                throw new Fail(t, "unexpected_token", t.text());
            }
            case IDENT -> {
                next();
                if (t.word("unwired"))
                    return new Unwired(t);
                if (t.word("unlinked"))
                    return new Unlinked(t);
                if (peek().sym("("))
                    return call(t);
                return new Name(t.text(), t);
            }
            default -> throw new Fail(t, "expected", "a value");
        }
    }

    private Expr call(Token fn) {
        expectSym("(");
        depth++;
        List<Expr> args = new ArrayList<>();
        if (!peek().sym(")")) {
            args.add(expr());
            while (peek().sym(",")) {
                next();
                args.add(expr());
            }
        }
        expectSym(")");
        depth--;
        ValueType as = null;
        if (peek().word("as")) {
            next();
            as = valueType(expectIdent("number or boolean"));
        }
        return new Call(fn.text().toLowerCase(Locale.ROOT), args, as, fn);
    }

    private Expr number(Token t, boolean negative) {
        String s = t.text();
        Unit unit = Unit.NONE;
        char last = s.charAt(s.length() - 1);
        if (Character.isLetter(last)) {
            unit = switch (last) {
                case 's' -> Unit.SECONDS;
                case 'm' -> Unit.MINUTES;
                case 'h' -> Unit.HOURS;
                default -> Unit.TICKS;
            };
            s = s.substring(0, s.length() - 1);
        }
        float v;
        try {
            v = Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new Fail(t, "bad_number", t.text());
        }
        return new Num(negative ? -v : v, unit, t);
    }

    private int integer() {
        Token t = peek();
        boolean neg = false;
        if (t.sym("-")) {
            next();
            neg = true;
            t = peek();
        }
        if (t.kind() != Kind.NUMBER || t.text().contains(".") || Character.isLetter(t.text().charAt(t.text().length() - 1)))
            throw new Fail(t, "expected", "a whole number");
        next();
        int v = Integer.parseInt(t.text());
        return neg ? -v : v;
    }

    private Expr ref() {
        Token at = next();
        if (peek().word("local")) {
            next();
            return new Ref(DataNodeRef.local(integer()), at);
        }
        int x = integer(), y = integer(), z = integer();
        Direction side = null;
        if (peek().kind() == Kind.IDENT && !peek().word("voxel")) {
            Token s = next();
            side = Direction.byName(s.text().toLowerCase(Locale.ROOT));
            if (side == null)
                throw new Fail(s, "bad_side", s.text());
        }
        int voxel = -1;
        if (peek().word("voxel")) {
            next();
            voxel = integer();
        }
        return new Ref(new DataNodeRef(new BlockPos(x, y, z), side, voxel), at);
    }
}
