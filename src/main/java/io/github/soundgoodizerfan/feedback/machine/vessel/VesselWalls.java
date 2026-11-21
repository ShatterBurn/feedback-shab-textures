// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;

/**
 * A vessel's own thermal mass from what its walls are: so many Mu of a material times that material's specific heat, as
 * {@link VesselKind#mass} does for the furnaces. The invented figure stands only where the material table does not have the wall.
 */
public final class VesselWalls {

    private VesselWalls() {
    }

    /** {@code mu} of {@code material}'s walls as Work per Tu; {@code fallback} if the material or its specific heat is not there. */
    public static ThermalMass of(String material, float mu, ThermalMass fallback) {
        var wall = MaterialTable.get().materials().get(ResourceLocation.fromNamespaceAndPath("feedback", material));
        return wall == null ? fallback : wall.thermal().specificHeat().map(h -> new ThermalMass(h.workPerMuPerTu() * mu)).orElse(fallback);
    }
}
