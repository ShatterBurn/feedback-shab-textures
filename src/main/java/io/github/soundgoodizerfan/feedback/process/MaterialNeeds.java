// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.unit.Unit;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

/**
 * What a role asks of a material, by name: a stated trait ({@code fibrous}), a built-in ({@code magnetic}, {@code edible}), a band of a
 * figure's ladder ({@code hardness<=yielding}, {@code strength>=fair}, {@code heat_conduction:insulator}) or a composite that names a
 * use ({@code cushioning}). Ladders and composites are {@code data/feedback/trait_rule/<name>.json}, read once from the jar like
 * {@link FormShape}; the bands are absolute, so "soft" never means soft for a metal. A figure a material does not state is on no band.
 *
 * <p>A ladder file is {@code {"ladder": {"figure": ..., "bands": [{"name", "max"}, ...]}}}, the last band with no {@code max}. A composite file
 * is {@code {"all": [...], "any": [...], "none": [...]}}, each entry a need.
 */
public final class MaterialNeeds {

    private MaterialNeeds() {}

    private record Band(String name, float max) {}

    private record Ladder(String figure, List<Band> bands) {
        int rank(String band) {
            for (int i = 0; i < bands.size(); i++)
                if (bands.get(i).name().equals(band))
                    return i;
            return -1;
        }
    }

    private record Composite(List<String> all, List<String> any, List<String> none) {}

    private static volatile Map<String, Ladder> ladders;
    private static volatile Map<String, Composite> composites;
    private static final Set<String> WARNED = new HashSet<>();

    private static void load() {
        if (ladders != null)
            return;
        synchronized (MaterialNeeds.class) {
            if (ladders != null)
                return;
            Map<String, Ladder> lad = new TreeMap<>();
            Map<String, Composite> com = new TreeMap<>();
            FormItems.jarJson("trait_rule").forEach((id, element) -> {
                JsonObject o = element.getAsJsonObject();
                if (o.has("ladder")) {
                    JsonObject l = o.getAsJsonObject("ladder");
                    List<Band> bands = new ArrayList<>();
                    for (JsonElement b : l.getAsJsonArray("bands")) {
                        JsonObject bo = b.getAsJsonObject();
                        bands.add(new Band(bo.get("name").getAsString(), bo.has("max") ? bo.get("max").getAsFloat() : Float.POSITIVE_INFINITY));
                    }
                    lad.put(id.getPath(), new Ladder(l.get("figure").getAsString(), List.copyOf(bands)));
                } else {
                    com.put(id.getPath(), new Composite(strings(o, "all"), strings(o, "any"), strings(o, "none")));
                }
            });
            composites = com;
            ladders = lad;
        }
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        if (o.has(key))
            for (JsonElement e : o.getAsJsonArray(key))
                out.add(e.getAsString());
        return List.copyOf(out);
    }

    /** Whether {@code material} meets {@code need}. */
    public static boolean has(Material material, String need) {
        load();
        return has(material, need, 0);
    }

    /** Whether {@code material} meets every need of {@code needs}. */
    public static boolean hasAll(Material material, List<String> needs) {
        for (String n : needs)
            if (!has(material, n))
                return false;
        return true;
    }

    private static boolean has(Material m, String need, int depth) {
        if (depth > 4)
            return false;
        int le = need.indexOf("<="), ge = need.indexOf(">="), eq = need.indexOf(':');
        if (le > 0)
            return compare(m, need.substring(0, le), need.substring(le + 2), -1);
        if (ge > 0)
            return compare(m, need.substring(0, ge), need.substring(ge + 2), 1);
        if (eq > 0)
            return compare(m, need.substring(0, eq), need.substring(eq + 1), 0);
        switch (need) {
            case "magnetic":
                return m.separation().magnetic();
            case "edible":
                return m.food().isPresent();
            case "propellant":
                return m.propellant().isPresent();
            case "fluid":
                return m.kind().filter(k -> k == Material.Kind.FLUID).isPresent();
            default:
        }
        Composite c = composites.get(need);
        if (c != null) {
            for (String n : c.all())
                if (!has(m, n, depth + 1))
                    return false;
            if (!c.any().isEmpty() && c.any().stream().noneMatch(n -> has(m, n, depth + 1)))
                return false;
            for (String n : c.none())
                if (has(m, n, depth + 1))
                    return false;
            return true;
        }
        for (Material.Trait t : Material.Trait.values())
            if (t.getSerializedName().equals(need))
                return m.traits().contains(t);
        warn(need);
        return false;
    }

    /** {@code direction} -1 for at most the band, +1 for at least, 0 for exactly. */
    private static boolean compare(Material m, String ladderName, String band, int direction) {
        Ladder l = ladders.get(ladderName);
        if (l == null) {
            warn(ladderName);
            return false;
        }
        int want = l.rank(band);
        if (want < 0) {
            warn(ladderName + ":" + band);
            return false;
        }
        Optional<Float> v = figure(m, l.figure());
        if (v.isEmpty())
            return false;
        int have = 0;
        while (have < l.bands().size() - 1 && v.get() > l.bands().get(have).max())
            have++;
        return direction < 0 ? have <= want : direction > 0 ? have >= want : have == want;
    }

    /** The figure a ladder reads, in its own file's units; empty where the material states none. */
    private static Optional<Float> figure(Material m, String name) {
        // Authored figures compared with authored thresholds: a classification, not physics, so the plain value is read.
        return switch (name) {
            case "hardness" -> Optional.of(m.hardness().raw());
            case "heat_conduction" -> m.thermal().conductivity().map(Unit::raw);
            case "strength" -> m.mechanical().yieldStrength().map(Unit::raw);
            case "toughness" -> m.mechanical().toughness().map(Unit::raw);
            case "weight" -> m.density().muPerMb() > 0 ? Optional.of(m.density().muPerMb()) : Optional.empty();
            case "heat_limit" -> m.thermal().serviceTemperature().map(Unit::raw);
            case "electric_conduction" -> m.ampacity();
            default -> {
                warn("figure " + name);
                yield Optional.empty();
            }
        };
    }

    private static void warn(String what) {
        synchronized (WARNED) {
            if (WARNED.add(what))
                Feedback.LOGGER.error("A role asks for the unknown need '{}'", what);
        }
    }

    /** Every need name a role may use: for a lang check and the tooltip's gloss. */
    public static List<String> composites() {
        load();
        return List.copyOf(composites.keySet());
    }

    /** The bands of ladder {@code name} as {@code name, max} pairs for a gloss, empty for no such ladder. */
    public static List<Map.Entry<String, Float>> bands(String name) {
        load();
        Ladder l = ladders.get(name);
        if (l == null)
            return List.of();
        List<Map.Entry<String, Float>> out = new ArrayList<>();
        for (Band b : l.bands())
            out.add(Map.entry(b.name(), b.max()));
        return out;
    }
}
