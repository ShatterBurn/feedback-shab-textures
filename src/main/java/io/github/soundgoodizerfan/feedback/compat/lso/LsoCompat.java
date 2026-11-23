// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Legendary Survival Overhaul (LSO) soft compat: its body temperature and thirst follow Feedback's world. Nothing of LSO's types
 * appears here, so this class loads without it; {@link LsoPlugin} (which names them) is touched only when the mod is present.
 *
 * <p>Licence: LSO's terms are unclear (its Modrinth jar and gradle file say MIT, its CurseForge page all rights reserved, its
 * repository carries stock Forge MDK text). It is used as a compile-only API and as datapack formats; no code or asset is copied.
 * Taken: the public {@code api} package (modifier base, thirst util, item attribute registry) and the thirst block data format.
 * Rejected: its own thermometer and canteen items, wetness and body damage (Feedback has no use for them yet).
 */
public final class LsoCompat {

    public static final String MOD_ID = "legendarysurvivaloverhaul";

    private LsoCompat() {
    }

    /** Called from the mod constructor, before any registry event: LSO's modifier list must still be open. */
    public static void init(IEventBus modBus) {
        if (ModList.get().isLoaded(MOD_ID))
            LsoPlugin.init(modBus);
    }
}
