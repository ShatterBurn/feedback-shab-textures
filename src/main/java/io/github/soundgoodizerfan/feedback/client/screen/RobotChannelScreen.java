// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.net.RobotChannelPickPayload;
import io.github.soundgoodizerfan.feedback.robot.RobotChannel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A robot's or arm's channels as two lists, what it can be told to do and what it can tell: one click picks the channel a Programmer
 * card will link to (what the Data Connector used to step to, one use at a time). An instrument panel ({@link GuiSkin#CONSOLE}), like
 * {@link CreativeSourceScreen}. The channel in hand is marked; the pick goes to the server, which holds it, and the screen closes.
 */
public class RobotChannelScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.CONSOLE;
    private static final int COL_W = 112, GAP = 8, PAD = 12, ROW_H = 13, HEAD_H = 34;

    private final Opening opening = new Opening(SKIN);
    private final List<RobotChannel> actions = java.util.Arrays.stream(RobotChannel.values()).filter(RobotChannel::actuator).toList();
    private final List<RobotChannel> senses = java.util.Arrays.stream(RobotChannel.values()).filter(c -> !c.actuator()).toList();
    private final int selected;

    private RobotChannelScreen(int selected) {
        super(Component.translatable("gui.feedback.robot_channels.title"));
        this.selected = selected;
    }

    public static void open(int selected) {
        Minecraft.getInstance().setScreen(new RobotChannelScreen(selected));
    }

    private int w() {
        return 2 * COL_W + GAP + 2 * PAD;
    }

    private int h() {
        return HEAD_H + Math.max(actions.size(), senses.size()) * ROW_H + PAD + 18;
    }

    private int left() {
        return (width - w()) / 2;
    }

    private int top() {
        return (height - h()) / 2;
    }

    private int colX(int col) {
        return left() + PAD + col * (COL_W + GAP);
    }

    @Override
    protected void init() {
        opening.start();
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(),
                left() + w() / 2 - 30, top() + h() - PAD - 12, 60, 14));
    }

    /** The channel under the pointer, or null. */
    private RobotChannel at(double mx, double my) {
        for (int col = 0; col < 2; col++) {
            List<RobotChannel> list = col == 0 ? actions : senses;
            int i = (int) Math.floor((my - (top() + HEAD_H)) / ROW_H);
            if (mx >= colX(col) && mx < colX(col) + COL_W && my >= top() + HEAD_H && i >= 0 && i < list.size())
                return list.get(i);
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        int x = left(), y = top();
        RobotChannel hover = opening.settled() ? at(mx, my) : null;
        opening.render(g, width, height, x, y, w(), h(), () -> {
            SKIN.panel(g, x, y, w(), h());
            Component t = Component.literal(title.getString().toUpperCase(Locale.ROOT));
            g.drawString(font, t, x + (w() - font.width(t)) / 2, y + 8, SKIN.ink(), false);
            for (int col = 0; col < 2; col++) {
                List<RobotChannel> list = col == 0 ? actions : senses;
                g.drawString(font, Component.translatable("gui.feedback.robot_channels." + (col == 0 ? "actions" : "senses")),
                        colX(col) + 2, y + HEAD_H - 11, SKIN.dim(), false);
                for (int i = 0; i < list.size(); i++) {
                    RobotChannel c = list.get(i);
                    int ry = y + HEAD_H + i * ROW_H;
                    SKIN.row(g, colX(col), ry, COL_W, ROW_H - 1, c.ordinal() == selected, c == hover);
                    g.drawString(font, Component.translatable(c.langKey()), colX(col) + 4, ry + 3,
                            c.ordinal() == selected || c == hover ? SKIN.accent() : SKIN.ink(), false);
                }
            }
            super.render(g, mx, my, partial);
        });
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled())
            return true;
        RobotChannel c = button == 0 ? at(mx, my) : null;
        if (c != null) {
            PacketDistributor.sendToServer(new RobotChannelPickPayload(c.ordinal()));
            onClose();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        return opening.closing() || super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
