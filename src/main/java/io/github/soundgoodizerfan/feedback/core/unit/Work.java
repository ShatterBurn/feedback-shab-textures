// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Energy delivered, carried or stored with no other type to say what kind it is: the work in a
 * blow, a pulse, a fall. Spelled out and never abbreviated, so nobody reads it as a currency to
 * convert into (philosophy 21). Not {@link Wu}: that is the count of blows beaten into a
 * workpiece, an integer; this is the continuous quantity a blow carries.
 */
public record Work(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.work";
    }
}
