// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.treatise.Treatises;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.net.ResearchSyncPayload;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What starts a research at the Research Table: for a treatise volume the Blank Treatise, the volume before it (held, not used up) and the
 * specimen types it takes; for an attunement the kind of offering and whether a potion stands in. Verbs and words, never the board's answers.
 */
public class ResearchCategory implements IRecipeCategory<ResearchCategory.Card> {

    /** One research: a volume ({@code school} set, {@code specimens} the type paths) or an attunement ({@code school} null). */
    public record Card(School school, int volume, List<String> specimens, ResourceLocation effect, String grade, String property, boolean reagent) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "research", Card.class);

    private static final int WIDTH = 168, HEIGHT = 62;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public ResearchCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.RESEARCH_TABLE_ITEM.get()));
    }

    /** Every volume with the types it takes, then every attunement that has to be researched. */
    static List<Card> cards(List<ResearchSyncPayload.Entry> first, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Attunements.Effect> effects) {
        List<Card> out = new ArrayList<>();
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++) {
                List<String> specimens = new ArrayList<>();
                if (v == 1) {
                    for (var e : first)
                        if (e.school().equals(school.id()) && e.volume() == 1)
                            specimens.addAll(e.types());
                } else {
                    final int gate = v - 1;
                    types.forEach((id, type) -> {
                        if (School.of(type.school()) == school && School.volumeOf(type.school()) == gate)
                            specimens.add(id.getPath());
                    });
                }
                out.add(new Card(school, v, List.copyOf(specimens), null, "", "", false));
            }
        for (var e : effects.values()) {
            if (e.starter())
                continue;
            String grade = e.target() < 0.35f ? "modest" : e.target() < 0.65f ? "good" : e.target() < 0.85f ? "fine" : "exceptional";
            out.add(new Card(null, 0, List.of(), e.id(), grade, e.offering().orElse("hardness"), e.reagent().isPresent()));
        }
        return out;
    }

    /** The item a specimen type is shown as: its own item, or the hafted tool under the type's name for a face (a cleaver, a maul, a spear). */
    private static ItemStack specimen(String path) {
        var item = BuiltInRegistries.ITEM.get(Feedback.id(path));
        if (item != Items.AIR)
            return new ItemStack(item);
        ItemStack stack = new ItemStack(FModular.HAFTED_TOOL.get());
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.feedback." + path));
        return stack;
    }

    @Override
    public RecipeType<Card> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("feedback.jei.research");
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
        if (card.school() == null)
            return;
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground().addItemStack(new ItemStack(FModular.BLANK_TREATISE.get()));
        if (card.volume() > 1)
            builder.addSlot(RecipeIngredientRole.CATALYST, 21, 18).setStandardSlotBackground().addItemStack(Treatises.of(card.school(), card.volume() - 1));
        var specimens = builder.addSlot(RecipeIngredientRole.INPUT, 51, 18).setStandardSlotBackground();
        for (String s : card.specimens())
            specimens.addItemStack(specimen(s));
        builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(Treatises.of(card.school(), card.volume()));
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        if (card.school() == null) {
            graphics.drawString(font, Component.translatable("attunement.feedback." + card.effect().getPath()), 1, 1, TITLE_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.offering",
                    Component.translatable("gui.feedback.research_table.grade." + card.grade()),
                    Component.translatable("gui.feedback.research_table.property." + card.property())), 1, 16, VALUE_COLOUR, false);
            if (card.reagent())
                graphics.drawString(font, Component.translatable("feedback.jei.research.reagent"), 1, 28, VALUE_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.cost"), 1, 50, LABEL_COLOUR, false);
            return;
        }
        graphics.drawString(font, Component.translatable(card.school().itemKey(card.volume())), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.research.blank"), 1, 40, LABEL_COLOUR, false);
        if (card.volume() > 1)
            graphics.drawString(font, Component.translatable("feedback.jei.research.held"), 21, 40, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.research.specimen"), 51, 40, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.research.cost"), 1, 51, LABEL_COLOUR, false);
    }
}
