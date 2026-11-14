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

/** The research plans the server has loaded ({@code data/feedback/research}), by file name: {@code weapons_2}, {@code attunement}. */
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

    /** The plan every attunement is researched by, or null. */
    public ResearchPlan attunement() {
        return plans.get("attunement");
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
