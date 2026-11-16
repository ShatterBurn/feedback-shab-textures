// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * The Gap's sky: the Overworld's night with no cloud layer (a cloud level that is not a number draws none, as the End's does),
 * and no fog. The dimension type names it as {@code feedback:gap} in {@code effects}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GapEffects extends DimensionSpecialEffects {

    private GapEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    @SubscribeEvent
    public static void register(RegisterDimensionSpecialEffectsEvent event) {
        event.register(Feedback.id("gap"), new GapEffects());
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
        return fog;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }
}
