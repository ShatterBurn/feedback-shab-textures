// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * One plate and one rod of the same metal, anywhere in the grid: one coupling sleeve in that metal. Any material with a
 * yield strength counts. Code, not JSON, because the result is made of whatever went in.
 */
public class CouplingSleeveRecipe extends CustomRecipe {

    public CouplingSleeveRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : CouplingSleeveItem.of(material);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        ResourceLocation material = null;
        boolean plate = false, rod = false;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty())
                continue;
            var found = Material.find(MaterialIngredient.materialsNow(), stack).orElse(null);
            if (found == null || found.material().mechanical().yieldStrength().isEmpty())
                return null;
            if (material != null && !material.equals(found.id()))
                return null;
            material = found.id();
            if (found.form().equals("plate") && !plate)
                plate = true;
            else if (found.form().equals("rod") && !rod)
                rod = true;
            else
                return null;
        }
        return plate && rod ? material : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.COUPLING_SLEEVE.get();
    }
}
