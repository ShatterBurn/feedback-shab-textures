// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.basin.CastingBasinMenu;
import io.github.soundgoodizerfan.feedback.client.gui.Gauge;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.HeatGlow;
import io.github.soundgoodizerfan.feedback.client.gui.SkinnedScreen;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;
import io.github.soundgoodizerfan.feedback.client.item.FormTint;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Casting Basin: molds in a column, the first at the top filling first, and a gauge for the
 * open pool beside them, one tick to the ingot. Stone ({@link GuiSkin#STONE}).
 */
public class CastingBasinScreen extends SkinnedScreen<CastingBasinMenu> {

    private static final int GAUGE_X = 130, GAUGE_Y = 18, GAUGE_W = 10, GAUGE_H = 86;

    private final Gauge pool = Gauge.of(() -> menu.poolMb() / (float) MachineTuning.BASIN_POOL_MB).ticks(9).vertical();

    /** Sparks and steam over the molds (panel-relative), and when each mold last gained ingots, for the ring that runs across a bar as it sets. */
    private final Specks specks = new Specks(40);
    private final float[] setAt = new float[CastingBasinBlockEntity.SLOTS];
    private final int[] had = new int[CastingBasinBlockEntity.SLOTS];

    /** The heat of the stack in {@code slot}: its stamped temperature, or 0 for none. */
    private float heatOf(int slot) {
        ItemStack stack = menu.getSlot(slot).getItem();
        var level = Minecraft.getInstance().level;
        return stack.isEmpty() || level == null || !stack.has(FDataComponents.TEMPERATURE.get()) ? 0f : ItemHeat.get(stack, level).value();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        specks.tick(0f);
        for (int i = 0; i < CastingBasinBlockEntity.SLOTS; i++) {
            int count = menu.getSlot(i).getItem().getCount();
            if (count > had[i]) { // a bar has just set: a ring runs across it and a small clink rings
                setAt[i] = GuiStyle.time();
                skin.cue(GuiSkin.Cue.SEAT);
            }
            had[i] = count;
            float tu = heatOf(i);
            int sx = CastingBasinMenu.SLOT_X, sy = CastingBasinMenu.SLOT_Y + i * CastingBasinMenu.SLOT_STEP;
            if (FormTint.glowing(menu.getSlot(i).getItem()))
                HeatGlow.embers(specks, sx, sy, tu);
            if (tu >= 100f)
                HeatGlow.steam(specks, sx, sy);
        }
    }

    public CastingBasinScreen(CastingBasinMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.STONE);
        imageWidth = 176;
        imageHeight = 198;
        inventoryLabelY = CastingBasinMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        for (int i = 0; i < CastingBasinBlockEntity.SLOTS; i++)
        {
            int sx = leftPos + CastingBasinMenu.SLOT_X, sy = topPos + CastingBasinMenu.SLOT_Y + i * CastingBasinMenu.SLOT_STEP;
            skin.slot(g, sx, sy);
            if (FormTint.glowing(menu.getSlot(i).getItem())) // a hot ingot glows in its well
                HeatGlow.slot(g, sx, sy, heatOf(i));
        }
        inventory(g, CastingBasinMenu.INVENTORY_X, CastingBasinMenu.INVENTORY_Y);
        pool.render(g, skin, leftPos + GAUGE_X, topPos + GAUGE_Y, GAUGE_W, GAUGE_H);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        specks.draw(g, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        for (int i = 0; i < CastingBasinBlockEntity.SLOTS; i++) { // the set: one bright ring across the bar
            float age = (GuiStyle.time() - setAt[i]) / 9f;
            if (age >= 0f && age < 1f)
                GuiStyle.ring(g, CastingBasinMenu.SLOT_X + 8, CastingBasinMenu.SLOT_Y + i * CastingBasinMenu.SLOT_STEP + 8, 5 + Math.round(age * 9f), 1,
                        (int) ((1f - age) * 0xC0) << 24 | 0xFFF0C0);
        }
        g.drawString(font, Component.translatable("gui.feedback.casting_basin.molds"), 12, CastingBasinMenu.SLOT_Y + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.casting_basin.pool"), GAUGE_X - 4, GAUGE_Y - 9, skin.dim(), false);
    }
}
