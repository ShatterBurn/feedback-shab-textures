// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.soundgoodizerfan.feedback.client.ClientSettings;
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
    /** How fast the opened thing turns, RPM: set by the screen, 0 at rest; decor with `spin` turns at its factor times this. */
    private float spinRpm;
    private final java.util.IdentityHashMap<SkinDef.Decor, Spinner> spinners = new java.util.IdentityHashMap<>();

    private final String file;

    private final Specks motes = new Specks(24);
    private final net.minecraft.util.RandomSource moteRng = net.minecraft.util.RandomSource.create();
    private float moteClock = Float.NaN;

    @Override
    public Feel feel() {
        SkinDef d = def();
        return d != null && d.feel() != null ? d.feel() : super.feel();
    }

    /** The skin's idle driver: motes rise from the bottom edge while the thing is hot or turning, and none when it is still. */
    private void idle(GuiGraphics g, SkinDef.Idle idle, int x, int y, int w, int h) {
        float now = GuiStyle.time();
        float dt = Float.isNaN(moteClock) ? 0f : Mth.clamp(now - moteClock, 0f, 4f);
        boolean running = heatFraction() > 0f || spinRpm != 0f;
        int ticks = (int) dt;
        for (int i = 0; i < ticks; i++) { // advance whole ticks only, so the motes move at game speed
            if (running && moteRng.nextFloat() < idle.rate())
                motes.spawn(x + 6 + moteRng.nextFloat() * (w - 12), y + h - 4, (moteRng.nextFloat() - 0.5f) * 0.4f, -0.5f - moteRng.nextFloat() * 0.4f,
                        idle.colour(), 30 + moteRng.nextInt(30), idle.pixels());
            motes.tick(0f);
        }
        moteClock = Float.isNaN(moteClock) ? now : moteClock + ticks;
        motes.draw(g, 0f);
    }

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
        spinners.clear();
        SkinDef d = def();
        if (d != null)
            recolour(d.ink(), d.dim(), d.accent(), d.warn(), d.well(), d.trim(), d.plate());
    }

    private SkinDef def() {
        return SkinDefs.get(file);
    }

    @Override
    public void spin(float rpm) {
        spinRpm = rpm;
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
    public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        SkinDef d = def();
        if (d == null || !have(d.panel())) {
            g.fill(x, y, x + w, y + h, 0xFF404040);
            g.renderOutline(x, y, w, h, 0xFF909090);
            return;
        }
        boolean tab = inTab; // a folder tab borrows the panel: it takes the face and frame, not the ornaments that belong to a whole window
        if (d.shadow() && !tab)
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x50000000);
        g.blitSprite(d.panel(), x, y, w, h);
        if (d.wash() != 0)
            g.fill(x, y, x + w, y + h, d.wash());
        if (tab) {
            if (have(d.frame()))
                g.blitSprite(d.frame(), x, y, w, h);
            return;
        }
        for (SkinDef.Glow glow : d.glow())
            glow(g, glow, x, y, w, h);
        if (d.glint() != null && !ClientSettings.reduceMotion())
            glint(g, d.glint(), x, y, w, h);
        if (have(d.frame()))
            g.blitSprite(d.frame(), x, y, w, h);
        for (SkinDef.Decor layer : d.decor())
            decor(g, layer, x, y, w, h);
        if (d.idle() != null && !ClientSettings.reduceMotion())
            idle(g, d.idle(), x, y, w, h);
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

    /**
     * The selected tab stands on the panel: its own face (frame and tile, clipped to the tab) above, and over the panel's top border
     * between the tab's rails a patch of the panel's tile and wash, aligned to the panel's own tiling so the pattern runs through
     * unbroken, with the skin's two joint sprites where the rails meet the border. Unselected tabs are the base class's.
     */
    @Override
    void folderTabFace(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow, int[] host) {
        SkinDef d = def();
        SkinDef.Tabs tabs = d == null ? null : d.tabs();
        if (tabs == null || !have(tabs.normal()) || !have(tabs.selected())) {
            super.folderTabFace(g, x, y, w, h, selected, glow, host);
            return;
        }
        if (!selected) { // sits back two pixels, its bottom hidden behind the panel's border
            g.enableScissor(x, y, x + w, y + h);
            g.blitSprite(tabs.normal(), x, y + 2, w, h + 12);
            if (glow > 0)
                g.fill(x, y, x + w, y + h, (int) (0x30 * glow) << 24 | accent() & 0xFFFFFF);
            g.disableScissor();
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        g.blitSprite(tabs.selected(), x, y, w, h + 14);
        g.disableScissor();
        if (host != null && have(d.panel())) {
            // The patch: the panel's tile, unshifted, from under the tab's top rim down over the border band, between the tab's rails.
            int px = x + tabs.inset(), py = y + tabs.inset(), pw = w - 2 * tabs.inset(), ph = host[1] + 12 - py; // the whole face inside the rim, so the tile runs through the tab and the panel as one
            if (pw > 0) {
                var sprite = Minecraft.getInstance().getGuiSprites().getSprite(d.panel()).contents();
                int tw = Math.max(1, sprite.width()), th = Math.max(1, sprite.height());
                int ox = host[0] + Math.floorDiv(px - host[0], tw) * tw, oy = host[1] + Math.floorDiv(py - host[1], th) * th;
                g.enableScissor(px, py, px + pw, py + ph);
                g.blitSprite(d.panel(), ox, oy, (px + pw - ox + tw - 1) / tw * tw, (py + ph - oy + th - 1) / th * th);
                if (d.wash() != 0)
                    g.fill(px, py, px + pw, py + ph, d.wash());
                g.disableScissor();
            }
            drawJoint(g, tabs.left(), x, host[1]);
            drawJoint(g, tabs.right(), x + w, host[1]);
        }
    }

    private void drawJoint(GuiGraphics g, SkinDef.Joint joint, int ax, int ay) {
        if (joint != null && have(joint.sprite()))
            g.blitSprite(joint.sprite(), ax + joint.x(), ay + joint.y(), joint.w(), joint.h());
    }

    /** A band of light crossing the glass in a quick pass, then nothing until the next period. */
    private static void glint(GuiGraphics g, SkinDef.Glint glint, int x, int y, int w, int h) {
        float t = (GuiStyle.time() % glint.period()) / (glint.period() * 0.24f);
        if (t > 1f)
            return;
        int centre = (int) (-40 + t * (w + 80));
        int rgb = glint.colour() & 0xFFFFFF;
        for (int j = 2; j < h - 2; j += 2) {
            int a = x + centre + (int) (j * glint.slope()), b = a + glint.width();
            int l = Math.max(x + 1, a), r = Math.min(x + w - 1, b);
            if (r > l)
                g.fill(l, y + j, r, y + Math.min(j + 2, h - 1), glint.alpha() << 24 | rgb);
        }
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
        if (layer.breathe() > 0f) { // a lamp that breathes: steady at 0.8 under reduce motion
            float breath = ClientSettings.reduceMotion() ? 0.8f : 0.5f + 0.5f * Mth.sin(GuiStyle.time() * layer.breathe());
            alpha *= 0.3f + 0.7f * breath;
        }
        String a = layer.anchor();
        int ax = a.contains("right") ? x + w - layer.w() : a.equals("top") || a.equals("center") || a.equals("bottom") ? x + (w - layer.w()) / 2 : x;
        int ay = a.contains("bottom") ? y + h - layer.h() : a.equals("left") || a.equals("right") || a.equals("center") ? y + (h - layer.h()) / 2 : y;
        if (alpha < 1f) {
            RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, alpha);
        }
        if (layer.spin() != 0f) { // turns about its own centre at the screen's speed
            spinners.computeIfAbsent(layer, k -> new Spinner()).draw(g, layer.sprite(), ax + layer.x() + layer.w() / 2, ay + layer.y() + layer.h() / 2,
                    layer.w(), layer.h(), spinRpm * layer.spin());
            if (alpha < 1f)
                g.setColor(1f, 1f, 1f, 1f);
            return;
        }
        g.blitSprite(layer.sprite(), ax + layer.x(), ay + layer.y(), layer.w(), layer.h());
        if (alpha < 1f)
            g.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void inset(GuiGraphics g, int x, int y, int w, int h) {
        SkinDef d = def();
        if (d != null && have(d.inset()))
            g.blitSprite(d.inset(), x, y, w, h);
        else
            super.inset(g, x, y, w, h);
    }

    @Override
    public void plaque(GuiGraphics g, int x, int y, int w, int h) {
        SkinDef d = def();
        if (d != null && have(d.plaque()))
            g.blitSprite(d.plaque(), x, y, w, h);
        else
            super.plaque(g, x, y, w, h);
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
            case "plates" -> {
                int gap = Math.round((w / 2f + 6) * t);
                g.enableScissor(x + w / 2 - gap - 1, y - 4, x + w / 2 + gap + 1, y + h + 8);
            }
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
        if (d == null || t >= 1 || !(d.reveal().equals("slide") || d.reveal().equals("shutters") || d.reveal().equals("plates")))
            return;
        g.flush();
        g.disableScissor();
        if (d.reveal().equals("plates")) { // two plates drawing back from the middle into their housings
            int mid = x + w / 2, gap = Math.round((w / 2f + 6) * t);
            int left = mid - gap - x, right = x + w - (mid + gap);
            if (left > 0) {
                panel(g, x, y, left, h);
                g.fill(x + left - 2, y, x + left, y + h, accent());
            }
            if (right > 0) {
                panel(g, mid + gap, y, right, h);
                g.fill(mid + gap, y, mid + gap + 2, y + h, accent());
            }
        }
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
