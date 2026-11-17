// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/** Anything bonded under a pair id that the {@link PairBook} knows the place of: an ender port or a Data Port. The Tuned Eye tunes to either. */
public interface PairEnd {

    /** The id this end is bonded under, or null. */
    @Nullable
    UUID pairId();
}
