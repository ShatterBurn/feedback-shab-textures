// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.List;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Token;

/** The typed syntax tree of a program text: statements and the expressions on their right. */
public final class TextAst {

    private TextAst() {
    }

    /** What a duration literal was written in: the unit decides its ticks. */
    public enum Unit {
        NONE(0), TICKS(1), SECONDS(20), MINUTES(1200), HOURS(72000);

        public final int ticks;

        Unit(int ticks) {
            this.ticks = ticks;
        }
    }

    public sealed interface Expr permits Num, Name, Str, Unwired, Unlinked, Ref, Bin, Not, Call {
        Token at();
    }

    public record Num(float value, Unit unit, Token at) implements Expr {
    }

    /** A program-local name: another statement's value, or a link alias. */
    public record Name(String name, Token at) implements Expr {
    }

    /** A quoted name (a variable). */
    public record Str(String text, Token at) implements Expr {
    }

    /** An input left without a wire. */
    public record Unwired(Token at) implements Expr {
    }

    /** A sensor or actuator with no target yet. */
    public record Unlinked(Token at) implements Expr {
    }

    /** {@code @x y z [side] [voxel n]} or {@code @local n}: a node reference, the form the graph itself holds. */
    public record Ref(DataNodeRef ref, Token at) implements Expr {
    }

    /** {@code op} is one of {@code > < + - * / and or}. */
    public record Bin(String op, Expr left, Expr right, Token at) implements Expr {
    }

    public record Not(Expr operand, Token at) implements Expr {
    }

    /** {@code fn} is lower case; {@code as} is the optional type of a delay's {@code as} suffix. */
    public record Call(String fn, List<Expr> args, ValueType as, Token at) implements Expr {
    }

    public sealed interface Stmt permits VarDecl, LinkDecl, Assign {
        Token at();
    }

    public record VarDecl(String name, ValueType type, Token at) implements Stmt {
    }

    public record LinkDecl(String name, Expr target, Token at) implements Stmt {
    }

    /** {@code name = expr}, or a bare {@code expr} (an actuator or a write) with a null name. */
    public record Assign(String name, Expr expr, Token at) implements Stmt {
    }
}
