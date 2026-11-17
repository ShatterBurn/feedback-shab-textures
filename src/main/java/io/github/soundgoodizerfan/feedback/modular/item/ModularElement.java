// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.Element;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An amethyst resonator or a sculk lattice (family ELEMENT): parts laid in a crafting grid, baked once into an {@link Element} whose fixed
 * figures a circuit board takes when it is soldered. The name and tooltip say its character in a word; the figures are for the component
 * tester once it is on a circuit.
 */
public class ModularElement extends ModularItem {

    public ModularElement(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Element e = stack.get(FDataComponents.ELEMENT.get());
        return e == null ? super.getName(stack)
                : Component.translatable("item.feedback.element.named", Component.translatable(e.character().translationKey()), super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Element e = stack.get(FDataComponents.ELEMENT.get());
        tooltip.add(Component.translatable(e != null && e.kind() == io.github.soundgoodizerfan.feedback.process.Board.Kind.LATTICE
                ? "tooltip.feedback.role.lattice" : "tooltip.feedback.role.resonator").withStyle(ChatFormatting.GRAY));
    }
}
