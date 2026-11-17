// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBays;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerFile;
import io.github.soundgoodizerfan.feedback.control.computer.ComputerFileActionPayload;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * The Internal disk app: the files the Computer keeps itself, with how full it is. Click a file to pick it; Run or Edit a program, Delete
 * a file, or right-click for Copy and Move onto a blank Floppy or USB drive in a bay. A program or an app disk goes the other way from
 * the Drive menu ("Save to internal disk"); an app installed here runs with no disk in a bay.
 */
final class InternalWindow extends DesktopWindow {

    private static final int ROW = 12;

    private final ComputerContext ctx;
    private final ScrollArea scroll = new ScrollArea();
    private Button run, edit, delete;
    private int selected = -1;
    private int firstRowY;

    InternalWindow(ComputerContext ctx) {
        super(30, 12, 190, 126, 150, 90, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.internal");
    }

    @Override
    public Glyph glyph() {
        return Glyph.FILES;
    }

    private List<ComputerBlockEntity.FileInfo> files() {
        ComputerBlockEntity computer = ctx.computer();
        return computer == null ? List.of() : computer.internalFiles();
    }

    @Override
    protected void build(GuiSkin skin) {
        run = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.run"), b -> {
            if (selected >= 0)
                ctx.sendFile(ComputerFileActionPayload.RUN, selected, 0);
        }, 0, 0, 40, 14), 0, 0);
        edit = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.edit"), b -> {
            if (selected >= 0) {
                ctx.sendFile(ComputerFileActionPayload.EDIT, selected, 0);
                desktop().open("editor");
            }
        }, 0, 0, 40, 14), 0, 0);
        delete = add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.delete"), b -> {
            if (selected >= 0)
                ctx.sendFile(ComputerFileActionPayload.DELETE, selected, 0);
        }, 0, 0, 40, 14), 0, 0);
    }

    @Override
    protected void layout(int cw, int ch) {
        place(run, 0, ch - 16);
        place(edit, 44, ch - 16);
        place(delete, 88, ch - 16);
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        List<ComputerBlockEntity.FileInfo> files = files();
        if (selected >= files.size())
            selected = -1;
        boolean program = selected >= 0 && files.get(selected).kind() == ComputerFile.Kind.PROGRAM;
        boolean editing = ctx.computer() != null && ctx.computer().editing();
        run.active = program && !editing;
        edit.active = program && !editing;
        delete.active = selected >= 0 && !editing;

        int w = ui.w();
        ui.text(0, 0, Component.translatable("gui.feedback.computer.internal_count", files.size(), ControlTuning.INTERNAL_DISK_FILES), skin.dim());
        ui.bar(0, 11, w, 4, files.size() / (float) ControlTuning.INTERNAL_DISK_FILES);
        if (files.isEmpty()) {
            ui.wrapped(0, 20, w, Component.translatable("gui.feedback.computer.internal_empty"), skin.dim());
            return;
        }
        int top = 19;
        int y = scroll.begin(ui, 0, top, w, ui.h() - top - 20);
        int start = y;
        firstRowY = y;
        int rowW = scroll.innerWidth();
        for (int i = 0; i < files.size(); i++) {
            ComputerBlockEntity.FileInfo f = files.get(i);
            ui.row(0, y, rowW, ROW - 1, i == selected, false);
            ui.text(3, y + 2, kindMark(f.kind()), f.kind() == ComputerFile.Kind.APP ? skin.accent() : skin.dim());
            ui.clipped(14, y + 2, rowW - 14 - 36, f.name(), skin.ink());
            ui.right(rowW - 3, y + 2, bytes(f.bytes()), skin.dim());
            y += ROW;
        }
        scroll.end(ui, y - start);
    }

    private static String kindMark(ComputerFile.Kind kind) {
        return switch (kind) {
            case PROGRAM -> "P";
            case APP -> "A";
            case NOTE -> "N";
        };
    }

    private static String bytes(int b) {
        return b >= 1024 ? (b / 1024) + "k" : b + "b";
    }

    private int rowAt(double lx, double ly) {
        if (lx < 0 || lx >= contentW() || ly < 19)
            return -1;
        int row = (int) Math.floor((ly - firstRowY) / ROW);
        return row >= 0 && row < files().size() ? row : -1;
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return scroll.scrolled(lx, ly, wheel);
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        int row = rowAt(lx, ly);
        if (row < 0)
            return false;
        selected = row;
        return true;
    }

    @Override
    protected List<ContextMenu.Item> contextItems(double lx, double ly) {
        int row = rowAt(lx, ly);
        if (row < 0)
            return List.of();
        selected = row;
        List<ContextMenu.Item> items = new ArrayList<>();
        for (int drive = 0; drive < ComputerBays.SIZE; drive++) {
            int d = drive;
            String letter = (char) ('A' + drive) + ":";
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.copy_to", letter),
                    () -> ctx.sendFile(ComputerFileActionPayload.COPY, row, d)));
            items.add(new ContextMenu.Item(Component.translatable("gui.feedback.computer.move_to", letter),
                    () -> ctx.sendFile(ComputerFileActionPayload.MOVE, row, d)));
        }
        return items;
    }
}
