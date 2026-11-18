// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads every {@link OreVein} from {@code data/<namespace>/ore_vein/*.json} (mechanics §9.2).
 * Server-only: worldgen runs on the server; the resolved table is copied to clients for the recipe browser.
 *
 * <p>A file that sets no {@code weight} is drawn by the abundance of the material its ore belongs to. Material and vein
 * tables reload in no fixed order, so the figure is worked out on first use after either changed, not at load.
 */
public class OreVeinTable extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "ore_vein";

    /** Weight per tenfold of abundance (ppm); with a deposit's {@code share} it is the whole conversion. Invented. */
    public static final float WEIGHT_PER_DECADE = 6f;

    private static final OreVeinTable INSTANCE = new OreVeinTable();

    private Map<ResourceLocation, OreVein> raw = Map.of();
    private volatile Resolved resolved;

    /** The deposits with their draw weights, and the loaded-materials generation they were worked out against. */
    private record Resolved(int generation, List<OreVein> entries, Map<ResourceLocation, OreVein> byId) {}

    private OreVeinTable() {
        super(new GsonBuilder().create(), DIRECTORY);
    }

    public static OreVeinTable get() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager resources, ProfilerFiller profiler) {
        Map<ResourceLocation, OreVein> loaded = new TreeMap<>();
        json.forEach((id, element) -> OreVein.CODEC.parse(JsonOps.INSTANCE, element)
                .resultOrPartial(error -> Feedback.LOGGER.error("Bad ore vein {}: {}", id, error))
                .ifPresent(vein -> loaded.put(id, vein)));
        raw = loaded;
        resolved = null;
        Feedback.LOGGER.info("Loaded {} ore veins", loaded.size());
    }

    /** Every deposit, each carrying the weight it is drawn by; a deposit with no weight and no material abundance is left out. */
    public List<OreVein> entries() {
        return resolve().entries();
    }

    /** The deposit a file id names, or null. */
    public OreVein byId(ResourceLocation id) {
        return resolve().byId().get(id);
    }

    private Resolved resolve() {
        Resolved r = resolved;
        int generation = MaterialTable.generation();
        if (r != null && r.generation() == generation)
            return r;
        synchronized (this) {
            r = resolved;
            if (r != null && r.generation() == generation)
                return r;
            Map<ResourceLocation, Float> abundanceByOre = new HashMap<>();
            for (Material m : MaterialTable.get().materials().values())
                if (m.abundance().isPresent()) {
                    ResourceLocation ore = m.forms().get("ore");
                    if (ore != null)
                        abundanceByOre.put(ore, m.abundance().get());
                }
            Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
            List<OreVein> list = new ArrayList<>();
            Map<ResourceLocation, OreVein> byId = new HashMap<>();
            raw.forEach((id, vein) -> {
                int weight = vein.weight();
                if (weight <= 0) {
                    Float ppm = vein.material().map(m -> materials.get(m)).filter(java.util.Objects::nonNull)
                            .flatMap(Material::abundance).orElse(abundanceByOre.get(vein.ore()));
                    if (ppm == null) {
                        Feedback.LOGGER.error("Ore vein {} sets no weight and its material has no abundance (ore {})", id, vein.ore());
                        return;
                    }
                    weight = Math.max(1, Math.round(WEIGHT_PER_DECADE * (float) Math.log10(1.0 + ppm) * vein.share()));
                }
                OreVein v = vein.resolved(id, weight);
                list.add(v);
                byId.put(id, v);
            });
            r = new Resolved(generation, List.copyOf(list), Map.copyOf(byId));
            resolved = r;
            return r;
        }
    }
}
