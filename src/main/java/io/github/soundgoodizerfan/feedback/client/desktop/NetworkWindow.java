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
 * The Network app: the pair this Computer's endernet upgrade is bonded under, and what is on the endernet beyond it. Four tabs. <b>Pair</b> (layer 1)
 * shows the pair (a short tail of its id, the far end's distance and bearing, whether it is carrying, away or dark) and what is linked to the far end,
 * with a Flip button on a switch. <b>Tune</b> (layer 2) steps the channel (1 with no circuit, more for a resonator's selectivity) and the address (a
 * number, 0 for none) of this Computer. <b>Send</b> (layer 3) lists the live addresses on the channel; pick a row and send a message or the program
 * in a chosen bay. <b>Inbox</b> lists what arrived, and what was overheard on the channel. Every click names a row and its key, or steps a tuning by
 * one: the server holds the lists and checks them again. Everything shown is what the server last sent. Without an upgrade the app says so.
 */
final class NetworkWindow extends DesktopWindow {

    private static final int ROWS = 6, ROW = 14, B = 18, TOP = B + 40;

    private final ComputerContext ctx;
    private final Button[] flip = new Button[ROWS];
    private Button channelMinus, channelPlus, addressMinus, addressPlus, sendMessage, sendFile, bayButton, clear;
    private net.minecraft.client.gui.components.EditBox message;
    private net.minecraft.client.gui.components.AbstractWidget tabs;
    private int scroll, mode, selected = -1, bay;

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
        tabs = add(GuiStyle.tabs(skin, 0, 0, w, 14, java.util.List.of(Component.translatable("gui.feedback.computer.network_tab_pair"),
                Component.translatable("gui.feedback.computer.network_tab_tune"), Component.translatable("gui.feedback.computer.network_tab_send"),
                Component.translatable("gui.feedback.computer.network_tab_inbox")), () -> mode, i -> {
                    mode = i;
                    scroll = 0;
                }), 0, 0);
        for (int i = 0; i < ROWS; i++) {
            final int row = i;
            flip[i] = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.network_flip"),
                    b -> flip(row), 0, 0, 36, 12), w - 38, TOP + i * ROW + 1);
        }
        channelMinus = add(GuiStyle.button(skin, Component.literal("-"), b -> act(ComputerBlockEntity.NET_TUNE_CHANNEL, -1, 0, 0, ""), 0, 0, 14, 12), w - 40, B);
        channelPlus = add(GuiStyle.button(skin, Component.literal("+"), b -> act(ComputerBlockEntity.NET_TUNE_CHANNEL, 1, 0, 0, ""), 0, 0, 14, 12), w - 20, B);
        addressMinus = add(GuiStyle.button(skin, Component.literal("-"), b -> act(ComputerBlockEntity.NET_TUNE_ADDRESS, -1, 0, 0, ""), 0, 0, 14, 12), w - 40, B + 16);
        addressPlus = add(GuiStyle.button(skin, Component.literal("+"), b -> act(ComputerBlockEntity.NET_TUNE_ADDRESS, 1, 0, 0, ""), 0, 0, 14, 12), w - 20, B + 16);
        message = add(new net.minecraft.client.gui.components.EditBox(net.minecraft.client.Minecraft.getInstance().font, 0, 0, w, 14, Component.empty()), 0, TOP + ROWS * ROW + 6);
        message.setMaxLength(io.github.soundgoodizerfan.feedback.control.ControlTuning.MESSAGE_MAX);
        sendMessage = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.network_send_message"), b -> sendMessage(), 0, 0, 70, 12), 0, TOP + ROWS * ROW + 24);
        sendFile = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.network_send_file"), b -> sendFile(), 0, 0, 56, 12), 74, TOP + ROWS * ROW + 24);
        bayButton = add(GuiStyle.button(skin, Component.literal("A"), b -> {
            bay = (bay + 1) % io.github.soundgoodizerfan.feedback.control.computer.ComputerBays.SIZE;
            b.setMessage(Component.translatable("gui.feedback.computer.network_bay", String.valueOf((char) ('A' + bay))));
        }, 0, 0, 40, 12), 134, TOP + ROWS * ROW + 24);
        bayButton.setMessage(Component.translatable("gui.feedback.computer.network_bay", "A"));
        clear = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.network_clear"), b -> act(ComputerBlockEntity.NET_CLEAR_INBOX, 0, 0, 0, ""),
                0, 0, 50, 12), w - 52, B);
    }

    @Override
    protected void layout(int cw, int ch) {
        place(tabs, 0, 0);
        tabs.setWidth(cw);
        for (int i = 0; i < ROWS; i++)
            place(flip[i], cw - 38, TOP + i * ROW + 1);
        place(channelMinus, cw - 40, B);
        place(channelPlus, cw - 20, B);
        place(addressMinus, cw - 40, B + 16);
        place(addressPlus, cw - 20, B + 16);
        place(clear, cw - 52, B);
        message.setWidth(cw);
    }

    private void act(int op, int a, int b, int c, String text) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new io.github.soundgoodizerfan.feedback.control.computer.ComputerNetPayload(ctx.menu().getPos(), op, a, b, c, text));
    }

    private void sendMessage() {
        ComputerBlockEntity c = ctx.computer();
        if (c == null || selected < 0 || selected >= c.directory().size() || message.getValue().isBlank())
            return;
        act(ComputerBlockEntity.NET_SEND_MESSAGE, selected, io.github.soundgoodizerfan.feedback.control.endernet.Packets.Row.key(selected, c.directory().get(selected).pos()), 0, message.getValue());
        message.setValue("");
    }

    private void sendFile() {
        ComputerBlockEntity c = ctx.computer();
        if (c == null || selected < 0 || selected >= c.directory().size())
            return;
        act(ComputerBlockEntity.NET_SEND_FILE, selected, io.github.soundgoodizerfan.feedback.control.endernet.Packets.Row.key(selected, c.directory().get(selected).pos()), bay, "");
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
    protected boolean mouseClicked(double lx, double ly, int button) {
        ComputerBlockEntity c = ctx.computer();
        if (mode == 2 && c != null && ly >= TOP && ly < TOP + ROWS * ROW) {
            int i = scroll + (int) ((ly - TOP) / ROW);
            if (i < c.directory().size() && lx < contentW() - 4) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(lx, ly, button);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        ComputerBlockEntity c = ctx.computer();
        int total = c == null ? 0 : mode == 0 ? c.net().entries().size() : mode == 2 ? c.directory().size() : c.inbox().size();
        scroll = Math.max(0, Math.min(Math.max(0, total - ROWS), scroll - (int) Math.signum(wheel)));
        return true;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ComputerBlockEntity computer = ctx.computer();
        for (Button b : flip)
            b.visible = false;
        for (Button b : new Button[] {channelMinus, channelPlus, addressMinus, addressPlus, sendMessage, sendFile, bayButton, clear})
            b.visible = false;
        message.visible = false;
        if (computer == null)
            return;
        ComputerBlockEntity.NetState net = computer.net();
        int w = ui.w();
        if (net.pair() == 0) {
            ui.wrapped(0, B, w, Component.translatable("gui.feedback.computer.network_none"), skin.warn());
            return;
        }
        switch (mode) {
            case 1 -> tune(ui, skin, computer, net, w);
            case 2 -> send(ui, skin, computer, net, w);
            case 3 -> inbox(ui, skin, computer, w);
            default -> pair(ui, skin, net, w);
        }
    }

    private void pair(Ui ui, GuiSkin skin, ComputerBlockEntity.NetState net, int w) {
        String state = switch (net.pair()) {
            case 1 -> "gui.feedback.computer.network_dark";
            case 2 -> "gui.feedback.computer.network_away";
            default -> "gui.feedback.computer.network_linked";
        };
        ui.row(0, B, w, Component.translatable("gui.feedback.computer.network_pair").getString(), net.pairTail(), skin.ink());
        ui.row(0, B + 10, w, Component.translatable("gui.feedback.computer.network_state").getString(), Component.translatable(state).getString(),
                net.pair() == 3 ? skin.accent() : skin.warn());
        ui.row(0, B + 20, w / 2 - 4, Component.translatable("gui.feedback.computer.network_far").getString(),
                net.distance() + " " + bearing(net.dx(), net.dz()), skin.ink());
        ui.row(w / 2 + 4, B + 20, w / 2 - 4, Component.translatable("gui.feedback.computer.network_channels").getString(), String.valueOf(net.channels()), skin.ink());
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

    private void tune(Ui ui, GuiSkin skin, ComputerBlockEntity c, ComputerBlockEntity.NetState net, int w) {
        for (Button b : new Button[] {channelMinus, channelPlus, addressMinus, addressPlus})
            b.visible = true;
        ui.text(0, B + 2, Component.translatable("gui.feedback.computer.network_channel", c.channel(), net.channels()), skin.ink());
        ui.text(0, B + 18, Component.translatable("gui.feedback.computer.network_address", c.address() == 0
                ? Component.translatable("gui.feedback.computer.network_no_address").getString() : String.valueOf(c.address())), skin.ink());
        ui.wrapped(0, B + 38, w, Component.translatable("gui.feedback.computer.network_tune_help"), skin.dim());
    }

    private void send(Ui ui, GuiSkin skin, ComputerBlockEntity c, ComputerBlockEntity.NetState net, int w) {
        message.visible = sendMessage.visible = sendFile.visible = bayButton.visible = true;
        ui.text(0, B + 2, Component.translatable("gui.feedback.computer.network_send_head", c.channel(), c.address()), skin.ink());
        ui.inset(0, TOP - 2, w, ROWS * ROW + 4);
        var rows = c.directory();
        if (c.address() == 0) {
            ui.text(4, TOP + 2, Component.translatable("gui.feedback.computer.network_need_address"), skin.warn());
            return;
        }
        if (rows.isEmpty()) {
            ui.text(4, TOP + 2, Component.translatable("gui.feedback.computer.network_nobody"), skin.dim());
            return;
        }
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            var r = rows.get(scroll + i);
            ui.row(0, TOP + i * ROW, w - 6, ROW, scroll + i == selected, false);
            ui.text(4, TOP + i * ROW + 3, Component.translatable("gui.feedback.computer.network_row", r.address(), r.distance()), skin.ink());
        }
        ui.scrollbar(w - 3, TOP - 2, ROWS * ROW + 4, rows.size(), ROWS, scroll);
    }

    private void inbox(Ui ui, GuiSkin skin, ComputerBlockEntity c, int w) {
        clear.visible = true;
        var mail = c.inbox();
        ui.text(0, B + 2, Component.translatable("gui.feedback.computer.network_inbox_head", mail.size(), io.github.soundgoodizerfan.feedback.control.ControlTuning.INBOX_MAX), skin.ink());
        ui.inset(0, B + 14, w, ROWS * ROW + 40);
        if (mail.isEmpty()) {
            ui.text(4, B + 18, Component.translatable("gui.feedback.computer.network_inbox_empty"), skin.dim());
            return;
        }
        int rows = (ROWS * ROW + 36) / ROW;
        for (int i = 0; i < rows && scroll + i < mail.size(); i++) {
            var m = mail.get(scroll + i);
            String key = m.overheard() ? (m.file() ? "gui.feedback.computer.network_heard_file" : "gui.feedback.computer.network_heard")
                    : m.file() ? "gui.feedback.computer.network_file" : "gui.feedback.computer.network_mail";
            ui.clipped(4, B + 18 + i * ROW, w - 12, Component.translatable(key, m.from(), m.to(), m.text()).getString(), m.overheard() ? skin.dim() : skin.ink());
        }
        ui.scrollbar(w - 3, B + 14, ROWS * ROW + 40, mail.size(), rows, scroll);
    }
}
