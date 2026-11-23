// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import sfiomn.legendarysurvivaloverhaul.registry.TemperatureModifierRegistry;

/**
 * Wires the bridge into LSO. Loaded only behind {@link LsoCompat#init}'s mod check.
 *
 * <p>LSO sums the modifiers of its own {@code DeferredRegister} (it iterates {@code MODIFIERS.getEntries()}, not the registry),
 * so ours is registered on that register while it is still open, which it is until the registry event fires after every mod
 * constructor has run.
 */
final class LsoPlugin {

    private LsoPlugin() {
    }

    static void init(IEventBus modBus) {
        TemperatureModifierRegistry.MODIFIERS.register("integration/feedback", FeedbackTemperatureModifier::new);
        NeoForge.EVENT_BUS.addListener(LsoInsulation::onAttributes);
        NeoForge.EVENT_BUS.addListener(LsoMeals::onEaten);
        if (FMLEnvironment.dist == Dist.CLIENT)
            modBus.addListener(LsoGauge::register);
    }
}
