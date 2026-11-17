// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Kind;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer.Token;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

/**
 * Grammar-driven completion: what may stand at the caret, from the tokens before it on the statement and the names the whole text declares.
 * A statement's first word offers its keywords and the names so far; after {@code sensor(} the quantities, then the kinds, colors and reductions; after
 * {@code read(} and {@code write(} the declared variables; after {@code var x:} or {@code as} the two types; elsewhere the names of the
 * cards and the functions and operators of a value. It reads the text and the kind tags, never the world, so it offers no block.
 *
 * <p>Design from Super Factory Manager (MPL-2.0, design only): grammar-driven completion of keywords, port names and declared names.
 */
public final class TextCompletion {

    /** What the caret is in the middle of, and what could complete it. */
    public record Suggestions(int from, String prefix, List<String> items) {
    }

    private TextCompletion() {
    }

    /** Suggestions at {@code caret} (an offset into {@code text}); empty items when nothing fits. */
    public static Suggestions at(String text, int caret) {
        caret = Math.max(0, Math.min(caret, text.length()));
        List<Token> all = TextLexer.lex(text);
        // the word being typed: the identifier token that ends at the caret
        int from = caret;
        while (from > 0 && (Character.isLetterOrDigit(text.charAt(from - 1)) || text.charAt(from - 1) == '_'))
            from--;
        String prefix = text.substring(from, caret).toLowerCase(Locale.ROOT);
        // tokens of the current statement before the word
        List<Token> before = new ArrayList<>();
        int depth = 0;
        for (Token t : all) {
            if (t.end() > from || t.kind() == Kind.EOF)
                break;
            if (t.kind() == Kind.NEWLINE && depth <= 0) {
                before.clear();
                continue;
            }
            if (t.sym("("))
                depth++;
            if (t.sym(")"))
                depth--;
            before.add(t);
        }
        Set<String> out = new LinkedHashSet<>();
        Token last = before.isEmpty() ? null : before.get(before.size() - 1);
        if (before.isEmpty()) {
            out.addAll(List.of("var", "actuate", "write"));
            out.addAll(names(all));
        } else if (last.word("as") || last.sym(":") && before.size() >= 2 && before.get(0).word("var")) {
            out.addAll(List.of("number", "boolean"));
        } else {
            String fn = openCall(before);
            int arg = argIndex(before);
            if (fn == null) {
                out.addAll(operands(all));
            } else if (fn.equals("sensor") && arg == 0) {
                for (Quantity q : Quantity.values())
                    out.add(q.getSerializedName());
            } else if (fn.equals("sensor") && arg >= 1 || fn.equals("actuate") && arg >= 1) {
                for (var kind : io.github.soundgoodizerfan.feedback.control.data.DataSpec.allKinds())
                    out.add(TextWords.kind(kind));
                for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values())
                    out.add(dye.getName());
                if (fn.equals("sensor"))
                    out.addAll(List.of("avg", "min", "max"));
            } else if (fn.equals("read") && arg == 0 || fn.equals("write") && arg == 0) {
                out.addAll(variables(all));
            } else {
                out.addAll(operands(all));
            }
        }
        List<String> items = new ArrayList<>();
        for (String s : out)
            if (s.toLowerCase(Locale.ROOT).startsWith(prefix) && !s.equalsIgnoreCase(prefix))
                items.add(s);
        return new Suggestions(from, text.substring(from, caret), items);
    }

    private static List<String> operands(List<Token> all) {
        List<String> out = new ArrayList<>(names(all));
        out.addAll(List.of("sensor", "read", "delay", "min", "max", "not", "and", "or", "unwired"));
        return out;
    }

    /** The innermost call still open at the end of {@code before}, or null. */
    private static String openCall(List<Token> before) {
        int depth = 0;
        for (int i = before.size() - 1; i >= 0; i--) {
            Token t = before.get(i);
            if (t.sym(")"))
                depth++;
            else if (t.sym("(")) {
                if (depth == 0)
                    return i > 0 && before.get(i - 1).kind() == Kind.IDENT ? before.get(i - 1).text().toLowerCase(Locale.ROOT) : null;
                depth--;
            }
        }
        return null;
    }

    /** How many top-level commas of the innermost open call lie before the caret. */
    private static int argIndex(List<Token> before) {
        int depth = 0, commas = 0;
        for (int i = before.size() - 1; i >= 0; i--) {
            Token t = before.get(i);
            if (t.sym(")"))
                depth++;
            else if (t.sym("(")) {
                if (depth == 0)
                    break;
                depth--;
            } else if (t.sym(",") && depth == 0)
                commas++;
        }
        return commas;
    }

    /** The names a text declares: its cards, by scanning {@code name =} at statement starts. */
    private static List<String> names(List<Token> all) {
        List<String> out = new ArrayList<>();
        boolean start = true;
        for (int i = 0; i < all.size(); i++) {
            Token t = all.get(i);
            if (t.kind() == Kind.NEWLINE) {
                start = true;
                continue;
            }
            if (start && t.kind() == Kind.IDENT && i + 1 < all.size() && all.get(i + 1).sym("=") && !TextParser.isKeyword(t.text()))
                out.add(t.text());
            start = false;
        }
        return out;
    }

    private static List<String> variables(List<Token> all) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i + 1 < all.size(); i++)
            if (all.get(i).word("var") && (all.get(i + 1).kind() == Kind.IDENT || all.get(i + 1).kind() == Kind.STRING)) {
                String n = all.get(i + 1).text();
                out.add(all.get(i + 1).kind() == Kind.STRING ? "\"" + n + "\"" : n);
            }
        return out;
    }
}
