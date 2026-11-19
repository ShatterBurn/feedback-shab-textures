// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.item.CircuitItem;
import io.github.soundgoodizerfan.feedback.process.Circuit;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A circuit slot on a machine. A host that plays a recipe data stick (the Power Cutter, the Auto Engineer) has its circuit built in by the
 * engineering recipe ({@link #fromComponents}, {@link #toComponents}) and works only with a live one ({@link #live}); the hosts that still
 * take a circuit by hand ({@link #seat}) work without one, at their old pace and accuracy: {@code Circuit.NONE} reads zero speed and full
 * noise, which this turns into a pace of 1 and no improvement. A circuit, read at its warmth ({@link Circuit#of(ItemStack, Level)}), sets two things:
 * <ul>
 * <li>pace: playback runs {@code 1 + CIRCUIT_PACE_GAIN x speed} times as fast;</li>
 * <li>accuracy: noise is how often input the stick does not cover goes wrong. A quiet circuit scales the bad odds down and now and then
 * holds off guessing at all ({@link #declines}), so there are fewer improvised or scrapped results; it never refuses by design.</li>
 * </ul>
 * A tubed circuit's heater is the host's: it draws {@link #heaterAmps} extra from the line and warms the circuit with whatever the line
 * delivers ({@link #heat}), exactly as an Electric Controller does.
 */
public final class CircuitSlot {

    private ItemStack stack = ItemStack.EMPTY;
    /** The share of its wanted current the line last delivered, 0 to 1. */
    private float supply;

    public ItemStack get() {
        return stack;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** Whether a hand's stack is a circuit this slot takes. */
    public static boolean accepts(ItemStack in) {
        return in.getItem() instanceof CircuitItem;
    }

    /** Seats one of {@code in}; the circuit it replaces comes back. */
    public ItemStack seat(ItemStack in) {
        ItemStack old = stack;
        stack = in.split(1);
        return old;
    }

    public ItemStack take() {
        ItemStack old = stack;
        stack = ItemStack.EMPTY;
        return old;
    }

    public void supply(float share) {
        supply = share;
    }

    /** The circuit as it is now, at the warmth of its stack. */
    public Circuit read(Level level) {
        return Circuit.of(stack, level);
    }

    /** How many times its old pace the machine plays at: 1 with no circuit. */
    public float pace(Level level) {
        return 1f + MachineTuning.CIRCUIT_PACE_GAIN * read(level).speed();
    }

    /** The scale on the odds of a bad outcome for input the stick does not cover: 1 with no circuit, down to {@code 1 - CIRCUIT_ACCURACY_CUT} for a silent one. */
    public float oddsFactor(Level level) {
        return 1f - MachineTuning.CIRCUIT_ACCURACY_CUT * (1f - read(level).noise());
    }

    /** Whether, this cycle, the machine holds off guessing at input the stick does not cover: never with no circuit, more often the quieter the circuit. */
    public boolean declines(Level level, RandomSource random) {
        return random.nextFloat() > oddsFactor(level);
    }

    /** The gain of the circuit seated, at its warmth. */
    public float gain(Level level) {
        return stack.isEmpty() ? 0f : Circuit.of(stack, level).gain();
    }

    /** Amps the tube heater wants from a line of {@code packetEu}. */
    public float heaterAmps(float packetEu) {
        return packetEu <= 0f ? 0f : Circuit.of(stack).heaterPower() / packetEu;
    }

    /** Warms a tubed circuit with the current the line delivered, and lifts the traces of one taken past their limit. */
    public void heat(Level level, int ticks) {
        if (stack.isEmpty())
            return;
        Circuit.warm(stack, level, ticks, supply);
        Circuit.scorch(stack, level);
    }

    /** Whether a live circuit is in the slot: a host that plays a recipe stick works only with one. */
    public boolean live() {
        return !stack.isEmpty() && Circuit.of(stack).live();
    }

    /** Takes the circuit an engineering recipe built into the machine item (the {@code circuit_stack} component), as the item is placed. */
    public void fromComponents(@org.jetbrains.annotations.Nullable ItemStack built) {
        stack = built == null ? ItemStack.EMPTY : built.copy();
    }

    /** Puts the circuit back on the item the block drops, so it travels with the machine. */
    public void toComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        if (!stack.isEmpty())
            components.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT_STACK.get(), stack.copy());
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Circuit", stack.saveOptional(registries));
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        stack = ItemStack.parseOptional(registries, tag.getCompound("Circuit"));
    }
}
