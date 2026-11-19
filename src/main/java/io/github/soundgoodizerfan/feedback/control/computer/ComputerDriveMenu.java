// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * The physical drive way: the Computer's four bays, the running slot and the player's inventory, an ordinary slot menu. It is where a
 * medium goes in and comes out; what is on the media is the desktop's business (sneak to open this, plain use for the desktop).
 */
public class ComputerDriveMenu extends AbstractContainerMenu {

    public static final int WIDTH = 176, HEIGHT = 168;
    /** Where the bays sit, left to right, then the running slot after a gap. */
    public static final int BAY_X = 20, BAY_Y = 24, BAY_STEP = 26, RUN_X = 20 + 4 * 26 + 8, CIRCUIT_X = RUN_X + 26;
    public static final int INVENTORY_Y = 86, HOTBAR_Y = 144;

    private final BlockPos pos;
    @Nullable
    private final ComputerBlockEntity computer;
    private final Container bays;
    private final Container running;

    public ComputerDriveMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof ComputerBlockEntity c ? c : null);
    }

    public ComputerDriveMenu(int id, Inventory inventory, BlockPos pos, @Nullable ComputerBlockEntity computer) {
        super(FMenus.COMPUTER_DRIVES.get(), id);
        this.pos = pos;
        this.computer = computer;
        this.bays = computer != null ? computer.bays() : new SimpleContainer(ComputerBays.SIZE);
        this.running = computer != null ? computer.core() : new SimpleContainer(1);
        for (int i = 0; i < ComputerBays.SIZE; i++)
            addSlot(new Slot(bays, i, BAY_X + i * BAY_STEP, BAY_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return bays.canPlaceItem(0, stack);
                }
            });
        addSlot(new Slot(running, 0, RUN_X, BAY_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return running.canPlaceItem(0, stack);
            }

            @Override
            public boolean mayPickup(Player player) {
                // a card made from an internal file is the Computer's, not an item
                return !ComputerFiles.isTransient(getItem());
            }
        });
        if (computer != null && computer.core().hasCircuitSlot() && !computer.core().circuitBuiltIn())
            addSlot(new CircuitSlot(computer.core(), CIRCUIT_X, BAY_Y));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, INVENTORY_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, 8 + column * 18, HOTBAR_Y));
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public ComputerBlockEntity getComputer() {
        return computer;
    }

    @Override
    public boolean stillValid(Player player) {
        return computer == null ? player.level().getBlockEntity(pos) instanceof ComputerBlockEntity : computer.core().stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        int firstInventory = slots.size() - 36;
        if (index < firstInventory) {
            if (!moveItemStackTo(held, firstInventory, slots.size(), true))
                return ItemStack.EMPTY;
        } else if (held.is(FTags.PUNCH_CARDS)) {
            if (!moveItemStackTo(held, 0, ComputerBays.SIZE, false))
                return ItemStack.EMPTY;
        } else if (firstInventory > ComputerBays.SIZE + 1 && computer.core().canFitCircuit(held)) {
            if (!moveItemStackTo(held, ComputerBays.SIZE + 1, ComputerBays.SIZE + 2, false))
                return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        return moved;
    }

    /** The circuit slot of the Computer's controller, as a menu slot: a circuit goes in when it is empty, and comes out unless the program running needs the variables its lattice holds. */
    private static final class CircuitSlot extends Slot {

        private final io.github.soundgoodizerfan.feedback.control.controller.ControllerCore core;

        CircuitSlot(io.github.soundgoodizerfan.feedback.control.controller.ControllerCore core, int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
            this.core = core;
        }

        @Override
        public ItemStack getItem() {
            return core.circuit();
        }

        @Override
        public boolean hasItem() {
            return !core.circuit().isEmpty();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return core.canFitCircuit(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return core.canTakeCircuit();
        }

        @Override
        public void set(ItemStack stack) {
            if (stack.isEmpty())
                core.takeCircuit();
            else
                core.fitCircuit(stack);
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            set(stack);
        }

        @Override
        public ItemStack remove(int amount) {
            return core.canTakeCircuit() ? core.takeCircuit() : ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setChanged() {
        }
    }
}
