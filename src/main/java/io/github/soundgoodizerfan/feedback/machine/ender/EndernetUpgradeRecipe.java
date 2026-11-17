// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A bonded Data Port and a metal plate make an endernet upgrade bearing the port's pair id: the port is spent into an antenna, so its
 * partner stays a port and the upgrade is the other end of the same bond. A port with no bond makes nothing.
 */
public class EndernetUpgradeRecipe extends CustomRecipe {

    public EndernetUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack port(CraftingInput input, Level level) {
        var materials = ClientMaterials.on(level);
        ItemStack port = null;
        int plates = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FItems.DATA_PORT.get()) && port == null && s.has(DataComponents.CUSTOM_DATA)
                    && s.get(DataComponents.CUSTOM_DATA).contains(EnderPortBlockEntity.PAIR_KEY))
                port = s;
            else if (Material.find(materials, s).map(l -> l.form().equals("plate")).orElse(false))
                plates++;
            else
                return null;
        }
        return plates == 1 ? port : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return port(input, level) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack port = null;
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FItems.DATA_PORT.get()))
                port = input.getItem(i);
        ItemStack out = new ItemStack(FItems.ENDERNET_UPGRADE.get());
        if (port != null)
            out.set(DataComponents.CUSTOM_DATA, CustomData.of(port.get(DataComponents.CUSTOM_DATA).copyTag()));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.ENDERNET_UPGRADE.get();
    }
}
