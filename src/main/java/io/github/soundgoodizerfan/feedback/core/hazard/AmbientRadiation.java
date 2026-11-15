// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * Radiation from open sky, by dimension: {@code data/<namespace>/ambient_radiation/<path>.json}, {@code <namespace>:<path>} the
 * dimension's own id, holding {@code {"dose": <Ru>}}, what a person with nothing between them and the sky takes in. Nothing
 * listed means none. The atmosphere stops it, so only a place with no air lists one. Server only.
 */
public class AmbientRadiation extends SimpleJsonResourceReloadListener {
    private static final Codec<Float> DOSE = Codec.floatRange(0f, 1000f).fieldOf("dose").codec();
    private static final AmbientRadiation INSTANCE = new AmbientRadiation();

    private volatile Map<ResourceLocation, Float> byDimension = Map.of();

    private AmbientRadiation() {
        super(new GsonBuilder().create(), "ambient_radiation");
    }

    public static AmbientRadiation get() {
        return INSTANCE;
    }

    /** The open-sky dose rate in Ru of {@code level}'s dimension. */
    public static float at(Level level) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), 0f);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Float> loaded = new HashMap<>();
        json.forEach((id, element) -> DOSE.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad ambient radiation {}: {}", id, error))
                .ifPresent(d -> loaded.put(id, d)));
        byDimension = Map.copyOf(loaded);
    }
}
