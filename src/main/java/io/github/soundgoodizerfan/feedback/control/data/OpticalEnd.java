// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

/**
 * A data node that can be one end of a fibre link: the Optical Terminal. A fibre carries light only between two of these; what the
 * link does at any moment follows what each end is doing now, so a dark end (no power, no crystal) leaves the whole line dark.
 */
public interface OpticalEnd extends DataNode {

    /** Whether this end is giving light now: powered, with a crystal seated. */
    boolean lit();

    /** The share of the loss budget this end's crystal allows, 0 to 1 ({@code ControlTuning#FIBRE_CRYSTAL_FLOOR} plus the slope times its rectifying figure). */
    float budgetShare();

    /** The gain of the circuit seated in this end, 0 with none. */
    float gain();
}
