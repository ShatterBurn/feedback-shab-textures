// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * A force on a body, in kilonewtons: Sable's force unit, where a block is a metre and Sable's mass
 * unit (kpg, 1000 {@link Mu}) a tonne ({@link Units#kpg}). A thruster's push, a wing's lift, a body's weight are all {@code Fu}.
 * Not a count of blows: that is {@link Wu}, which is work beaten into a workpiece.
 */
public record Fu(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.fu";
    }
}
