// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program.text;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataSpec;
import io.github.soundgoodizerfan.feedback.control.data.Reduction;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import org.jetbrains.annotations.Nullable;

/**
 * The words a program text uses for a card's request: a signal ({@code temperature}), a kind ({@code furnace}), a color
 * ({@code red}) and a reduction ({@code avg}, {@code min}, {@code max}). This mod's own ids are written short; another
 * namespace's are written whole and quoted ({@code "othermod:boiler"}). A dye name or a reduction word is never read as a kind.
 */
final class TextWords {

    private TextWords() {
    }

    static String signal(ResourceLocation id) {
        return id.getNamespace().equals(Feedback.MOD_ID) ? id.getPath() : id.toString();
    }

    @Nullable
    static ResourceLocation signalOf(String text) {
        return text.indexOf(':') >= 0 ? ResourceLocation.tryParse(text) : ResourceLocation.tryBuild(Feedback.MOD_ID, text);
    }

    static String kind(ResourceLocation id) {
        return id.getNamespace().equals(Feedback.MOD_ID) && id.getPath().startsWith(DataSpec.KIND_PREFIX)
                ? id.getPath().substring(DataSpec.KIND_PREFIX.length()) : id.toString();
    }

    @Nullable
    static ResourceLocation kindOf(String text) {
        return text.indexOf(':') >= 0 ? ResourceLocation.tryParse(text) : ResourceLocation.tryBuild(Feedback.MOD_ID, DataSpec.KIND_PREFIX + text);
    }

    /** A dye color's id, or -1. */
    static int colorOf(String text) {
        DyeColor dye = DyeColor.byName(text.toLowerCase(java.util.Locale.ROOT), null);
        return dye == null ? -1 : dye.getId();
    }

    static String color(int id) {
        return DyeColor.byId(id).getName();
    }

    @Nullable
    static Reduction reductionOf(String text) {
        return switch (text.toLowerCase(java.util.Locale.ROOT)) {
            case "avg", "average" -> Reduction.AVERAGE;
            case "min" -> Reduction.MIN;
            case "max" -> Reduction.MAX;
            default -> null;
        };
    }

    static String reduction(Reduction reduction) {
        return switch (reduction) {
            case AVERAGE -> "avg";
            case MIN -> "min";
            case MAX -> "max";
        };
    }
}
