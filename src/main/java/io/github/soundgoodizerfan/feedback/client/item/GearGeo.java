// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Worn armour's extra pieces as Blockbench geo files: {@code assets/feedback/models/armor/<extra>.geo.json} (Bedrock geometry; one
 * bone named for the vanilla part it rides, cubes in Bedrock's biped space) and the table {@code models/armor/gear.json} (which
 * extras each slot's plate variant wears, which of them are studs, and the fit rules). Read at every resource reload (F3+T), baked
 * into a vanilla humanoid mesh per shell; a file that fails to parse is logged and left out.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearGeo {

    /** The vanilla part's pivot in Bedrock's biped space (x, y up from the floor, z). */
    private static final Map<String, float[]> PIVOTS = Map.of(
            "head", new float[]{0, 24, 0}, "body", new float[]{0, 24, 0},
            "right_arm", new float[]{-5, 22, 0}, "left_arm", new float[]{5, 22, 0},
            "right_leg", new float[]{-1.9f, 12, 0}, "left_leg", new float[]{1.9f, 12, 0});

    /** One cube in the vanilla part's own pixel space (y down), its texture offset, and the shift on the thin leggings shell. */
    record Cube(float x, float y, float z, float w, float h, float d, int u, int v, float[] inner) {
        float[] box(boolean innerShell) {
            float[] s = innerShell ? inner : new float[3];
            return new float[]{x + s[0], y + s[1], z + s[2], w, h, d};
        }
    }

    /** An extra: the vanilla part it rides and its cubes. */
    record Extra(String name, String part, List<Cube> cubes) {
    }

    /** A fit rule: when {@code slot}'s plate axis is (or is not) in {@code band}, add and remove extras. */
    record Rule(String slot, String axis, String band, boolean not, List<String> add, List<String> remove) {
    }

    private static Map<String, Extra> extras = Map.of();
    private static Map<String, Map<String, List<String>>> slots = Map.of();
    private static Map<String, List<String>> studs = Map.of();
    private static List<Rule> rules = List.of();
    private static ModelPart outerRoot, innerRoot;
    private static int generation;

    private GearGeo() {
    }

    /** Bumps at every reload, so a cached model knows to rebuild. */
    static int generation() {
        return generation;
    }

    static Map<String, Extra> extras() {
        return extras;
    }

    static List<Rule> rules() {
        return rules;
    }

    /** The extras a slot's plate variant wears ("default" when the variant has no line of its own). */
    static List<String> wears(String slot, String variant) {
        var table = slots.get(slot);
        if (table == null)
            return List.of();
        var list = table.get(variant);
        return list != null ? list : table.getOrDefault("default", List.of());
    }

    /** The extras of a slot drawn in the stud material's colour. */
    static List<String> studs(String slot) {
        return studs.getOrDefault(slot, List.of());
    }

    /** The baked humanoid mesh with the extras on it (vanilla's cubes at the armour layer's deformation), rebuilt each reload. */
    static ModelPart root(boolean inner) {
        return inner ? innerRoot : outerRoot;
    }

    @SubscribeEvent
    public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) GearGeo::load);
    }

    private static void load(ResourceManager rm) {
        Map<String, Extra> found = new LinkedHashMap<>();
        rm.listResources("models/armor", id -> id.getPath().endsWith(".geo.json")).forEach((id, resource) -> {
            if (!id.getNamespace().equals(Feedback.MOD_ID))
                return;
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".geo.json".length());
            try (Reader reader = resource.openAsReader()) {
                found.put(name, parse(name, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                Feedback.LOGGER.error("Armour model {} did not load, leaving it out", id, e);
            }
        });
        extras = found;
        try (Reader reader = rm.getResourceOrThrow(Feedback.id("models/armor/gear.json")).openAsReader()) {
            table(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            Feedback.LOGGER.error("Armour table models/armor/gear.json did not load", e);
            slots = Map.of();
            studs = Map.of();
            rules = List.of();
        }
        outerRoot = layer(false).bakeRoot();
        innerRoot = layer(true).bakeRoot();
        generation++;
    }

    private static Extra parse(String name, JsonObject file) {
        JsonObject geometry = file.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonObject bone = geometry.getAsJsonArray("bones").get(0).getAsJsonObject();
        String part = bone.get("name").getAsString();
        float[] pivot = PIVOTS.get(part);
        if (pivot == null)
            throw new IllegalArgumentException("bone " + part + " is not a vanilla part");
        List<Cube> cubes = new ArrayList<>();
        for (JsonElement el : bone.getAsJsonArray("cubes")) {
            JsonObject c = el.getAsJsonObject();
            JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
            float ox = o.get(0).getAsFloat(), oy = o.get(1).getAsFloat(), oz = o.get(2).getAsFloat();
            float w = s.get(0).getAsFloat(), h = s.get(1).getAsFloat(), d = s.get(2).getAsFloat();
            float[] shift = new float[3];
            if (c.has("inner_offset")) {
                JsonArray i = c.getAsJsonArray("inner_offset");
                for (int k = 0; k < 3; k++)
                    shift[k] = i.get(k).getAsFloat();
            }
            // Bedrock's y runs up from the floor, the vanilla part's runs down from its pivot.
            cubes.add(new Cube(ox - pivot[0], pivot[1] - oy - h, oz - pivot[2], w, h, d, uv.get(0).getAsInt(), uv.get(1).getAsInt(), shift));
        }
        return new Extra(name, part, cubes);
    }

    private static void table(JsonObject json) {
        Map<String, Map<String, List<String>>> s = new HashMap<>();
        for (var slot : json.getAsJsonObject("slots").entrySet()) {
            Map<String, List<String>> variants = new HashMap<>();
            for (var v : slot.getValue().getAsJsonObject().entrySet())
                variants.put(v.getKey(), names(v.getValue()));
            s.put(slot.getKey(), variants);
        }
        slots = s;
        Map<String, List<String>> st = new HashMap<>();
        for (var e : json.getAsJsonObject("studs").entrySet())
            st.put(e.getKey(), names(e.getValue()));
        studs = st;
        List<Rule> r = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("fit")) {
            JsonObject o = el.getAsJsonObject();
            boolean not = o.has("band_not");
            r.add(new Rule(o.get("slot").getAsString(), o.get("axis").getAsString(),
                    o.get(not ? "band_not" : "band").getAsString(), not,
                    o.has("add") ? names(o.get("add")) : List.of(), o.has("remove") ? names(o.get("remove")) : List.of()));
        }
        rules = r;
    }

    private static List<String> names(JsonElement array) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : array.getAsJsonArray())
            out.add(e.getAsString());
        return List.copyOf(out);
    }

    private static LayerDefinition layer(boolean inner) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(inner ? 0.5f : 1.0f), 0f);
        PartDefinition root = mesh.getRoot();
        for (Extra e : extras.values()) {
            CubeListBuilder cubes = CubeListBuilder.create();
            for (Cube c : e.cubes()) {
                float[] b = c.box(inner);
                cubes.texOffs(c.u(), c.v()).addBox(b[0], b[1], b[2], b[3], b[4], b[5]);
            }
            root.getChild(e.part()).addOrReplaceChild(e.name(), cubes, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
