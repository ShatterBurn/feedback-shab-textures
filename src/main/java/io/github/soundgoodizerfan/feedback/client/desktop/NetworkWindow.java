// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.endernet.Endernet;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The Network app (endernet layer 1): the pair this Computer's endernet upgrade is bonded under, and the nodes on the far side of it. The top shows
 * the pair (a short tail of its id, the far end's distance and bearing, and whether the pair is carrying, away or dark) and the rows below list
 * what is linked to the far end (machines, computers, robots, ports) with a switch's state. A row with a switch has a Flip button: the click
 * names the row, and the server flips what it listed there, checking the pair again. Everything shown is what the server last sent; messages and
 * files are not built. Without an upgrade the app says so.
 */
final class NetworkWindow extends DesktopWindow {

    private static final int ROWS = 8, ROW = 14, TOP = 40;

    private final ComputerContext ctx;
    private final Button[] flip = new Button[ROWS];
    private int scroll;

    NetworkWindow(ComputerContext ctx) {
        super(36, 8, 250, 170, 220, 130, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.network");
    }

    @Override
    public Glyph glyph() {
        return Glyph.NETWORK;
    }

    @Override
    protected void build(GuiSkin skin) {
        int w = contentW();
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            flip[i] = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.network_flip"),
                    b -> flip(row), 0, 0, 36, 12), w - 38, TOP + i * ROW + 1);
        }
    }

    @Override
    protected void layout(int cw, int ch) {
        for (int i = 0; i < ROWS; i++)
            place(flip[i], cw - 38, TOP + i * ROW + 1);
    }

    /** Names the row, with a check on its place so a list that moved under the click flips nothing. */
    private void flip(int row) {
        ComputerBlockEntity c = ctx.computer();
        int i = scroll + row;
        if (c == null || i >= c.net().entries().size())
            return;
        ctx.send(ComputerActionPayload.FAR_FLIP, ComputerBlockEntity.farKey(i, c.net().entries().get(i).ref().pos()));
    }

    private static String bearing(int dx, int dz) {
        if (Math.abs(dx) < 2 && Math.abs(dz) < 2)
            return "";
        String ns = dz < -Math.abs(dx) / 2 ? "N" : dz > Math.abs(dx) / 2 ? "S" : "";
        String ew = dx > Math.abs(dz) / 2 ? "E" : dx < -Math.abs(dz) / 2 ? "W" : "";
        return ns + ew;
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        ComputerBlockEntity c = ctx.computer();
        int total = c == null ? 0 : c.net().entries().size();
        scroll = Math.max(0, Math.min(Math.max(0, total - ROWS), scroll - (int) Math.signum(wheel)));
        return true;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ComputerBlockEntity computer = ctx.computer();
        for (Button b : flip)
            b.visible = false;
        if (computer == null)
            return;
        ComputerBlockEntity.NetState net = computer.net();
        int w = ui.w();
        if (net.pair() == 0) {
            ui.wrapped(0, 0, w, Component.translatable("gui.feedback.computer.network_none"), skin.warn());
            return;
        }
        String state = switch (net.pair()) {
            case 1 -> "gui.feedback.computer.network_dark";
            case 2 -> "gui.feedback.computer.network_away";
            default -> "gui.feedback.computer.network_linked";
        };
        ui.row(0, 0, w, Component.translatable("gui.feedback.computer.network_pair").getString(), net.pairTail(), skin.ink());
        ui.row(0, 10, w, Component.translatable("gui.feedback.computer.network_state").getString(), Component.translatable(state).getString(),
                net.pair() == 3 ? skin.accent() : skin.warn());
        ui.row(0, 20, w / 2 - 4, Component.translatable("gui.feedback.computer.network_far").getString(),
                net.distance() + " " + bearing(net.dx(), net.dz()), skin.ink());
        ui.row(w / 2 + 4, 20, w / 2 - 4, Component.translatable("gui.feedback.computer.network_channels").getString(), String.valueOf(net.channels()), skin.ink());
        int listH = ROWS * ROW;
        ui.inset(0, TOP - 2, w, listH + 4);
        var entries = net.entries();
        if (entries.isEmpty()) {
            ui.text(4, TOP + 2, Component.translatable("gui.feedback.computer.network_empty"), skin.dim());
            return;
        }
        for (int i = 0; i < ROWS && scroll + i < entries.size(); i++) {
            Endernet.Entry e = entries.get(scroll + i);
            int y = TOP + i * ROW;
            ui.clipped(4, y + 3, w - 4 - 80 - (e.switchable() ? 40 : 0),
                    Component.translatable(e.blockKey()).getString(), skin.ink());
            ui.right(w - (e.switchable() ? 42 : 4), y + 3, Component.translatable("gui.feedback.computer.network_kind." + e.kind().name().toLowerCase(java.util.Locale.ROOT)).getString(), skin.dim());
            if (e.switchable()) {
                ui.text(w - 78, y + 3, Component.translatable(e.engaged() ? "gui.feedback.computer.network_on" : "gui.feedback.computer.network_off"),
                        e.engaged() ? skin.accent() : skin.dim());
                flip[i].visible = true;
                flip[i].active = net.pair() == 3;
            }
        }
        ui.scrollbar(w - 3, TOP - 2, listH + 4, entries.size(), ROWS, scroll);
    }
}
