// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Computer's desktop: no slots at all. The drives and the internal disk are icons down the left, in the manner of a Plasma desktop, and
 * what is on them is read off the block entity the server keeps in step ({@link #volume}). Where a medium goes in and comes out is the
 * physical drive way, {@link ComputerDriveMenu}. Everything the desktop does (windows, monitor, links) is drawn by the screen from the
 * block entity; what changes the world goes through {@link ComputerActionPayload} and {@link ComputerFileActionPayload}.
 */
public class ComputerMenu extends AbstractContainerMenu implements io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerAccess {

    /** The desktop panel's size; the slots below are placed against it. */
    public static final int PANEL_W = 440, PANEL_H = 260;
    /** Where the drive icons' slots sit, relative to the panel: a column down the left of the desktop, a label under each. */
    public static final int ICON_X = 14, ICON_Y = 10, ICON_STEP = 29;
    /** The drive icons' column when the settings put it on the right. */
    public static final int ICON_X_RIGHT = PANEL_W - ICON_X - 16;
    /** The taskbar's height and the gap under the desktop. */
    public static final int BAR_H = 20, BAR_GAP = 4;
    /** The dock: the hotbar, a floating strip centred along the bottom of the desktop, above the taskbar. */
    public static final int DOCK_X = (PANEL_W - 9 * 18) / 2 + 1, DOCK_Y = PANEL_H - BAR_H - BAR_GAP - 30;
    public static final int RUNNING = ComputerBays.SIZE;

    private final BlockPos pos;
    @Nullable
    private final ComputerBlockEntity computer;
    private final boolean iconsRight;

    public ComputerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, pos, inventory.player.level().getBlockEntity(pos) instanceof ComputerBlockEntity c ? c : null);
    }

    public ComputerMenu(int id, Inventory inventory, BlockPos pos, @Nullable ComputerBlockEntity computer) {
        super(FMenus.COMPUTER.get(), id);
        this.pos = pos;
        this.computer = computer;
        this.iconsRight = computer != null && computer.settings().iconsRight();
        if (computer != null && !inventory.player.level().isClientSide) {
            // a medium left in the editor (a restart, a crash) goes home when the next desktop opens on an idle Computer
            if (computer.watching() == 0 && inventory.player instanceof net.minecraft.server.level.ServerPlayer server)
                computer.closeEditor(server);
            computer.watch(1);
        }
    }

    /** The medium in drive {@code index} (0 to 3, or {@link #RUNNING}), as the block entity last said; empty for none. */
    public ItemStack volume(int index) {
        if (computer == null)
            return ItemStack.EMPTY;
        return index == RUNNING ? computer.core().getItem(0) : index >= 0 && index < ComputerBays.SIZE ? computer.bays().getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    /** Where the icon column's slots are, in this menu: a setting read when the desktop opened. */
    public int iconX() {
        return iconsRight ? ICON_X_RIGHT : ICON_X;
    }

    @Nullable
    public ComputerBlockEntity getComputer() {
        return computer;
    }

    @Override
    @Nullable
    public io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity getProgrammer() {
        return computer == null ? null : computer.programmer();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (computer != null && !player.level().isClientSide) {
            computer.watch(-1);
            if (player instanceof net.minecraft.server.level.ServerPlayer server)
                computer.closeEditor(server);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return computer == null ? player.level().getBlockEntity(pos) instanceof ComputerBlockEntity : computer.core().stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
