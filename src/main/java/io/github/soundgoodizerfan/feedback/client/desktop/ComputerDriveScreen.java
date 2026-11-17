// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerDriveMenu;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerSettings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The physical drive way: the four bays, the running slot and the inventory as plain slots, in the skin of the Computer's phosphor. The
 * desktop shows what is on the media; this is where they go in and come out.
 */
public class ComputerDriveScreen extends SkinnedScreen<ComputerDriveMenu> {

    public ComputerDriveScreen(ComputerDriveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, skinOf(menu));
        imageWidth = ComputerDriveMenu.WIDTH;
        imageHeight = ComputerDriveMenu.HEIGHT;
    }

    private static GuiSkin skinOf(ComputerDriveMenu menu) {
        ComputerBlockEntity computer = menu.getComputer();
        ComputerSettings settings = computer == null ? ComputerSettings.DEFAULT : computer.settings();
        return switch (settings.phosphor()) {
            case GREEN -> GuiSkin.TERMINAL;
            case AMBER -> GuiSkin.TERMINAL_AMBER;
            case WHITE -> GuiSkin.TERMINAL_WHITE;
            case BLUE -> GuiSkin.TERMINAL_BLUE;
        };
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.drawString(font, title, leftPos + 8, topPos + 7, skin.ink(), false);
        for (int i = 0; i < ComputerBays.SIZE; i++)
            g.drawString(font, String.valueOf((char) ('A' + i)), leftPos + ComputerDriveMenu.BAY_X + i * ComputerDriveMenu.BAY_STEP + 6,
                    topPos + ComputerDriveMenu.BAY_Y - 10, skin.dim(), false);
        g.drawString(font, "RUN", leftPos + ComputerDriveMenu.RUN_X - 1, topPos + ComputerDriveMenu.BAY_Y - 10, skin.accent(), false);
        if (menu.slots.size() > ComputerBays.SIZE + 1 + 36)
            g.drawString(font, "CIR", leftPos + ComputerDriveMenu.CIRCUIT_X, topPos + ComputerDriveMenu.BAY_Y - 10, skin.dim(), false);
        g.drawString(font, playerInventoryTitle, leftPos + 8, topPos + ComputerDriveMenu.INVENTORY_Y - 11, skin.dim(), false);
        for (var slot : menu.slots)
            skin.slot(g, leftPos + slot.x, topPos + slot.y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
    }
}
