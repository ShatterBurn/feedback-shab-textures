// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A simple metal tool hammered out hot: a striking hand tool (the hammer) and the metal pieces the tool is made of, in
 * any arrangement, with every piece at its metal's working temperature. A cold piece does not match, so the grid offers
 * nothing and nothing is spent; the hammer wears one point per craft like any hand work. Wrenches and crowbars are made
 * this way (heat spec section 2); tongs have their own recipe with the same rule.
 */
public class HotToolRecipe extends CustomRecipe {

    /**
     * What the tool is made of: {@code count} pieces hammered to {@code result}. A piece is either the one item {@code piece}, or any stock
     * of {@code form} (an ingot) whose material is at least {@code minHardness} hard, all of one material: a tool is made of whatever metal
     * is strong enough, never of one named id.
     */
    private final Optional<ResourceLocation> piece;
    private final Optional<String> form;
    private final float minHardness;
    private final int count;
    private final ItemStack result;

    public HotToolRecipe(CraftingBookCategory category, Optional<ResourceLocation> piece, Optional<String> form, float minHardness, int count,
                         ItemStack result) {
        super(category);
        this.piece = piece;
        this.form = form;
        this.minHardness = minHardness;
        this.count = count;
        this.result = result;
    }

    public Optional<ResourceLocation> piece() {
        return piece;
    }

    public Optional<String> form() {
        return form;
    }

    public float minHardness() {
        return minHardness;
    }

    /** Whether {@code stock} is a piece this recipe takes, apart from its being hot: the item, or stock of the form that is hard enough. */
    public boolean takes(ItemStack stock) {
        if (piece.isPresent())
            return BuiltInRegistries.ITEM.getKey(stock.getItem()).equals(piece.get());
        return form.isPresent() && Material.find(io.github.soundgoodizerfan.feedback.process.MaterialIngredient.materialsNow(), stock.getItem())
                .filter(found -> found.form().equals(form.get()) && found.material().hardness().value() >= minHardness).isPresent();
    }

    public int count() {
        return count;
    }

    public ItemStack result() {
        return result;
    }

    public static final class Serializer implements RecipeSerializer<HotToolRecipe> {
        private static final MapCodec<HotToolRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                ResourceLocation.CODEC.optionalFieldOf("piece").forGetter(HotToolRecipe::piece),
                Codec.STRING.optionalFieldOf("form").forGetter(HotToolRecipe::form),
                Codec.FLOAT.optionalFieldOf("min_hardness", 0f).forGetter(HotToolRecipe::minHardness),
                Codec.intRange(1, 9).fieldOf("count").forGetter(HotToolRecipe::count),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(HotToolRecipe::result)).apply(i, HotToolRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, HotToolRecipe> STREAM = StreamCodec.composite(
                CraftingBookCategory.STREAM_CODEC, CustomRecipe::category,
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), HotToolRecipe::piece,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), HotToolRecipe::form,
                ByteBufCodecs.FLOAT, HotToolRecipe::minHardness,
                ByteBufCodecs.VAR_INT, HotToolRecipe::count,
                ItemStack.STREAM_CODEC, HotToolRecipe::result,
                HotToolRecipe::new);

        @Override
        public MapCodec<HotToolRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HotToolRecipe> streamCodec() {
            return STREAM;
        }
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return find(input, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return find(input, null) ? result.copy() : ItemStack.EMPTY;
    }

    /** The tool the grid makes; {@code level} (the clock heat is read against) is null only when assembling, after a match. */
    private boolean find(CraftingInput input, Level level) {
        boolean hammer = false;
        int found = 0;
        net.minecraft.world.item.Item made = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.getItem() instanceof HandTool tool && tool.getStrength(stack).value() > 0f) {
                if (hammer)
                    return false;
                hammer = true;
                continue;
            }
            if (!takes(stack))
                return false;
            // One tool is one metal: pieces of two metals do not make it.
            if (made != null && made != stack.getItem())
                return false;
            made = stack.getItem();
            found++;
            if (level != null && !workable(stack, level))
                return false;
        }
        return hammer && found == count;
    }

    /** Whether {@code stack} is at least as hot as its material needs to be worked. */
    private static boolean workable(ItemStack stack, Level level) {
        return Material.find(ClientMaterials.on(level), stack.getItem())
                .map(found -> ClientMaterials.on(level).get(found.id()))
                .map(m -> ItemHeat.get(stack, level).value() >= m.minTemperature().value())
                .orElse(false);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.HOT_TOOL.get();
    }
}
