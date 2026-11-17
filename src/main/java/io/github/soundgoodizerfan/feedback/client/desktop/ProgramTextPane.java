// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.text.Diag;
import io.github.soundgoodizerfan.feedback.control.program.text.TextCompiler;
import io.github.soundgoodizerfan.feedback.control.program.text.TextCompletion;
import io.github.soundgoodizerfan.feedback.control.program.text.TextLexer;
import io.github.soundgoodizerfan.feedback.control.program.text.TextParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The text view of a program, inside the Computer's Editor window: a small code editor (lines with numbers, a caret, keyword colouring, the
 * compiler's findings underlined and listed below, grammar-driven completion at the caret). It compiles on every edit with
 * {@link TextCompiler}; it never changes the program itself, the window applies a text that compiled. Nothing here names an item or a block.
 *
 * <p>Keys: arrows, Home, End, Page Up and Down move; Enter, Backspace, Delete edit; Tab takes the highlighted completion (or inserts two
 * spaces); Ctrl+Space opens completion; Ctrl+A selects all, and Ctrl+C, Ctrl+X and Ctrl+V copy, cut and paste it or the clipboard.
 */
final class ProgramTextPane {

    private static final int LINE_H = 10, DIAG_LINES = 5, TOP = 24, MAX_ITEMS = 6, MAX_CHARS = 20000;

    private final Font font = Minecraft.getInstance().font;
    private String text = "", loaded = "";
    private int caret, scroll, wantCol = -1;
    private boolean selectAll, forced;
    private TextCompiler.Result result;
    private TextCompletion.Suggestions suggestions = new TextCompletion.Suggestions(0, "", List.of());
    private int pick;
    private int areaW, areaH, gutter;

    void load(String printed) {
        text = loaded = printed;
        caret = Math.min(caret, text.length());
        scroll = 0;
        selectAll = false;
        recompile();
    }

    String text() {
        return text;
    }

    boolean dirty() {
        return !text.equals(loaded);
    }

    TextCompiler.Result result() {
        return result;
    }

    private void recompile() {
        result = TextCompiler.compile(text, ControllerLimits.MAX);
        suggest();
    }

    private void suggest() {
        suggestions = TextCompletion.at(text, caret);
        pick = 0;
        if (suggestions.prefix().isEmpty() && !forced)
            suggestions = new TextCompletion.Suggestions(suggestions.from(), "", List.of());
    }

    // --- geometry ---------------------------------------------------------------------------

    private List<String> lines() {
        return List.of(text.split("\n", -1));
    }

    private int lineOf(int offset) {
        int n = 0;
        for (int i = 0; i < Math.min(offset, text.length()); i++)
            if (text.charAt(i) == '\n')
                n++;
        return n;
    }

    private int lineStart(int line) {
        int at = 0;
        for (int n = 0; n < line; n++) {
            int nl = text.indexOf('\n', at);
            if (nl < 0)
                return text.length();
            at = nl + 1;
        }
        return at;
    }

    private int visible() {
        return Math.max(1, (areaH - TOP - DIAG_LINES * LINE_H - 8) / LINE_H);
    }

    private void keepCaretVisible() {
        int line = lineOf(caret);
        if (line < scroll)
            scroll = line;
        else if (line >= scroll + visible())
            scroll = line - visible() + 1;
    }

    // --- rendering --------------------------------------------------------------------------

    void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        areaW = ui.w();
        areaH = ui.h();
        gutter = font.width("000") + 6;
        int textH = visible() * LINE_H + 4;
        ui.inset(0, TOP - 2, areaW, textH);
        List<String> lines = lines();
        ui.clip(1, TOP - 1, areaW - 2, textH - 2);
        for (int i = 0; i < visible() && scroll + i < lines.size(); i++) {
            int n = scroll + i, y = TOP + i * LINE_H;
            ui.text(2, y, String.valueOf(n + 1), skin.dim());
            drawLine(ui, skin, lines.get(n), gutter, y);
            for (Diag d : result.diags())
                if (d.line() == n + 1 && d.line() > 0 && (d.col() > 1 || d.severity() == Diag.Severity.ERROR || d.severity() == Diag.Severity.WARN)) {
                    int x0 = gutter + font.width(lines.get(n).substring(0, Math.min(lines.get(n).length(), d.col() - 1)));
                    int x1 = gutter + font.width(lines.get(n).substring(0, Math.min(lines.get(n).length(), d.col() - 1 + d.length())));
                    ui.fill(x0, y + LINE_H - 2, Math.max(3, x1 - x0), 1, color(skin, d));
                }
        }
        if (selectAll)
            ui.fill(gutter, TOP, areaW - gutter - 2, Math.min(lines.size() - scroll, visible()) * LINE_H, 0x40FFFFFF);
        int cl = lineOf(caret) - scroll;
        if (cl >= 0 && cl < visible() && (System.currentTimeMillis() / 500) % 2 == 0) {
            String line = lines.get(lineOf(caret));
            int col = caret - lineStart(lineOf(caret));
            ui.fill(gutter + font.width(line.substring(0, Math.min(col, line.length()))), TOP + cl * LINE_H, 1, LINE_H - 1, skin.accent());
        }
        ui.unclip();
        ui.scrollbar(areaW - 4, TOP - 2, textH, lines.size(), visible(), scroll);
        drawPopup(ui, skin, lines);
        drawDiagnostics(ui, skin, TOP + textH + 2);
    }

    private static int color(GuiSkin skin, Diag d) {
        return switch (d.severity()) {
            case ERROR -> skin.warn();
            case PRINT -> skin.dim();
            case WARN -> skin.accent();
        };
    }

    private void drawLine(Ui ui, GuiSkin skin, String line, int x, int y) {
        int comment = commentAt(line);
        String code = comment >= 0 ? line.substring(0, comment) : line;
        int drawn = 0;
        for (TextLexer.Token t : TextLexer.lex(code)) {
            if (t.kind() == TextLexer.Kind.NEWLINE || t.kind() == TextLexer.Kind.EOF)
                continue;
            int tx = x + font.width(code.substring(0, t.start()));
            int c = t.kind() == TextLexer.Kind.IDENT && TextParser.isKeyword(t.text()) ? skin.accent()
                    : t.kind() == TextLexer.Kind.BAD ? skin.warn() : skin.ink();
            ui.text(tx, y, code.substring(t.start(), t.end()), c);
            drawn = t.end();
        }
        if (comment >= 0)
            ui.text(x + font.width(code), y, line.substring(comment), skin.dim());
    }

    private static int commentAt(String line) {
        boolean quoted = false;
        for (int i = 0; i + 1 < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"')
                quoted = !quoted;
            else if (!quoted && c == '-' && line.charAt(i + 1) == '-')
                return i;
        }
        return -1;
    }

    private void drawPopup(Ui ui, GuiSkin skin, List<String> lines) {
        if (suggestions.items().isEmpty())
            return;
        int cl = lineOf(caret) - scroll;
        if (cl < 0 || cl >= visible())
            return;
        String line = lines.get(lineOf(caret));
        int from = Math.min(line.length(), suggestions.from() - lineStart(lineOf(caret)));
        int x = gutter + font.width(line.substring(0, Math.max(0, from)));
        int n = Math.min(MAX_ITEMS, suggestions.items().size()), w = 8;
        for (String s : suggestions.items())
            w = Math.max(w, font.width(s) + 8);
        int y = TOP + (cl + 1) * LINE_H + 1;
        ui.plaque(x, y, w, n * LINE_H + 4);
        int first = Math.max(0, Math.min(pick - MAX_ITEMS + 1, suggestions.items().size() - n));
        for (int i = 0; i < n; i++) {
            boolean on = first + i == pick;
            if (on)
                ui.fill(x + 2, y + 2 + i * LINE_H, w - 4, LINE_H, skin.accent() & 0xFFFFFF | 0x50000000);
            ui.text(x + 4, y + 2 + i * LINE_H + 1, suggestions.items().get(first + i), on ? skin.accent() : skin.ink());
        }
    }

    private void drawDiagnostics(Ui ui, GuiSkin skin, int y) {
        List<Diag> shown = new ArrayList<>(result.diags());
        shown.sort(java.util.Comparator.comparingInt((Diag d) -> d.severity().ordinal()).thenComparingInt(Diag::line));
        if (shown.isEmpty()) {
            ui.text(2, y + 1, Component.translatable("gui.feedback.computer.text_ok", result.graph() == null ? 0 : result.graph().nodes().size()), skin.accent());
            return;
        }
        int more = Math.max(0, shown.size() - DIAG_LINES);
        for (int i = 0; i < Math.min(shown.size(), DIAG_LINES); i++) {
            Diag d = shown.get(i);
            String msg = (d.arg() == null ? Component.translatable(d.key()) : Component.translatable(d.key(), d.arg())).getString();
            String prefix = d.line() + ":" + d.col() + " ";
            ui.clipped(2, y + i * LINE_H, areaW - 6, prefix + msg, color(skin, d));
        }
        if (more > 0)
            ui.text(areaW - 40, y + (DIAG_LINES - 1) * LINE_H, "+" + more, skin.dim());
    }

    // --- input ------------------------------------------------------------------------------

    private void insert(String s) {
        if (selectAll) {
            text = "";
            caret = 0;
            selectAll = false;
        }
        if (text.length() + s.length() > MAX_CHARS)
            return;
        text = text.substring(0, caret) + s + text.substring(caret);
        caret += s.length();
        changed();
    }

    private void changed() {
        wantCol = -1;
        forced = false;
        keepCaretVisible();
        recompile();
    }

    private void moved() {
        selectAll = false;
        keepCaretVisible();
        suggest();
    }

    private void accept() {
        String item = suggestions.items().get(pick);
        text = text.substring(0, suggestions.from()) + item + text.substring(caret);
        caret = suggestions.from() + item.length();
        // a call is completed up to its bracket: the caret goes inside it
        if (java.util.List.of("sensor", "read", "delay", "min", "max", "actuate", "write").contains(item) && !text.startsWith("(", caret)) {
            text = text.substring(0, caret) + "()" + text.substring(caret);
            caret++;
        }
        changed();
    }

    boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 2) != 0;
        boolean popup = !suggestions.items().isEmpty();
        if (ctrl && key == 32) {
            forced = true;
            suggest();
            return true;
        }
        if (ctrl && key == 65) {
            selectAll = true;
            return true;
        }
        if (ctrl && (key == 67 || key == 88)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            if (key == 88) {
                text = "";
                caret = 0;
                changed();
            }
            return true;
        }
        if (ctrl && key == 86) {
            insert(Minecraft.getInstance().keyboardHandler.getClipboard().replace("\r", ""));
            return true;
        }
        switch (key) {
            case 256 -> {
                if (popup) {
                    forced = false;
                    suggestions = new TextCompletion.Suggestions(0, "", List.of());
                    return true;
                }
                return false;
            }
            case 257, 335 -> {
                if (popup)
                    accept();
                else
                    insert("\n");
                return true;
            }
            case 258 -> {
                if (popup)
                    accept();
                else
                    insert("  ");
                return true;
            }
            case 259 -> {
                if (selectAll) {
                    text = "";
                    caret = 0;
                    selectAll = false;
                } else if (caret > 0) {
                    text = text.substring(0, caret - 1) + text.substring(caret);
                    caret--;
                }
                changed();
                return true;
            }
            case 261 -> {
                if (caret < text.length()) {
                    text = text.substring(0, caret) + text.substring(caret + 1);
                    changed();
                }
                return true;
            }
            case 262 -> {
                caret = Math.min(text.length(), caret + 1);
                wantCol = -1;
                moved();
                return true;
            }
            case 263 -> {
                caret = Math.max(0, caret - 1);
                wantCol = -1;
                moved();
                return true;
            }
            case 264, 265 -> {
                if (popup) {
                    int n = suggestions.items().size();
                    pick = (pick + (key == 264 ? 1 : n - 1)) % n;
                    return true;
                }
                moveLine(key == 264 ? 1 : -1);
                return true;
            }
            case 266, 267 -> {
                moveLine(key == 267 ? visible() : -visible());
                return true;
            }
            case 268 -> {
                caret = lineStart(lineOf(caret));
                wantCol = -1;
                moved();
                return true;
            }
            case 269 -> {
                int nl = text.indexOf('\n', caret);
                caret = nl < 0 ? text.length() : nl;
                wantCol = -1;
                moved();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void moveLine(int by) {
        int line = lineOf(caret);
        int col = wantCol >= 0 ? wantCol : caret - lineStart(line);
        wantCol = col;
        int to = Math.max(0, Math.min(lines().size() - 1, line + by));
        int start = lineStart(to);
        int len = lines().get(to).length();
        caret = start + Math.min(col, len);
        selectAll = false;
        keepCaretVisible();
        suggest();
    }

    boolean charTyped(char c) {
        if (c < 32 || c == 127)
            return false;
        insert(String.valueOf(c));
        return true;
    }

    boolean mouseClicked(double lx, double ly) {
        if (ly < TOP - 2 || ly > TOP + visible() * LINE_H)
            return false;
        int line = Math.max(0, Math.min(lines().size() - 1, scroll + (int) ((ly - TOP) / LINE_H)));
        String s = lines().get(line);
        int col = 0;
        while (col < s.length() && gutter + font.width(s.substring(0, col + 1)) <= lx + 2)
            col++;
        caret = lineStart(line) + col;
        wantCol = -1;
        moved();
        return true;
    }

    boolean mouseScrolled(double wheel) {
        scroll = Math.max(0, Math.min(Math.max(0, lines().size() - visible()), scroll - (int) Math.signum(wheel) * 3));
        return true;
    }
}
