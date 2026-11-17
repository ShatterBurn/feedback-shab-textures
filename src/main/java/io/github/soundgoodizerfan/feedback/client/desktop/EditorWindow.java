// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerActionPayload;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerBlockEntity;
import io.github.soundgoodizerfan.feedback.control.programmer.ProgrammerLinkedPayload;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.control.program.text.TextPrinter;
import io.github.soundgoodizerfan.feedback.client.screen.ProgramEditor;

import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Editor app: the same {@link ProgramEditor} the Programmer block's screen shows, in a window. It edits whatever medium the Computer
 * has put in its embedded programmer (the Drive window's Edit does), and closing the window commits the edit and sends the medium back to
 * its drive. The editor draws in screen coordinates, so this window hands it the content rectangle each frame and turns the pointer back
 * from content coordinates.
 *
 * <p>The window has two views of the same program: the node canvas, and its text (a {@link ProgramTextPane}). The text is only ever a view:
 * opening it prints the graph canonically ({@code control/program/text}), editing compiles it live, and Apply (or leaving the view, or closing
 * the window) takes a text that compiled as the working copy; a text that does not compile never touches the graph. A printed card shows its
 * text read-only.
 */
final class EditorWindow extends DesktopWindow {

    private final ComputerContext ctx;
    private ProgramEditor editor;
    private GuiSkin skin;
    private int originX, originY;
    private ProgramTextPane pane;
    private boolean textMode;
    private Button toggle, apply, revert;
    private net.minecraft.network.chat.Component note = net.minecraft.network.chat.Component.empty();
    private long noteUntil;

    EditorWindow(ComputerContext ctx) {
        super(0, 0, 380, 180, 300, 140, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.editor");
    }

    @Override
    public Glyph glyph() {
        return Glyph.EDITOR;
    }

    private ProgramEditor.Source source() {
        return new ProgramEditor.Source() {
            @Override
            public BlockPos pos() {
                return ctx.menu().getPos();
            }

            @Override
            public ProgrammerBlockEntity programmer() {
                return ctx.computer() == null ? null : ctx.computer().programmer();
            }

            @Override
            public boolean offerAttached(java.util.function.IntConsumer pick) {
                var computer = ctx.computer();
                if (computer == null || computer.attached().isEmpty() || computer.programmer() == null)
                    return false;
                var items = new java.util.ArrayList<ContextMenu.Item>();
                items.add(new ContextMenu.Item(net.minecraft.network.chat.Component.translatable("gui.feedback.computer.pick_selected"), () -> pick.accept(-1)));
                var level = net.minecraft.client.Minecraft.getInstance().level;
                int i = 0;
                for (var a : computer.attached()) {
                    int index = i++;
                    net.minecraft.network.chat.Component block = level == null ? net.minecraft.network.chat.Component.empty()
                            : level.getBlockState(a.ref().pos()).getBlock().getName();
                    String figure = a.quantity() == io.github.soundgoodizerfan.feedback.instrument.Quantity.STATE ? (a.value() > 0.5f ? "on" : "off")
                            : io.github.soundgoodizerfan.feedback.client.tooltip.Readout.figure(a.quantity(), a.value());
                    items.add(new ContextMenu.Item(net.minecraft.network.chat.Component.empty().append(block)
                            .append(" " + a.quantity().getSerializedName() + " " + figure), () -> pick.accept(index)));
                }
                desktop().openMenuAtPointer(items);
                return true;
            }

            @Override
            public ItemStack medium() {
                ProgrammerBlockEntity p = programmer();
                return p == null ? ItemStack.EMPTY : p.getItem(0);
            }
        };
    }

    @Override
    protected void build(GuiSkin skin) {
        if (editor != null) {
            editor.flush();
            editor = new ProgramEditor(source(), skin);
        }
        this.skin = skin;
        if (editor != null)
            editor.build();
        toggle = add(GuiStyle.button(skin, net.minecraft.network.chat.Component.translatable("gui.feedback.computer.text_view"), b -> toggleView(), 0, 0, 44, 14), 4, 26);
        apply = add(GuiStyle.primary(skin, net.minecraft.network.chat.Component.translatable("gui.feedback.computer.text_apply"), b -> applyText(true), 0, 0, 44, 14,
                io.github.soundgoodizerfan.feedback.registry.FSounds.UI_PUNCH::get), 52, 4);
        revert = add(GuiStyle.button(skin, net.minecraft.network.chat.Component.translatable("gui.feedback.computer.text_revert"), b -> {
            if (pane != null && editor != null)
                pane.load(TextPrinter.print(editor.shownGraph()));
        }, 0, 0, 44, 14), 100, 4);
        pane = pane == null ? new ProgramTextPane() : pane;
    }

    private void say(String key) {
        note = net.minecraft.network.chat.Component.translatable(key);
        noteUntil = net.minecraft.Util.getMillis() + 4000;
    }

    /** Takes the text as the working copy if it compiled. Returns whether it did (or there was nothing to apply). */
    private boolean applyText(boolean say) {
        if (editor == null || pane == null || !textMode)
            return true;
        if (editor.readOnly()) {
            return true;
        }
        if (!pane.result().ok()) {
            if (say)
                say("gui.feedback.computer.text_refused");
            return false;
        }
        if (pane.dirty()) {
            editor.applyText(pane.result().graph());
            pane.load(TextPrinter.print(editor.shownGraph()));
            if (say)
                say("gui.feedback.computer.text_applied");
        }
        return true;
    }

    private void toggleView() {
        if (editor == null || pane == null)
            return;
        if (!textMode) {
            pane.load(TextPrinter.print(editor.shownGraph()));
            textMode = true;
            return;
        }
        // leaving the text: a text that compiled is taken, one that did not is dropped (the graph was never touched)
        if (pane.dirty() && !editor.readOnly() && !pane.result().ok())
            say("gui.feedback.computer.text_discarded");
        else
            applyText(false);
        textMode = false;
    }

    @Override
    protected void opened() {
        editor = new ProgramEditor(source(), skin);
        editor.build();
    }

    @Override
    protected void closed() {
        applyText(false);
        if (editor != null)
            editor.flush();
        editor = null;
        ctx.send(ComputerActionPayload.CLOSE_EDITOR, 0);
    }

    /** Sends an unsent edit before the desktop closes, while the server still has the menu open. */
    void flush() {
        applyText(false);
        if (editor != null)
            editor.flush();
    }

    void applyLink(ProgrammerLinkedPayload payload) {
        if (editor != null)
            editor.applyLink(payload);
    }

    @Override
    protected void tick() {
        if (editor != null)
            editor.tick();
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        originX = ui.ax(0);
        originY = ui.ay(0);
        boolean empty = ctx.computer() == null || ctx.computer().programmer() == null || ctx.computer().programmer().getItem(0).isEmpty();
        toggle.visible = editor != null && !empty;
        toggle.setMessage(net.minecraft.network.chat.Component.translatable(textMode ? "gui.feedback.computer.node_view" : "gui.feedback.computer.text_view"));
        place(toggle, 4, textMode ? 4 : 26);
        apply.visible = revert.visible = textMode && !empty && editor != null;
        apply.active = textMode && editor != null && pane != null && !editor.readOnly() && pane.dirty() && pane.result().ok();
        revert.active = textMode && pane != null && pane.dirty();
        if (empty)
            textMode = false;
        if (textMode && !empty && editor != null) {
            place(apply, 52, 4);
            place(revert, 100, 4);
            ui.plaque(0, 0, ui.w(), 22);
            if (net.minecraft.Util.getMillis() < noteUntil)
                ui.text(150, 7, note, ui.skin().warn());
            else if (editor.readOnly())
                ui.text(150, 7, net.minecraft.network.chat.Component.translatable("gui.feedback.computer.text_printed"), ui.skin().dim());
            else if (pane.dirty())
                ui.text(150, 7, net.minecraft.network.chat.Component.translatable("gui.feedback.computer.text_unapplied"), ui.skin().dim());
            pane.render(ui, mx, my);
            return;
        }
        if (editor == null)
            return;
        ProgrammerBlockEntity programmer = ctx.computer() == null ? null : ctx.computer().programmer();
        if (programmer == null || programmer.getItem(0).isEmpty()) {
            ui.wrapped(4, 4, ui.w() - 8, Component.translatable("gui.feedback.computer.editor_empty"), ui.skin().dim());
            return;
        }
        editor.viewport(originX, originY, ui.w(), ui.h());
        editor.render(ui.graphics(), mx + originX, my + originY, 0f);
    }

    @Override
    protected boolean mouseClicked(double lx, double ly, int button) {
        if (textMode && pane != null)
            return pane.mouseClicked(lx, ly);
        return editor != null && editor.mouseClicked(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseDragged(double lx, double ly, int button, double dx, double dy) {
        return !textMode && editor != null && editor.mouseDragged(lx + originX, ly + originY, button, dx, dy);
    }

    @Override
    protected boolean mouseReleased(double lx, double ly, int button) {
        return !textMode && editor != null && editor.mouseReleased(lx + originX, ly + originY, button);
    }

    @Override
    protected boolean mouseScrolled(double lx, double ly, double wheel) {
        if (textMode && pane != null)
            return pane.mouseScrolled(wheel);
        return editor != null && editor.mouseScrolled(lx + originX, ly + originY, wheel);
    }

    @Override
    protected boolean keyPressed(int key, int scan, int modifiers) {
        if (textMode && pane != null)
            return pane.keyPressed(key, scan, modifiers);
        return editor != null && editor.keyPressed(key, scan, modifiers);
    }

    @Override
    protected boolean charTyped(char c, int modifiers) {
        if (textMode && pane != null)
            return pane.charTyped(c);
        return editor != null && editor.charTyped(c, modifiers);
    }
}
