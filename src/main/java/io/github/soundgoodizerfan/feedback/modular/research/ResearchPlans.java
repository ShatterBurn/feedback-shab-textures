// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.soundgoodizerfan.feedback.modular.treatise.School;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** The research plans the server has loaded ({@code data/feedback/research}), by path: {@code weapons_2}, {@code attunement/bane}, {@code app/scope}. */
public final class ResearchPlans extends SimpleJsonResourceReloadListener {

    private static final ResearchPlans INSTANCE = new ResearchPlans();

    private Map<String, ResearchPlan> plans = Map.of();
    /** The traits each school lends a card as a joker ({@code data/feedback/research/school/<school>.json}: {@code traits}), by school id. */
    private Map<String, java.util.Set<String>> schoolTraits = Map.of();

    private ResearchPlans() {
        super(new GsonBuilder().create(), "research");
    }

    public static ResearchPlans get() {
        return INSTANCE;
    }

    /** The plan for volume {@code volume} of {@code school}, or null. */
    public ResearchPlan volume(School school, int volume) {
        return plans.get(school.id() + "_" + volume);
    }

    /**
     * The jokers a player holding the volumes {@code mask} ({@link io.github.soundgoodizerfan.feedback.modular.treatise.Treatises#mask}) brings to the
     * volume {@code volume} of {@code school}: every other held volume, bar the one researched and the one before it that it is built on.
     */
    public java.util.List<TreatiseHand.Joker> jokers(int mask, School school, int volume) {
        java.util.List<TreatiseHand.Joker> out = new java.util.ArrayList<>();
        for (School s : School.values())
            for (int v = 1; v <= s.volumes(); v++) {
                if ((mask & (1 << s.bit(v))) == 0 || s == school && (v == volume || v == volume - 1))
                    continue;
                java.util.Set<String> traits = schoolTraits.get(s.id());
                if (traits != null && !traits.isEmpty())
                    out.add(new TreatiseHand.Joker(s.id(), v, traits));
            }
        return out;
    }

    /** The plan the attunement {@code id} is researched by ({@code data/feedback/research/attunement/<id>.json}), or null. */
    public ResearchPlan attunement(String id) {
        return plans.get("attunement/" + id);
    }

    /** The plan the app {@code id} is researched by ({@code data/feedback/research/app/<id>.json}), or null. */
    public ResearchPlan app(String id) {
        return plans.get("app/" + id);
    }

    /** The apps' subjects for the client's recipe browser: school {@code app}, the app id and its subject in {@code types}. */
    public java.util.List<io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry> appSubjects() {
        java.util.List<io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry> out = new java.util.ArrayList<>();
        for (String app : io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.DISK_APPS) {
            ResearchPlan plan = app(app);
            if (plan != null && !plan.subject.isEmpty())
                out.add(new io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry("app", 0, java.util.List.of(app, plan.subject)));
        }
        return out;
    }


    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ResearchPlan> out = new HashMap<>();
        Map<String, java.util.Set<String>> schools = new HashMap<>();
        for (var e : files.entrySet()) {
            try {
                String key = e.getKey().getPath();
                if (key.startsWith("school/")) {
                    java.util.Set<String> traits = new java.util.LinkedHashSet<>();
                    for (JsonElement t : ((JsonObject) e.getValue()).getAsJsonArray("traits"))
                        traits.add(t.getAsString());
                    schools.put(key.substring("school/".length()), traits);
                    continue;
                }
                out.put(key, ResearchPlan.parse(key, (JsonObject) e.getValue()));
            } catch (RuntimeException ex) {
                io.github.soundgoodizerfan.feedback.Feedback.LOGGER.warn("Bad research plan {}: {}", e.getKey(), ex.toString());
            }
        }
        plans = Map.copyOf(out);
        schoolTraits = Map.copyOf(schools);
    }
}
