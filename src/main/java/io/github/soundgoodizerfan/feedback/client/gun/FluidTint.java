// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * The colour a fluid shows when it has no colour of its own: the average of the opaque pixels of its still sprite (the first frame), times
 * the fluid's tint. Used where a stream or splash tints by fluid and the material file carries no {@code color}. Cached per fluid, so a
 * resource reload must {@link #clear}.
 */
public final class FluidTint {

    private static final Map<ResourceLocation, Integer> CACHE = new HashMap<>();

    private FluidTint() {
    }

    public static void clear() {
        CACHE.clear();
    }

    /** 24-bit RGB for the fluid with this id, or {@code fallback} when it has no readable sprite. */
    public static int of(ResourceLocation fluid, int fallback) {
        return CACHE.computeIfAbsent(fluid, id -> average(id, fallback));
    }

    private static int average(ResourceLocation id, int fallback) {
        var f = BuiltInRegistries.FLUID.get(id);
        var ext = IClientFluidTypeExtensions.of(f);
        ResourceLocation tex = ext.getStillTexture();
        if (tex == null)
            return fallback;
        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(tex);
        NativeImage img = sprite.contents().getOriginalImage();
        int size = Math.min(img.getWidth(), img.getHeight()); // an animated sprite is a strip of square frames: the first
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                int p = img.getPixelRGBA(x, y); // ABGR
                if ((p >>> 24) < 128)
                    continue;
                r += p & 255;
                g += p >> 8 & 255;
                b += p >> 16 & 255;
                n++;
            }
        if (n == 0)
            return fallback;
        int tint = ext.getTintColor();
        int tr = tint >> 16 & 255, tg = tint >> 8 & 255, tb = tint & 255;
        return (int) (r / n * tr / 255) << 16 | (int) (g / n * tg / 255) << 8 | (int) (b / n * tb / 255);
    }
}
