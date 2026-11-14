// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.HashSet;
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
 * way has to link up to reach the new idea. {@code labels} names every concept, {@code relations} are the pairs that really belong together
 * (the rest only look as if they might), {@code known} the few the researcher already sees, {@code start} the concept the researcher owns and
 * {@code goal} the one to be reached. {@code replay} is what the finished chain shows, {@code question} the one blank to fill after it, and
 * {@code eureka} the real thing whose counter earns extra ink when done beforehand ({@code kind}: used, crafted or mined; {@code item}; {@code threshold}; {@code ink}).
 * {@code specimens} lists the types a first volume takes (later volumes take the types gated by the volume before). The same text is
 * read on the server (to validate and to send) and on the client (to play), so it is parsed from the one string.
 */
public final class ResearchPlan {

    public record Eureka(String kind, String item, int threshold, int ink) {
    }

    public final String key, title, start, goal, question;
    public final Map<String, String> labels = new LinkedHashMap<>();
    private final Set<String> relations = new HashSet<>(), known = new HashSet<>();
    public final List<String> replay = new ArrayList<>(), options = new ArrayList<>(), specimens = new ArrayList<>();
    public final int answer;
    public final Eureka eureka;
    private final String json;

    private ResearchPlan(String key, String json, JsonObject o) {
        this.key = key;
        this.json = json;
        title = o.get("title").getAsString();
        start = o.get("start").getAsString();
        goal = o.get("goal").getAsString();
        question = o.get("question").getAsString();
        answer = o.get("answer").getAsInt();
        for (var e : o.getAsJsonObject("labels").entrySet())
            labels.put(e.getKey(), e.getValue().getAsString());
        pairs(o.getAsJsonArray("relations"), relations);
        pairs(o.getAsJsonArray("known"), known);
        strings(o.getAsJsonArray("replay"), replay);
        strings(o.getAsJsonArray("options"), options);
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

    private static void pairs(JsonArray a, Set<String> out) {
        for (JsonElement e : a) {
            JsonArray p = e.getAsJsonArray();
            out.add(pair(p.get(0).getAsString(), p.get(1).getAsString()));
        }
    }

    private static String pair(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Whether the two concepts really belong together. */
    public boolean related(String a, String b) {
        return relations.contains(pair(a, b));
    }

    /** Whether the researcher can already see that the two belong together. */
    public boolean knows(String a, String b) {
        return known.contains(pair(a, b));
    }

    /** The plan as the string it was parsed from. */
    public String json() {
        return json;
    }
}
