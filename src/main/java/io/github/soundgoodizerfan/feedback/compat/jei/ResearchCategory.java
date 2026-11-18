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
 * What starts a research at the Research Table: for a treatise volume the Blank Treatise and the volume before it (held, not used up); for an
 * attunement the kind of offering and whether a potion stands in. Verbs and words, never the board's answers.
 */
public class ResearchCategory implements IRecipeCategory<ResearchCategory.Card> {

    /**
     * One research: a volume ({@code school} set), an attunement ({@code school} and {@code app} null) or an
     * app ({@code app} set, {@code subject} an item id or {@code #tag}).
     */
    public record Card(School school, int volume, ResourceLocation effect, String grade, String property, boolean reagent, String app, String subject) {
    }

    static final RecipeType<Card> TYPE = RecipeType.create("feedback", "research", Card.class);

    private static final int WIDTH = 168, HEIGHT = 72;
    private static final int TITLE_COLOUR = 0xFF3F3F3F, LABEL_COLOUR = 0xFF7A7A7A, VALUE_COLOUR = 0xFF262626;

    private final IDrawable icon;

    public ResearchCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FModular.RESEARCH_TABLE_ITEM.get()));
    }

    /** Every volume with the types it takes, then every attunement (each is researched into a rite, again for a better one). */
    static List<Card> cards(List<ResearchSyncPayload.Entry> first, Map<ResourceLocation, Attunements.Effect> effects) {
        List<Card> out = new ArrayList<>();
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++)
                out.add(new Card(school, v, null, "", "", false, null, ""));
        for (var e : effects.values()) {
            String grade = e.target() < 0.35f ? "modest" : e.target() < 0.65f ? "good" : e.target() < 0.85f ? "fine" : "exceptional";
            out.add(new Card(null, 0, e.id(), grade, e.offering().orElse("hardness"), e.reagent().isPresent(), null, ""));
        }
        for (var e : first)
            if (e.school().equals("app") && e.types().size() == 2)
                out.add(new Card(null, 0, null, "", "", false, e.types().get(0), e.types().get(1)));
        return out;
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

    /** The stacks an app's subject can be: the item named, or every item of the tag named. */
    private static List<ItemStack> subjects(String subject) {
        List<ItemStack> out = new ArrayList<>();
        if (subject.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(subject.substring(1));
            if (tag != null)
                BuiltInRegistries.ITEM.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag))
                        .ifPresent(set -> set.forEach(h -> out.add(new ItemStack(h.value()))));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(subject);
            if (id != null && BuiltInRegistries.ITEM.containsKey(id))
                out.add(new ItemStack(BuiltInRegistries.ITEM.get(id)));
        }
        return out;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Card card, IFocusGroup focuses) {
        if (card.app() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground().addItemStack(new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.FLOPPY_DISK.get()));
            var subject = builder.addSlot(RecipeIngredientRole.CATALYST, 51, 18).setStandardSlotBackground();
            for (ItemStack s : subjects(card.subject()))
                subject.addItemStack(s);
            builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(io.github.soundgoodizerfan.feedback.control.computer.ComputerApps.disk(card.app()));
            return;
        }
        if (card.school() == null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground().addItemStack(new ItemStack(FModular.BLANK_RITE.get()));
            builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(io.github.soundgoodizerfan.feedback.modular.attunement.Rites.of(card.effect(), 1f));
            return;
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18).setStandardSlotBackground().addItemStack(new ItemStack(FModular.BLANK_TREATISE.get()));
        if (card.volume() > 1)
            builder.addSlot(RecipeIngredientRole.CATALYST, 21, 18).setStandardSlotBackground().addItemStack(Treatises.of(card.school(), card.volume() - 1));
        builder.addOutputSlot(143, 18).setOutputSlotBackground().addItemStack(Treatises.of(card.school(), card.volume()));
    }

    @Override
    public void draw(Card card, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        if (card.app() != null) {
            graphics.drawString(font, Component.translatable("item.feedback.app." + card.app()), 1, 1, TITLE_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.disk"), 1, 40, LABEL_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.subject"), 51, 40, LABEL_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.cost"), 1, 61, LABEL_COLOUR, false);
            return;
        }
        if (card.school() == null) {
            graphics.drawString(font, Component.translatable("attunement.feedback." + card.effect().getPath()), 1, 1, TITLE_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.offering",
                    Component.translatable("gui.feedback.research_table.grade." + card.grade()),
                    Component.translatable("gui.feedback.research_table.property." + card.property())), 1, 40, VALUE_COLOUR, false);
            if (card.reagent())
                graphics.drawString(font, Component.translatable("feedback.jei.research.reagent"), 1, 50, VALUE_COLOUR, false);
            graphics.drawString(font, Component.translatable("feedback.jei.research.cost"), 1, 61, LABEL_COLOUR, false);
            return;
        }
        graphics.drawString(font, Component.translatable(card.school().itemKey(card.volume())), 1, 1, TITLE_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.research.blank"), 1, 40, LABEL_COLOUR, false);
        if (card.volume() > 1)
            graphics.drawString(font, Component.translatable("feedback.jei.research.held"), 21, 40, LABEL_COLOUR, false);
        graphics.drawString(font, Component.translatable("feedback.jei.research.cost"), 1, 61, LABEL_COLOUR, false);
    }
}
