// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.creeper.CreeperPenBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What a creeper pen holds, fetched for the one block looked at: occupied, charged, how full its reserve is, what is in the stomach, whether it is primed. */
public class PenServerData implements IServerDataProvider<BlockAccessor> {

    public static final PenServerData INSTANCE = new PenServerData();

    static final String PEN = "Pen", CHARGED = "PenCharged", FILL = "PenFill", OXIDISER = "PenOxidiser", FUEL = "PenFuel", PRIMED = "PenPrimed";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof CreeperPenBlockEntity pen))
            return;
        data.putBoolean(PEN, pen.occupied());
        if (!pen.occupied())
            return;
        data.putBoolean(CHARGED, pen.charged());
        data.putFloat(FILL, pen.cap() <= 0f ? 0f : pen.charge() / pen.cap());
        data.putBoolean(OXIDISER, pen.stomachOxidiser() > 0f);
        data.putBoolean(FUEL, pen.stomachFuel() > 0f);
        data.putBoolean(PRIMED, pen.warningLeft() >= 0);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("pen");
    }
}
