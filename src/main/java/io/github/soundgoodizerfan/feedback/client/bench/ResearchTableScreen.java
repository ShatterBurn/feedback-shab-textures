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
    private static final int LIST_X = 84, LIST_Y = 38, LIST_W = 142, LIST_H = 150, ROW = 26, TAB_Y = 22, TAB_W = 70;

    private int tabMode = -1;
    private int scroll, descIndex = -1, descMode = -1;
    private List<net.minecraft.util.FormattedCharSequence> desc = List.of();
    private final int[] rowTop = new int[16], rowIndex = new int[16];
    private int rowsShown;
    private net.minecraft.world.item.ItemStack[] icons;
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
        tabs();
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.research_table.research"),
                b -> press(ResearchTableMenu.BTN_RESEARCH), leftPos + 8, topPos + 100, 56, 16, null));
    }

    /** The two tabs, the picked one drawn as the primary (lit) button; rebuilt when the pick changes. */
    private void tabs() {
        if (treatises != null)
            removeWidget(treatises);
        if (attunements != null)
            removeWidget(attunements);
        tabMode = menu.mode();
        var a = Component.translatable("gui.feedback.research_table.tab.treatises");
        var b = Component.translatable("gui.feedback.research_table.tab.attunements");
        treatises = addRenderableWidget(tabMode == 0
                ? GuiStyle.primary(TREATISES, a, x -> press(ResearchTableMenu.BTN_TREATISES), leftPos + LIST_X, topPos + TAB_Y, TAB_W, 14, null)
                : GuiStyle.button(TREATISES, a, x -> press(ResearchTableMenu.BTN_TREATISES), leftPos + LIST_X, topPos + TAB_Y, TAB_W, 14));
        attunements = addRenderableWidget(tabMode == 1
                ? GuiStyle.primary(ATTUNEMENTS, b, x -> press(ResearchTableMenu.BTN_ATTUNEMENTS), leftPos + LIST_X + LIST_W - TAB_W, topPos + TAB_Y, TAB_W, 14, null)
                : GuiStyle.button(ATTUNEMENTS, b, x -> press(ResearchTableMenu.BTN_ATTUNEMENTS), leftPos + LIST_X + LIST_W - TAB_W, topPos + TAB_Y, TAB_W, 14));
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (tabMode != menu.mode())
            tabs();
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        scroll = 0;
        descIndex = -1;
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

    /** The icon of the {@code i}-th entry: a volume's treatise, or a book for an attunement. */
    private net.minecraft.world.item.ItemStack icon(int i) {
        if (menu.mode() == 1)
            return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        if (icons == null) {
            List<int[]> all = Researching.volumes();
            icons = new net.minecraft.world.item.ItemStack[all.size()];
            for (int k = 0; k < icons.length; k++)
                icons[k] = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        io.github.soundgoodizerfan.feedback.Feedback.id(School.values()[all.get(k)[0]].itemId(all.get(k)[1]))));
        }
        return icons[i];
    }

    /** The picked entry's flavour and what it asks for, wrapped; rebuilt only when the pick changes. */
    private void refreshDesc() {
        if (descIndex == menu.index() && descMode == menu.mode())
            return;
        descIndex = menu.index();
        descMode = menu.mode();
        desc = font.split(Component.empty().append(blurb()).append(Component.literal("\n")).append(specimenHint()), LIST_W - 48);
    }

    /** Lays the visible rows out: the picked one is taller, holding its flavour and its specimen line. */
    private void layout(int total) {
        refreshDesc();
        scroll = Mth.clamp(scroll, 0, Math.max(0, total - 1));
        int y = 2;
        rowsShown = 0;
        for (int i = scroll; i < total && rowsShown < rowTop.length; i++) {
            int h = i == menu.index() ? ROW + 10 * desc.size() + 2 : ROW;
            if (y + h > LIST_H - 2 && rowsShown > 0)
                break;
            rowTop[rowsShown] = y;
            rowIndex[rowsShown++] = i;
            y += h;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + ResearchTableMenu.SLOT_X, topPos + ResearchTableMenu.BLANK_Y);
        skin.slot(g, leftPos + ResearchTableMenu.SLOT_X, topPos + ResearchTableMenu.SPECIMEN_Y);
        GuiSkin ls = listSkin();
        ls.panel(g, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
        List<Component> names = names();
        layout(names.size());
        for (int k = 0; k < rowsShown; k++) {
            int i = rowIndex[k];
            boolean picked = i == menu.index();
            int x = leftPos + LIST_X + 3, y = topPos + LIST_Y + rowTop[k];
            int h = picked ? ROW + 10 * desc.size() + 2 : ROW;
            boolean hover = mx >= x && mx < x + LIST_W - 12 && my >= y && my < y + h;
            ls.row(g, x, y, LIST_W - 12, h - 2, picked, hover);
            ls.slot(g, x + 3, y + 3);
            GuiStyle.spinItem(g, icon(i), x + 11, y + 11, 1f, picked || hover);
            String label = font.plainSubstrByWidth(names.get(i).getString().replace(" Treatise", ""), LIST_W - 40);
            g.drawString(font, label, x + 26, y + 9, picked ? ls.accent() | 0xFF000000 : ls.ink(), false);
            if (picked)
                for (int l = 0; l < desc.size(); l++)
                    g.drawString(font, desc.get(l), x + 26, y + ROW + l * 10 - 2, ls.dim(), false);
        }
        if (names.size() > rowsShown || scroll > 0)
            ls.scrollbar(g, leftPos + LIST_X + LIST_W - 8, topPos + LIST_Y + 2, LIST_H - 4, names.size(), Math.max(1, rowsShown), scroll);
        inventory(g, ResearchTableMenu.INV_X, ResearchTableMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        g.drawString(font, Component.translatable("gui.feedback.research_table.blank"), ResearchTableMenu.SLOT_X + 20, ResearchTableMenu.BLANK_Y + 5, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.research_table.specimen"), ResearchTableMenu.SLOT_X + 20, ResearchTableMenu.SPECIMEN_Y + 5, skin.dim(), false);
        int n = menu.note();
        if (n > 0)
            g.drawString(font, Component.translatable("gui.feedback.research_table.note." + n), 10, LIST_Y + LIST_H + 6,
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
        if (settled() && !closing() && button == 0 && mx >= leftPos + LIST_X && mx < leftPos + LIST_X + LIST_W - 8) {
            for (int k = 0; k < rowsShown; k++) {
                int i = rowIndex[k];
                int h = i == menu.index() ? ROW + 10 * desc.size() + 2 : ROW;
                int y = topPos + LIST_Y + rowTop[k];
                if (my >= y && my < y + h) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.BTN_PICK + i);
                    minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(listSkin().click(), 1f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= leftPos + LIST_X && mx < leftPos + LIST_X + LIST_W && my >= topPos + LIST_Y && my < topPos + LIST_Y + LIST_H) {
            scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, names().size() - 1));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
