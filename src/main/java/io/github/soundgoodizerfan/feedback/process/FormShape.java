// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.soundgoodizerfan.feedback.registry.FormItems;

/**
 * The shape class of a form: {@code data/feedback/form_shape/<class>.json} lists the form names (and name endings) of one class
 * ({@code long}, {@code sheet}, {@code cord}, {@code lump}, {@code board}, {@code loose}). A role asks for {@code @long} and takes
 * any form of that class, so a new wood or a new kind of stick needs no role edit. A role names no pattern; patterns live here. A form in
 * no class (a lens, a motor, a part item) matches only by its own name.
 *
 * <p>Read once from the jar, like {@link FormType}, so both sides hold the same table with no datapack sync.
 */
public final class FormShape {

    private FormShape() {}

    /** One class: its exact forms and the endings that also belong to it. */
    private record Shape(String name, List<String> forms, List<String> suffixes) {}

    private static volatile List<Shape> shapes;
    private static final Map<String, String> MEMO = new ConcurrentHashMap<>();

    private static List<Shape> shapes() {
        List<Shape> have = shapes;
        if (have == null) {
            List<Shape> out = new ArrayList<>();
            FormItems.jarJson("form_shape").forEach((id, element) -> {
                JsonObject o = element.getAsJsonObject();
                out.add(new Shape(id.getPath(), strings(o, "forms"), strings(o, "suffixes")));
            });
            shapes = have = List.copyOf(out);
        }
        return have;
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key))
            for (JsonElement e : o.getAsJsonArray(key))
                out.add(e.getAsString());
        return out;
    }

    /** The class {@code form} belongs to, or empty for a form in none. */
    public static String of(String form) {
        return MEMO.computeIfAbsent(form, f -> {
            for (Shape s : shapes())
                if (s.forms().contains(f) || s.suffixes().stream().anyMatch(f::endsWith))
                    return s.name();
            return "";
        });
    }

    /**
     * Whether a role's {@code entry} takes {@code form}: {@code *} any form, {@code @long} any form of that class, else the form by its own name.
     */
    public static boolean fits(String entry, String form) {
        if (entry.equals("*"))
            return true;
        if (entry.startsWith("@"))
            return of(form).equals(entry.substring(1));
        return entry.equals(form);
    }

    /** Whether two role entries can take one form: the audit's overlap test. */
    public static boolean meet(String a, String b) {
        if (a.equals("*") || b.equals("*"))
            return true;
        boolean ca = a.startsWith("@"), cb = b.startsWith("@");
        if (ca && cb)
            return a.equals(b);
        if (ca)
            return of(b).equals(a.substring(1));
        if (cb)
            return of(a).equals(b.substring(1));
        return a.equals(b);
    }
}
