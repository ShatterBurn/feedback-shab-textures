// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.fishwheel;

import io.github.soundgoodizerfan.feedback.registry.FAutomation;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A creel over a water wheel: the fish wheel, built on that wheel, so it keeps the parts and the figures
 * the wheel was made from. Code, not JSON, because the result is made of whatever wheel went in.
 */
public class FishWheelRecipe extends CustomRecipe {

    public FishWheelRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() == 1 && input.height() == 2
                && input.getItem(0).is(FAutomation.CREEL_ITEM.get()) && input.getItem(1).is(FModular.WATER_WHEEL.get());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack wheel = input.getItem(1);
        ItemStack out = new ItemStack(FAutomation.FISH_WHEEL_ITEM.get());
        carry(wheel, out, FDataComponents.BLOCK_FIGURES.get());
        carry(wheel, out, FModular.DESIGN.get());
        carry(wheel, out, FModular.FIGURES.get());
        return out;
    }

    private static <T> void carry(ItemStack from, ItemStack to, DataComponentType<T> type) {
        T value = from.get(type);
        if (value != null)
            to.set(type, value);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.FISH_WHEEL.get();
    }
}
