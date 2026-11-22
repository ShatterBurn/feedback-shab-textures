// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * A block entity a fixed line can be tied to: a piton, or a winch. It keeps a {@link LineEnd} and says where the line is fixed on it; the
 * rest of the line's behaviour (tying, climbing, load) is {@link FixedLines}.
 */
public interface LineHost {

    LineEnd end();

    BlockPos getBlockPos();

    /** Called on the server after the line at this end changed: save, sync to clients and refresh the index. */
    void lineChanged();

    /** Where the line is fixed, in the world. */
    Vec3 eye();

    /** The block this end is held by, or null when it is bolted fast and holds any load (a winch). */
    BlockPos behind();

    /** Blocks per tick this end draws a body hanging on the line toward itself; negative pays it out. A piton draws nothing. */
    default float reel() {
        return 0f;
    }

    /** The mass, in Mu, hanging on the line this tick; a winch turns it into its load. */
    default void hung(float mass) {
    }

    /** Whether this end is straining against more than its drive can carry, so the line takes the load several times over. */
    default boolean stalled() {
        return false;
    }

    default BlockPos partner() {
        return end().partner();
    }

    default boolean primary() {
        return end().primary(getBlockPos());
    }
}
