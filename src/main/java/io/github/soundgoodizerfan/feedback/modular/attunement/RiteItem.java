// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.attunement;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A rite: the notes on one attunement (its effect and quality, {@link Rites.Rite}). It sits in a chiseled bookshelf by an Enchanting Table and is never used up. */
public class RiteItem extends Item {

    public RiteItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Rites.Rite rite = stack.get(FDataComponents.RITE.get());
        return rite == null ? super.getName(stack)
                : Component.translatable("item.feedback.rite.of", Component.translatable("attunement.feedback." + rite.effect().getPath()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Rites.Rite rite = stack.get(FDataComponents.RITE.get());
        if (rite == null)
            return;
        tooltip.add(Component.translatable("tooltip.feedback.rite").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.feedback.rite.notes." + Rites.maxLevel(rite.quality()) ).withStyle(ChatFormatting.DARK_AQUA));
    }
}
