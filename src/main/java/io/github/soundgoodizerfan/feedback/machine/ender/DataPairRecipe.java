// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * One pearl, two metal plates and a length of fine wire (any arrangement) make two Data Ports that carry the same pair id: the ender
 * port's recipe with the wire as the data. The parts are told by what they are (a spatial anchor tag, the {@code plate} and
 * {@code fine_wire} forms), never by material or id.
 */
public class DataPairRecipe extends CustomRecipe {

    public DataPairRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        var materials = ClientMaterials.on(level);
        int pearls = 0, plates = 0, wires = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(EnderPairRecipe.SPATIAL_ANCHORS))
                pearls++;
            else {
                String form = Material.find(materials, s).map(Material.Located::form).orElse("");
                if (form.equals("plate"))
                    plates++;
                else if (form.equals("fine_wire"))
                    wires++;
                else
                    return false;
            }
        }
        return pearls == 1 && plates == 2 && wires == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = new ItemStack(FItems.DATA_PORT.get(), 2);
        CompoundTag tag = new CompoundTag();
        tag.putUUID(EnderPortBlockEntity.PAIR_KEY, UUID.randomUUID());
        out.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.DATA_PAIR.get();
    }
}
