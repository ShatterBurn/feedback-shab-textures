// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What the air does to a body's heat. A body bleeds heat to the room through air; where the dimension's air thins the
 * leak thins with it, and in a vacuum a vessel keeps its heat. The way out is a radiator: a panel touching the vessel
 * sheds heat by its own conductance, whatever the air. A body with no place (an item) leaks as ever.
 * shortcut: the figure is the dimension's sea-level density, not the density at the body's height; a vessel in the
 * thin air of the Overworld's top loses heat as it does at sea level. Upgrade to the density at the body's world
 * height if the climb needs it.
 */
public final class AirCoupling {
    private AirCoupling() {
    }

    /** The share of a body's leak that still goes through the air, 0 to 1. */
    public static float of(ThermalBody body) {
        if (body instanceof BlockEntity be && be.getLevel() != null)
            return Math.min(1f, AirDensityTable.at(be.getLevel()));
        return 1f;
    }

    /** Work per tick per Tu the radiator panels beside {@code body} add to its leak; 0 where the air carries the heat anyway. */
    public static float radiated(ThermalBody body) {
        if (!(body instanceof BlockEntity be) || be.getLevel() == null || AirDensityTable.at(be.getLevel()) >= 1f)
            return 0f;
        int panels = 0;
        for (Direction d : Direction.values())
            if (be.getLevel().getBlockState(be.getBlockPos().relative(d)).is(FBlocks.RADIATOR_PANEL.get()))
                panels++;
        return panels * ThermalTuning.RADIATOR_CONDUCTANCE.workPerTickPerTu();
    }

    /** A body's whole leak in Work per tick per Tu: through the air, plus what its radiators shed. */
    public static float leak(ThermalBody body) {
        return body.getLeak().workPerTickPerTu() * of(body) + radiated(body);
    }
}
