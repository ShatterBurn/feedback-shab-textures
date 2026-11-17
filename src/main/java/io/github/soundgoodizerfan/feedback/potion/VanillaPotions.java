// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A vanilla potion bottle that turns up (a chest, a villager's offer, a pocket) becomes the bottle it would have been here
 * ({@code ModularConvert} calls this as it does for tools). Drink, splash and lingering are one item here (every potion throws), so
 * all three become {@link PotionItem}. A bottle holds one effect: a vanilla potion with several (the turtle master's slowness and
 * resistance) gives its first, the one its name is for. Fresh, in water, with no taste: nothing about its making is known. Water
 * bottles, awkward and mundane potions have no effect and stay vanilla's.
 */
public final class VanillaPotions {

    private VanillaPotions() {
    }

    public static boolean convertible(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION))
            return false;
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.getAllEffects().iterator().hasNext();
    }

    public static ItemStack convert(ItemStack stack) {
        if (!convertible(stack))
            return stack;
        MobEffectInstance first = stack.get(DataComponents.POTION_CONTENTS).getAllEffects().iterator().next();
        var key = first.getEffect().unwrapKey();
        if (key.isEmpty())
            return stack;
        ItemStack out = new ItemStack(FItems.POTION.get(), stack.getCount());
        out.set(FDataComponents.POTION_STATE.get(), new PotionState(Optional.of(key.get().location()), first.getAmplifier(), first.getDuration(),
                1f, "water", List.of(0f, 0f, 0f), 0L, 20f, -1));
        return out;
    }
}
