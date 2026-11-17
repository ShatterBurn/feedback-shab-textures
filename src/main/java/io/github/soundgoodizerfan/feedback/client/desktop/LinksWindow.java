// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** The Links app: the blocks linked to the Computer, with their positions and link colors. */
final class LinksWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private final ScrollArea scroll = new ScrollArea();

    LinksWindow(ComputerContext ctx) {
        super(100, 30, 200, 120, 140, 80, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.links");
    }

    @Override
    public Glyph glyph() {
        return Glyph.LINKS;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        ProgramGraph graph = ctx.running();
        var computer = ctx.computer();
        java.util.List<DataNodeRef> refs = computer == null ? java.util.List.of() : new java.util.ArrayList<>(computer.getNodeLinks());
        refs.sort(java.util.Comparator.naturalOrder());
        if (refs.isEmpty()) {
            ui.wrapped(0, 0, ui.w(), Component.translatable("gui.feedback.computer.no_links"), skin.dim());
            return;
        }
        int y = scroll.begin(ui, 0, 0, ui.w(), ui.h());
        int top = y;
        int width = scroll.innerWidth() - 4;
        var level = Minecraft.getInstance().level;
        for (DataNodeRef ref : refs) {
            var node = level == null ? null : io.github.soundgoodizerfan.feedback.control.data.DataNode.resolveOrNull(level, ref);
            String kind = node != null && node.dataSpec().writes() ? "out" : "in";
            int color = io.github.soundgoodizerfan.feedback.control.data.DataLinkStore.colorOf(level, computer.getNodeRef(), ref);
            String where = level == null ? "?" : level.getBlockState(ref.pos()).getBlock().getName().getString()
                    + " " + ref.pos().getX() + "," + ref.pos().getY() + "," + ref.pos().getZ()
                    + (color >= 0 ? " (" + net.minecraft.network.chat.Component.translatable("color.minecraft." + net.minecraft.world.item.DyeColor.byId(color).getName()).getString() + ")" : "");
            ui.text(4, y, kind, skin.dim());
            ui.clipped(24, y, width - 20, where, skin.ink());
            y += Ui.LINE;
        }
        scroll.end(ui, y - top + 4);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        return scroll.scrolled(lx, ly, wheel);
    }
}
