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

    /** The specimen types of every first volume's plan, for the client's recipe browser. */
    public java.util.List<io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry> specimens() {
        java.util.List<io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry> out = new java.util.ArrayList<>();
        for (var e : plans.entrySet()) {
            int cut = e.getKey().lastIndexOf('_');
            if (cut < 0 || e.getKey().contains("/") || e.getValue().specimens.isEmpty())
                continue;
            try {
                out.add(new io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload.Entry(e.getKey().substring(0, cut), Integer.parseInt(e.getKey().substring(cut + 1)), java.util.List.copyOf(e.getValue().specimens)));
            } catch (NumberFormatException ignored) {
                // not a volume plan ("attunement")
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ResearchPlan> out = new HashMap<>();
        for (var e : files.entrySet()) {
            try {
                String key = e.getKey().getPath();
                out.put(key, ResearchPlan.parse(key, (JsonObject) e.getValue()));
            } catch (RuntimeException ex) {
                io.github.soundgoodizerfan.feedback.Feedback.LOGGER.warn("Bad research plan {}: {}", e.getKey(), ex.toString());
            }
        }
        plans = Map.copyOf(out);
    }
}
