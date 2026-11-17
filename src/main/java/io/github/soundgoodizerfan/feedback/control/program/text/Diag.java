// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

/**
 * One finding of the text compiler, at a 1-based line and column ({@code length} columns long). {@code key} is a full lang key
 * ({@code feedback.programmer.text.*} for what the text itself got wrong, {@code feedback.programmer.error.*} for what the graph's own
 * checks say) and {@code arg} fills its {@code %s}.
 *
 * @param severity ERROR blocks applying the text; PRINT is a problem only the print-time check ({@code ProgramGraph#findError}) finds, so the
 *                 text still applies as a working copy; WARN is a lint, never a refusal.
 */
public record Diag(Severity severity, int line, int col, int length, String key, String arg) {

    public enum Severity { ERROR, PRINT, WARN }

    public static final String TEXT = "feedback.programmer.text.";
    public static final String ERROR = "feedback.programmer.error.";

    public boolean blocks() {
        return severity == Severity.ERROR;
    }
}
