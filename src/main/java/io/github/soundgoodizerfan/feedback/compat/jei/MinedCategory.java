// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What breaking an ore block gives: the ore block (and its deepslate twin) in, the material's lump or raw piece out. Read off the material's
 * forms, so a pack's ore appears with no data of its own; the loot table stays server-side. It is how galena and pyrite are obtained.
 */
public class MinedCategory implements IRecipeCategory<MinedCategory.Card> {

    public record Card(List<Item> ores, Item drop) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "mined", Card.class);

    private static final int WIDTH = 168, HEIGHT = 56;

    private final IDrawable icon;

    public MinedCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FItems.GALENA_ORE.get()));
    }

    /** One card per material that has an ore block and a lump or raw form. */
    static List<Card> cards(Map<ResourceLocation, Material> materials) {
        List<Card> out = new ArrayList<>();
        materials.forEach((id, m) -> {
            var drop = m.item(id, "lump").or(() -> m.item(id, "raw"));
            var ore = m.item(id, "ore");
            if (drop.isEmpty() || ore.isEmpty())
                return;
            List<Item> ores = new ArrayList<>(List.of(ore.get()));
            m.item(id, "deepslate_ore").filter(d -> d != ore.get()).ifPresent(ores::add);
            out.add(new Card(ores, drop.get()));
        });
        return out;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.mined");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 14).setStandardSlotBackground()
                .addItemStacks(card.ores().stream().map(ItemStack::new).toList());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 14).setOutputSlotBackground().addItemStack(new ItemStack(card.drop()));
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, new ItemStack(card.ores().get(0)).getHoverName().getString().toUpperCase(java.util.Locale.ROOT), 1, 1, 0xFF3F3F3F, false);
        int y = 36;
        for (var part : font.split(Component.translatable("feedback.jei.mined.note"), WIDTH - 2)) {
            graphics.drawString(font, part, 1, y, 0xFF7A7A7A, false);
            y += 10;
        }
    }
}
