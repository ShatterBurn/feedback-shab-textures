// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/**
 * A data skin as its JSON describes it ({@code assets/feedback/gui_skin/<name>.json}, format in {@code docs/gui/gui_skins.md}): the
 * seven colours, the GUI sprites for the panel, slot and button states, sounds by id, the reveal kind, decor layers and an optional
 * named code effect. The panel is a tiled or nine-slice sprite, optionally dimmed by a {@code wash} colour so ink reads on it and
 * edged by a {@code frame} sprite (nine-slice, transparent centre). Immutable; a reload replaces it. Sprites are vanilla GUI sprites ({@code textures/gui/sprites/<path>.png}, with
 * a {@code .mcmeta} for nine-slice borders or tiling), so the sprite atlas does the slicing.
 */
record SkinDef(int ink, int dim, int accent, int warn, int well, int trim, int plate,
               ResourceLocation panel, boolean shadow, int wash, ResourceLocation frame, ResourceLocation slot, Map<String, ResourceLocation> buttons,
               String click, String open, String close, Map<String, String> cues, String reveal, List<Decor> decor, List<Glow> glow, Glint glint, Tabs tabs, Feel feel, Idle idle, String effect) {

    /** One sprite laid over the panel: {@code anchor} names the panel point it hangs from, (x, y) is the offset, w by h its size. */
    record Decor(ResourceLocation sprite, String anchor, int x, int y, int w, int h, boolean heat, float cold, float breathe, float spin) {
    }

    /**
     * A folder tab's own art: {@code normal} (sits back) and {@code selected} are nine-slice sprites for the tab body (a small border so
     * the 14 px tab has room for its label), and {@code inset} is the selected tab's rail thickness, so the tile patch that erases the
     * panel's top border runs between the rails; each joint is a sprite where the tab's rail meets that border, anchored at the
     * tab's bottom-left (left joint) or bottom-right (right joint) corner on the panel's top edge, offset by (x, y).
     */
    record Tabs(int inset, ResourceLocation normal, ResourceLocation selected, Joint left, Joint right) {
    }

    record Joint(ResourceLocation sprite, int x, int y, int w, int h) {
    }

    /** Ambient motes rising from the panel's bottom edge while the thing opened runs (hot or turning): {@code rate} per tick, {@code pixels} square. */
    record Idle(int colour, float rate, int pixels) {
    }

    /** A soft diagonal band of light that crosses the panel in a quick pass once every {@code period} ticks and rests between (a pass takes a quarter of it). */
    record Glint(int colour, int alpha, int period, int width, float slope) {
    }

    /** Firelight along the panel's bottom edge: {@code height} px of colour fading up, {@code base} alpha (0 to 255) when the screen gives no heat, {@code hot} at full heat, {@code lip} a bright line on the edge itself. */
    record Glow(int colour, int height, int base, int hot, int lip) {
    }

    static SkinDef parse(JsonObject json) {
        JsonObject c = json.getAsJsonObject("colours");
        JsonObject buttons = obj(json, "button");
        Map<String, ResourceLocation> button = new HashMap<>();
        buttons.entrySet().forEach(e -> button.put(e.getKey(), ResourceLocation.parse(e.getValue().getAsString())));
        JsonObject sounds = obj(json, "sounds");
        Map<String, String> cues = new HashMap<>();
        obj(sounds, "cues").entrySet().forEach(e -> cues.put(e.getKey(), e.getValue().getAsString()));
        List<Decor> decor = new ArrayList<>();
        JsonArray layers = json.has("decor") ? json.getAsJsonArray("decor") : new JsonArray();
        for (JsonElement e : layers) {
            JsonObject d = e.getAsJsonObject();
            decor.add(new Decor(ResourceLocation.parse(d.get("sprite").getAsString()), str(d, "anchor", "top_left"),
                    num(d, "x", 0), num(d, "y", 0), d.get("w").getAsInt(), d.get("h").getAsInt(),
                    d.has("heat") && d.get("heat").getAsBoolean(), d.has("cold") ? d.get("cold").getAsFloat() : 0f, d.has("breathe") ? d.get("breathe").getAsFloat() : 0f, d.has("spin") ? d.get("spin").getAsFloat() : 0f));
        }
        List<Glow> glow = new ArrayList<>();
        if (json.has("glow"))
            for (JsonElement e : json.getAsJsonArray("glow")) {
                JsonObject o = e.getAsJsonObject();
                glow.add(new Glow(colour(o, "colour"), num(o, "height", 28), num(o, "base", 0), num(o, "hot", 160), num(o, "lip", 0)));
            }
        Tabs tabs = null;
        if (json.has("tabs")) {
            JsonObject o = json.getAsJsonObject("tabs");
            tabs = new Tabs(num(o, "inset", 3), o.has("normal") ? ResourceLocation.parse(o.get("normal").getAsString()) : null,
                    o.has("selected") ? ResourceLocation.parse(o.get("selected").getAsString()) : null, joint(o, "joint_left"), joint(o, "joint_right"));
        }
        Glint glint = null;
        if (json.has("glint")) {
            JsonObject o = json.getAsJsonObject("glint");
            glint = new Glint(colour(o, "colour"), num(o, "alpha", 20), num(o, "period", 100), num(o, "width", 14),
                    o.has("slope") ? o.get("slope").getAsFloat() : 0.7f);
        }
        Feel feel = null;
        Idle idle = null;
        if (json.has("feel")) {
            JsonObject o = json.getAsJsonObject("feel");
            feel = new Feel(o.has("weight") ? o.get("weight").getAsFloat() : 0f, o.has("slop") ? o.get("slop").getAsFloat() : 0f);
            if (o.has("idle")) {
                JsonObject i = o.getAsJsonObject("idle");
                idle = new Idle(colour(i, "colour"), i.has("rate") ? i.get("rate").getAsFloat() : 0.1f, num(i, "pixels", 1));
            }
        }
        JsonObject panel = json.has("panel") ? json.getAsJsonObject("panel") : new JsonObject();
        return new SkinDef(colour(c, "ink"), colour(c, "dim"), colour(c, "accent"), colour(c, "warn"), colour(c, "well"), colour(c, "trim"),
                colour(c, "plate"),
                panel.has("sprite") ? ResourceLocation.parse(panel.get("sprite").getAsString()) : null,
                !panel.has("shadow") || panel.get("shadow").getAsBoolean(),
                panel.has("wash") ? colour(panel, "wash") : 0,
                panel.has("frame") ? ResourceLocation.parse(panel.get("frame").getAsString()) : null,
                json.has("slot") ? ResourceLocation.parse(json.get("slot").getAsString()) : null, Map.copyOf(button),
                str(sounds, "click", null), str(sounds, "open", null), str(sounds, "close", null), Map.copyOf(cues),
                str(json, "reveal", "none"), List.copyOf(decor), List.copyOf(glow), glint, tabs, feel, idle, str(json, "effect", null));
    }

    private static Joint joint(JsonObject o, String key) {
        if (!o.has(key))
            return null;
        JsonObject j = o.getAsJsonObject(key);
        return new Joint(ResourceLocation.parse(j.get("sprite").getAsString()), num(j, "x", 0), num(j, "y", 0), j.get("w").getAsInt(), j.get("h").getAsInt());
    }

    private static JsonObject obj(JsonObject o, String key) {
        return o.has(key) ? o.getAsJsonObject(key) : new JsonObject();
    }

    private static String str(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    private static int num(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    /** {@code "#RRGGBB"} (opaque) or {@code "#AARRGGBB"}. */
    private static int colour(JsonObject o, String key) {
        String s = o.get(key).getAsString();
        if (s.startsWith("#"))
            s = s.substring(1);
        long v = Long.parseLong(s, 16);
        return s.length() <= 6 ? (int) (0xFF000000L | v) : (int) v;
    }
}
