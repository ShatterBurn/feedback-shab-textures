// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads every {@link OreDimension} from {@code data/<namespace>/ore_dimension/*.json}, keyed by dimension. Server-only. */
public class OreDimensionTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "ore_dimension";

    private static final OreDimensionTable INSTANCE = new OreDimensionTable();

    private Map<ResourceLocation, OreDimension> byDimension = Map.of();

    private OreDimensionTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static OreDimensionTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, OreDimension> loaded = new HashMap<>();
        json.forEach((id, element) -> OreDimension.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad ore dimension {}: {}", id, error))
                .ifPresent(d -> loaded.put(d.dimension(), d)));
        byDimension = Map.copyOf(loaded);
        Feedback.LOGGER.info("Loaded {} ore dimensions", byDimension.size());
    }

    /** The dimension's entry, or null when it holds no veins. */
    public OreDimension get(ResourceLocation dimension) {
        return byDimension.get(dimension);
    }
}
