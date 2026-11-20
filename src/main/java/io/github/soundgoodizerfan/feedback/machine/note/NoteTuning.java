// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.note;

/** The note block's constants. Invented. */
public final class NoteTuning {

    private NoteTuning() {
    }

    /** Noisemakers a note block can carry: one, and an optional second. */
    public static final int VOICES = 2;
    /** Loudness of a noisemaker: this plus {@link #LOUD_PER_ST} per point of its material's hardness, held between {@link #MIN_VOLUME} and {@link #MAX_VOLUME}. */
    public static final float LOUD_BASE = 1.5f;
    public static final float LOUD_PER_ST = 0.1f;
    public static final float MIN_VOLUME = 1f;
    public static final float MAX_VOLUME = 3f;
    /** A noisemaker heavier than this density (Mu per mB) plays lower, a lighter one higher: {@link #SHIFT_OCTAVES_PER_DOUBLING} octaves for each doubling, held to +-{@link #MAX_SHIFT_OCTAVES}. */
    public static final float REF_DENSITY = 0.0079f;
    public static final float SHIFT_OCTAVES_PER_DOUBLING = 0.25f;
    public static final float MAX_SHIFT_OCTAVES = 0.5f;
    /** The note a body with no pitch figure plays: the middle of the two octaves. */
    public static final int DEFAULT_NOTE = 12;
}
