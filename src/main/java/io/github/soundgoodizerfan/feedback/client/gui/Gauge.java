// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * A gauge linked to what it shows: the figure, the tick and the colour are read every frame, so a screen or HUD says
 * what the gauge is of and the skin ({@link GuiSkin#gauge}) decides how it looks. A temperature bar, a progress bar and
 * a stored-charge bar are the same widget with different suppliers.
 */
public final class Gauge {

    private static final int TILE = 16;

    private final DoubleSupplier fill;
    private Supplier<Fluid> liquid;
    private DoubleSupplier mark = () -> -1;
    private IntSupplier lead;
    private int ticks;
    private boolean vertical;

    private Gauge(DoubleSupplier fill) {
        this.fill = fill;
    }

    /** A gauge filled to {@code fill}, 0..1. */
    public static Gauge of(DoubleSupplier fill) {
        return new Gauge(fill);
    }

    /** A tick at {@code mark} 0..1 with the part past it drawn as over the limit; negative for none. */
    public Gauge mark(DoubleSupplier mark) {
        this.mark = mark;
        return this;
    }

    /** The fill colour, instead of the skin's ink. */
    public Gauge lead(IntSupplier lead) {
        this.lead = lead;
        return this;
    }

    /** Graduations along the side, one a band, for a gauge that steps rather than slides. */
    public Gauge ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    /** Filling upward rather than rightward. */
    public Gauge vertical() {
        vertical = true;
        return this;
    }

    /**
     * Fills with the fluid itself instead of a flat colour: its flowing sprite tiled up from the floor of the well, tinted by the fluid's
     * tint (or {@link #lead}, when set), with a bright line on the surface. Vertical gauges only; a fluid with no sprite falls back to the
     * flat colour.
     */
    public Gauge liquid(Supplier<Fluid> fluid) {
        this.liquid = fluid;
        return this;
    }

    /** Draws it in {@code skin}, its fill area at (x, y), {@code w} by {@code h} inside the well's rim. */
    public void render(GuiGraphics g, GuiSkin skin, int x, int y, int w, int h) {
        float f = (float) fill.getAsDouble();
        Fluid fluid = liquid == null || !vertical ? null : liquid.get();
        ResourceLocation texture = fluid == null ? null : IClientFluidTypeExtensions.of(fluid).getFlowingTexture();
        int tint = lead != null ? lead.getAsInt() : fluid == null ? skin.ink() : IClientFluidTypeExtensions.of(fluid).getTintColor() | 0xFF000000;
        if (texture == null) {
            skin.gauge(g, x, y, w, h, vertical, f, (float) mark.getAsDouble(), tint, ticks);
            return;
        }
        skin.gauge(g, x, y, w, h, true, 0f, (float) mark.getAsDouble(), tint, ticks); // the well and its graduations
        int filled = Math.round(Mth.clamp(f, 0f, 1f) * h);
        if (filled <= 0)
            return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int top = y + h - filled;
        g.enableScissor(x, top, x + w, y + h);
        for (int ty = y + h - TILE; ty > top - TILE; ty -= TILE)
            for (int tx = x; tx < x + w; tx += TILE)
                g.blit(tx, ty, 0, TILE, TILE, sprite, (tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, 1f);
        g.disableScissor();
        if (filled < h)
            g.fill(x, top, x + w, top + 1, 0xA0FFFFFF);
    }
}
