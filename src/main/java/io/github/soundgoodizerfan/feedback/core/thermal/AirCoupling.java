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
 * The figure is the density of the dimension's curve at the body's world height (a body on a Sable body is carried through
 * its pose to where it really is), so a vessel high in the thin air of the Overworld loses less heat than at sea level.
 */
public final class AirCoupling {
    private AirCoupling() {
    }

    /** The share of a body's leak that still goes through the air, 0 to 1. */
    public static float of(ThermalBody body) {
        if (body instanceof BlockEntity be && be.getLevel() != null)
            return Math.min(1f, AirDensityTable.at(be.getLevel(), worldHeight(be)));
        return 1f;
    }

    /** The height {@code be} really is at: its own for a block in the world, carried through the body's pose for one on a Sable body. */
    private static double worldHeight(BlockEntity be) {
        net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos());
        var body = dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(be);
        return body == null ? at.y : body.logicalPose().transformPosition(at).y;
    }

    /** Work per tick per Tu the radiator panels beside {@code body} add to its leak; 0 where the air carries the heat anyway. */
    public static float radiated(ThermalBody body) {
        if (!(body instanceof BlockEntity be) || be.getLevel() == null || of(body) >= 1f)
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
