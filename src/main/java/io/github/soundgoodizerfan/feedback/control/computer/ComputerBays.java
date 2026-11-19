// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** The Computer's drive bays: a few slots that each hold one program medium. */
public class ComputerBays extends SimpleContainer {

    public static final int SIZE = 4;

    private final Runnable changed;
    private final java.util.function.Predicate<ItemStack> takes;

    public ComputerBays(Runnable changed, java.util.function.Predicate<ItemStack> takes) {
        super(SIZE);
        this.changed = changed;
        this.takes = takes;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return takes.test(stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        changed.run();
    }
}
