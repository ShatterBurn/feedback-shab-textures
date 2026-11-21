// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * What a screen is made of. Each skin is a material, and it looks and sounds like one: the era of
 * the block picks it (a forge-era vessel is iron, a drafting stand is paper). Everything is drawn
 * with fills, so there is nothing to license and every screen on a skin moves when this file does.
 */
public abstract class GuiSkin {

    private static final java.util.List<GuiSkin> ALL = new java.util.ArrayList<>();


    /**
     * Cyanotype on a drafting stand: white ink on blue paper, a grid, a ruled double border. Buttons
     * are drawn boxes that firm up under the pointer; the primary one is a red rubber stamp. The
     * sheet unrolls from the left when the screen opens and rolls back up when it closes.
     */
    public static final GuiSkin BLUEPRINT = new GuiSkin("BLUEPRINT", 0xFFE8F1FF, 0xFF8FB3DA, 0xFFFFE070, 0xFFFF7A6E, 0x38000000, 0x90E8F1FF, 0x28FFFFFF) {
        private static final int PAPER = 0xFF1B4C86, STAMP = 0xFFE85A4F;
        /** Fibres in the paper, fixed so the sheet does not crawl. Fractions of the sheet, x then y. */
        private static final float[] SPECKLE = speckle();

        private static float[] speckle() {
            Random r = new Random(0x5EED);
            float[] out = new float[2 * 180];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        @Override
        public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x50000000); // the sheet's shadow on the stand
            g.fill(x, y, x + w, y + h, PAPER);
            g.fillGradient(x, y, x + w, y + 48, 0x16FFFFFF, 0x00FFFFFF);
            g.fillGradient(x, y + h - 48, x + w, y + h, 0x00000000, 0x2A000000);
            for (int i = 0; i < 4; i++) { // the edges darken where the paper curls
                int a = (0x1C - i * 6) << 24;
                g.fill(x + i * 2, y, x + i * 2 + 2, y + h, a);
                g.fill(x + w - i * 2 - 2, y, x + w - i * 2, y + h, a);
            }
            for (int gx = 6; gx < w; gx += 6)
                g.fill(x + gx, y, x + gx + 1, y + h, gx % 24 == 0 ? 0x1CFFFFFF : 0x0CFFFFFF);
            for (int gy = 6; gy < h; gy += 6)
                g.fill(x, y + gy, x + w, y + gy + 1, gy % 24 == 0 ? 0x1CFFFFFF : 0x0CFFFFFF);
            for (int i = 0; i < SPECKLE.length; i += 2) {
                int sx = x + (int) (SPECKLE[i] * w), sy = y + (int) (SPECKLE[i + 1] * h);
                g.fill(sx, sy, sx + 1, sy + 1, (i & 2) == 0 ? 0x14FFFFFF : 0x18000000);
            }
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, 0xB0E8F1FF);
            g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0x40E8F1FF);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0x30000000);
            g.renderOutline(x - 1, y - 1, 18, 18, 0x90E8F1FF);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int ink = !active ? 0x60E8F1FF : primary ? lerp(0xC0000000 | (STAMP & 0xFFFFFF), 0xFFFF8A80, glow)
                    : lerp(dim(), ink(), glow);
            int o = down ? 1 : 0;
            if (down)
                g.fill(x, y, x + w, y + h, primary ? 0x30E85A4F : 0x30FFFFFF);
            else if (glow > 0)
                g.fill(x, y, x + w, y + h, ((int) (glow * 0x1C) << 24) | (primary ? STAMP & 0xFFFFFF : 0xFFFFFF));
            g.renderOutline(x, y, w, h, ink);
            if (primary)
                g.renderOutline(x + 2, y + 2, w - 4, h - 4, ink);
            // Drafting ticks run out past the corners as the pointer arrives.
            int t = Math.round(glow * 2);
            if (t > 0) {
                g.fill(x - 1 - t, y, x, y + 1, ink);
                g.fill(x + w, y + h - 1, x + w + 1 + t, y + h, ink);
                g.fill(x, y - 1 - t, x + 1, y, ink);
                g.fill(x + w - 1, y + h, x + w, y + h + 1 + t, ink);
            }
            var font = Minecraft.getInstance().font;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, ink, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
        }

        @Override
        public SoundEvent open() {
            return FSounds.UI_UNROLL.get();
        }

        @Override
        public SoundEvent close() {
            return FSounds.UI_ROLL.get();
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t < 1)
                g.enableScissor(x - 4, y - 4, x + Math.round(w * t), y + h + 8);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            if (t >= 1)
                return;
            g.flush();
            g.disableScissor();
            // The rest of the sheet, still rolled, riding the edge.
            int rx = x + Math.round(w * t);
            g.fill(rx - 1, y - 3, rx + 7, y + h + 3, 0xFF15406F);
            g.fill(rx, y - 2, rx + 6, y + h + 2, 0xFF2A64A8);
            g.fill(rx + 1, y - 2, rx + 3, y + h + 2, 0xFF6A9CD8);
            g.fill(rx + 5, y - 2, rx + 6, y + h + 2, 0xFF143A66);
        }
    };

    /**
     * The forge's: blackened iron lit only by its fire. Soot-dark at the top, a coal glow rising from the bottom edge,
     * dull scale on the plate, edges that catch the firelight. Buttons are cold iron that warm under the pointer; the
     * primary one is metal heated to glowing. The screen opens like a hatch, two iron shutters parting from the middle.
     */
    public static final GuiSkin FORGE = new SpriteSkin("forge");

    /**
     * Atomic-age laboratory enamel: a dusty sage hammered-enamel cabinet face, never white, stencilled in dark ink. Cut corners, a cream
     * and rust double stripe across the top between a starburst and a cream pilot lamp that breathes, chrome rivets at the foot, a
     * printed scale ruler along the bottom edge. Buttons are putty push-plates; the primary one is rust enamel. The calm light sibling
     * of {@link #CONSOLE}. Fills and a fixed-seed dimple table, so nothing crawls but the lamp.
     */
    public static final GuiSkin ENAMEL = new SpriteSkin("enamel");

    /**
     * The same atomic-age laboratory after dark: a charcoal-teal enamel console with brass corner brackets and rails, a row of
     * indicator lamps along the foot (one breathing, the rest steady), and a slow glint of light drifting across the glass. Buttons are
     * brass-bezelled; the primary one is a lit amber lamp. Cream ink. The machine-room sibling of {@link #ENAMEL}.
     */
    public static final GuiSkin CONSOLE = new SpriteSkin("console");

    /**
     * The first hearth: cobblestone, dimmed and flattened under a smoke-grey wash so it reads as
     * old masonry and not as a block face. No trim, no hardware; buttons are cut slabs. It opens
     * and closes with no ceremony. Vanilla's cobblestone is drawn in place, not copied.
     */
    public static final GuiSkin STONE = new SpriteSkin("stone");

    /**
     * The enchanting table's: deep indigo going to violet, a faint noise of glyphs, trimmed in gold leaf with
     * a diamond at each corner. Buttons are violet with a gold rim that glows under the pointer; the primary one
     * is gold leaf. Opens with the table's own sound and has no ceremony beyond it.
     */
    public static final GuiSkin MYSTIC = new GuiSkin("MYSTIC", 0xFFE6DCFF, 0xFF9C8CC4, 0xFFF2CC5A, 0xFFFF6A8A, 0xFF0C0818, 0x90C9A44A, 0xFF2A1A52) {
        private static final int DEEP = 0xFF150C30, VIOLET = 0xFF2E1A66, GOLD = 0xFFC9A44A, GOLD_HI = 0xFFF6DD8A;
        /** Glyph fragments, fixed so nothing crawls: x, y as fractions of the panel, then a shape 0..2 (bar, stroke, tick). */
        private static final float[] GLYPHS = glyphs();

        private static float[] glyphs() {
            Random r = new Random(0x5E1D);
            float[] out = new float[3 * 150];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        /** Animation clock: game time plus partial tick, so it holds still when the game is paused; wall clock with no world. */
        private static float seconds() {
            var mc = Minecraft.getInstance();
            if (mc.level == null)
                return net.minecraft.Util.getMillis() / 1000f;
            return (mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false)) / 20f;
        }

        private static void diamond(GuiGraphics g, int cx, int cy) {
            g.fill(cx - 1, cy - 3, cx + 2, cy + 4, GOLD);
            g.fill(cx - 3, cy - 1, cx + 4, cy + 2, GOLD);
            g.fill(cx, cy - 1, cx + 1, cy + 2, GOLD_HI);
        }

        @Override
        public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x60000000);
            g.fillGradient(x, y, x + w, y + h, VIOLET, DEEP);
            float t = seconds();
            // Aurora: three soft violet bands drifting vertically, each two gradients (fade in, fade out), clipped to the panel.
            for (int k = 0; k < 3; k++) {
                float c = y + h * (0.5f + 0.4f * Mth.sin(t * (0.12f + 0.05f * k) + k * 2.1f));
                int half = 14 + 4 * k, top = Math.max(y + 1, (int) c - half), mid = Mth.clamp((int) c, y + 1, y + h - 1),
                        bot = Math.min(y + h - 1, (int) c + half);
                int col = k == 1 ? 0x6A3CC8 : 0x8A4CE0, a = (int) (0x14 + 0x0C * Mth.sin(t * 0.3f + k)) << 24;
                if (mid > top)
                    g.fillGradient(x + 1, top, x + w - 1, mid, col, a | col);
                if (bot > mid)
                    g.fillGradient(x + 1, mid, x + w - 1, bot, a | col, col);
            }
            // Glyphs drift upward slowly and twinkle; alpha fades to nothing at the wrap so none pop.
            int span = Math.max(1, h - 16);
            for (int i = 0; i < GLYPHS.length; i += 3) {
                float pos = (GLYPHS[i + 1] * span + t * 1.5f) % span;
                int gx = x + 8 + (int) (GLYPHS[i] * (w - 16)), gy = y + 8 + (int) pos;
                float env = Mth.sin(pos / span * Mth.PI) * (0.55f + 0.45f * Mth.sin(t * 0.9f + GLYPHS[i + 2] * 40f));
                int shape = (int) (GLYPHS[i + 2] * 3), c = (int) (0x1C * env) << 24 | 0xF2CC5A;
                if (shape == 0)
                    g.fill(gx, gy, gx + 4, gy + 1, c);
                else if (shape == 1) {
                    g.fill(gx, gy, gx + 1, gy + 4, c);
                    g.fill(gx, gy + 3, gx + 3, gy + 4, c);
                } else
                    g.fill(gx, gy, gx + 2, gy + 2, c);
            }
            // Rare sparkles: three slots, each a 7 s cycle with a short fade; spot is hashed from the cycle number.
            for (int k = 0; k < 3; k++) {
                float u = t / 7f + k / 3f;
                int cyc = (int) u;
                float ph = u - cyc;
                if (ph > 0.12f)
                    continue;
                float f = Mth.sin(ph / 0.12f * Mth.PI);
                int hsh = (cyc * 73856093) ^ (k * 19349663);
                int sx = x + 8 + Math.floorMod(hsh, Math.max(1, w - 16)), sy = y + 8 + Math.floorMod(hsh >> 9, span);
                int c = (int) (0x90 * f) << 24 | 0xFFF0B0, c2 = (int) (0x40 * f) << 24 | 0xFFF0B0;
                g.fill(sx, sy, sx + 1, sy + 1, c);
                g.fill(sx - 2, sy, sx + 3, sy + 1, c2);
                g.fill(sx, sy - 2, sx + 1, sy + 3, c2);
            }
            g.fillGradient(x, y, x + w, y + 30, 0x18FFFFFF, 0x00FFFFFF);
            g.renderOutline(x, y, w, h, 0xFF08041A);
            g.renderOutline(x + 3, y + 3, w - 6, h - 6, GOLD);
            g.renderOutline(x + 5, y + 5, w - 10, h - 10, 0x50C9A44A);
            diamond(g, x + 4, y + 4);
            diamond(g, x + w - 5, y + 4);
            diamond(g, x + 4, y + h - 5);
            diamond(g, x + w - 5, y + h - 5);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0C0818);
            g.renderOutline(x - 1, y - 1, 18, 18, 0xA0C9A44A);
            g.fill(x, y, x + 16, y + 1, 0x30000000);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int top, bottom, text, rim;
            if (!active) {
                top = 0xFF2A2240;
                bottom = 0xFF1C1630;
                text = 0xFF6E6490;
                rim = 0xFF4A4060;
            } else if (primary) {
                top = lerp(0xFFD8B050, 0xFFF6DD8A, glow);
                bottom = lerp(0xFF8E6E26, 0xFFB8903A, glow);
                text = 0xFF1E1230;
                rim = GOLD_HI;
            } else {
                top = lerp(0xFF4A2E94, 0xFF6440B8, glow);
                bottom = lerp(0xFF2A1A5C, 0xFF3A2482, glow);
                text = 0xFFEDE4FF;
                rim = lerp(GOLD, GOLD_HI, glow);
            }
            if (down) {
                int t = top;
                top = bottom;
                bottom = t;
            }
            if (glow > 0 && active) // the soft glow
                g.renderOutline(x - 1, y - 1, w + 2, h + 2, (int) (glow * 0x60) << 24 | 0xF2CC5A);
            g.fillGradient(x, y, x + w, y + h, top, bottom);
            g.renderOutline(x, y, w, h, rim);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.AMETHYST_BLOCK_CHIME;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.ENCHANTMENT_TABLE_USE;
        }
    };

    /**
     * The look of vanilla's own inventory: light grey with a white bevel up-left and a dark one down-right, notched
     * black corners, sunken grey slots, plain grey buttons. For what sits over the world rather than in a block's
     * screen (the HUD), so it reads as the game's own and not a machine's. It opens with no ceremony.
     */
    public static final GuiSkin VANILLA = new GuiSkin("VANILLA", 0xFF404040, 0xFF707070, 0xFF2E7D32, 0xFFB02020, 0xFF8B8B8B, 0xFF373737, 0xFFC6C6C6) {
        private static final int BODY = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADE = 0xFF555555, BLACK = 0xFF000000;

        @Override
        public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 2, y, x + w - 2, y + h, BLACK); // the notched silhouette
            g.fill(x, y + 2, x + w, y + h - 2, BLACK);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BLACK);
            g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BODY);
            g.fill(x + 2, y + 2, x + w - 3, y + 4, LIGHT);
            g.fill(x + 2, y + 2, x + 4, y + h - 3, LIGHT);
            g.fill(x + 3, y + h - 4, x + w - 2, y + h - 2, SHADE);
            g.fill(x + w - 4, y + 3, x + w - 2, y + h - 2, SHADE);
            g.fill(x + w - 3, y + 2, x + w - 2, y + 3, BODY);
            g.fill(x + 2, y + h - 3, x + 3, y + h - 2, BODY);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            g.fill(x, y, x + 18, y + 18, LIGHT);
            g.fill(x, y, x + 17, y + 17, 0xFF8B8B8B);
            g.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
            g.fill(x - 1, y - 1, x, y + 16, 0xFF373737);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int face = !active ? 0xFF8B8B8B : lerp(0xFF6C6C6C, 0xFF7E7E9C, glow);
            g.fill(x, y, x + w, y + h, BLACK);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
            if (active && !down) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFA8A8A8);
                g.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFA8A8A8);
                g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF3C3C3C);
                g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF3C3C3C);
            }
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            int text = !active ? 0xFFA0A0A0 : primary ? 0xFFFFFFA0 : 0xFFFFFFFF;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, true);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_BUTTON_CLICK.value();
        }
    };

    /**
     * The kitchen's: vanilla's bricks drawn in place under a warm wash (as STONE does cobblestone), a calcite-cream
     * trim round the panel and its sunken areas, brick buttons with the primary one a cream calcite slab. Opens and closes with calcite's
     * own sounds and no ceremony. Sunken areas stay dark so the cream text on them reads; the sockets are calcite.
     */
    public static final GuiSkin BRICK = new SpriteSkin("brick");

    /**
     * The woodworking table's: vanilla's oak planks drawn in place under a dark wash, a pale-grain trim round the panel,
     * sockets cut into the board, buttons of darker board with the primary one a bright cut face. Wood's own sounds, no ceremony.
     */
    public static final GuiSkin WOOD = new SpriteSkin("wood");

    /**
     * The anvil's: vanilla's anvil texture drawn in place under a cold wash, a polished-steel trim round the panel (the anvil's
     * worked face), hammer-dimple sockets, buttons of dark cast iron with the primary one bright struck steel. Anvil sounds only.
     */
    public static final GuiSkin ANVIL = new SpriteSkin("anvil");

    /**
     * The smoker's: smoke-cured hickory, dark and warm. Broad boards with seams and grain, soot gathering at the top, a low
     * ember glow at the bottom edge. Buttons are dark wood that warms under the pointer; the primary one is a live coal.
     */
    public static final GuiSkin SMOKE = new SpriteSkin("smoke");

    /**
     * The crude blast furnace's: heavy gunmetal plate in a plain riveted casing, and the cracks of cooling slag glowing at
     * the bottom edge. Buttons are cast iron; the primary one is molten orange.
     */
    public static final GuiSkin SLAG = new SpriteSkin("slag");


    /** The phosphor terminal in green: see {@link Terminal}. */
    public static final GuiSkin TERMINAL = new Terminal("TERMINAL", 0xFF5CFF7A, 0xFF2E9E48, 0xFFC4FFCE, 0xFFFFB02E, 0xFF020804, 0xFF1F6B33, 0xFF07140B);
    /** The terminal in amber phosphor. */
    public static final GuiSkin TERMINAL_AMBER = new Terminal("TERMINAL_AMBER", 0xFFFFB347, 0xFF9E6A1F, 0xFFFFE2A8, 0xFFFF5A3C, 0xFF080502, 0xFF6B4210, 0xFF140D05);
    /** The terminal in white phosphor. */
    public static final GuiSkin TERMINAL_WHITE = new Terminal("TERMINAL_WHITE", 0xFFE6EAE6, 0xFF8C928C, 0xFFFFFFFF, 0xFFFFB02E, 0xFF050605, 0xFF4F544F, 0xFF101210);
    /** The terminal in blue phosphor. */
    public static final GuiSkin TERMINAL_BLUE = new Terminal("TERMINAL_BLUE", 0xFF5CC8FF, 0xFF2E7A9E, 0xFFC4EBFF, 0xFFFFB02E, 0xFF020608, 0xFF1F4F6B, 0xFF07101A);

    /**
     * The powered suit's own material: matte carbon weave under armour plate, cyan hairlines where it is lit, amber where it warns. Corners
     * are chamfered and braced, the top edge carries a row of latches, and a slot is a dark socket with cyan corner brackets. It opens as two
     * armour plates draw apart from the middle with a servo's push, and closes the same way in reverse. Built from fills and a fixed-seed
     * speckle, so nothing crawls.
     */
    public static final GuiSkin SUIT = new SpriteSkin("suit");

    /**
     * A sleek console menu, for the combat skill chart: no frame, only dark glass. Panels are a translucent dark gradient with a hairline
     * edge; buttons are round-ended pills, a faint white fill with a thin ring that brightens on hover, the primary one solid white with
     * dark text. {@link #backdrop} lays a soft shaft of light from the top left and slow drifting specks of light over the view, as a
     * console's home screen does. Cool white ink, a pale blue accent. Fixed-seed specks, nothing but fills.
     */
    public static final GuiSkin GLASS = new GuiSkin("GLASS", 0xFFF2F3F6, 0xFF9496A2, 0xFFA9CBFF, 0xFFFF7A6B, 0xFF08090C, 0xFF3A3D48, 0xFF15171C) {
        private static final float[] SPECK = specks();

        private static float[] specks() {
            Random r = new Random(0x5E17);
            float[] out = new float[5 * 70];
            for (int i = 0; i < out.length; i++)
                out[i] = r.nextFloat();
            return out;
        }

        /** A filled disc by scanlines. */
        private static void disc(GuiGraphics g, int cx, int cy, int r, int argb) {
            for (int dy = -r; dy <= r; dy++) {
                int half = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
                g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, argb);
            }
        }

        /** A pill: ends rounded by half its height. */
        private static void pill(GuiGraphics g, int x, int y, int w, int h, int argb) {
            int r = h / 2;
            g.fill(x + r, y, x + w - r, y + h, argb);
            for (int dy = 0; dy < h; dy++) {
                float c = dy + 0.5f - h / 2f;
                int inset = r - (int) Math.round(Math.sqrt(Math.max(0f, r * r - c * c)));
                g.fill(x + inset, y + dy, x + r, y + dy + 1, argb);
                g.fill(x + w - r, y + dy, x + w - inset, y + dy + 1, argb);
            }
        }

        /** A pill's one-pixel ring. */
        private static void ring(GuiGraphics g, int x, int y, int w, int h, int argb) {
            int r = h / 2;
            g.fill(x + r, y, x + w - r, y + 1, argb);
            g.fill(x + r, y + h - 1, x + w - r, y + h, argb);
            for (int dy = 0; dy < h; dy++) {
                float c = dy + 0.5f - h / 2f;
                int inset = r - (int) Math.round(Math.sqrt(Math.max(0f, r * r - c * c)));
                g.fill(x + inset, y + dy, x + inset + 1, y + dy + 1, argb);
                g.fill(x + w - inset - 1, y + dy, x + w - inset, y + dy + 1, argb);
            }
        }

        @Override
        public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            g.fillGradient(x, y, x + w, y + h, 0xC4101216, 0xB0161920);
            g.fill(x, y, x + w, y + 1, 0x26FFFFFF);
            g.fill(x, y + h - 1, x + w, y + h, 0x26FFFFFF);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            g.fill(x - 1, y - 1, x + 17, y + 17, 0x66000000);
            ring(g, x - 1, y - 1, 18, 18, 0x50FFFFFF);
        }

        @Override
        public void backdrop(GuiGraphics g, int x, int y, int w, int h, long ms) {
            // a shaft of light from the top left, fading down and across
            int bands = 28;
            for (int i = 0; i < bands; i++) {
                float k = 1f - i / (float) bands;
                int top = y + i * h / bands, bot = y + (i + 1) * h / bands;
                int reach = (int) (w * (0.12f + 0.38f * k));
                int a = (int) (26 * k * k);
                g.fillGradient(x, top, x + reach, bot, a << 24 | 0xFFEBD8, 0);
            }
            // drifting specks, slow and upward, some larger and softer
            for (int i = 0; i < SPECK.length; i += 5) {
                float speed = 0.004f + 0.012f * SPECK[i + 3];
                float fy = ((SPECK[i + 1] - ms * speed / 1000f) % 1f + 1f) % 1f;
                int sx = x + (int) (SPECK[i] * w + 6 * Math.sin(ms / 2600.0 + SPECK[i + 4] * 6.28)), sy = y + (int) (fy * h);
                int r = SPECK[i + 2] > 0.9f ? 4 : SPECK[i + 2] > 0.6f ? 2 : 1;
                float twinkle = 0.55f + 0.45f * (float) Math.sin(ms / 900.0 + SPECK[i + 4] * 12);
                int a = (int) ((r > 1 ? 24 : 70) * twinkle);
                int c = SPECK[i + 4] > 0.5f ? 0xFFD9A0 : 0xFFFFFF;
                disc(g, sx, sy, r, a << 24 | c);
            }
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            int fill, line, text;
            if (!active) {
                fill = 0x14FFFFFF;
                line = 0x26FFFFFF;
                text = 0xFF5A5C66;
            } else if (primary) {
                fill = lerpA(0xE0, 0xFF, glow) << 24 | 0xF2F3F6;
                line = 0xFFFFFFFF;
                text = 0xFF14161B;
            } else {
                fill = lerpA(0x22, 0x46, glow) << 24 | 0xFFFFFF;
                line = lerpA(0x70, 0xFF, glow) << 24 | 0xFFFFFF;
                text = lerp(dim(), ink(), glow);
            }
            if (down)
                fill = lerp(fill, 0xFF000000, 0.2f);
            pill(g, x, y, w, h, fill);
            ring(g, x, y, w, h, line);
            var font = Minecraft.getInstance().font;
            int o = down ? 1 : 0;
            g.drawString(font, label, x + (w - font.width(label)) / 2 + o, y + (h - 8) / 2 + o, text, false);
        }

        private static int lerpA(int a, int b, float t) {
            return Math.round(a + (b - a) * t);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.UI_BUTTON_CLICK.value();
        }

        @Override
        public SoundEvent open() {
            return null;
        }

        @Override
        public SoundEvent close() {
            return null;
        }
    };

    /** The Kiln's fired clay, a data skin: everything it is lives in {@code gui_skin/kiln.json} and its sprites. */
    public static final GuiSkin KILN = new SpriteSkin("kiln");
    /** The Annealing Furnace's dull cast iron, a data skin. */
    public static final GuiSkin ANNEALING_FURNACE = new SpriteSkin("annealing_furnace");
    /** The Improved Furnace's stone with brass fittings and its thermowell, a data skin. */
    public static final GuiSkin IMPROVED_FURNACE = new SpriteSkin("improved_furnace");

    /**
     * A phosphor terminal: black glass with a faint glow, scanlines, one slow bright band rolling down it, light-on-black text. Buttons
     * are outlined boxes that invert under the pointer; the primary one is double-ruled. It opens as a CRT does, a bright line at the
     * middle that grows to the full screen. The Computer's desktop, in one of four phosphors (the Computer's settings pick which): one
     * class, the palette is its only difference; the drawing is {@link TerminalLook}. {@link #tinted} makes any other phosphor.
     */
    private static final class Terminal extends GuiSkin {

        Terminal(String name, int ink, int dim, int accent, int warn, int well, int trim, int plate) {
            super(name, ink, dim, accent, warn, well, trim, plate);
        }

        @Override
        public void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
            TerminalLook.panel(this, g, x, y, w, h);
        }

        @Override
        public void slot(GuiGraphics g, int x, int y) {
            TerminalLook.slot(this, g, x, y);
        }

        @Override
        void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down, boolean active,
                    boolean primary) {
            TerminalLook.button(this, g, x, y, w, h, label, glow, down, active, primary);
        }

        @Override
        public SoundEvent click() {
            return SoundEvents.COMPARATOR_CLICK;
        }

        @Override
        public SoundEvent open() {
            return SoundEvents.BEACON_ACTIVATE;
        }

        @Override
        public SoundEvent close() {
            return SoundEvents.BEACON_DEACTIVATE;
        }

        @Override
        public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.beginReveal(this, g, x, y, w, h, t);
        }

        @Override
        public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
            TerminalLook.endReveal(this, g, x, y, w, h, t);
        }
    }

    /**
     * The terminal in any phosphor colour {@code ink} (0xRRGGBB): every other colour is the ink scaled toward black, so a new tint
     * needs one number. Not registered in {@link #values()}.
     */
    public static GuiSkin tinted(int ink) {
        int rgb = ink & 0xFFFFFF;
        GuiSkin skin = new Terminal("TERMINAL_" + Integer.toHexString(rgb), 0xFF000000 | rgb, TerminalLook.shade(rgb, 0.6f), lerp(0xFF000000 | rgb, 0xFFFFFFFF, 0.6f),
                0xFFFFB02E, TerminalLook.shade(rgb, 0.02f), TerminalLook.shade(rgb, 0.4f), TerminalLook.shade(rgb, 0.08f));
        ALL.remove(skin); // the constructor lists every skin; a tint is made on demand
        return skin;
    }

    /** Tell the skin how fast the opened thing turns (RPM) so a data skin's spinning decor can turn with it; code-drawn skins ignore it. */
    public void spin(float rpm) {
    }

    /** Tell the skin how hot the opened thing is (Tu) so a data skin's heat decor can glow; code-drawn skins ignore it. */
    public void heat(float tu) {
    }

    /** Light laid over a view before its contents: nothing unless a skin has some. */
    public void backdrop(GuiGraphics g, int x, int y, int w, int h, long ms) {
    }

    private final String name;
    private int ink, dim, accent, warn, well, trim, plate;

    /** Every skin, in declaration order. */
    public static GuiSkin[] values() {
        return ALL.toArray(new GuiSkin[0]);
    }

    public static GuiSkin valueOf(String name) {
        for (GuiSkin skin : ALL)
            if (skin.name.equals(name))
                return skin;
        throw new IllegalArgumentException("no GUI skin " + name);
    }

    public final String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    /** {@code well}: the floor of a sunken area; {@code trim}: its rim; {@code plate}: a raised label plate. */
    protected GuiSkin(String name, int ink, int dim, int accent, int warn, int well, int trim, int plate) {
        this.name = name;
        ALL.add(this);
        this.ink = ink;
        this.dim = dim;
        this.accent = accent;
        this.warn = warn;
        this.well = well;
        this.trim = trim;
        this.plate = plate;
    }

    /** Sets every colour again, for a data skin whose JSON has just reloaded. */
    final void recolour(int ink, int dim, int accent, int warn, int well, int trim, int plate) {
        this.ink = ink;
        this.dim = dim;
        this.accent = accent;
        this.warn = warn;
        this.well = well;
        this.trim = trim;
        this.plate = plate;
    }

    /** Body text and lines. */
    public int ink() {
        return ink;
    }

    /** Labels and secondary lines. */
    public int dim() {
        return dim;
    }

    /** The thing selected, a value just changed. */
    public int accent() {
        return accent;
    }

    /** The rim of a sunken area and a skin's trim line. */
    public int trim() {
        return trim;
    }

    /** A figure past its limit. */
    public int warn() {
        return warn;
    }


    /** A sunken area: gauges, text wells, readouts. Drawn from (x, y), {@code w} by {@code h} including the rim. */
    public void inset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, well);
        g.renderOutline(x, y, w, h, trim);
    }

    /** A raised plate for a stamped readout between two controls. */
    public void plaque(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, plate);
        g.renderOutline(x, y, w, h, lerp(trim, 0xFFFFFFFF, 0.2f));
    }

    /**
     * A horizontal gauge in a well: {@code fill} of the way along, a tick at {@code mark} (both 0..1
     * of the width; {@code mark} negative for none), and the part past the tick in {@link #warn()}.
     */
    public void bar(GuiGraphics g, int x, int y, int w, int h, float fill, float mark) {
        bar(g, x, y, w, h, fill, mark, ink);
    }

    /** {@link #bar(GuiGraphics, int, int, int, int, float, float)} with the part before the tick in {@code lead}, to tell stacked gauges apart. */
    public void bar(GuiGraphics g, int x, int y, int w, int h, float fill, float mark, int lead) {
        gauge(g, x, y, w, h, false, fill, mark, lead, 0);
    }

    /**
     * The one gauge every screen and HUD draws through ({@link Gauge} links it to a figure): a well {@code w} by
     * {@code h}, filled {@code fill} of the way along the long axis ({@code vertical} from the bottom, else from the
     * left) in {@code lead}, a tick at {@code mark} with the part past it in {@link #warn()} ({@code mark} negative for
     * none), and {@code ticks} graduations outside the well when above 1.
     */
    public void gauge(GuiGraphics g, int x, int y, int w, int h, boolean vertical, float fill, float mark, int lead, int ticks) {
        inset(g, x - 1, y - 1, w + 2, h + 2);
        int len = vertical ? h : w;
        int f = Math.round(Mth.clamp(fill, 0, 1) * len), m = mark < 0 ? len : Math.round(Mth.clamp(mark, 0, 1) * len);
        if (vertical) {
            g.fill(x, y + h - Math.min(f, m), x + w, y + h, lead);
            if (f > m)
                g.fill(x, y + h - f, x + w, y + h - m, warn);
            if (mark >= 0)
                g.fill(x - 2, y + h - m, x + w + 2, y + h - m + 1, accent);
            for (int i = 1; i < ticks; i++)
                g.fill(x + w + 2, y + h - i * h / ticks, x + w + (i % 3 == 0 ? 6 : 4), y + h - i * h / ticks + 1, trim);
        } else {
            g.fill(x, y, x + Math.min(f, m), y + h, lead);
            if (f > m)
                g.fill(x + m, y, x + f, y + h, warn);
            if (mark >= 0)
                g.fill(x + m, y - 2, x + m + 1, y + h + 2, accent);
            for (int i = 1; i < ticks; i++)
                g.fill(x + i * w / ticks, y + h + 2, x + i * w / ticks + 1, y + h + (i % 3 == 0 ? 6 : 4), trim);
        }
    }

    /** A 10x10 check box at (x, y), {@code glow} 0..1 for the pointer being over it. */
    void checkbox(GuiGraphics g, int x, int y, boolean on, float glow) {
        inset(g, x, y, 10, 10);
        if (glow > 0)
            g.fill(x + 1, y + 1, x + 9, y + 9, (int) (glow * 0x24) << 24 | 0xFFFFFF);
        if (on) {
            GuiStyle.line(g, x + 2, y + 5, x + 4, y + 7, accent);
            GuiStyle.line(g, x + 4, y + 7, x + 8, y + 2, accent);
            GuiStyle.line(g, x + 2, y + 4, x + 4, y + 6, accent);
            GuiStyle.line(g, x + 4, y + 6, x + 8, y + 1, accent);
        }
    }

    /** A slider's groove and thumb in a 12-high box: {@code frac} 0..1 along it, {@code glow} 0..1 for pointer or drag. */
    void slider(GuiGraphics g, int x, int y, int w, float frac, float glow, boolean active) {
        inset(g, x, y + 4, w, 4);
        int tx = x + Math.round((w - 6) * frac);
        if (frac > 0)
            g.fill(x + 1, y + 5, tx + 3, y + 7, active ? lerp(dim, accent, glow) : dim);
        plaque(g, tx, y, 6, 12);
        g.fill(tx + 2, y + 2, tx + 4, y + 10, active ? lerp(dim, accent, glow) : dim);
    }

    /**
     * One folder tab, standing on the top edge of a panel whose top is at {@code y + h}. It is the panel's own face (drawn by {@link #panel}
     * and clipped to the tab), so it matches on every skin. The selected tab is flush: its face runs over the panel's top border with no seam.
     * An unselected one sits back, two pixels lower and darker, and the panel's border runs under it. Draw it after the panel.
     */
    public void folderTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow) {
        folderTab(g, x, y, w, h, selected, glow, null);
    }

    /**
     * As above; {@code host} is the real panel's rectangle (x, y, w, h) when the caller knows it. The selected tab's flush patch is then the
     * host panel's own face, drawn at its true size and lifted so its border falls above the patch, so textured faces (crinkle, courses,
     * grain) match instead of being rescaled into the tab's small rectangle.
     */
    public void folderTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow, int[] host) {
        inTab = true;
        try {
            folderTabFace(g, x, y, w, h, selected, glow, host);
        } finally {
            inTab = false;
        }
    }

    /** True while a folder tab borrows {@link #panel}: a skin leaves out ornaments that belong to a whole window (stripes, lamps, brackets). */
    static boolean inTab;

    void folderTabFace(GuiGraphics g, int x, int y, int w, int h, boolean selected, float glow, int[] host) {
        if (selected) {
            // The tab itself, with its own frame and rails, standing above the panel.
            g.enableScissor(x, y, x + w, y + h);
            panel(g, x, y, w, h + 14);
            g.disableScissor();
            // Below it, only the plate's face (inset past the rails) runs over the panel's top border, so the tab and the panel are one piece.
            g.enableScissor(x + 6, y + h, x + w - 6, y + h + FLUSH);
            if (host != null)
                panel(g, host[0], host[1] - FLUSH, host[2], host[3]);
            else
                panel(g, x, y, w, h + 14);
            g.disableScissor();
            return;
        }
        g.enableScissor(x, y, x + w, y + h);
        panel(g, x, y + 2, w, h + 14);
        g.fill(x, y, x + w, y + h, 0x58000000);
        if (glow > 0)
            g.fill(x, y, x + w, y + h, (int) (0x30 * glow) << 24 | accent & 0xFFFFFF);
        g.disableScissor();
    }

    /** How many rows under a selected folder tab the panel's own top border is covered by the tab's face. */
    private static final int FLUSH = 9;

    /**
     * A small tab standing on a {@link #plaque} whose top edge is at {@code y + h}: the selected one is the plaque's own plate and runs one
     * pixel down over its rim, so tab and box are one piece; the others sit back in the well. Draw the plaque first. For tabs a few dozen
     * pixels wide inside a panel, where {@link #folderTab}'s whole-panel face is too heavy.
     */
    public void plaqueTab(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        int rim = lerp(trim, 0xFFFFFFFF, 0.2f);
        if (selected) {
            g.fill(x, y, x + w, y + h + 1, plate);
            g.fill(x, y, x + w, y + 1, rim);
            g.fill(x, y, x + 1, y + h + 1, rim);
            g.fill(x + w - 1, y, x + w, y + h + 1, rim);
            return;
        }
        g.fill(x, y + 2, x + w, y + h, well);
        g.renderOutline(x, y + 2, w, h - 2, trim);
        if (hovered)
            g.fill(x + 1, y + 3, x + w - 1, y + h - 1, 0x20FFFFFF);
    }

    /** One tab of a strip. The selected one is a plate with an underline; the rest sit in the well. */
    void tab(GuiGraphics g, int x, int y, int w, int h, Component label, boolean selected, boolean active, float glow) {
        if (selected) {
            plaque(g, x, y, w, h);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, accent);
        } else {
            g.fill(x, y, x + w, y + h, well);
            g.renderOutline(x, y, w, h, trim);
            if (glow > 0)
                g.fill(x + 1, y + 1, x + w - 1, y + h - 1, (int) (glow * 0x1C) << 24 | 0xFFFFFF);
        }
        var font = Minecraft.getInstance().font;
        int text = !active ? lerp(dim, 0xFF000000, 0.35f) : selected ? ink : lerp(dim, ink, glow);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, text, false);
    }

    /** A list row's highlight: the picked one in the accent, the one under the pointer lightly. */
    public void row(GuiGraphics g, int x, int y, int w, int h, boolean picked, boolean hovered) {
        if (picked)
            g.fill(x, y, x + w, y + h, 0x60 << 24 | accent & 0xFFFFFF);
        else if (hovered)
            g.fill(x, y, x + w, y + h, 0x20FFFFFF);
    }

    /** A scrollbar thumb in a track {@code h} tall: {@code total} of something, {@code visible} shown from {@code at}. Nothing when it all fits. */
    public void scrollbar(GuiGraphics g, int x, int y, int h, int total, int visible, int at) {
        if (total <= visible)
            return;
        g.fill(x, y, x + 2, y + h, 0x30000000);
        int bar = Math.max(8, h * visible / total);
        int by = y + (h - bar) * at / Math.max(1, total - visible);
        g.fill(x, by, x + 2, by + bar, dim);
    }

    /**
     * The window itself, raised. Draws the skin's {@link #drawPanel}, then anything a screen queued in {@link #afterPanel} (the folder
     * tabs' bodies), so the tabs sit on the panel but behind everything the screen draws next.
     */
    public final void panel(GuiGraphics g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h);
        Runnable r = afterPanel;
        if (r != null) {
            afterPanel = null;
            r.run();
        }
    }

    /** Run once right after the next {@link #panel} (set and cleared by {@link SkinnedScreen}). */
    static Runnable afterPanel;

    public abstract void drawPanel(GuiGraphics g, int x, int y, int w, int h);

    /** A socket around the 16x16 item at (x, y). */
    public abstract void slot(GuiGraphics g, int x, int y);

    abstract void button(GuiGraphics g, int x, int y, int w, int h, Component label, float glow, boolean down,
                         boolean active, boolean primary);

    /** What pressing one of this skin's buttons sounds like, unless the button says otherwise. */
    public abstract SoundEvent click();

    /** Played as a screen opens; null for none. */
    public SoundEvent open() {
        return null;
    }

    public SoundEvent close() {
        return null;
    }

    /** Clip the screen to how far it has opened, {@code t} 0 shut to 1 open; {@link #endReveal} after drawing it. */
    public void beginReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
    }

    public void endReveal(GuiGraphics g, int x, int y, int w, int h, float t) {
    }

    /** Beats of a flow screen (docs/gui/gui_flow_feel.md) that every skin answers with a sound and a move. */
    public enum Cue { SEAT, REJECT, STAGE, START, FINISH, ABORT }

    /** Sound floor: the same cue inside this many ms of its last play is dropped, so rapid presses do not stack. */
    private static final long CUE_GAP_MS = 70;
    private static final long[] CUE_LAST = new long[Cue.values().length];

    /** The vanilla sound standing in for {@code cue} on this skin (null for none). */
    public SoundEvent sound(Cue cue) {
        return switch (name) {
            case "BLUEPRINT" -> switch (cue) {
                case SEAT -> SoundEvents.BOOK_PAGE_TURN;
                case REJECT -> SoundEvents.BOOK_PUT;
                case STAGE -> FSounds.UI_UNROLL.get();
                case START -> SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
                case FINISH -> FSounds.UI_STAMP.get();
                case ABORT -> FSounds.UI_ROLL.get();
            };
            case "FORGE" -> switch (cue) {
                case SEAT -> SoundEvents.CHAIN_PLACE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE -> FSounds.UI_HATCH_OPEN.get();
                case START -> SoundEvents.FIRECHARGE_USE;
                case FINISH -> SoundEvents.ANVIL_USE;
                case ABORT -> FSounds.UI_HATCH_CLOSE.get();
            };
            case "ENAMEL", "CONSOLE" -> switch (cue) {
                case SEAT -> FSounds.UI_DETENT.get();
                case REJECT -> SoundEvents.DISPENSER_FAIL;
                case STAGE -> FSounds.UI_TOGGLE.get();
                case START -> SoundEvents.LEVER_CLICK;
                case FINISH -> SoundEvents.COMPARATOR_CLICK;
                case ABORT -> FSounds.UI_TOGGLE.get();
            };
            case "STONE" -> switch (cue) {
                case SEAT -> SoundEvents.STONE_PLACE;
                case REJECT -> SoundEvents.STONE_HIT;
                case STAGE -> SoundEvents.UI_STONECUTTER_SELECT_RECIPE;
                case START -> SoundEvents.GRINDSTONE_USE;
                case FINISH -> SoundEvents.UI_STONECUTTER_TAKE_RESULT;
                case ABORT -> SoundEvents.STONE_BREAK;
            };
            case "MYSTIC" -> switch (cue) {
                case SEAT -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case REJECT -> SoundEvents.AMETHYST_BLOCK_HIT;
                case STAGE, START -> SoundEvents.ENCHANTMENT_TABLE_USE;
                case FINISH -> SoundEvents.AMETHYST_BLOCK_RESONATE;
                case ABORT -> SoundEvents.AMETHYST_BLOCK_BREAK;
            };
            case "BRICK" -> switch (cue) {
                case SEAT, START -> SoundEvents.CALCITE_PLACE;
                case REJECT -> SoundEvents.CALCITE_HIT;
                case STAGE -> SoundEvents.CALCITE_FALL;
                case FINISH -> SoundEvents.CALCITE_HIT;
                case ABORT -> SoundEvents.CALCITE_BREAK;
            };
            case "WOOD" -> switch (cue) { // vanilla wood events until real ones are edited from existing assets
                case SEAT -> SoundEvents.WOODEN_BUTTON_CLICK_ON;
                case REJECT -> SoundEvents.BAMBOO_WOOD_HIT;
                case STAGE -> SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON;
                case START -> SoundEvents.CHEST_OPEN;
                case FINISH -> SoundEvents.BAMBOO_WOOD_PLACE;
                case ABORT -> SoundEvents.CHEST_CLOSE;
            };
            case "ANVIL" -> switch (cue) {
                case SEAT -> SoundEvents.ANVIL_PLACE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE -> SoundEvents.ANVIL_HIT;
                case START -> SoundEvents.ANVIL_PLACE;
                case FINISH -> SoundEvents.ANVIL_USE;
                case ABORT -> SoundEvents.ANVIL_BREAK;
            };
            case "SMOKE" -> switch (cue) {
                case SEAT -> SoundEvents.WOODEN_BUTTON_CLICK_ON;
                case REJECT -> SoundEvents.BAMBOO_WOOD_HIT;
                case STAGE, START -> SoundEvents.CAMPFIRE_CRACKLE;
                case FINISH -> SoundEvents.BAMBOO_WOOD_PLACE;
                case ABORT -> SoundEvents.WOODEN_TRAPDOOR_CLOSE;
            };
            case "SLAG" -> switch (cue) {
                case SEAT -> SoundEvents.IRON_TRAPDOOR_CLOSE;
                case REJECT -> SoundEvents.ANVIL_LAND;
                case STAGE, START -> SoundEvents.FIRECHARGE_USE;
                case FINISH -> SoundEvents.IRON_DOOR_OPEN;
                case ABORT -> SoundEvents.IRON_DOOR_CLOSE;
            };
            case "TERMINAL", "TERMINAL_AMBER", "TERMINAL_WHITE", "TERMINAL_BLUE" -> switch (cue) {
                case SEAT -> SoundEvents.COMPARATOR_CLICK;
                case REJECT -> SoundEvents.NOTE_BLOCK_BASS.value();
                case STAGE, START -> SoundEvents.UI_BUTTON_CLICK.value();
                case FINISH -> SoundEvents.BEACON_POWER_SELECT;
                case ABORT -> SoundEvents.BEACON_DEACTIVATE;
            };
            case "SUIT" -> switch (cue) {
                case SEAT -> SoundEvents.IRON_TRAPDOOR_CLOSE;
                case REJECT -> SoundEvents.DISPENSER_FAIL;
                case STAGE -> SoundEvents.PISTON_EXTEND;
                case START -> SoundEvents.BEACON_POWER_SELECT;
                case FINISH -> SoundEvents.BEACON_ACTIVATE;
                case ABORT -> SoundEvents.PISTON_CONTRACT;
            };
            case "GLASS" -> switch (cue) {
                case SEAT -> SoundEvents.UI_BUTTON_CLICK.value();
                case REJECT -> SoundEvents.NOTE_BLOCK_BASS.value();
                case STAGE, START -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case FINISH -> SoundEvents.AMETHYST_CLUSTER_PLACE;
                case ABORT -> SoundEvents.UI_BUTTON_CLICK.value();
            };
            case "VANILLA" -> cue == Cue.REJECT ? SoundEvents.DISPENSER_FAIL : click();
            default -> click();
        };
    }

    /**
     * Pitch carried by what the stock is made of: metal rings high, stone and grain sit low. Reads
     * the material's {@code kind}, never its name; null is neutral.
     */
    public static float kindPitch(Material.Kind kind) {
        if (kind == null)
            return 1f;
        return switch (kind) {
            case METAL -> 1.12f;
            case GLASS, CRYSTAL -> 1.25f;
            case MINERAL -> 0.85f;
            case CERAMIC, POLYMER -> 0.9f;
            case GRANULAR -> 0.8f;
            case ORGANIC -> 0.95f;
            default -> 1f;
        };
    }

    private static float rejectAt = Float.NEGATIVE_INFINITY;

    /** {@link GuiStyle#time()} when a {@link Cue#REJECT} last played. */
    final float rejectAt() {
        return rejectAt;
    }

    /**
     * How this skin's controls move: heavy iron presses deep and slow with no give, wood settles with a little, a terminal key is
     * instant. Data skins override it with the {@code feel} block of their JSON.
     */
    public Feel feel() {
        return switch (name) {
            case "FORGE", "SLAG", "ANVIL" -> new Feel(0.9f, 0.1f);
            case "STONE", "BRICK" -> new Feel(0.8f, 0.3f);
            case "WOOD", "KILN", "SMOKE", "ANNEALING_FURNACE", "IMPROVED_FURNACE" -> new Feel(0.5f, 0.6f);
            case "MYSTIC" -> new Feel(0.2f, 0.5f);
            case "BLUEPRINT" -> new Feel(0.3f, 0.2f);
            case "ENAMEL", "VANILLA" -> new Feel(0.5f, 0.1f);
            default -> Feel.DIGITAL; // terminals, console, glass, suit
        };
    }

    /** {@link #cue(Cue, Material.Kind, float)} with a neutral stock and no outcome bias. */
    public void cue(Cue cue) {
        cue(cue, null, 1f);
    }

    /**
     * Plays this skin's sound for {@code cue}. {@code kind}
     * is the stock's material kind (null for none) and shifts the pitch; {@code bias} is the
     * outcome: above 1 for a result that beats the alternative, below 1 for a worse one. Reject and
     * abort sound lower and quieter. The same cue inside {@value #CUE_GAP_MS} ms of its last play is
     * silent.
     */
    public void cue(Cue cue, Material.Kind kind, float bias) {
        if (cue == Cue.REJECT)
            rejectAt = GuiStyle.time(); // the control whose press caused it shakes and flashes by itself
        long now = net.minecraft.Util.getMillis();
        int i = cue.ordinal();
        if (now - CUE_LAST[i] >= CUE_GAP_MS) {
            CUE_LAST[i] = now;
            float base = cue == Cue.REJECT ? 0.7f : cue == Cue.ABORT ? 0.75f : 1f;
            play(sound(cue), base * kindPitch(kind) * bias, cue == Cue.REJECT ? 0.5f : 0.6f);
        }
    }

    /** A screen sound, a touch off-pitch each time so repeats do not sound stamped out. */
    public static void play(SoundEvent sound, float pitch) {
        play(sound, pitch, 1f);
    }

    /** As {@link #play(SoundEvent, float)}, at a fraction of full volume. */
    public static void play(SoundEvent sound, float pitch, float volume) {
        if (sound != null)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,
                    pitch * (0.96f + 0.08f * ThreadLocalRandom.current().nextFloat()), volume));
    }

    public static int lerp(int a, int b, float t) {
        return FastColor.ARGB32.lerp(t, a, b);
    }
}
