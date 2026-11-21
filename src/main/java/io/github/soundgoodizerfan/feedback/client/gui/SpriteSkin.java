// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * A skin drawn from sprites instead of fills: everything it looks and sounds like comes from its {@link SkinDef} (the JSON under
 * {@code gui_skin/}), read live, so a reload shows an edit. Panel and slot are GUI sprites (nine-slice and tiling set in the sprite's
 * {@code .mcmeta}), each button state is a sprite, decor layers hang off the panel's edges, and a named {@link Effect} adds code on
 * top for what a sprite cannot do. A missing file or sprite draws a plain stand-in, never a crash. The sunken areas, plaques, gauges
 * and tabs the library builds from the seven colours work as on every skin.
 */
final class SpriteSkin extends GuiSkin {

    /** Code a data skin may name in its JSON, drawn over the panel's sprites (an aurora, a flicker, a heat glow). */
    interface Effect {
        void draw(SpriteSkin skin, GuiGraphics g, int x, int y, int w, int h);
    }

    private static final Map<String, Effect> EFFECTS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> HAVE = new HashMap<>();
    private static final List<SpriteSkin> ALL_SPRITE = new ArrayList<>();

    /** The live heat the thing opened is at, Tu, for decor layers marked {@code heat}: set by the screen, 0 for cold. */
    private float heatTu;

    private final String file;

    SpriteSkin(String name) {
        super(name.toUpperCase(java.util.Locale.ROOT), 0xFFFFFFFF, 0xFFA0A0A0, 0xFFFFC060, 0xFFFF5040, 0xFF202020, 0xFF606060, 0xFF404040);
        this.file = name;
        ALL_SPRITE.add(this);
        refresh();
    }

    static void register(String name, Effect effect) {
        EFFECTS.put(name, effect);
    }

    /** Called after every reload: sprite lookups and colours are taken again. */
    static void reloaded() {
        HAVE.clear();
        ALL_SPRITE.forEach(SpriteSkin::refresh);
    }

    private void refresh() {
        SkinDef d = def();
        if (d != null)
            recolour(d.ink(), d.dim(), d.accent(), d.warn(), d.well(), d.trim(), d.plate());
    }

    private SkinDef def() {
        return SkinDefs.get(file);
    }

    @Override
    public void heat(float tu) {
        heatTu = tu;
    }

    private static boolean have(ResourceLocation sprite) {
        if (sprite == null)
            return false;
        return HAVE.computeIfAbsent(sprite, s -> !Minecraft.getInstance().getGuiSprites().getSprite(s).contents().name()
                .equals(MissingTextureAtlasSprite.getLocation()));
    }

    @Override
    public void panel(GuiGraphics g, int x, int y, int w, int h) {
        SkinDef d = def();
        if (d == null || !have(d.panel())) {
            g.fill(x, y, x + w, y + h, 0xFF404040);
            g.renderOutline(x, y, w, h, 0xFF909090);
            return;
        }
        if (d.shadow())
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x50000000);
        g.blitSprite(d.panel(), x, y, w, h);
        if (d.wash() != 0)
            g.fill(x, y, x + w, y + h, d.wash());
        for (SkinDef.Glow glow : d.glow())
            glow(g, glow, x, y, w, h);
        if (have(d.frame()))
            g.blitSprite(d.frame(), x, y, w, h);
        for (SkinDef.Decor layer : d.decor())
            decor(g, layer, x, y, w, h);
        Effect effect = d.effect() == null ? null : EFFECTS.get(d.effect());
        if (effect != null)
            effect.draw(this, g, x, y, w, h);
    }

    /** 0 to 1 how hot the opened thing is, by the orange band: the screen gives it through {@link #heat}. */
    private float heatFraction() {
        return Incandescence.light(heatTu) <= 0 ? 0f : Mth.clamp(heatTu / 1100f, 0f, 1f);
    }

    /** Firelight rising from the bottom edge: a steady glow with no heat, brighter and flickering as the chamber heats. */
    private void glow(GuiGraphics g, SkinDef.Glow glow, int x, int y, int w, int h) {
        float f = heatFraction();
        float pulse = f > 0f ? HeatGlow.pulse() : 1f;
        int rgb = glow.colour() & 0xFFFFFF;
        int top = Math.max(y + 1, y + h - 1 - glow.height());
        int a = Mth.clamp(Math.round((glow.base() + (glow.hot() - glow.base()) * f) * pulse), 0, 255);
        g.fillGradient(x + 1, top, x + w - 1, y + h - 1, rgb, a << 24 | rgb);
        if (glow.lip() > 0)
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, Mth.clamp(Math.round(glow.lip() * (0.6f + 0.4f * f) * pulse), 0, 255) << 24 | rgb);
    }

    private void decor(GuiGraphics g, SkinDef.Decor layer, int x, int y, int w, int h) {
        if (!have(layer.sprite()))
            return;
        float alpha = 1f;
        if (layer.heat()) { // visible at its cold level (0 for a layer that only shows when glowing), full at the orange band
            alpha = layer.cold() + (1f - layer.cold()) * heatFraction() * HeatGlow.pulse();
            if (alpha <= 0.01f)
                return;
            alpha = Mth.clamp(alpha, 0f, 1f);
        }
        String a = layer.anchor();
        int ax = a.contains("right") ? x + w - layer.w() : a.equals("top") || a.equals("center") || a.equals("bottom") ? x + (w - layer.w()) / 2 : x;
        int ay = a.contains("bottom") ? y + h - layer.h() : a.equals("left") || a.equals("right") || a.equals("center") ? y + (h - layer.h()) / 2 : y;
        if (alpha < 1f) {
            RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, alpha);
        }
        g.blitSprite(layer.sprite(), ax + layer.x(), ay + layer.y(), layer.w(), layer.h());
        if (alpha < 1f)
            g.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void slot(GuiGraphics g, int x, int y) {
        SkinDef d = def();
        if (d != null && have(d.slot()))
            g.blitSprite(d.slot(), x - 1, y - 1, 18, 18);
        else {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0x60000000);
            g.renderOutline(x - 1, y - 1, 18, 18, 0xFF909090);
        }
    }

    @Override
    void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active, boolean primary) {
        SkinDef d = def();
        ResourceLocation base = d == null ? null : d.buttons().getOrDefault(primary && d.buttons().containsKey("primary") ? "primary" : "normal",
                d.buttons().get("normal"));
        ResourceLocation state = null;
        if (d != null)
            state = !active ? d.buttons().get("off") : down ? d.buttons().get("down") : null;
        if (state != null && have(state))
            g.blitSprite(state, x, y, w, h);
        else if (base != null && have(base)) {
            g.blitSprite(base, x, y, w, h);
            ResourceLocation hover = d.buttons().get(primary && d.buttons().containsKey("primary_hover") ? "primary_hover" : "hover");
            if (active && glow > 0f && hover != null && have(hover)) {
                RenderSystem.enableBlend();
                g.setColor(1f, 1f, 1f, glow);
                g.blitSprite(hover, x, y, w, h);
                g.setColor(1f, 1f, 1f, 1f);
            }
        } else {
            g.fill(x, y, x + w, y + h, active ? 0xFF505050 : 0xFF303030);
            g.renderOutline(x, y, w, h, 0xFF909090);
        }
        var font = Minecraft.getInstance().font;
        int o = down ? 1 : 0;
        int text = !active ? dim() : lerp(primary ? accent() : dim(), ink(), glow);
        g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
    }

    private static SoundEvent sound(String id) {
        return id == null ? null : BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(id));
    }

    @Override
    public SoundEvent click() {
        SkinDef d = def();
        SoundEvent s = d == null ? null : sound(d.click());
        return s != null ? s : SoundEvents.UI_BUTTON_CLICK.value();
    }

    @Override
    public SoundEvent open() {
        SkinDef d = def();
        return d == null ? null : sound(d.open());
    }

    @Override
    public SoundEvent close() {
        SkinDef d = def();
        return d == null ? null : sound(d.close());
    }

    @Override
    public SoundEvent sound(Cue cue) {
        SkinDef d = def();
        SoundEvent s = d == null ? null : sound(d.cues().get(cue.name().toLowerCase(java.util.Locale.ROOT)));
        return s != null ? s : click();
    }

    @Override
    public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
        SkinDef d = def();
        if (d == null || t >= 1)
            return;
        switch (d.reveal()) {
            case "slide" -> g.enableScissor(x - 4, y - 4, x + Math.round(w * t), y + h + 8);
            case "shutters" -> {
                int gap = Math.round((h / 2f + 6) * t);
                g.enableScissor(x - 4, y + h / 2 - gap, x + w + 8, y + h / 2 + gap);
            }
            default -> { }
        }
    }

    @Override
    public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
        SkinDef d = def();
        if (d == null || t >= 1 || !(d.reveal().equals("slide") || d.reveal().equals("shutters")))
            return;
        g.flush();
        g.disableScissor();
        if (d.reveal().equals("shutters")) { // two halves of the panel sliding back into the frame
            int mid = y + h / 2, gap = Math.round((h / 2f + 6) * t);
            int top = mid - gap - y, bottom = y + h - (mid + gap);
            if (top > 0) {
                panel(g, x, y, w, top);
                g.fill(x + 1, y + top - 2, x + w - 1, y + top, trim());
            }
            if (bottom > 0) {
                panel(g, x, mid + gap, w, bottom);
                g.fill(x + 1, mid + gap, x + w - 1, mid + gap + 2, trim());
            }
        }
    }
}
