// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import java.util.ArrayList;
import java.util.List;

/**
 * The tolerant lexer: it never throws. Anything it cannot read becomes a {@code BAD} token (the parser reports it and goes on), so one
 * stray character does not hide the other errors of a text. {@code --} starts a comment to the end of the line; a newline or {@code ;}
 * ends a statement.
 *
 * <p>Design from Super Factory Manager (MPL-2.0, design only, no code copied): taken, the tolerant lexer feeding a typed syntax tree, so a
 * half-typed text still yields tokens for completion and every error carries a line and column; rejected, its imperative statements, its
 * resource-id and tag routing and its {@code every} trigger. See THIRD-PARTY-LICENSES.md.
 */
public final class TextLexer {

    public enum Kind { IDENT, NUMBER, STRING, SYM, NEWLINE, EOF, BAD }

    /** One token: {@code line} and {@code col} are 1-based, {@code start} and {@code end} offsets into the text. */
    public record Token(Kind kind, String text, int line, int col, int start, int end) {
        public boolean sym(String s) {
            return kind == Kind.SYM && text.equals(s);
        }

        /** A keyword or name matches case-insensitively here; only keywords are compared this way. */
        public boolean word(String w) {
            return kind == Kind.IDENT && text.equalsIgnoreCase(w);
        }
    }

    private TextLexer() {
    }

    public static List<Token> lex(String s) {
        List<Token> out = new ArrayList<>();
        int i = 0, line = 1, lineStart = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            int col = i - lineStart + 1;
            if (c == '\n') {
                out.add(new Token(Kind.NEWLINE, "\n", line, col, i, i + 1));
                i++;
                line++;
                lineStart = i;
            } else if (c == ' ' || c == '\t' || c == '\r') {
                i++;
            } else if (c == ';') {
                out.add(new Token(Kind.NEWLINE, ";", line, col, i, i + 1));
                i++;
            } else if (c == '-' && i + 1 < n && s.charAt(i + 1) == '-') {
                while (i < n && s.charAt(i) != '\n')
                    i++;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i + 1;
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_'))
                    j++;
                out.add(new Token(Kind.IDENT, s.substring(i, j), line, col, i, j));
                i = j;
            } else if (Character.isDigit(c)) {
                int j = i + 1;
                while (j < n && Character.isDigit(s.charAt(j)))
                    j++;
                if (j + 1 < n && s.charAt(j) == '.' && Character.isDigit(s.charAt(j + 1))) {
                    j++;
                    while (j < n && Character.isDigit(s.charAt(j)))
                        j++;
                }
                // a unit letter right after the digits (5s, 40t, 2m, 1h) belongs to the number
                if (j < n && "smht".indexOf(s.charAt(j)) >= 0 && (j + 1 >= n || !Character.isLetterOrDigit(s.charAt(j + 1)) && s.charAt(j + 1) != '_'))
                    j++;
                out.add(new Token(Kind.NUMBER, s.substring(i, j), line, col, i, j));
                i = j;
            } else if (c == '"') {
                int j = i + 1;
                while (j < n && s.charAt(j) != '"' && s.charAt(j) != '\n')
                    j++;
                boolean closed = j < n && s.charAt(j) == '"';
                out.add(new Token(closed ? Kind.STRING : Kind.BAD, s.substring(i + 1, j), line, col, i, closed ? j + 1 : j));
                i = closed ? j + 1 : j;
            } else if ("><+-*/(),=:@".indexOf(c) >= 0) {
                out.add(new Token(Kind.SYM, String.valueOf(c), line, col, i, i + 1));
                i++;
            } else {
                out.add(new Token(Kind.BAD, String.valueOf(c), line, col, i, i + 1));
                i++;
            }
        }
        out.add(new Token(Kind.EOF, "", line, n - lineStart + 1, n, n));
        return out;
    }
}
