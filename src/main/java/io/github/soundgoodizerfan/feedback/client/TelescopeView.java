// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularTelescope;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * What looking through a {@link ModularTelescope} does to the view, on the client only: the field of view narrows by the telescope's
 * {@code MAGNIFICATION}, a scope mask frames it (vanilla's own spyglass texture), and the {@code TELESCOPE_BLUR} figure lays a pale haze over
 * it, thicker the softer the image. The server knows nothing of the view.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class TelescopeView {

    private static final ResourceLocation SCOPE = ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png");
    /** The haze at full blur, as a share of opaque. */
    private static final float HAZE_AT_FULL_BLUR = 0.6f;

    private TelescopeView() {
    }

    /** The telescope the player is looking through now, or empty. */
    private static ItemStack looking(LocalPlayer player) {
        if (player == null || !player.isUsingItem())
            return ItemStack.EMPTY;
        ItemStack stack = player.getUseItem();
        return stack.getItem() instanceof ModularTelescope && !ModularStacks.broken(stack) ? stack : ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        ItemStack stack = looking(Minecraft.getInstance().player);
        if (stack.isEmpty())
            return;
        ModularFigures f = ModularStacks.figures(stack);
        float magnification = f == null ? 1f : Math.max(1f, f.get(Fig.MAGNIFICATION));
        event.setNewFovModifier(event.getNewFovModifier() / magnification);
    }

    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ItemStack stack = looking(mc.player);
        if (stack.isEmpty() || !mc.options.getCameraType().isFirstPerson())
            return;
        GuiGraphics g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        int size = Math.min(w, h), x = (w - size) / 2, y = (h - size) / 2;
        g.blit(SCOPE, x, y, 0, 0f, 0f, size, size, size, size);
        g.fill(0, 0, x, h, 0xFF000000);
        g.fill(x + size, 0, w, h, 0xFF000000);
        g.fill(x, 0, x + size, y, 0xFF000000);
        g.fill(x, y + size, x + size, h, 0xFF000000);
        ModularFigures f = ModularStacks.figures(stack);
        float blur = f == null ? 0f : f.get(Fig.TELESCOPE_BLUR);
        int alpha = Math.round(255f * HAZE_AT_FULL_BLUR * Math.max(0f, Math.min(1f, blur)));
        if (alpha > 0)
            g.fill(x, y, x + size, y + size, alpha << 24 | 0xE6E6E6);
    }
}
