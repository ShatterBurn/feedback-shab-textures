// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine-shop machine whose work is a replaceable head's (the Press, Rolling Mill, Wire Drawer, Lathe, Drill
 * Press, Grinding Wheel): the head carries the condition, a stroke that lands nothing wears the head and not the
 * body, and with no head the machine does nothing and wears nothing. Seating and removal are the blocks'.
 */
public abstract class HeadedShopMachineBlockEntity extends ShopMachineBlockEntity implements Headed {

    private final HeadSlot head = new HeadSlot();
    /** An optional circuit on a machine whose output can spoil ({@link #takesCircuit}): its noise cuts the chance a stroke spoils the work. The machine works without one, as it always did. */
    private final CircuitSlot circuit = new CircuitSlot();

    /** Whether this machine has a circuit slot: the ones whose strokes can ruin the workpiece (the Wire Drawer, the Drill Press). */
    protected boolean takesCircuit() {
        return false;
    }

    public CircuitSlot circuit() {
        return circuit;
    }

    /** The scale on this machine's chance of spoiling a stroke: 1 with no circuit, down to {@code 1 - CIRCUIT_ACCURACY_CUT} for a silent one. */
    protected float spoilFactor() {
        return level == null ? 1f : circuit.oddsFactor(level);
    }

    protected HeadedShopMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public HeadSlot getHead() {
        return head;
    }

    /** The modular type id of the head this machine takes (a roll, a wire die...); null for one that takes a hand-hammer head. */
    public String headKind() {
        return null;
    }

    /**
     * A player right-clicks with {@code stack}: if it is this machine's kind of head it is seated and the one it
     * replaces comes back to the player. Returns whether it was a head.
     */
    public boolean tryHead(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (takesCircuit() && CircuitSlot.accepts(stack)) {
            if (!player.level().isClientSide) {
                give(player, circuit.seat(stack));
                sync();
            }
            return true;
        }
        String kind = headKind();
        if (kind == null || !io.github.soundgoodizerfan.feedback.modular.item.ModularMachineHead.is(stack, kind))
            return false;
        if (!player.level().isClientSide)
            give(player, seatHead(stack));
        return true;
    }

    /** The empty-handed take-out once the workpiece is out: the head. */
    public void giveHead(net.minecraft.world.entity.player.Player player) {
        give(player, getHead().isEmpty() ? circuit.take() : removeHead());
    }

    public static void give(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (stack.isEmpty())
            return;
        if (!player.getInventory().add(stack))
            player.drop(stack, false);
    }

    /** Seats one of {@code stack}; the head it replaces comes back. */
    public ItemStack seatHead(ItemStack stack) {
        ItemStack old = head.swap(stack);
        sync();
        return old;
    }

    public ItemStack removeHead() {
        ItemStack taken = head.take();
        sync();
        return taken;
    }

    @Override
    public float getCondition() {
        return head.condition();
    }

    @Override
    protected void wear(float stAbsorbed) {
        if (stAbsorbed <= 0)
            return;
        setChanged();
        if (head.wear(stAbsorbed, MachineTuning.SHOP_MACHINE_WEAR_PER_ST, MachineTuning.SHOP_MACHINE_CONDITION_FLOOR))
            sync();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        head.save(tag, registries);
        circuit.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        head.load(tag, registries);
        circuit.load(tag, registries);
    }
}
