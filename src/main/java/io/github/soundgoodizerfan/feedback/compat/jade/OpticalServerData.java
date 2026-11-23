// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.data.DataWires;
import io.github.soundgoodizerfan.feedback.control.data.OpticalEnd;
import io.github.soundgoodizerfan.feedback.machine.optical.OpticalTerminalBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * What an optical terminal is doing and what its fibre does, by words (no figures: a number costs an instrument): whether it has a crystal and
 * light, and for its fibre link how clean the reading is (exact, two figures, one figure, or lost), which says why a line is dark.
 */
public class OpticalServerData implements IServerDataProvider<BlockAccessor> {

    public static final OpticalServerData INSTANCE = new OpticalServerData();

    static final String STATE = "OpticalState", LINK = "OpticalLink";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof OpticalTerminalBlockEntity terminal) || terminal.getLevel() == null)
            return;
        data.putInt(STATE, terminal.crystal().isEmpty() ? 0 : terminal.lit() ? 2 : 1);
        for (DataNodeRef other : terminal.getNodeLinks()) {
            var loss = DataWires.fibreLoss(terminal.getLevel(), terminal.getNodeRef(), other);
            if (loss.isEmpty())
                continue;
            if (!(io.github.soundgoodizerfan.feedback.control.data.DataNode.resolveOrNull(terminal.getLevel(), other) instanceof OpticalEnd far)) {
                data.putInt(LINK, 4);
                break;
            }
            DataWires.Grade grade = !terminal.lit() || !far.lit() ? null : DataWires.fibreGrade(loss.get(), DataWires.budgetDb(terminal, far));
            data.putInt(LINK, grade == null ? 4 : switch (grade) {
                case FIBRE -> 1;
                case COARSE -> 2;
                default -> 3;
            });
            break;
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("optical");
    }
}
