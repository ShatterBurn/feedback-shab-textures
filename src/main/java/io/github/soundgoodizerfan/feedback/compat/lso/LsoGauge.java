// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import sfiomn.legendarysurvivaloverhaul.common.integration.curios.CuriosUtil;
import sfiomn.legendarysurvivaloverhaul.config.Config;
import sfiomn.legendarysurvivaloverhaul.util.AttachmentUtil;

/**
 * The body temperature as a figure, for a player who carries a Feedback temperature instrument. LSO's own icon stays and its
 * words need nothing; the number is what an instrument is for (philosophy 10). It is read to the instrument's step scaled to the
 * body's 0 to 40 ({@link LsoTuning#BODY_STEP_SHARE}) and never finer. With LSO's own thermometer worn, LSO draws its number
 * and this stays out of the way.
 */
final class LsoGauge {

    private LsoGauge() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("lso_body_temperature"), LsoGauge::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator() || !Config.Baked.temperatureEnabled
                || CuriosUtil.isThermometerEquipped)
            return;
        Instrument best = Instruments.reading(player, Quantity.TEMPERATURE);
        if (best == null)
            return;
        float step = best.resolution(Quantity.TEMPERATURE) * LsoTuning.BODY_STEP_SHARE;
        float body = AttachmentUtil.getTempAttachment(player).getTemperatureLevel();
        Component line = Component.translatable("hud.feedback.lso_body", Instruments.format(body, step));
        g.drawString(mc.font, line, g.guiWidth() / 2 - 91, g.guiHeight() - 59, 0xFFFFFF, true);
    }
}
