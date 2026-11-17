// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * One piece of research, authored as data ({@code data/feedback/research/<school>_<volume>.json}): the concepts a person who only knows the old
 * way has to link up to reach the new idea. {@code concepts} maps each concept to its label and its two or three {@code traits}; two concepts
 * link when they share a trait, which the screen shows, so a link is a fact the player can check and not the author's opinion. {@code start} is
 * the concept the researcher owns and {@code goal} the one to be reached. {@code replay} is what the finished chain shows, {@code question} the one blank to fill after it, and
 * {@code eureka} the real thing whose counter earns extra ink when done beforehand ({@code kind}: used, crafted or mined; {@code item}; {@code threshold}; {@code ink}).
 * {@code specimens} lists the types a first volume takes (later volumes take the types gated by the volume before). The same text is
 * read on the server (to validate and to send) and on the client (to play), so it is parsed from the one string.
 */
public final class ResearchPlan {

    public record Eureka(String kind, String item, int threshold, int ink) {
    }

    public final String key, title, start, goal, question;
    public final Map<String, String> labels = new LinkedHashMap<>();
    /** Each concept's traits, in the order authored. */
    public final Map<String, Set<String>> traits = new LinkedHashMap<>();
    public final List<String> replay = new ArrayList<>(), options = new ArrayList<>(), specimens = new ArrayList<>();
    public final int answer;
    public final Eureka eureka;
    /** What an app's research is about, an item id or a {@code #tag} (the thing the app is the use of); empty for every other plan. */
    public final String subject;
    private final String json;

    private ResearchPlan(String key, String json, JsonObject o) {
        this.key = key;
        this.json = json;
        title = o.get("title").getAsString();
        start = o.get("start").getAsString();
        goal = o.get("goal").getAsString();
        question = o.get("question").getAsString();
        answer = o.get("answer").getAsInt();
        for (var e : o.getAsJsonObject("concepts").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            labels.put(e.getKey(), c.get("label").getAsString());
            Set<String> ts = new LinkedHashSet<>();
            for (JsonElement t : c.getAsJsonArray("traits"))
                ts.add(t.getAsString());
            traits.put(e.getKey(), ts);
        }
        strings(o.getAsJsonArray("replay"), replay);
        strings(o.getAsJsonArray("options"), options);
        subject = o.has("subject") ? o.get("subject").getAsString() : "";
        if (o.has("specimens"))
            strings(o.getAsJsonArray("specimens"), specimens);
        if (o.has("eureka")) {
            JsonObject e = o.getAsJsonObject("eureka");
            eureka = new Eureka(e.get("kind").getAsString(), e.get("item").getAsString(), e.get("threshold").getAsInt(), e.get("ink").getAsInt());
        } else
            eureka = null;
    }

    public static ResearchPlan parse(String key, String json) {
        return new ResearchPlan(key, json, JsonParser.parseString(json).getAsJsonObject());
    }

    public static ResearchPlan parse(String key, JsonObject o) {
        return new ResearchPlan(key, o.toString(), o);
    }

    private static void strings(JsonArray a, List<String> out) {
        for (JsonElement e : a)
            out.add(e.getAsString());
    }

    /** The traits the two concepts share, in {@code a}'s order; empty when they do not link. */
    public List<String> shared(String a, String b) {
        List<String> out = new ArrayList<>(3);
        Set<String> other = traits.get(b);
        if (other != null)
            for (String t : traits.getOrDefault(a, Set.of()))
                if (other.contains(t))
                    out.add(t);
        return out;
    }

    /** Whether the two concepts link: they share a trait. */
    public boolean related(String a, String b) {
        return !shared(a, b).isEmpty();
    }

    /** The plan as the string it was parsed from. */
    public String json() {
        return json;
    }
}
