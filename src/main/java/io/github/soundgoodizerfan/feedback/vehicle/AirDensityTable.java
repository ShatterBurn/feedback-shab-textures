// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;

/**
 * Air density by dimension, relative to the overworld's (1.0). Loaded from
 * {@code data/<namespace>/air_density/<path>.json}, {@code <namespace>:<path>} being the dimension's
 * own id, holding {@code {"density": <number>, "oxygen": <share 0..1 or bool>}} or, for a curve, {@code {"points":
 * [{"y": <height>, "density": <number>}, ...], "oxygen": <share 0..1 or bool>}} (linear between points, flat beyond the
 * first and last). Density sets drag and lift; zero is vacuum. Oxygen (default 1) is the share of the air that is oxygen: above 0 an air-breathing thruster has something to burn with, and a person breathes density times share, so a
 * thin atmosphere with no oxygen (the End) still drags and lifts a little. An unlisted dimension is an
 * ordinary atmosphere (1.0, oxygen), so a modded dimension is a line of JSON. Server only: the physics step and the thruster are the readers.
 */
public class AirDensityTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "air_density";

    private record Point(float y, float density) {
        static final Codec<Point> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.fieldOf("y").forGetter(Point::y),
                Codec.floatRange(0f, 100f).fieldOf("density").forGetter(Point::density)).apply(i, Point::new));
    }

    /** A flat density is one point; a curve is two or more sorted by height. Read on the physics thread. */
    private record Entry(float[] ys, float[] densities, float oxygen) {
        static Entry flat(float density, float oxygen) {
            return new Entry(new float[] {0f}, new float[] {density}, oxygen);
        }

        float at(double y) {
            int n = ys.length;
            if (n == 1 || y <= ys[0])
                return densities[0];
            if (y >= ys[n - 1])
                return densities[n - 1];
            int i = 1;
            while (ys[i] < y)
                i++;
            float t = (float) ((y - ys[i - 1]) / (ys[i] - ys[i - 1]));
            return densities[i - 1] + (densities[i] - densities[i - 1]) * t;
        }
    }

    private record Raw(java.util.Optional<Float> density, List<Point> points, float oxygen) {
    }

    private static final Codec<Raw> RAW = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0f, 100f).optionalFieldOf("density").forGetter(Raw::density),
            Point.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(Raw::points),
            // A share 0..1 of the air that is oxygen; true and false still read as 1 and 0.
            Codec.either(Codec.BOOL, Codec.floatRange(0f, 1f)).xmap(
                    e -> e.map(b -> b ? 1f : 0f, f -> f), Either::right).optionalFieldOf("oxygen", 1f).forGetter(Raw::oxygen)).apply(i, Raw::new));
    private static final Entry ORDINARY = Entry.flat(1f, 1f);
    private static final AirDensityTable INSTANCE = new AirDensityTable();

    // Read on the physics thread, replaced on reload.
    private volatile Map<ResourceLocation, Entry> byDimension = Map.of();

    private AirDensityTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static AirDensityTable get() {
        return INSTANCE;
    }

    /** Density at sea level of the dimension's curve (the lowest point), for callers with no height. */
    public static float at(Level level) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY).densities()[0];
    }

    /** Density at a height, interpolated along the dimension's curve. */
    public static float at(Level level, double y) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY).at(y);
    }

    /** Whether the air here carries oxygen to burn. */
    public static boolean oxygen(Level level) {
        return INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY).oxygen() > 0f;
    }

    /** The oxygen a lung finds at a height: the density there times the share of the air that is oxygen. 1 is the Overworld at sea level. */
    public static float breathable(Level level, double y) {
        var e = INSTANCE.byDimension.getOrDefault(level.dimension().location(), ORDINARY);
        return e.at(y) * e.oxygen();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, Entry> loaded = new HashMap<>();
        json.forEach((id, element) -> RAW.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad air density {}: {}", id, error))
                .ifPresent(r -> {
                    if (r.points().isEmpty()) {
                        loaded.put(id, Entry.flat(r.density().orElse(1f), r.oxygen()));
                        return;
                    }
                    List<Point> sorted = new ArrayList<>(r.points());
                    sorted.sort(Comparator.comparing(Point::y));
                    float[] ys = new float[sorted.size()], ds = new float[sorted.size()];
                    for (int k = 0; k < ys.length; k++) {
                        ys[k] = sorted.get(k).y();
                        ds[k] = sorted.get(k).density();
                    }
                    loaded.put(id, new Entry(ys, ds, r.oxygen()));
                }));
        byDimension = Map.copyOf(loaded);
    }
}
