// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.process.Tube;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A vacuum tube ({@code vacuum_tube}, family TUBE): an envelope, a cathode, a plate and any grids, put together in a crafting grid and
 * still full of air. A vacuum dryer evacuates and seals it ({@code machine/dryer}); the seal gives it its {@link Tube} figures and its
 * character, which the item's name and tooltip say in words. Only a sealed tube mounts on a board ({@code Circuit.part}); one that is
 * taken apart and rebuilt has lost its seal ({@code ModularStacks.write}).
 */
public class ModularTube extends ModularItem {

    public ModularTube(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Tube sealed = stack.get(FDataComponents.TUBE.get());
        return sealed == null ? super.getName(stack)
                : Component.translatable("item.feedback.vacuum_tube.sealed", Component.translatable(sealed.kind().translationKey()), super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Tube sealed = stack.get(FDataComponents.TUBE.get());
        tooltip.add(sealed == null ? Component.translatable("tooltip.feedback.tube.unsealed").withStyle(ChatFormatting.GRAY)
                : Component.translatable("tooltip.feedback.tube.sealed", Component.translatable(vacuumWord(sealed.vacuum()))).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.feedback.role.tube").withStyle(ChatFormatting.GRAY));
    }

    /** The adjective for a vacuum, by the share of the best a seal can hold. */
    static String vacuumWord(float vacuum) {
        return vacuum >= 0.8f ? "feedback.tube.vacuum.hard" : vacuum >= 0.5f ? "feedback.tube.vacuum.good" : "feedback.tube.vacuum.soft";
    }
}
