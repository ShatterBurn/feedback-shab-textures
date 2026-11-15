// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.client.diagram.DiagramPage;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.vehicle.FlightSensorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Diagram app: the Contraption Diagram of the vehicle a Flight Sensor of the running program sits on.
 * The sensors are the ones the program's Input cards are linked to; a button steps between them. The page
 * is the same {@link DiagramPage} the sensor's own screen uses, with the angle, merge, centre of mass and
 * assembly controls (the per-cause toggles are the screen's).
 */
final class DiagramWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private DiagramPage page;
    private int pick;

    DiagramWindow(ComputerContext ctx) {
        super(24, 8, 300, 250, 230, 200, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.diagram");
    }

    @Override
    public Glyph glyph() {
        return Glyph.DIAGRAM;
    }

    @Override
    protected void build(GuiSkin skin) {
        add(GuiStyle.button(skin, Component.literal("<"), b -> turn(-0.3f, 0), 0, 0, 16, 14), 0, 0);
        add(GuiStyle.button(skin, Component.literal(">"), b -> turn(0.3f, 0), 0, 0, 16, 14), 18, 0);
        add(GuiStyle.button(skin, Component.literal("^"), b -> turn(0, -0.2f), 0, 0, 16, 14), 36, 0);
        add(GuiStyle.button(skin, Component.literal("v"), b -> turn(0, 0.2f), 0, 0, 16, 14), 54, 0);
        add(GuiStyle.checkbox(skin, Component.translatable("screen.feedback.diagram.merge"), () -> page != null && page.merge,
                b -> { if (page != null) page.merge = !page.merge; }, 0, 0, 70), 76, 0);
        add(GuiStyle.checkbox(skin, Component.translatable("screen.feedback.diagram.assembly"), () -> page != null && page.assembly,
                b -> { if (page != null) page.assembly = !page.assembly; }, 0, 0, 84), 150, 0);
        add(GuiStyle.button(skin, Component.translatable("gui.feedback.computer.diagram_next"), b -> { pick++; page = null; }, 0, 0, 50, 14), 240, 0);
    }

    private void turn(float yaw, float pitch) {
        if (page != null)
            page.turn(yaw, pitch);
    }

    /** Positions of the Flight Sensors the running program reads. */
    private List<BlockPos> sensors() {
        List<BlockPos> out = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null)
            return out;
        for (ProgramNode node : ctx.running().nodes())
            if (node instanceof SensorNode s && s.target() != null && level.getBlockEntity(s.target().pos()) instanceof FlightSensorBlockEntity
                    && !out.contains(s.target().pos()))
                out.add(s.target().pos());
        return out;
    }

    @Override
    protected void closed() {
        close();
    }

    private void close() {
        if (page != null) {
            page.close();
            page = null;
        }
    }

    @Override
    protected void tick() {
        List<BlockPos> sensors = sensors();
        if (sensors.isEmpty()) {
            close();
            return;
        }
        BlockPos at = sensors.get(Math.floorMod(pick, sensors.size()));
        if (page == null || !page.sensor.equals(at)) {
            boolean merge = page == null || page.merge, assembly = page != null && page.assembly;
            close();
            page = new DiagramPage(at);
            page.merge = merge;
            page.assembly = assembly;
        }
        page.tick();
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        if (page == null) {
            ui.text(6, 24, Component.translatable("gui.feedback.computer.diagram_none"), skin.dim());
            return;
        }
        int side = Math.max(96, Math.min(ui.w(), ui.h() - 34));
        Component hover = page.draw(ui.graphics(), skin, Minecraft.getInstance().font, ui.ax((ui.w() - side) / 2), ui.ay(20), side,
                mx + ui.ax(0), my + ui.ay(0), 0f);
        ui.text(0, 22 + side, page.massText(), skin.ink());
        if (hover != null)
            ui.text(0, 32 + side, hover, skin.accent());
    }
}
