// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FFluids;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * A brewed potion as a fluid. One registered fluid ({@link FFluids#POTION}); what potion it is rides on the stack as the same
 * {@link PotionState} component a bottle carries, so a bottle poured out and filled again is the same state, stamp and all, and fades the
 * same on the lazy clock whether it sits in a bottle or a tank. A bottle is {@link PotionTuning#BOTTLE_MB} of it.
 * <p>
 * Design after Create's potion fluid (MIT, design only: one virtual fluid whose stack carries the potion in a component, a fixed amount per
 * bottle, bottle fill and drain as an item handler that checks the amount). Rejected: its flat-rate effect that ignores the amount, the
 * per-fluid effect handler registry, vanilla potion contents as the carrier, and its water-bottle special case. Create has no fade, so
 * merging two tanks of one brew by weighted potency is ours.
 */
public final class PotionFluids {

    private PotionFluids() {}

    private static DataComponentType<PotionState> type() {
        return FDataComponents.POTION_STATE.get();
    }

    /** {@code amount} mB of the potion {@code state}. */
    public static FluidStack of(PotionState state, int amount) {
        FluidStack stack = new FluidStack(FFluids.POTION.get(), amount);
        stack.set(type(), state);
        return stack;
    }

    /** Whether {@code stack} is potion fluid. */
    public static boolean is(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == FFluids.POTION.get();
    }

    /** The potion a stack carries, or null for anything that is not a potion fluid. */
    public static PotionState state(FluidStack stack) {
        return is(stack) ? stack.get(type()) : null;
    }

    /**
     * What {@code resource} makes with what a tank already holds: the merged stack when both are potion of the same brew, null when they
     * cannot share a tank (the caller refuses the fill).
     */
    public static FluidStack merged(FluidStack held, FluidStack resource, int fill) {
        PotionState a = state(held), b = state(resource);
        if (a == null || b == null || !a.sameBrew(b))
            return null;
        FluidStack out = held.copy();
        out.setAmount(held.getAmount() + fill);
        out.set(type(), a.merge(held.getAmount(), b, fill));
        return out;
    }

    /**
     * A tank that takes any fluid and lets two potions of one brew run together at different stamps (a fresh and an aged bottle). Anything
     * else follows the stock rule: the same fluid and the same components.
     */
    public static class MergingTank extends FluidHandlerItemStack {

        public MergingTank(java.util.function.Supplier<DataComponentType<net.neoforged.neoforge.fluids.SimpleFluidContent>> component, ItemStack container, int capacity) {
            super(component, container, capacity);
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack held = getFluid();
            if (!held.isEmpty() && is(held) && is(resource) && !FluidStack.isSameFluidSameComponents(held, resource)) {
                int fill = Math.min(capacity - held.getAmount(), resource.getAmount());
                FluidStack mixed = fill <= 0 || container.getCount() != 1 ? null : merged(held, resource, fill);
                if (mixed == null)
                    return 0;
                if (action.execute())
                    setFluid(mixed);
                return fill;
            }
            return super.fill(resource, action);
        }
    }

    /** A potion bottle as a one-bottle tank: it drains {@link PotionTuning#BOTTLE_MB} of its potion and is left a glass bottle. Never takes fluid. */
    public static class Full implements IFluidHandlerItem {

        private ItemStack container;

        public Full(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        private FluidStack contents() {
            PotionState state = container.get(type());
            return state == null || container.getCount() != 1 ? FluidStack.EMPTY : of(state, PotionTuning.BOTTLE_MB);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents();
        }

        @Override
        public int getTankCapacity(int tank) {
            return PotionTuning.BOTTLE_MB;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            FluidStack held = contents();
            return !held.isEmpty() && FluidStack.isSameFluidSameComponents(held, resource) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            FluidStack held = contents();
            if (held.isEmpty() || maxDrain < PotionTuning.BOTTLE_MB)
                return FluidStack.EMPTY;
            if (action.execute())
                container = new ItemStack(Items.GLASS_BOTTLE);
            return held;
        }
    }

    /** An empty glass bottle as a one-bottle tank: it takes {@link PotionTuning#BOTTLE_MB} of potion fluid and becomes that potion. Never gives fluid. */
    public static class Empty implements IFluidHandlerItem {

        private ItemStack container;

        public Empty(ItemStack container) {
            this.container = container;
        }

        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return PotionTuning.BOTTLE_MB;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return state(stack) != null;
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            PotionState state = state(resource);
            if (state == null || container.getCount() != 1 || resource.getAmount() < PotionTuning.BOTTLE_MB)
                return 0;
            if (action.execute()) {
                ItemStack bottle = new ItemStack(FItems.POTION.get());
                bottle.set(type(), state);
                container = bottle;
            }
            return PotionTuning.BOTTLE_MB;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
