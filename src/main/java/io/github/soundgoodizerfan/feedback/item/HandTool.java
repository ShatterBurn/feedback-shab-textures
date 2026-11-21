// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.process.Operation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

/**
 * Something a player works a crafting grid with by hand: a strength, an operation and a noise ({@link HandToolItem} says why those
 * three and nothing more). An interface so a tool that is also something else (a hafted tool whose face is a hammer's) can be one
 * without a shared superclass.
 */
public interface HandTool {

    /** How hard one application of this tool lands, in St. */
    St getStrength(ItemStack stack);

    Operation getOperation();

    /** What this stack performs: its item's operation, unless the stack is a tool whose face decides (a hafted tool with a file head files, one with a hammer head strikes). */
    default Operation getOperation(ItemStack stack) {
        return getOperation();
    }

    SoundEvent getWorkSound();
}
