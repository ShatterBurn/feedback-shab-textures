// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Thin air hurts a person. Where what a lung finds ({@code AirDensityTable.breathable}) falls under
 * {@link SpaceTuning#UNAIDED_BREATHABLE} and no worn {@link BreathingGear} supplies air, the vanilla air bar drains and
 * then hurts as drowning does; before it does, a laboured breath and a swimming head say what is coming. Nothing is
 * instant, and putting the gear on late recovers. Reads the air table at the eyes, never the dimension. Players only.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Breath {
    private Breath() {
    }

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator() || p.isEyeInFluid(FluidTags.WATER))
            return;
        float lung = AirDensityTable.breathable(p.level(), p.getEyeY());
        if (lung >= SpaceTuning.UNAIDED_BREATHABLE)
            return;
        var gear = CuriosApi.getCuriosInventory(p)
                .flatMap(inv -> inv.findFirstCurio(s -> s.getItem() instanceof BreathingGear));
        if (gear.isPresent() && ((BreathingGear) gear.get().stack().getItem()).supply(gear.get().stack(), p, lung))
            return;
        event.setCanBreathe(false);
        if (p.tickCount % SpaceTuning.WARNING_INTERVAL == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, true, false));
            // shortcut: vanilla's breath sound stands in until the add-sound skill brings a laboured-breath sample.
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1f, 0.7f);
        }
    }
}
