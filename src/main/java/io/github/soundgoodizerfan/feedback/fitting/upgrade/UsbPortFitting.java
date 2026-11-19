// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A socket fitted to a Computer so its bays take USB Drives (the {@code feedback:port_cards} tag). One per Computer; stateless, the holder
 * persists a flag.
 */
public class UsbPortFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.USB_PORT.get());
    }
}
