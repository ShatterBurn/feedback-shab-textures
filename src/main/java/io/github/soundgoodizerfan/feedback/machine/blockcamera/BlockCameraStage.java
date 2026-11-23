// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.blockcamera;

import net.minecraft.util.StringRepresentable;

/**
 * Where a Block Camera is in its one job. It sits {@link #IDLE} until a player with Bad Omen comes in range, then
 * locks on and blinks its red light ({@link #LOCK_ON} and {@link #LOCK_OFF} alternate), then reports the player
 * and is {@link #SPENT} for good: a dead lens and no light.
 */
public enum BlockCameraStage implements StringRepresentable {
    IDLE("idle"),
    LOCK_ON("lock_on"),
    LOCK_OFF("lock_off"),
    SPENT("spent");

    private final String key;

    BlockCameraStage(String key) {
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    /** Whether the camera is part way through reporting a player. */
    public boolean locked() {
        return this == LOCK_ON || this == LOCK_OFF;
    }
}
