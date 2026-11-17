// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

/**
 * A data node that carries a circuit which can amplify: a Controller, a Computer, a Data Port, or a machine with a circuit slot. The gain
 * (a tubed circuit's, at the warmth its stack is at) lengthens a data link that ends here: {@link DataWires#range} grows by
 * {@code 1 + CIRCUIT_WIRE_RANGE_GAIN x gain}, the larger of the two ends' gains, at the same wire and the same cost per block. A property of the
 * circuit, never of an item.
 */
public interface Amplifying {

    /** The gain of the circuit seated here, 0 with none or one with no tube or a cold one. */
    float amplifierGain();
}
