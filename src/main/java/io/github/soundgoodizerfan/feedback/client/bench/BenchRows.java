// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.modular.type.ModularMeasures;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * How a {@link BenchUses.Use} sits in the Blueprint Table's figure list beside the shared figures: its short label, whether it is a bar or
 * a word, what a full bar is, and what Shift prints. None of it is stored in the {@code Use} record, so a new use id works without any
 * entry here: it gets a bar, the label {@code <line key>.label} (else its id's last word), and the number its line carries.
 */
final class BenchRows {

    private BenchRows() {
    }

    /** The uses that are words, ranks or yes/no and not quantities: printed as text in the list, never as a bar. They still sort (by rank) and hover like any row. */
    private static final Set<String> TEXT = Set.of("cell.band", "machine.band", "egun.band", ModularMeasures.BLOCK + "Band", ModularMeasures.BLOCK + "Divergent");

    /** Whether {@code id} is read as a word rather than a quantity. */
    static boolean text(String id) {
        return TEXT.contains(id) || id.endsWith(".band");
    }

    /** What a full bar is for a share that is a fraction of a whole (1 is all of it); NaN for every other id, which takes the first drawing's value times 2.5, grown as the figures' bars are. */
    static float fixedTop(String id) {
        return switch (id) {
            case "egun.hold", "egun.pump", "egun.zoom", "egun.gauge", "egun.sense", "egun.lining", "egun.loss" -> 1f;
            default -> Float.NaN;
        };
    }

    /** The lang key of a row's short label: {@code <the line's key>.label}, except an axis, whose own name is already short. */
    static String labelKey(String id) {
        String base = ModularMeasures.langKey(id);
        return id.startsWith(ModularMeasures.AXIS) ? base : base + ".label";
    }

    /** The short name a row carries in the label column. */
    static String label(BenchUses.Use u) {
        String key = labelKey(u.id());
        if (I18n.exists(key))
            return I18n.get(key);
        String id = u.id();
        String last = id.substring(id.lastIndexOf('.') + 1);
        return last.isEmpty() ? id : Character.toUpperCase(last.charAt(0)) + last.substring(1);
    }

    /** What a text row says: a band's name, yes or no, else the line's own number. */
    static String word(BenchUses.Use u) {
        if (u.id().equals(ModularMeasures.BLOCK + "Divergent"))
            return I18n.get(u.value() > 0 ? "gui.feedback.blueprint_table.word.yes" : "gui.feedback.blueprint_table.word.no");
        if (text(u.id())) {
            VoltageBand[] bands = VoltageBand.values();
            return I18n.get(bands[Math.max(0, Math.min(bands.length - 1, Math.round(u.value())))].key());
        }
        return readout(u);
    }

    private static final Pattern UNIT = Pattern.compile("^(%%|\\s?(Su|Eu|Mu|Pu|Tu|St|A|RPM|mB|m|s|t)(?![A-Za-z]))");

    /** What Shift prints for a bar: the number the line shows and its unit (the line's translation says it after the number). */
    static String readout(BenchUses.Use u) {
        Component line = u.line();
        if (line.getContents() instanceof TranslatableContents tc && tc.getArgs().length > 0) {
            Object a = tc.getArgs()[0];
            String num = a instanceof Component c ? c.getString() : String.valueOf(a);
            String tpl = Language.getInstance().getOrDefault(tc.getKey());
            int at = tpl.indexOf("%s");
            if (at >= 0) {
                Matcher m = UNIT.matcher(tpl.substring(at + 2));
                if (m.find())
                    return num + (m.group().equals("%%") ? "%" : " " + m.group().trim());
            }
            return num;
        }
        String s = line.getString();
        int i = s.indexOf(": ");
        return i >= 0 ? s.substring(i + 2) : s;
    }
}
