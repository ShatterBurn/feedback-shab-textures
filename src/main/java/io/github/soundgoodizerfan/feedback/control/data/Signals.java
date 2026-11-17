// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * The vocabulary of numbers a data block can offer: a signal is a {@link ResourceLocation}, so a new one is a
 * new id and nothing else (no enum to widen, no editor to teach). Every {@link Quantity} is a signal of the same
 * name in this mod's namespace; {@link #quantity} maps back so unit checks and instrument tiers still apply to them.
 */
public final class Signals {

    public static final ResourceLocation STATE = of(Quantity.STATE);
    public static final ResourceLocation CONTROL = of(Quantity.CONTROL);

    private Signals() {
    }

    public static ResourceLocation of(Quantity quantity) {
        return Feedback.id(quantity.getSerializedName());
    }

    /** The quantity this signal is, or null for a signal that is not one of the built-in measures. */
    @Nullable
    public static Quantity quantity(ResourceLocation signal) {
        if (!signal.getNamespace().equals(Feedback.MOD_ID))
            return null;
        for (Quantity quantity : Quantity.values())
            if (quantity.getSerializedName().equals(signal.getPath()))
                return quantity;
        return null;
    }

    /** Two-state signals read as a boolean in a program; every other signal is a number. */
    public static boolean isBoolean(ResourceLocation signal) {
        return STATE.equals(signal);
    }
}
