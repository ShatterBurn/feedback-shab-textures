// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * What a removable medium holds, in files: one number per medium, a default per item that a stack may override with its own
 * {@code MEDIA_CAPACITY}, so a bigger drive is a data change and never a config knob.
 */
public final class ComputerMedia {

    private ComputerMedia() {
    }

    public static int capacity(ItemStack stack) {
        Integer own = stack.get(FDataComponents.MEDIA_CAPACITY.get());
        if (own != null)
            return own;
        if (stack.is(FItems.USB_DRIVE.get()))
            return ControlTuning.USB_DRIVE_FILES;
        if (stack.is(FItems.FLOPPY_DISK.get()))
            return ControlTuning.FLOPPY_DISK_FILES;
        return ControlTuning.PUNCH_CARD_FILES;
    }
}
