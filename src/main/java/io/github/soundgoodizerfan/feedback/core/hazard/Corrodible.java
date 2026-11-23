// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

/**
 * A bare metal machine the world can eat at: it loses condition to its own floor when told to, and never past it. Separate from
 * {@code Wearing}, which is the read side an instrument depends on and deliberately has no way to wear anything; what the corrosion does to
 * a machine is still its own business, this only asks it to take some. See {@link FluxRust}.
 */
public interface Corrodible {

    /** Takes {@code amount} (a fraction of full condition) off the machine's condition, down to its floor. */
    void corrode(float amount);
}
