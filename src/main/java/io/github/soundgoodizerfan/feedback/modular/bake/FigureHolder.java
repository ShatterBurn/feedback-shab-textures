// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

/**
 * A block entity that keeps the design its block was built from, through a {@link FigureStore}. The client reads the part
 * materials from it to tint the block (see {@code client/item/FigureTint}), so such an entity also sends its update tag
 * ({@code getUpdateTag} and {@code getUpdatePacket}) to the client: the design rides in the tag the store saves.
 */
public interface FigureHolder {

    FigureStore figures();
}
