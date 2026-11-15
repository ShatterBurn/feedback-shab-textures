// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableMenu;
import io.github.soundgoodizerfan.feedback.modular.research.Researching;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Research Table as an open book. The left page is a list of what can be researched, in a skin of its own (the console's for treatises, the
 * mystic's for attunements) with a tab for each, spinning icons and the wheel to scroll. The right page is the picked one's: its name, what the book
 * is said to hold (a line of what is in it, then a silly line of what it has been used for), what it asks for, then the slots the research is
 * done on (a blank and a specimen for a treatise; an offering and a reagent potion for an attunement) and the button. Inventory underneath.
 */
public class ResearchTableScreen extends SkinnedScreen<ResearchTableMenu> {

    private static final GuiSkin TREATISES = GuiSkin.CONSOLE, ATTUNEMENTS = GuiSkin.MYSTIC;
    private static final int LIST_X = 10, LIST_Y = 28, LIST_W = 136, LIST_H = 142, ROW = 26, TAB_Y = 10, TAB_W = 66, PAGE_X = ResearchTableMenu.PAGE_X, PAGE_W = 138;

    private int tabMode = -1, scroll, descIndex = -1, descMode = -1;
    private List<FormattedCharSequence> nameLines = List.of(), blurbLines = List.of(), hintLines = List.of();
    private Component labelA = Component.empty(), labelB = Component.empty();
    private final int[] rowIndex = new int[16];
    private int rowsShown;
    private ItemStack[] icons;
    private ItemStack book;
    private Button treatises, attunements;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.WOOD);
        imageWidth = ResearchTableMenu.W;
        imageHeight = ResearchTableMenu.H;
        titleLabelX = 4000;
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
                b -> press(ResearchTableMenu.BTN_RESEARCH), leftPos + PAGE_X + 4, topPos + 191, 70, 16, null));
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
        if (id == ResearchTableMenu.BTN_TREATISES || id == ResearchTableMenu.BTN_ATTUNEMENTS) {
            scroll = 0;
            descIndex = -1;
        }
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
    private ItemStack icon(int i) {
        if (menu.mode() == 1) {
            if (book == null)
                book = new ItemStack(Items.ENCHANTED_BOOK);
            return book;
        }
        if (icons == null) {
            List<int[]> all = Researching.volumes();
            icons = new ItemStack[all.size()];
            for (int k = 0; k < icons.length; k++)
                icons[k] = new ItemStack(BuiltInRegistries.ITEM.get(Feedback.id(School.values()[all.get(k)[0]].itemId(all.get(k)[1]))));
        }
        return icons[i];
    }

    /** The right page's words for the picked entry, wrapped once per pick. */
    private void refreshPage(List<Component> names) {
        if (descIndex == menu.index() && descMode == menu.mode())
            return;
        descIndex = menu.index();
        descMode = menu.mode();
        Font f = font;
        nameLines = names.isEmpty() ? List.of() : f.split(Component.empty().append(names.get(Mth.clamp(menu.index(), 0, names.size() - 1))).withStyle(net.minecraft.ChatFormatting.BOLD), PAGE_W);
        blurbLines = f.split(blurb(), PAGE_W);
        hintLines = f.split(hint(), PAGE_W);
        labelA = Component.translatable(menu.mode() == 0 ? "gui.feedback.research_table.blank" : "gui.feedback.research_table.offering");
        labelB = Component.translatable(menu.mode() == 0 ? "gui.feedback.research_table.specimen" : "gui.feedback.research_table.reagent");
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        // the spine
        g.fill(leftPos + 150, topPos + 8, leftPos + 152, topPos + 190, 0x66000000);
        GuiSkin ls = listSkin();
        ls.panel(g, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
        List<Component> names = names();
        refreshPage(names);
        int visible = (LIST_H - 4) / ROW;
        scroll = Mth.clamp(scroll, 0, Math.max(0, names.size() - visible));
        rowsShown = 0;
        for (int k = 0; k < visible && scroll + k < names.size() && k < rowIndex.length; k++) {
            int i = scroll + k;
            rowIndex[rowsShown++] = i;
            boolean picked = i == menu.index();
            int x = leftPos + LIST_X + 3, y = topPos + LIST_Y + 2 + k * ROW;
            boolean hover = mx >= x && mx < x + LIST_W - 12 && my >= y && my < y + ROW;
            ls.row(g, x, y, LIST_W - 12, ROW - 2, picked, hover);
            ls.slot(g, x + 3, y + 3);
            GuiStyle.spinItem(g, icon(i), x + 11, y + 11, 1f, picked || hover);
            String label = names.get(i).getString().replace(" Treatise", "");
            int avail = LIST_W - 46, over = font.width(label) - avail;
            int shift = 0;
            if (over > 0) {
                // marquee: dwell at each end, glide between, ping-pong
                double t = (net.minecraft.Util.getMillis() % (long) (over * 40 + 2400)) / (double) (over * 40 + 2400);
                double ph = t < 0.5 ? t * 2 : 2 - t * 2;
                shift = (int) (Mth.clamp((ph - 0.15) / 0.7, 0, 1) * over);
            }
            g.enableScissor(x + 26, y, x + 26 + avail, y + ROW);
            g.drawString(font, label, x + 26 - shift, y + 9, picked ? ls.accent() | 0xFF000000 : ls.ink(), false);
            g.disableScissor();
        }
        if (names.size() > visible)
            ls.scrollbar(g, leftPos + LIST_X + LIST_W - 8, topPos + LIST_Y + 2, LIST_H - 4, names.size(), visible, scroll);
        skin.slot(g, leftPos + ResearchTableMenu.BLANK_X, topPos + ResearchTableMenu.SLOT_Y);
        skin.slot(g, leftPos + ResearchTableMenu.SPECIMEN_X, topPos + ResearchTableMenu.SLOT_Y);
        inventory(g, ResearchTableMenu.INV_X, ResearchTableMenu.INV_Y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        super.renderLabels(g, mx, my);
        int y = 12;
        for (FormattedCharSequence line : nameLines) {
            g.drawString(font, line, PAGE_X + 4, y, listSkin().accent() | 0xFF000000, false);
            y += 10;
        }
        y += 4;
        for (FormattedCharSequence line : blurbLines) {
            g.drawString(font, line, PAGE_X + 4, y, skin.ink(), false);
            y += 10;
        }
        y += 4;
        for (FormattedCharSequence line : hintLines) {
            g.drawString(font, line, PAGE_X + 4, y, skin.dim(), false);
            y += 10;
        }
        g.drawString(font, labelA, ResearchTableMenu.BLANK_X, ResearchTableMenu.SLOT_Y - 10, skin.dim(), false);
        g.drawString(font, labelB, ResearchTableMenu.SPECIMEN_X, ResearchTableMenu.SLOT_Y - 10, skin.dim(), false);
        int n = menu.note();
        if (n > 0) {
            boolean good = n == ResearchTableMenu.NOTE_WRITTEN || n == ResearchTableMenu.NOTE_LEARNED;
            int ny = LIST_Y + LIST_H + 6;
            for (FormattedCharSequence line : font.split(Component.translatable("gui.feedback.research_table.note." + n), LIST_W)) {
                g.drawString(font, line, LIST_X, ny, (good ? skin.accent() : skin.warn()) | 0xFF000000, false);
                ny += 10;
            }
        }
    }

    /** Flavour: what the book is said to hold, then a silly line of what it has been used for. */
    private Component blurb() {
        if (menu.mode() == 1) {
            var list = Researching.unknown(minecraft.player);
            if (list.isEmpty())
                return Component.translatable("gui.feedback.research_table.nothing");
            return Component.translatable("gui.feedback.research_table.blurb.attuned." + list.get(Math.floorMod(menu.index(), list.size())).id().getPath());
        }
        int[] v = Researching.volume(menu.index());
        return Component.translatable("gui.feedback.research_table.blurb." + School.values()[v[0]].id() + "_" + v[1]);
    }

    /** What the research asks for. */
    private Component hint() {
        if (menu.mode() == 1) {
            var list = Researching.unknown(minecraft.player);
            if (list.isEmpty())
                return Component.empty();
            Attunements.Effect e = list.get(Math.floorMod(menu.index(), list.size()));
            String grade = e.target() < 0.35f ? "modest" : e.target() < 0.65f ? "good" : e.target() < 0.85f ? "fine" : "exceptional";
            Component line = Component.translatable("gui.feedback.research_table.hint.offer", Component.translatable("gui.feedback.research_table.grade." + grade),
                    Component.translatable("gui.feedback.research_table.property." + e.offering().orElse("hardness")));
            return e.reagent().isEmpty() ? line : Component.empty().append(line).append(" ").append(Component.translatable("gui.feedback.research_table.hint.reagent"));
        }
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
                int y = topPos + LIST_Y + 2 + k * ROW;
                if (my >= y && my < y + ROW) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ResearchTableMenu.BTN_PICK + rowIndex[k]);
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(listSkin().click(), 1f));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= leftPos + LIST_X && mx < leftPos + LIST_X + LIST_W && my >= topPos + LIST_Y && my < topPos + LIST_Y + LIST_H) {
            scroll = Mth.clamp(scroll - (int) Math.signum(dy), 0, Math.max(0, names().size() - (LIST_H - 4) / ROW));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }
}
