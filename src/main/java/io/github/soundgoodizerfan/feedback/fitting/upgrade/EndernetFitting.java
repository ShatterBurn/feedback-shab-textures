// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.machine.ender.EnderPortBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * An endernet upgrade fitted to an Electric Controller or Computer: one end of an entangled data pair (endernet layer 1,
 * {@code control/endernet/Endernet}), bonded under the pair id it carries. Unlike the other upgrades it holds state, the id, which
 * the holder persists. One per host; the host draws {@link ControlTuning#ENDERNET_UPGRADE_AMPS} while it is fitted. It has no tank:
 * the other end of its pair must be a Data Port, which pays for the crossing.
 */
public class EndernetFitting implements UpgradeFitting {

    private final UUID pair;

    public EndernetFitting(UUID pair) {
        this.pair = pair;
    }

    public UUID pair() {
        return pair;
    }

    /** Channels the upgrade offers before the host's circuit adds any: one. The host tunes among what its circuit separates ({@code ControllerCore.endernetChannels}). */
    public int channels() {
        return ControlTuning.ENDERNET_CHANNELS;
    }

    @Override
    public ItemStack getPickItem() {
        ItemStack out = new ItemStack(FItems.ENDERNET_UPGRADE.get());
        CompoundTag tag = new CompoundTag();
        tag.putUUID(EnderPortBlockEntity.PAIR_KEY, pair);
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return out;
    }
}
