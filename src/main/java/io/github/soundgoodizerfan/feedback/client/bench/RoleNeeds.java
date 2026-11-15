// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.MaterialNeeds;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * What a slot takes, as words: the shape classes and forms of its stock, then the needs a material must meet. Words are free; the
 * threshold behind a ladder word is glossed on Shift from the rule file that decides it ({@code docs/design_digest.md}).
 */
final class RoleNeeds {

    private RoleNeeds() { }

    /** The lines for a role's tooltip: what it takes, what it needs, and with Shift the thresholds. Empty for a role that takes a finished component. */
    static List<Component> lines(ModularType.Role role) {
        if (role.component())
            return List.of();
        List<String> forms = role.part().map(p -> p.stock()).orElse(role.forms());
        List<String> needs = new ArrayList<>(role.part().map(p -> p.traits()).orElse(role.traits()));
        List<String> any = role.part().isPresent() ? List.of() : role.anyTraits();
        List<Component> out = new ArrayList<>();
        if (!forms.isEmpty())
            out.add(Component.translatable("modular.feedback.takes", join(forms.stream().map(RoleNeeds::form).toList())).withStyle(ChatFormatting.GRAY));
        if (!needs.isEmpty())
            out.add(Component.translatable("modular.feedback.needs", join(needs.stream().map(RoleNeeds::need).toList())).withStyle(ChatFormatting.GRAY));
        if (!any.isEmpty())
            out.add(Component.translatable("modular.feedback.needs_any", join(any.stream().map(RoleNeeds::need).toList())).withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown())
            for (String n : concat(needs, any))
                gloss(n).ifPresent(out::add);
        return out;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private static String join(List<String> words) {
        return String.join(", ", words);
    }

    /** A form entry in words: a shape class, any form, or one form by its name. */
    private static String form(String entry) {
        if (entry.equals("*"))
            return I18n.get("modular.feedback.any_form");
        if (entry.startsWith("@"))
            return I18n.get("shape.feedback." + entry.substring(1));
        return entry.replace('_', ' ');
    }

    /** A need in words: a ladder band ({@code strength at least fair}), or a named trait or composite. */
    private static String need(String need) {
        String[] p = split(need);
        if (p == null)
            return I18n.exists("need.feedback." + need) ? I18n.get("need.feedback." + need) : need.replace('_', ' ');
        String op = switch (p[1]) {
            case "<=" -> I18n.get("modular.feedback.at_most");
            case ">=" -> I18n.get("modular.feedback.at_least");
            default -> "";
        };
        return (I18n.get("need.feedback.ladder." + p[0]) + " " + op + " " + I18n.get("need.feedback." + p[0] + "." + p[2])).replace("  ", " ");
    }

    /** The threshold behind a ladder need, in the figure's own file units: the line Shift shows. */
    private static java.util.Optional<Component> gloss(String need) {
        String[] p = split(need);
        if (p == null)
            return java.util.Optional.empty();
        List<Map.Entry<String, Float>> bands = MaterialNeeds.bands(p[0]);
        for (int i = 0; i < bands.size(); i++) {
            if (!bands.get(i).getKey().equals(p[2]))
                continue;
            float max = bands.get(i).getValue();
            float floor = i == 0 ? Float.NEGATIVE_INFINITY : bands.get(i - 1).getValue();
            String what = switch (p[1]) {
                case "<=" -> I18n.get("modular.feedback.gloss.up_to", fmt(max));
                case ">=" -> floor == Float.NEGATIVE_INFINITY ? "" : I18n.get("modular.feedback.gloss.over", fmt(floor));
                default -> floor == Float.NEGATIVE_INFINITY ? I18n.get("modular.feedback.gloss.up_to", fmt(max))
                        : max == Float.POSITIVE_INFINITY ? I18n.get("modular.feedback.gloss.over", fmt(floor))
                        : I18n.get("modular.feedback.gloss.between", fmt(floor), fmt(max));
            };
            if (what.isEmpty())
                return java.util.Optional.empty();
            return java.util.Optional.of(Component.literal(need(need) + ": " + what).withStyle(ChatFormatting.DARK_GRAY));
        }
        return java.util.Optional.empty();
    }

    private static String fmt(float v) {
        return v == Float.POSITIVE_INFINITY ? "" : Float.toString(v);
    }

    /** {@code ladder, op, band} for a ladder need, or null for any other. */
    private static String[] split(String need) {
        for (String op : new String[] {"<=", ">=", ":"}) {
            int i = need.indexOf(op);
            if (i > 0)
                return new String[] {need.substring(0, i), op, need.substring(i + op.length())};
        }
        return null;
    }
}
