// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.client.hud.HudReadouts;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A container screen made of a {@link GuiSkin}: it opens and closes the way its material does
 * (a blueprint unrolls), with the skin's sounds, the world dimming behind it as it arrives. Input
 * waits until it is open and stops once it starts closing. The panel itself is the subclass's
 * {@code renderBg}, drawn with {@code skin.panel}.
 */
public abstract class SkinnedScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {

    public final GuiSkin skin;
    private final Opening opening;

    protected SkinnedScreen(M menu, Inventory inventory, Component title, GuiSkin skin) {
        super(menu, inventory, title);
        this.skin = skin;
        this.opening = new Opening(skin);
    }

    @Override
    protected void init() {
        super.init();
        opening.start();
    }

    /**
     * Give the screen a folder-tab strip above its panel's left edge ({@link FolderTabs}). Call it from {@code init} right after
     * {@code super.init()} and before positioning widgets: it moves the panel down if the band above it would leave the screen. Fewer than
     * two tabs adds nothing. Returns the strip, or null.
     */
    protected FolderTabs folderTabs(List<FolderTabs.Def> tabs, IntSupplier selected, IntConsumer select) {
        if (tabs.size() < 2)
            return null;
        topPos = Math.max(topPos + FolderTabs.HEIGHT / 2, FolderTabs.HEIGHT);
        tabStrip = addRenderableWidget(new FolderTabs(skin, leftPos + 10, topPos - FolderTabs.HEIGHT, tabs, selected, select)
                .host(leftPos, topPos, imageWidth, imageHeight));
        return tabStrip;
    }

    protected boolean settled() {
        return opening.settled();
    }

    /** Closing has begun: nothing typed or clicked may still edit what the screen shows. */
    protected boolean closing() {
        return opening.closing();
    }

    /** A decaying shake of the whole panel (hammer blow, press, stamp): {@code amplitude} pixels at the start, gone in about seven ticks; none under reduce motion. */
    public void impact(float amplitude) {
        impactAt = GuiStyle.time();
        impactAmp = amplitude;
    }

    private static final float IMPACT_TICKS = 7f;
    private float impactAt = Float.NEGATIVE_INFINITY, impactAmp;
    private FolderTabs tabStrip;
    private net.minecraft.client.renderer.texture.DynamicTexture slideTex;
    private int slideDir, slidePw, slidePh;
    private float slideAt;
    private static final net.minecraft.resources.ResourceLocation SLIDE_TEX = io.github.soundgoodizerfan.feedback.Feedback.id("tab_slide");
    /** The panel's border is not part of what slides: this much is left fixed all round. */
    private static final int SLIDE_INSET = 6;

    /**
     * Called by the folder tabs just before the tab changes, {@code dir} being -1 or +1 for the tab's side: takes a picture of the
     * interior as it stands (the last frame is still in the main target) so the change can slide the old widgets off and the new
     * ones on. Nothing for a digital skin or under reduce motion.
     */
    void captureForSlide(int dir) {
        if (dir == 0 || skin.feel().slideTicks() <= 0f || io.github.soundgoodizerfan.feedback.client.ClientSettings.reduceMotion())
            return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        releaseSlide();
        try (var full = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            double k = (double) full.getWidth() / mc.getWindow().getGuiScaledWidth();
            int px = (int) Math.round((leftPos + SLIDE_INSET) * k), py = (int) Math.round((topPos + SLIDE_INSET) * k);
            slidePw = (int) Math.round((imageWidth - 2 * SLIDE_INSET) * k);
            slidePh = (int) Math.round((imageHeight - 2 * SLIDE_INSET) * k);
            if (px < 0 || py < 0 || px + slidePw > full.getWidth() || py + slidePh > full.getHeight())
                return;
            var crop = new com.mojang.blaze3d.platform.NativeImage(slidePw, slidePh, false);
            full.copyRect(crop, px, py, 0, 0, slidePw, slidePh, false, false);
            slideTex = new net.minecraft.client.renderer.texture.DynamicTexture(crop);
            mc.getTextureManager().register(SLIDE_TEX, slideTex);
            slideDir = dir;
            slideAt = GuiStyle.time();
        } catch (RuntimeException e) {
            releaseSlide();
        }
    }

    private void releaseSlide() {
        if (slideTex != null) {
            net.minecraft.client.Minecraft.getInstance().getTextureManager().release(SLIDE_TEX);
            slideTex = null;
        }
    }

    @Override
    public void removed() {
        releaseSlide();
        super.removed();
    }

    /** 0..1 how far the slide has gone, or -1 when none is running. */
    private float slideProgress() {
        if (slideTex == null)
            return -1f;
        Feel feel = skin.feel();
        float p = (GuiStyle.time() - slideAt) / feel.slideTicks();
        if (p >= 1f) {
            releaseSlide();
            if (feel.thunks()) // arrives at speed and stops dead
                impact(1.5f);
            return -1f;
        }
        return feel.slide(p);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        opening.render(g, width, height, leftPos, topPos, imageWidth, imageHeight, () -> {
            g.pose().pushPose();
            float age = GuiStyle.time() - impactAt;
            if (age >= 0 && age < IMPACT_TICKS && !io.github.soundgoodizerfan.feedback.client.ClientSettings.reduceMotion()) { // the whole panel, not the world behind it
                float fall = 1f - age / IMPACT_TICKS;
                g.pose().translate(Math.round(Mth.sin(age * 2.6f) * impactAmp * fall), Math.round(Mth.sin(age * 3.4f + 1f) * impactAmp * fall * 0.7f), 0);
            }
            super.render(g, mouseX, mouseY, partialTick);
            float c = slideProgress();
            if (c >= 0f) { // the interior only: the old sheet slides off, the new one is drawn again beside it and slides on
                int ix = leftPos + SLIDE_INSET, iy = topPos + SLIDE_INSET, iw = imageWidth - 2 * SLIDE_INSET, ih = imageHeight - 2 * SLIDE_INSET;
                int old = -slideDir * Math.round(iw * c);
                g.enableScissor(ix, iy, ix + iw, iy + ih);
                g.blit(SLIDE_TEX, ix + old, iy, iw, ih, 0f, 0f, slidePw, slidePh, slidePw, slidePh);
                g.pose().pushPose();
                g.pose().translate(old + slideDir * iw, 0, 0);
                super.render(g, mouseX, mouseY, partialTick);
                g.pose().popPose();
                g.disableScissor();
            }
            g.pose().popPose();
        });
        drawStatus(g);
        if (settled())
            renderTooltip(g, mouseX, mouseY);
    }

    private Component status = Component.empty();
    private long statusUntil;
    private static final int STATUS_MS = 4000;

    /**
     * A server's word about the open screen (a refusal, a result), said on a strip just under the panel in the screen's skin:
     * vanilla's action bar draws under the screen's dimming, where it is hard to read. Any other screen, or none, gets the
     * HUD readout stack.
     */
    public static void status(Component text) {
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof SkinnedScreen<?> s)
            s.say(text);
        else
            HudReadouts.add(text, 60, 0);
    }

    protected void say(Component text) {
        status = text;
        statusUntil = net.minecraft.Util.getMillis() + STATUS_MS;
    }

    private void drawStatus(GuiGraphics g) {
        long left = statusUntil - net.minecraft.Util.getMillis();
        if (left <= 0)
            return;
        int w = Math.min(imageWidth, font.width(status) + 12), x = leftPos + (imageWidth - w) / 2, y = topPos + imageHeight + 4;
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.setColor(1, 1, 1, Math.min(1f, left / 300f));
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        skin.plaque(g, x, y, w, 15);
        Component shown = font.width(status) <= w - 12 ? status
                : Component.literal(font.plainSubstrByWidth(status.getString(), w - 12 - font.width("...")) + "...");
        g.drawString(font, shown, x + 6, y + 4, skin.ink(), false);
        g.setColor(1, 1, 1, 1);
        g.pose().popPose();
    }

    /** The dimming is drawn in {@link #render}, faded in with the reveal; this draws only the panel. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiSkin.afterPanel = tabStrip == null ? null : () -> tabStrip.body(g); // the tabs sit on the panel, behind all else
        renderBg(g, partialTick, mouseX, mouseY);
        if (GuiSkin.afterPanel != null) { // a screen that drew no panel
            Runnable r = GuiSkin.afterPanel;
            GuiSkin.afterPanel = null;
            r.run();
        }
    }

    /** The player's inventory at (x, y) relative to the screen: three rows, then the hotbar 4 below. */
    protected void inventory(GuiGraphics g, int x, int y) {
        for (int row = 0; row < 4; row++)
            for (int column = 0; column < 9; column++)
                skin.slot(g, leftPos + x + column * 18, topPos + y + row * 18 + (row == 3 ? 4 : 0));
    }

    /** A thin bar at (x, y) down a list {@code h} tall: {@code total} of something, {@code visible} shown from {@code at}. Nothing when it all fits. */
    protected void scrollbar(GuiGraphics g, int x, int y, int h, int total, int visible, int at) {
        skin.scrollbar(g, x, y, h, total, visible, at);
    }

    /** The title and the inventory's label, in the skin's ink. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, skin.ink(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, skin.dim(), false);
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !settled() || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return !settled() || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return !settled() || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (opening.closing())
            return true;
        // A text field is the key's: the inventory key must not close the screen, a number must not swap a hotbar slot.
        if (typing() && key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (getFocused() != null)
                getFocused().keyPressed(key, scan, modifiers);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    /** Whether a text field has the typing focus; a screen with text fields of its own overrides this. */
    protected boolean typing() {
        return getFocused() instanceof net.minecraft.client.gui.components.EditBox box && box.isFocused();
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return opening.closing() || super.charTyped(c, modifiers);
    }
}
