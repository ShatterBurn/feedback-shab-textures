// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.ender.DataPairRecipe;
import io.github.soundgoodizerfan.feedback.machine.ender.EndernetUpgradeRecipe;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The endernet crafts: a pair of Data Ports, and a Data Port turned into an upgrade of the same bond. */
final class EndernetCraftingExtensions {

    private EndernetCraftingExtensions() {
    }

    private static ItemStack plate() {
        return new ItemStack(BuiltInRegistries.ITEM.get(Feedback.id("steel_plate")));
    }

    private static ItemStack wire() {
        return new ItemStack(BuiltInRegistries.ITEM.get(Feedback.id("copper_fine_wire")));
    }

    /** A pearl, two plates and a fine wire give two bonded Data Ports. */
    static final class Pair implements ICraftingCategoryExtension<DataPairRecipe> {
        @Override
        public void setRecipe(RecipeHolder<DataPairRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
            grid.createAndSetInputs(builder, List.of(List.of(new ItemStack(Items.ENDER_PEARL)), List.of(plate()), List.of(plate()), List.of(wire())), 2, 2);
            grid.createAndSetOutputs(builder, List.of(new ItemStack(FItems.DATA_PORT.get(), 2)));
        }

        @Override
        public int getWidth(RecipeHolder<DataPairRecipe> holder) {
            return 2;
        }

        @Override
        public int getHeight(RecipeHolder<DataPairRecipe> holder) {
            return 2;
        }
    }

    /** A bonded Data Port and a plate give an upgrade bearing its bond. */
    static final class Upgrade implements ICraftingCategoryExtension<EndernetUpgradeRecipe> {
        @Override
        public void setRecipe(RecipeHolder<EndernetUpgradeRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
            grid.createAndSetInputs(builder, List.of(List.of(new ItemStack(FItems.DATA_PORT.get())), List.of(plate())), 2, 1);
            grid.createAndSetOutputs(builder, List.of(new ItemStack(FItems.ENDERNET_UPGRADE.get())));
        }

        @Override
        public int getWidth(RecipeHolder<EndernetUpgradeRecipe> holder) {
            return 2;
        }

        @Override
        public int getHeight(RecipeHolder<EndernetUpgradeRecipe> holder) {
            return 1;
        }
    }
}
