// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;

import net.minecraft.world.level.Level;
import org.joml.Vector3d;

/**
 * The gravity Sable puts on a body in {@code level}: its per-dimension physics data ({@code dimension_physics}), 11 blocks/s^2
 * down in the Overworld and none in the Shelf. Anything that weighs a body (a ballast tank, a joint's arm, a gas cell's lift, the
 * diagram's weight arrow) reads this and not a constant, so it follows the dimension.
 */
public final class Gravity {
    private Gravity() {
    }

    /** The field as a vector in blocks/s^2, pointing the way things fall; a fresh vector. */
    public static Vector3d vector(Level level) {
        return DimensionPhysicsData.getGravity(level);
    }

    /** Its strength in blocks/s^2. */
    public static double of(Level level) {
        return DimensionPhysicsData.getGravity(level).length();
    }
}
