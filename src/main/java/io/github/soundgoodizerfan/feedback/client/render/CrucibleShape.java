// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;

import net.minecraft.util.Mth;

/**
 * The open pot a built crucible is drawn as, from two figures of its design: the hollow it holds (tank, mB) sets the outer size, and the
 * wall's mass per mB of that hollow sets how thick the wall is (a heavy wall round a small hollow is thick, a light one thin). All in
 * sixteenths of a block, whole units so the pot stays on the chunky grid. {@code scripts/crucible_model.py} writes the default pot
 * (tank 1296 mB, mass 25) as the item model from the same rules; the hitbox in {@code CrucibleBlock} is the largest pot.
 */
public record CrucibleShape(int width, int height, int wall) {

    private static final float REFERENCE_TANK_MB = VesselTuning.VESSEL_TANK_CAPACITY_MB;
    /** Mass per mB of the reference crucible: the wall that is two units thick. */
    private static final float REFERENCE_MASS_PER_MB = VesselTuning.CRUCIBLE_SMALL_MASS.workPerTu() / REFERENCE_TANK_MB;

    /** Outer width of the body (the lip is two wider), body height, wall thickness. */
    public static CrucibleShape of(float tankMb, float massWorkPerTu) {
        float scale = (float) Math.cbrt(Math.max(tankMb, 1f) / REFERENCE_TANK_MB);
        int width = Mth.clamp(Math.round(10f * scale / 2f) * 2, 6, 12);
        int height = Mth.clamp(Math.round(10f * scale / 2f) * 2, 8, 12);
        float perMb = massWorkPerTu / Math.max(tankMb, 1f);
        int wall = Mth.clamp(Math.round(2f * perMb / REFERENCE_MASS_PER_MB), 1, 3);
        return new CrucibleShape(width, height, wall);
    }

    /** Top of the floor inside the bowl. */
    public int floorTop() {
        return 2 + wall;
    }

    /** Where the metal can rise to: just under the lip. */
    public int brim() {
        return height - 2;
    }
}
