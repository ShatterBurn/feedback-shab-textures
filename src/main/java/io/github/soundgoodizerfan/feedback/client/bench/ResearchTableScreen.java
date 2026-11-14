// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableMenu;
import io.github.soundgoodizerfan.feedback.modular.research.Researching;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Research Table: the blank treatise and four samples in a row, a button that turns between treatise volumes and attunements not yet
 * known, one that steps through them, and the one that opens the research game. What is picked reads on the line above the buttons with the
 * property its research asks about; the table's answer to a refused run is the line under them.
 */
public class ResearchTableScreen extends SkinnedScreen<ResearchTableMenu> {

    private static final GuiSkin SKIN = GuiSkin.WOOD;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SKIN);
        imageWidth = ResearchTableMenu.W;
        imageHeight = ResearchTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = ResearchTableMenu.INV_X;
        inventoryLabelY = ResearchTableMenu.INV_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.research_table.mode"),
                b -> press(ResearchTableMenu.BTN_MODE), leftPos + 8, topPos + 66, 52, 16));
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.research_table.next"),
                b -> press(ResearchTableMenu.BTN_NEXT), leftPos + 64, topPos + 66, 52, 16));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.research_table.research"),
                b -> press(ResearchTableMenu.BTN_RESEARCH), leftPos + 120, topPos + 66, 48, 16, null));
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + ResearchTableMenu.BLANK_X, topPos + ResearchTableMenu.SLOT_Y);
        skin.slot(g, leftPos + ResearchTableMenu.SPECIMEN_X, topPos + ResearchTableMenu.SLOT_Y);
        inventory(g, ResearchTableMenu.INV_X, ResearchTableMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("gui.feedback.research_table.blank"), ResearchTableMenu.BLANK_X, ResearchTableMenu.SLOT_Y - 10, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.research_table.specimen"), ResearchTableMenu.SPECIMEN_X, ResearchTableMenu.SLOT_Y - 10, skin.dim(), false);
        Component picked = picked();
        g.drawString(font, picked, 10, 48, skin.ink(), false);
        g.drawString(font, specimenHint(), 10, 58, skin.dim(), false);
        int n = menu.note();
        if (n > 0)
            g.drawString(font, Component.translatable("gui.feedback.research_table.note." + n), 10, 88, (n >= ResearchTableMenu.NOTE_WRITTEN ? skin.accent() : skin.warn()) | 0xFF000000, false);
    }

    /** What the specimen for what is picked must be. */
    private Component specimenHint() {
        if (menu.mode() == 1)
            return Component.translatable("gui.feedback.research_table.hint.any");
        int[] v = Researching.volume(menu.index());
        var school = io.github.soundgoodizerfan.feedback.modular.treatise.School.values()[v[0]];
        if (v[1] <= 1) {
            var plan = Researching.plan(0, menu.index());
            return plan == null || plan.specimens.isEmpty() ? Component.translatable("gui.feedback.research_table.hint.plain")
                    : Component.translatable("gui.feedback.research_table.hint.first", String.join(", ", plan.specimens));
        }
        return Component.translatable("gui.feedback.research_table.hint.built", Component.translatable(school.itemKey(v[1] - 1)));
    }

    /** What is picked: a volume's name, or the unknown attunement's; empty-handed when the player knows every attunement. */
    private Component picked() {
        var player = minecraft.player;
        if (menu.mode() == 0) {
            int[] v = Researching.volume(menu.index());
            var school = io.github.soundgoodizerfan.feedback.modular.treatise.School.values()[v[0]];
            return Component.translatable("gui.feedback.research_table.volume", Component.translatable(school.itemKey(v[1])));
        }
        var list = Researching.unknown(player);
        if (list.isEmpty())
            return Component.translatable("gui.feedback.research_table.nothing");
        Attunements.Effect effect = list.get(Math.floorMod(menu.index(), list.size()));
        return Component.translatable("gui.feedback.research_table.attunement", Component.translatable(effect.key()));
    }
}
