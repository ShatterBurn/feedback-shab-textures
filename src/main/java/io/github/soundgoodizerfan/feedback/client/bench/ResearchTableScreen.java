// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableMenu;
import io.github.soundgoodizerfan.feedback.modular.research.Researching;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Research Table: the blank treatise, the specimen and the Research button down the left; on the right a list of what can be researched, in
 * a skin of its own (the console's for treatises, the mystic's for attunements) with a tab for each, scrolled by the wheel. The line under the list
 * is flavour: what, in the world, the treatise holds. The table's answer to a refused run is the line under that.
 */
public class ResearchTableScreen extends SkinnedScreen<ResearchTableMenu> {

    private static final GuiSkin TREATISES = GuiSkin.CONSOLE, ATTUNEMENTS = GuiSkin.MYSTIC;
    private static final int LIST_X = 84, LIST_Y = 38, LIST_W = 142, ROW = 14, ROWS = 6, TAB_Y = 22, TAB_W = 70;

    private int scroll;
    private Button treatises, attunements;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = ResearchTableMenu.W;
        imageHeight = ResearchTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = ResearchTableMenu.INV_X;
        inventoryLabelY = ResearchTableMenu.INV_Y - 11;
    }

    private GuiSkin listSkin() {
        return menu.mode() == 0 ? TREATISES : ATTUNEMENTS;
    }

    @Override
    protected void init() {
        super.init();
        treatises = addRenderableWidget(GuiStyle.button(TREATISES, Component.translatable("gui.feedback.research_table.tab.treatises"),
                b -> press(ResearchTableMenu.BTN_TREATISES), leftPos + LIST_X, topPos + TAB_Y, TAB_W, 14));
        attunements = addRenderableWidget(GuiStyle.button(ATTUNEMENTS, Component.translatable("gui.feedback.research_table.tab.attunements"),
                b -> press(ResearchTableMenu.BTN_ATTUNEMENTS), leftPos + LIST_X + LIST_W - TAB_W, topPos + TAB_Y, TAB_W, 14));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.research_table.research"),
                b -> press(ResearchTableMenu.BTN_RESEARCH), leftPos + 8, topPos + 100, 56, 16, null));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        treatises.active = menu.mode() != 0;
        attunements.active = menu.mode() != 1;
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        scroll = 0;
    }

    /** The names listed now, in the order the menu indexes them. */
    private List<Component> names() {
        List<Component> out = new ArrayList<>();
        if (menu.mode() == 0) {
            for (int[] v : Researching.volumes())
                out.add(Component.translatable(School.values()[v[0]].itemKey(v[1])));
        } else
            for (var e : Researching.unknown(minecraft.player))
                out.add(Component.translatable("attunement.feedback." + e.id().getPath()));
        return out;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + ResearchTableMenu.SLOT_X, topPos + ResearchTableMenu.BLANK_Y);
        skin.slot(g, leftPos + ResearchTableMenu.SLOT_X, topPos + ResearchTableMenu.SPECIMEN_Y);
        GuiSkin ls = listSkin();
        ls.panel(g, leftPos + LIST_X, topPos + LIST_Y, LIST_W, ROW * ROWS + 8);
        List<Component> names = names();
        scroll = Mth.clamp(scroll, 0, Math.max(0, names.size() - ROWS));
        for (int i = 0; i < ROWS && scroll + i < names.size(); i++) {
            int x = leftPos + LIST_X + 3, y = topPos + LIST_Y + 2 + i * ROW;
            boolean picked = scroll + i == menu.index();
            boolean hover = mx >= x && mx < x + LIST_W - 6 && my >= y && my < y + ROW;
            ls.row(g, x, y, LIST_W - 6, ROW - 1, picked, hover);
            String label = font.plainSubstrByWidth(names.get(scroll + i).getString().replace(" Treatise", ""), LIST_W - 14);
            g.drawString(font, label, x + 3, y + 3, picked ? ls.accent() | 0xFF000000 : ls.ink(), false);
        }
        if (names.size() > ROWS)
            scrollbar(g, LIST_X + LIST_W - 4, LIST_Y + 2, ROW * ROWS, names.size(), ROWS, scroll);
        inventory(g, ResearchTableMenu.INV_X, ResearchTableMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("gui.feedback.research_table.blank"), ResearchTableMenu.SLOT_X + 20, ResearchTableMenu.BLANK_Y + 5, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.research_table.specimen"), ResearchTableMenu.SLOT_X + 20, ResearchTableMenu.SPECIMEN_Y + 5, skin.dim(), false);
        int y = LIST_Y + ROW * ROWS + 12;
        for (var line : font.split(blurb(), imageWidth - 20))
            g.drawString(font, line, 10, y += 10, skin.dim(), false);
        y += 12;
        g.drawString(font, specimenHint(), 10, y, skin.ink(), false);
        int n = menu.note();
        if (n > 0)
            g.drawString(font, Component.translatable("gui.feedback.research_table.note." + n), 10, y + 12,
                    (n >= ResearchTableMenu.NOTE_WRITTEN && n != ResearchTableMenu.NOTE_NO_EARLIER && n != ResearchTableMenu.NOTE_NO_PLAN ? skin.accent() : skin.warn()) | 0xFF000000, false);
    }

    /** Flavour: what, in the world, is in the picked treatise or what the attunement is. */
    private Component blurb() {
        if (menu.mode() == 1) {
            var list = Researching.unknown(minecraft.player);
            return list.isEmpty() ? Component.translatable("gui.feedback.research_table.nothing") : Component.translatable("gui.feedback.research_table.blurb.attunement");
        }
        int[] v = Researching.volume(menu.index());
        return Component.translatable("gui.feedback.research_table.blurb." + School.values()[v[0]].id() + "_" + v[1]);
    }

    /** What the specimen for what is picked must be. */
    private Component specimenHint() {
        if (menu.mode() == 1)
            return Component.translatable("gui.feedback.research_table.hint.any");
        int[] v = Researching.volume(menu.index());
        var school = School.values()[v[0]];
        if (v[1] <= 1)
            return Component.translatable("gui.feedback.research_table.hint.plain");
        return Component.translatable("gui.feedback.research_table.hint.built", Component.translatable(school.itemKey(v[1] - 1)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (settled() && !closing() && button == 0) {
            List<Component> names = names();
            for (int i = 0; i < ROWS && scroll + i < names.size(); i++) {
                int x = leftPos + LIST_X + 3, y = topPos + LIST_Y + 2 + i * ROW;
                if (mx >= x && mx < x + LIST_W - 6 && my >= y && my < y + ROW) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.BTN_PICK + scroll + i);
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(listSkin().click(), 1f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= leftPos + LIST_X && mx < leftPos + LIST_X + LIST_W && my >= topPos + LIST_Y && my < topPos + LIST_Y + ROW * ROWS + 4) {
            scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, names().size() - ROWS));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
