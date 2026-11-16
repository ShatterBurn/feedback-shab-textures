// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The replaceable head of a machine-shop machine other than the Hammer and Press (whose heads are hand-hammer
 * stacks): a roll, a wire die, a lathe or drill bit, an abrasive wheel. One modular type each, family
 * {@code HEAD}; the bake gives a ceiling in {@code St} (how hard the machine may drive it) and the hardness of
 * its edge (what it can cut or draw). Its condition rides the stack ({@code TOOL_CONDITION}), as every head's does.
 */
public class ModularMachineHead extends ModularItem {

    public ModularMachineHead(Properties properties) {
        super(properties);
    }

    /** Whether {@code stack} is a machine head of the type called {@code kind} (the type id, which is the item id). */
    public static boolean is(ItemStack stack, String kind) {
        Item item = stack.getItem();
        return item instanceof ModularMachineHead && BuiltInRegistries.ITEM.getKey(item).getPath().equals(kind);
    }

    /** The most {@code St} the machine may drive this head with; 0 for an empty, broken or foreign stack. */
    public static float ceiling(ItemStack stack) {
        return figure(stack, Fig.STRIKE_ST);
    }

    /** The hardness of the head's edge, in {@code St}; 0 as above. */
    public static float hardness(ItemStack stack) {
        return figure(stack, Fig.HARDNESS);
    }

    private static float figure(ItemStack stack, Fig fig) {
        if (!(stack.getItem() instanceof ModularMachineHead) || ModularStacks.broken(stack))
            return 0f;
        ModularFigures f = ModularStacks.figures(stack);
        return f == null ? 0f : f.get(fig);
    }
}
