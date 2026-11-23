// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemporaryModifierGroupEnum;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;

/**
 * What a finished meal does through LSO: it hydrates by the water the food holds now (its nutrition times its moisture, so a thin
 * soup slakes and dry bread barely does, salted and dried food less), and a meal far from the room warms or chills for a while
 * by its real temperature, a hotter or colder one more strongly and longer. One server event per meal; nothing ticks.
 */
final class LsoMeals {

    private LsoMeals() {
    }

    static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null)
            return;
        Level level = player.level();
        float moisture = Cooking.findPerishable(level, stack)
                .map(entry -> {
                    FoodState now = Cooking.current(stack, entry, level, AmbientTable.at(level).value());
                    return now.moisture();
                }).orElse(0f);
        int hydration = Math.round(food.nutrition() * moisture * LsoTuning.HYDRATION_PER_NUTRITION);
        if (hydration > 0)
            ThirstUtil.takeDrink(player, hydration, 0f);
        float off = ItemHeat.get(stack, level).value() - AmbientTable.at(level).value();
        float size = Math.abs(off);
        if (size < LsoTuning.MEAL_DEADBAND)
            return;
        TemporaryModifierGroupEnum group = moisture >= LsoTuning.DRINK_MOISTURE ? TemporaryModifierGroupEnum.DRINK : TemporaryModifierGroupEnum.FOOD;
        int ticks = Mth.clamp(Math.round((size - LsoTuning.MEAL_DEADBAND) * LsoTuning.MEAL_TICKS_PER_TU) + LsoTuning.MEAL_MIN_TICKS,
                LsoTuning.MEAL_MIN_TICKS, LsoTuning.MEAL_MAX_TICKS);
        int amplifier = Math.min(2, (int) ((size - LsoTuning.MEAL_DEADBAND) / LsoTuning.MEAL_LEVEL_STEP));
        var effect = off > 0 ? group.hotEffect : group.coldEffect;
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }
}
