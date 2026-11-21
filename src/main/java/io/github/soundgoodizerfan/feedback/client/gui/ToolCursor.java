// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * The tool is the cursor: inside a work area the pointer is the hand tool itself (the hammer at the forge, the saw at the stonecutter),
 * its working tip on the click point, tipping forward while the button is held. The tool is the real item from its slot, so nothing is
 * drawn twice and modular tools show as built. The operating-system pointer is hidden only while the tool is drawn; over a slot, a
 * button or anywhere outside the work area the caller releases it and the plain pointer is back, so small controls stay easy to hit.
 * Client only; call {@link #hold} from the end of a screen's render while the pointer is over the work area, {@link #release}
 * otherwise and from the screen's {@code removed}.
 */
public final class ToolCursor {

    private static boolean hidden;

    private ToolCursor() {
    }

    /** Draw {@code tool} as the cursor at ({@code mx}, {@code my}) above everything, and hide the plain pointer. */
    public static void hold(GuiGraphics g, ItemStack tool, int mx, int my) {
        if (tool.isEmpty()) {
            release();
            return;
        }
        setHidden(true);
        boolean pressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
        g.pose().pushPose();
        g.pose().translate(mx, my, 450);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(pressed ? 30f : 0f)); // tips forward on the click, about its tip
        g.renderItem(tool, -3, -3);
        g.pose().popPose();
    }

    /** Give the plain pointer back. Safe to call every frame. */
    public static void release() {
        setHidden(false);
    }

    private static void setHidden(boolean hide) {
        if (hide == hidden)
            return;
        hidden = hide;
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_CURSOR, hide ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
    }
}
