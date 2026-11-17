// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;


import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A bowl of what came out of a stockpot. Named from what it is (broth, stew, scorched); the figures are baked on the stack. */
public class MealItem extends Item {

    public MealItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Meal meal = stack.get(FDataComponents.MEAL.get());
        return meal == null ? super.getName(stack) : Component.translatable("item.feedback.meal." + meal.dish());
    }
}
