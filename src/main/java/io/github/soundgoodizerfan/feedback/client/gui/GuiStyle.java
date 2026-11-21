// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gui;

import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The widgets and drawing helpers every skinned screen shares: buttons, toggle switches, check
 * boxes, steppers, sliders and tab strips drawn by a {@link GuiSkin}, easing, traced outlines, the drafting board, the stamp, flashing readouts
 * and the HUD card. Drawn with fills, not textures, so there is nothing to license.
 */
public final class GuiStyle {

    private GuiStyle() {}

    public static Button button(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h) {
        return new FButton(skin, false, null, 1f, x, y, w, h, label, press);
    }

    /** A button with its own sound, at a pitch: a stepper's detent rises one way and falls the other. */
    public static Button button(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h,
                                Supplier<SoundEvent> sound, float pitch) {
        return new FButton(skin, false, sound, pitch, x, y, w, h, label, press);
    }

    /** The one button on a screen that commits: drawn heavier, by the skin. */
    public static Button primary(GuiSkin skin, Component label, Button.OnPress press, int x, int y, int w, int h,
                                 Supplier<SoundEvent> sound) {
        return new FButton(skin, true, sound, 1f, x, y, w, h, label, press);
    }

    /** Fast out, slow in: for anything arriving. */
    public static float easeOut(float t) {
        float u = 1 - Mth.clamp(t, 0, 1);
        return 1 - u * u * u;
    }

    /** {@link #easeOut} that swings {@code over} (a fraction of the travel) past the end and back, landing exactly on 1; 0 is plain easeOut. */
    public static float outBack(float t, float over) {
        t = Mth.clamp(t, 0, 1);
        return easeOut(t) + over * Mth.sin(t * Mth.PI);
    }

    /** Where {@code t} sits in the span {@code a} to {@code b} (0 before it, 1 after): staggers beats off one timeline. */
    public static float window(float t, float a, float b) {
        return Mth.clamp((t - a) / (b - a), 0, 1);
    }

    /** 0 up to 1 and back to 0 over {@code t} 0..1: a pulse. */
    public static float hump(float t) {
        return Mth.sin(Mth.clamp(t, 0, 1) * Mth.PI);
    }

    /**
     * A number readout that turns like an odometer when its value changes, in the skin's {@link Feel} (a digital skin just shows it).
     * One per readout; the digits always settle on the exact value, and tooltips and figures are never delayed. Under reduce motion
     * it is plain text.
     */
    public static final class Rolling {
        private String shown = "", before = "";
        private float since = Float.NEGATIVE_INFINITY;

        /** Draw {@code value} with its left edge at {@code x}. */
        public void draw(GuiGraphics g, GuiSkin skin, net.minecraft.client.gui.Font font, int x, int y, int value, int colour) {
            String now = Integer.toString(value);
            if (!now.equals(shown)) {
                before = shown;
                shown = now;
                since = time();
            }
            Feel feel = skin.feel();
            float p = (time() - since) / feel.rollTicks();
            if (!feel.rolls() || ClientSettings.reduceMotion() || p >= 1f || before.isEmpty()) {
                g.drawString(font, now, x, y, colour, false);
                return;
            }
            float e = easeOut(p);
            int up = value > parse(before) ? 1 : -1;
            int pad = Math.max(0, now.length() - before.length());
            for (int i = 0; i < now.length(); i++) {
                char c = now.charAt(i);
                int oi = i - pad - Math.max(0, before.length() - now.length());
                char o = oi >= 0 && oi < before.length() ? before.charAt(oi) : ' ';
                int cx = x + font.width(now.substring(0, i));
                if (o == c) {
                    g.drawString(font, String.valueOf(c), cx, y, colour, false);
                    continue;
                }
                g.enableScissor(cx - 1, y - 1, cx + font.width(String.valueOf(c)) + 1, y + 9);
                g.drawString(font, String.valueOf(o), cx, y - Math.round(up * e * 9), colour, false);
                g.drawString(font, String.valueOf(c), cx, y + Math.round(up * (1 - e) * 9), colour, false);
                g.disableScissor();
            }
        }

        private static int parse(String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    /**
     * The motion clock in ticks: game time plus partial tick, so motion holds still while paused;
     * wall clock (20/s) with no world. A screen stores this when an event happens and passes it back.
     */
    public static float time() {
        var mc = Minecraft.getInstance();
        if (mc.level == null)
            return net.minecraft.Util.getMillis() / 50f;
        return mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
    }

    /** Ticks a shake lasts. */
    private static final float SHAKE_TICKS = 8;
    /** Ticks a press pulse lasts on a skin with no press depth. */
    private static final float PULSE_TICKS = 6;

    /**
     * Sideways pixels to draw a rejecting slot or control at, {@code start} being {@link #time()} when it
     * was rejected: a few pixels either way, dying out over 8 ticks. Always 0 under reduce motion or when
     * {@code start} is not a real time (pass {@link Float#NEGATIVE_INFINITY} for never).
     */
    public static int shake(float start, int amplitude) {
        float age = time() - start;
        if (age < 0 || age >= SHAKE_TICKS || ClientSettings.reduceMotion())
            return 0;
        return Math.round(Mth.sin(age * 2.6f) * amplitude * (1 - age / SHAKE_TICKS));
    }

    /**
     * Part of a rectangle's outline: {@code len} pixels clockwise from {@code from} pixels past its
     * top-left corner. A whole outline drawn by a growing {@code len} is one being traced; short
     * runs stepped along it are a marching dashed line.
     */
    public static void perimeter(GuiGraphics g, int x0, int y0, int x1, int y1, int from, int len, int color) {
        int w = x1 - x0 - 1, h = y1 - y0 - 1, p = 2 * (w + h);
        if (p <= 0 || len <= 0)
            return;
        from = Math.floorMod(from, p);
        while (len > 0) {
            int run;
            if (from < w) { // top, rightward
                run = Math.min(len, w - from);
                g.fill(x0 + from, y0, x0 + from + run, y0 + 1, color);
            } else if (from < w + h) { // right, downward
                int d = from - w;
                run = Math.min(len, h - d);
                g.fill(x1 - 1, y0 + d, x1, y0 + d + run, color);
            } else if (from < 2 * w + h) { // bottom, leftward
                int d = from - w - h;
                run = Math.min(len, w - d);
                g.fill(x1 - 1 - d - run + 1, y1 - 1, x1 - d, y1, color);
            } else { // left, upward
                int d = from - 2 * w - h;
                run = Math.min(len, h - d);
                g.fill(x0, y1 - 1 - d - run + 1, x0 + 1, y1 - d, color);
            }
            len -= run;
            from = (from + run) % p;
        }
    }

    /** A filled disc centred on (cx, cy), drawn a scanline at a time. */
    public static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** A ring of radius {@code r} and thickness {@code t}: a disc with the middle taken out. */
    public static void ring(GuiGraphics g, int cx, int cy, int r, int t, int color) {
        int in = r - t;
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            if (Math.abs(dy) >= in) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
                continue;
            }
            int inner = (int) Math.round(Math.sqrt(in * in - dy * dy + 0.25));
            g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
            g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
        }
    }

    /** A straight line of any slope, drawn as one fill per straight run of pixels (a shallow line is a few wide fills, not hundreds of dots). */
    public static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        if (dx >= dy) {
            int err = dx / 2, start = x0, y = y0;
            for (int x = x0; ; x += sx) {
                if (x == x1) {
                    g.fill(Math.min(start, x), y, Math.max(start, x) + 1, y + 1, color);
                    return;
                }
                err -= dy;
                if (err < 0) {
                    g.fill(Math.min(start, x), y, Math.max(start, x) + 1, y + 1, color);
                    y += sy;
                    err += dx;
                    start = x + sx;
                }
            }
        }
        int err = dy / 2, start = y0, x = x0;
        for (int y = y0; ; y += sy) {
            if (y == y1) {
                g.fill(x, Math.min(start, y), x + 1, Math.max(start, y) + 1, color);
                return;
            }
            err -= dx;
            if (err < 0) {
                g.fill(x, Math.min(start, y), x + 1, Math.max(start, y) + 1, color);
                x += sx;
                err += dy;
                start = y + sy;
            }
        }
    }

    /**
     * A drawing on a {@link GuiSkin#BLUEPRINT} sheet: framed, rulers along the top and left, and
     * while {@code live} a T-square and set-square following the pointer, marked on the rulers.
     */
    public static void drawingBoard(GuiGraphics g, int x, int y, int w, int h, int mouseX, int mouseY, boolean live) {
        int rule = 0x60E8F1FF, mark = GuiSkin.BLUEPRINT.accent();
        g.renderOutline(x, y, w, h, 0x70E8F1FF);
        for (int i = 6; i < w; i += 6)
            g.fill(x + i, y + 1, x + i + 1, y + 1 + (i % 24 == 0 ? 4 : 2), rule);
        for (int i = 6; i < h; i += 6)
            g.fill(x + 1, y + i, x + 1 + (i % 24 == 0 ? 4 : 2), y + i + 1, rule);
        if (live && mouseX > x && mouseX < x + w - 1 && mouseY > y && mouseY < y + h - 1) {
            g.fill(x + 1, mouseY, x + w - 1, mouseY + 1, 0x22FFFFFF);
            g.fill(mouseX, y + 1, mouseX + 1, y + h - 1, 0x22FFFFFF);
            g.fill(mouseX, y + 1, mouseX + 1, y + 7, mark);
            g.fill(x + 1, mouseY, x + 7, mouseY + 1, mark);
        }
    }

    /** A rubber stamp {@code age} ms after it came down at (cx, cy): lands big, settles, then fades. */
    public static void stamp(GuiGraphics g, Component text, float cx, float cy, long age, int rgb) {
        if (age < 0 || age > 1600)
            return;
        float scale = 1 + 0.8f * (1 - easeOut(Math.min(1, age / 110f)));
        int alpha = (int) (255 * Mth.clamp(1 - (age - 1200) / 400f, 0, 1));
        if (alpha < 8)
            return;
        var font = Minecraft.getInstance().font;
        int w = font.width(text) + 10, h = 15, colour = alpha << 24 | rgb & 0xFFFFFF;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 300);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
        pose.scale(scale, scale, 1);
        g.renderOutline(-w / 2, -h / 2, w, h, colour);
        g.renderOutline(-w / 2 + 2, -h / 2 + 2, w - 4, h - 4, colour);
        g.drawString(font, text, -font.width(text) / 2, -3, colour, false);
        pose.popPose();
    }

    /** A HUD card: vanilla's inventory grey with its bevel ({@link GuiSkin#VANILLA}'s look), a little translucent. */
    public static void hudCard(GuiGraphics g, int x0, int y0, int x1, int y1) {
        hudCard(g, x0, y0, x1, y1, 1f);
    }

    /** {@link #hudCard(GuiGraphics, int, int, int, int)} faded to {@code alpha} 0..1, for a card that comes and goes. */
    public static void hudCard(GuiGraphics g, int x0, int y0, int x1, int y1, float alpha) {
        int a = (int) (0xC4 * alpha) << 24; // a little see-through: the world shows behind the card
        g.fill(x0, y0, x1, y1, a);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, a | 0xC6C6C6);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, a | 0xFFFFFF);
        g.fill(x0 + 1, y0 + 1, x0 + 3, y1 - 1, a | 0xFFFFFF);
        g.fill(x0 + 3, y1 - 3, x1 - 1, y1 - 1, a | 0x555555);
        g.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 1, a | 0x555555);
    }

    /** A HUD sliding in from the right whenever what it shows for changes (a tool coming into hand). */
    public static final class Slide {
        private Object showing;
        private long since;

        /** How far right of its place to draw now, for {@code key} the thing it is shown for. */
        public int offset(Object key, int distance) {
            long now = net.minecraft.Util.getMillis();
            if (key != showing) {
                showing = key;
                since = now;
            }
            return Math.round((1 - easeOut((now - since) / 220f)) * distance);
        }
    }

    /** Readouts that flash the accent for a moment when their value changes. */
    public static final class Flash {
        private static final float MS = 450;
        private final String[] shown;
        private final long[] at;

        public Flash(int size) {
            shown = new String[size];
            at = new long[size];
        }

        /** The colour readout {@code i} draws {@code value} in now. */
        public int colour(int i, String value, int ink, int accent) {
            long now = net.minecraft.Util.getMillis();
            if (shown[i] != null && !shown[i].equals(value))
                at[i] = now;
            shown[i] = value;
            float f = Math.max(0, 1 - (now - at[i]) / MS);
            return f > 0 ? GuiSkin.lerp(ink, accent, f) : ink;
        }

        /** Forget what was shown, so the next values do not flash: the readouts now mean something else. */
        public void reset() {
            java.util.Arrays.fill(shown, null);
        }
    }

    /**
     * An instrument panel's toggle switch: a lamp, an engraved label, and a lever thrown up for on.
     * {@code state} is read every frame, so the switch shows what is set, not what was clicked.
     */
    public static Button toggle(GuiSkin skin, Component label, java.util.function.BooleanSupplier state, Button.OnPress press,
                                int x, int y, int w) {
        return new Toggle(skin, label, state, press, x, y, w);
    }


    /**
     * A value between two arrow buttons: {@code left} and {@code right} are the arrows' labels, {@code value}
     * the reading (flashing the accent when it changes), {@code step} told -1 or +1. With {@code plaque}
     * the reading sits on a stamped plate. Sounds are the skin's click unless {@code sound} gives one,
     * pitched down for the left arrow and up for the right, like a detent.
     */
    public static AbstractWidget stepper(GuiSkin skin, int x, int y, int w, int h, String left, String right,
                                         Supplier<String> value, IntConsumer step, boolean plaque, Supplier<SoundEvent> sound) {
        return new Stepper(skin, x, y, w, h, Component.literal(left), Component.literal(right), value, step, plaque, sound);
    }

    /** A check box with its label to the right. {@code state} is read every frame. */
    public static Button checkbox(GuiSkin skin, Component label, BooleanSupplier state, Button.OnPress press,
                                  int x, int y, int w) {
        return new Checkbox(skin, label, state, press, x, y, w);
    }

    /**
     * A slider over {@code min}..{@code max} that snaps to {@code step}. {@code value} is read every
     * frame; {@code set} is told each time the snapped value changes while dragging, and by Left/Right
     * (ten steps with Shift) when focused. Twelve high.
     */
    public static AbstractWidget slider(GuiSkin skin, int x, int y, int w, double min, double max, double step,
                                        DoubleSupplier value, DoubleConsumer set) {
        return new Slider(skin, x, y, w, min, max, step, value, set);
    }

    /** A strip of tabs sharing the width, {@code selected} read every frame; clicking another calls {@code select}. */
    public static AbstractWidget tabs(GuiSkin skin, int x, int y, int w, int h, List<Component> labels,
                                      IntSupplier selected, IntConsumer select) {
        return new Tabs(skin, x, y, w, h, labels, selected, select);
    }

    /** Draws text clipped to {@code width}; text wider than that slides back and forth, pausing at each end. */
    public static void marquee(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int width, int color) {
        int over = font.width(text) - width;
        int off = 0;
        if (over > 0) {
            long period = over * 25L + 1500L; // 40 px/s plus a pause at each end
            long t = net.minecraft.Util.getMillis() % (2 * period);
            long ping = t < period ? t : 2 * period - t;
            off = (int) Math.max(0, Math.min(over, (ping - 750L) / 25L));
        }
        g.enableScissor(x, y - 1, x + width, y + 10);
        g.drawString(font, text, x - off, y, color, false);
        g.disableScissor();
    }

    /** Moves {@code now} a fraction {@code k} (0..1) of the way to {@code target}; the last half pixel snaps, so a glide ends. */
    public static float ease(float now, float target, float k) {
        float d = target - now;
        return Math.abs(d) < 0.5f ? target : now + d * k;
    }

    /**
     * A scroll position in pixels that glides. The wheel moves {@link #by the target}, {@link #update} (once a frame) eases the shown position to
     * it, and {@link #setMax} bounds both. With reduce motion on it jumps. {@link #k} is the step the last update took, for easing other
     * things (a row's height) in step with the scroll.
     */
    public static final class Scroll {
        private float target, now, max, k = 1f;
        private long last;

        public void setMax(float max) {
            this.max = Math.max(0f, max);
            target = Mth.clamp(target, 0f, this.max);
            now = Mth.clamp(now, 0f, this.max);
        }

        public float max() {
            return max;
        }

        public float target() {
            return target;
        }

        /** Scrolls the target by {@code delta} pixels (a wheel notch is a row's height). */
        public void by(float delta) {
            to(target + delta);
        }

        public void to(float position) {
            target = Mth.clamp(position, 0f, max);
        }

        /** Puts both at {@code position} at once: a new list starts at its top, it does not glide there. */
        public void snap(float position) {
            target = now = Mth.clamp(position, 0f, max);
        }

        /** Eases toward the target; call once a frame, before reading {@link #px}. Returns the position. */
        public float update() {
            return update(16f);
        }

        /** As {@link #update()} closing on the target at {@code rate} per second (0 or less jumps): a skin's own scroll feel. */
        public float update(float rate) {
            long t = System.nanoTime();
            float dt = last == 0 ? 0f : Math.min(0.1f, (t - last) / 1e9f);
            last = t;
            k = ClientSettings.reduceMotion() || rate <= 0f ? 1f : 1f - (float) Math.exp(-dt * rate);
            now = ease(now, target, k);
            return now;
        }

        public float k() {
            return k;
        }

        public int px() {
            return Math.round(now);
        }
    }

    /**
     * A list of equal rows that scrolls smoothly in its skin's feel: the one way a screen lists things. Holds the glide; the screen
     * keeps its own rows. Each frame: {@link #size}, {@link #update}, then draw rows {@link #first} on at {@code top - }{@link #offset}
     * between {@link #begin} and {@link #end} (which clip to the view), then {@link #bar}. The wheel goes to {@link #wheel}; a click's
     * row is {@link #rowAt}. {@code rowPx} is a row's height, {@code viewPx} the list's visible height.
     */
    public static final class RowList {
        private final Scroll scroll = new Scroll();
        private final int rowPx;
        private int viewPx, rows;

        public RowList(int rowPx, int viewPx) {
            this.rowPx = rowPx;
            this.viewPx = viewPx;
        }

        /** The visible height, if it can change with the window. */
        public void view(int viewPx) {
            this.viewPx = viewPx;
        }

        /** How many rows there are now (call every frame; it clamps the position when the list shrinks). */
        public void size(int rows) {
            this.rows = rows;
            scroll.setMax(Math.max(0, rows * rowPx - viewPx));
        }

        /** The wheel moved by {@code dy} (vanilla's sign: positive is up): one row a notch. */
        public void wheel(double dy) {
            scroll.by(-(float) Math.signum(dy) * rowPx);
        }

        public void toRow(int row) {
            scroll.to(row * rowPx);
        }

        public void snapTo(int row) {
            scroll.snap(row * rowPx);
        }

        /** Glide one frame in {@code skin}'s feel. */
        public void update(GuiSkin skin) {
            scroll.update(skin.feel().scrollRate());
        }

        public int first() {
            return Math.max(0, scroll.px() / rowPx);
        }

        /** Pixels the first row sits above the list's top edge. */
        public int offset() {
            return scroll.px() % rowPx;
        }

        /** How many rows to draw from {@link #first} to fill the view (one more than fit, for the partly shown one). */
        public int drawn() {
            return Math.min(rows - first(), viewPx / rowPx + 2);
        }

        /** The row under pixel {@code my} of a list whose top is {@code top}, or -1 outside the rows. */
        public int rowAt(double my, int top) {
            if (my < top || my >= top + viewPx)
                return -1;
            int r = (int) ((my - top + scroll.px()) / rowPx);
            return r < rows ? r : -1;
        }

        public void begin(GuiGraphics g, int x, int top, int w) {
            g.enableScissor(x, top, x + w, top + viewPx);
        }

        public void end(GuiGraphics g) {
            g.disableScissor();
        }

        /** The skin's scroll thumb at (x, y), {@code h} tall, following the glide. */
        public void bar(GuiGraphics g, GuiSkin skin, int x, int y, int h) {
            skin.scrollbar(g, x, y, h, rows * rowPx, viewPx, scroll.px());
        }

        public boolean scrolls() {
            return rows * rowPx > viewPx;
        }
    }

    /**
     * Draws an item centred on ({@code cx}, {@code cy}), {@code scale} times its 16 px, and with {@code spin} turning it about its own middle
     * (still with reduce motion on). The depth is squashed so the turn stays in the box instead of swinging through the screen.
     */
    public static void spinItem(GuiGraphics g, net.minecraft.world.item.ItemStack stack, float cx, float cy, float scale, boolean spin) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 50);
        g.pose().scale(scale, scale, 0.05f);
        if (spin && !ClientSettings.reduceMotion()) {
            // renderItem stands the item 150 deep in the pose, so the turn is made about that depth.
            g.pose().translate(0, 0, 150);
            g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(time() * 4f % 360f));
            g.pose().translate(0, 0, -150);
        }
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    /** Eases a 0..1 hover glow toward {@code on}, {@code dt} seconds on. */
    private static float ease(float glow, boolean on, float dt) {
        return glow + Mth.clamp((on ? 1f : 0f) - glow, -dt * 8f, dt * 8f);
    }

    private static final class Stepper extends AbstractWidget {
        private static final int ARROW = 14;
        private final GuiSkin skin;
        private final Component left, right;
        private final Supplier<String> value;
        private final IntConsumer step;
        private final boolean plaque;
        private final Supplier<SoundEvent> sound;
        private final Flash flash = new Flash(1);
        private float glowLeft, glowRight;
        private long last;
        private String cur = "", prev = "";
        private int dir;
        private boolean thudded;
        private float since = Float.NEGATIVE_INFINITY;

        Stepper(GuiSkin skin, int x, int y, int w, int h, Component left, Component right, Supplier<String> value,
                IntConsumer step, boolean plaque, Supplier<SoundEvent> sound) {
            super(x, y, w, h, Component.empty());
            this.skin = skin;
            this.left = left;
            this.right = right;
            this.value = value;
            this.step = step;
            this.plaque = plaque;
            this.sound = sound;
        }

        private int side(double mouseX) {
            return mouseX < getX() + ARROW ? -1 : mouseX >= getX() + width - ARROW ? 1 : 0;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            int over = active && isHovered() ? side(mouseX) : 0;
            glowLeft = ease(glowLeft, over < 0, dt);
            glowRight = ease(glowRight, over > 0, dt);
            boolean pressed = Minecraft.getInstance().mouseHandler.isLeftPressed();
            skin.button(g, getX(), getY(), ARROW, height, left, glowLeft, pressed && over < 0, active, false);
            skin.button(g, getX() + width - ARROW, getY(), ARROW, height, right, glowRight, pressed && over > 0, active, false);
            if (plaque)
                skin.plaque(g, getX() + ARROW + 1, getY(), width - 2 * ARROW - 2, height);
            String v = value.get();
            var font = Minecraft.getInstance().font;
            int room = width - 2 * ARROW - 4;
            if (!v.equals(cur)) { // the reading changed: the old one makes way toward the arrow that was pressed
                prev = cur;
                cur = v;
                since = time();
                thudded = false;
            }
            Feel feel = skin.feel();
            float p = feel.slideTicks() <= 0f || ClientSettings.reduceMotion() || dir == 0 ? 1f : (time() - since) / feel.slideTicks();
            int colour = flash.colour(0, v, skin.ink(), skin.accent());
            if (p >= 1f) {
                float after = time() - since - feel.slideTicks();
                if (feel.thunks() && dir != 0 && !thudded && after >= 0f && after < 1.5f && !ClientSettings.reduceMotion()) {
                    thudded = true;
                    skin.thud();
                } // a heavy crisp skin lands with a one pixel thud, in the direction it was going
                text(g, font, v, room, feel.thunks() && dir != 0 && after >= 0f && after < 1.5f && !ClientSettings.reduceMotion() ? dir : 0, colour);
                return;
            }
            float c = feel.slide(p);
            int span = room + 8;
            g.enableScissor(getX() + ARROW + 1, getY(), getX() + width - ARROW - 1, getY() + height);
            text(g, font, prev, room, Math.round(-dir * span * c), skin.dim());
            text(g, font, v, room, Math.round(dir * span * (1 - c)), colour);
            g.disableScissor();
        }

        private void text(GuiGraphics g, net.minecraft.client.gui.Font font, String v, int room, int dx, int colour) {
            String shown = font.width(v) <= room ? v : font.plainSubstrByWidth(v, room - font.width("...")) + "...";
            g.drawString(font, shown, getX() + (width - font.width(shown)) / 2 + dx, getY() + (height - 8) / 2 + 1, colour, skin == GuiSkin.VANILLA);
        }

        private void press(int dir) {
            GuiSkin.play(sound != null ? sound.get() : skin.click(), dir < 0 ? 0.9f : 1.1f);
            this.dir = dir;
            step.accept(dir);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int dir = side(mouseX);
            if (dir != 0)
                press(dir);
        }

        @Override
        public void playDownSound(SoundManager manager) {
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (active && isFocused() && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
                press(key == InputConstants.KEY_LEFT ? -1 : 1);
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.literal(value.get()));
        }
    }

    private static final class Checkbox extends Button {
        private final GuiSkin skin;
        private final BooleanSupplier state;
        private float glow;
        private long last;

        Checkbox(GuiSkin skin, Component label, BooleanSupplier state, OnPress press, int x, int y, int w) {
            super(x, y, w, 12, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            glow = ease(glow, active && isHoveredOrFocused(), dt);
            boolean on = state.getAsBoolean();
            skin.checkbox(g, getX(), getY() + 1, on, glow);
            int text = !active ? GuiSkin.lerp(skin.dim(), 0xFF000000, 0.35f) : GuiSkin.lerp(skin.dim(), skin.ink(), Math.max(glow, on ? 1 : 0));
            g.drawString(Minecraft.getInstance().font, getMessage(), getX() + 15, getY() + 2, text, false);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(skin.click(), state.getAsBoolean() ? 0.9f : 1.1f);
        }
    }

    private static final class Slider extends AbstractWidget {
        private final GuiSkin skin;
        private final double min, max, step;
        private final DoubleSupplier value;
        private final DoubleConsumer set;
        private float glow;
        private long last;
        private boolean dragging;
        private float shownFrac = Float.NaN;

        Slider(GuiSkin skin, int x, int y, int w, double min, double max, double step, DoubleSupplier value, DoubleConsumer set) {
            super(x, y, w, 12, Component.empty());
            this.skin = skin;
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = value;
            this.set = set;
        }

        private float frac() {
            return (float) Mth.clamp((value.getAsDouble() - min) / (max - min), 0, 1);
        }

        private void put(double v) {
            v = Mth.clamp(Math.round((v - min) / step) * step + min, min, max);
            if (v == value.getAsDouble())
                return;
            GuiSkin.play(skin.click(), 0.8f + 0.4f * (float) ((v - min) / (max - min)));
            set.accept(v);
        }

        private void drag(double mouseX) {
            put(min + (max - min) * Mth.clamp((mouseX - getX() - 3) / (width - 6), 0, 1));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            glow = ease(glow, active && (dragging || isHoveredOrFocused()), dt);
            float target = frac(), rate = skin.feel().followRate();
            shownFrac = Float.isNaN(shownFrac) || rate <= 0f || ClientSettings.reduceMotion() ? target
                    : Math.abs(target - shownFrac) < 0.002f ? target : shownFrac + (target - shownFrac) * (1f - (float) Math.exp(-dt * rate));
            skin.slider(g, getX(), getY(), width, shownFrac, glow, active);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            dragging = true;
            drag(mouseX);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            drag(mouseX);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            dragging = false;
        }

        @Override
        public void playDownSound(SoundManager manager) {
        }

        @Override
        public boolean keyPressed(int key, int scan, int modifiers) {
            if (active && isFocused() && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
                double s = step * (net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1);
                put(value.getAsDouble() + (key == InputConstants.KEY_LEFT ? -s : s));
                return true;
            }
            return super.keyPressed(key, scan, modifiers);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, Component.literal(String.valueOf(value.getAsDouble())));
        }
    }

    private static final class Tabs extends AbstractWidget {
        private final GuiSkin skin;
        private final List<Component> labels;
        private final IntSupplier selected;
        private final IntConsumer select;
        private final float[] glow;
        private long last;

        Tabs(GuiSkin skin, int x, int y, int w, int h, List<Component> labels, IntSupplier selected, IntConsumer select) {
            super(x, y, w, h, Component.empty());
            this.skin = skin;
            this.labels = labels;
            this.selected = selected;
            this.select = select;
            this.glow = new float[labels.size()];
        }

        private int at(double mouseX) {
            return Mth.clamp((int) ((mouseX - getX()) * labels.size() / width), 0, labels.size() - 1);
        }

        /** Tab {@code i}'s left edge and width; the strip's width is shared out whole. */
        private int edge(int i) {
            return getX() + width * i / labels.size();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            int over = active && isHovered() ? at(mouseX) : -1, picked = selected.getAsInt();
            for (int i = 0; i < labels.size(); i++) {
                glow[i] = ease(glow[i], i == over, dt);
                skin.tab(g, edge(i), getY(), edge(i + 1) - edge(i), height, labels.get(i), i == picked, active, glow[i]);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            int i = at(mouseX);
            if (i != selected.getAsInt())
                select.accept(i);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(skin.click(), 1f);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            out.add(NarratedElementType.TITLE, labels.get(Mth.clamp(selected.getAsInt(), 0, labels.size() - 1)));
        }
    }

    private static final class Toggle extends Button {
        private final GuiSkin skin;
        private final java.util.function.BooleanSupplier state;
        private float lever = -1, glow;
        private long last;

        Toggle(GuiSkin skin, Component label, java.util.function.BooleanSupplier state, OnPress press, int x, int y, int w) {
            super(x, y, w, 16, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            boolean on = state.getAsBoolean();
            lever = lever < 0 ? (on ? 1 : 0) : lever + Mth.clamp((on ? 1 : 0) - lever, -dt * 14, dt * 14);
            glow += Mth.clamp((isHoveredOrFocused() ? 1f : 0f) - glow, -dt * 8, dt * 8);
            int x = getX(), y = getY(), w = getWidth();
            if (glow > 0)
                g.fill(x, y, x + w, y + 16, (int) (glow * 0x14) << 24 | 0xFFFFFF);
            // The lamp: dark glass, or lit with a halo.
            int lx = x + 3, ly = y + 4;
            if (on)
                g.fill(lx - 1, ly - 1, lx + 9, ly + 9, 0x40 << 24 | skin.accent() & 0xFFFFFF);
            g.fill(lx, ly, lx + 8, ly + 8, 0xFF0A0C0B);
            g.fill(lx + 1, ly + 1, lx + 7, ly + 7, on ? skin.accent() : 0xFF3A3020);
            g.fill(lx + 2, ly + 2, lx + 4, ly + 3, on ? 0xFFFFF0D0 : 0x30FFFFFF);
            var font = Minecraft.getInstance().font;
            g.drawString(font, getMessage(), x + 16, y + 4, lerp(skin.dim(), skin.ink(), Math.max(glow, on ? 1 : 0)), false);
            // The switch: a chrome nut, and the lever thrown from it.
            int nx = x + w - 12, ny = y + 5;
            g.fill(nx, ny, nx + 7, ny + 7, 0xFF8A908C);
            g.fill(nx + 1, ny + 1, nx + 6, ny + 6, 0xFF5A605C);
            int tip = Math.round(Mth.lerp(lever, 7, -2)), from = 3;
            int y0 = Math.min(ny + from, ny + tip), y1 = Math.max(ny + from, ny + tip) + 1;
            g.fill(nx + 2, y0, nx + 5, y1, 0xFFD8DCD8);
            g.fill(nx + 2, ny + tip, nx + 5, ny + tip + 2, 0xFFFFFFFF);
        }

        private static int lerp(int a, int b, float t) {
            return GuiSkin.lerp(a, b, t);
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(FSounds.UI_TOGGLE.get(), state.getAsBoolean() ? 0.9f : 1.1f);
        }
    }

    private static final class FButton extends Button {
        private final GuiSkin skin;
        private final boolean primary;
        private final Supplier<SoundEvent> sound;
        private final float pitch;
        private float glow;
        private long last;
        private boolean wasDown;
        private float from, shown, moved = Float.NEGATIVE_INFINITY, pressedAt = Float.NEGATIVE_INFINITY;

        FButton(GuiSkin skin, boolean primary, Supplier<SoundEvent> sound, float pitch, int x, int y, int w, int h,
                Component label, OnPress press) {
            super(x, y, w, h, label, press, DEFAULT_NARRATION);
            this.skin = skin;
            this.primary = primary;
            this.sound = sound;
            this.pitch = pitch;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            long now = net.minecraft.Util.getMillis();
            float dt = Math.min(now - last, 100L) / 1000f;
            last = now;
            float target = !active ? 0f : isHoveredOrFocused() ? 1f : 0f;
            glow += Mth.clamp(target - glow, -dt * 8f, dt * 8f);
            boolean down = active && isHovered() && Minecraft.getInstance().mouseHandler.isLeftPressed();
            Feel feel = skin.feel();
            float clock = time();
            if (down != wasDown) { // the press is a pure function of time since the change
                from = shown; // carry on from where it was drawn last
                moved = clock;
                wasDown = down;
            }
            shown = feel.travel(from, down ? feel.pressDepth() : 0f, clock - moved);
            int dx = rejectAge(clock) >= 0 ? shake(skin.rejectAt(), 2) : 0;
            g.pose().pushPose();
            g.pose().translate(dx, Math.round(shown), 0);
            skin.button(g, getX(), getY(), getWidth(), getHeight(), getMessage(), glow, down, active, primary);
            g.pose().popPose();
            // A skin with no press depth lights the face instead: the pulse rises and falls over a few ticks after the press.
            float since = clock - pressedAt;
            if (feel.pulses() && active && since >= 0f && since < PULSE_TICKS && !ClientSettings.reduceMotion())
                g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                        (Math.round(110 * hump(since / PULSE_TICKS)) << 24) | (skin.accent() & 0xFFFFFF));
            float age = rejectAge(clock);
            if (age >= 0 && age < SHAKE_TICKS && !ClientSettings.reduceMotion()) // the flash rides with the shake, never alone
                g.renderOutline(getX() + dx - 1, getY() - 1, getWidth() + 2, getHeight() + 2,
                        (Math.round(200 * (1 - age / SHAKE_TICKS)) << 24) | (skin.warn() & 0xFFFFFF));
        }

        /** Ticks since the skin rejected something this button's press caused, or -1. */
        private float rejectAge(float clock) {
            float r = skin.rejectAt();
            return r >= pressedAt && r - pressedAt < 12f && clock - r >= 0 ? clock - r : -1f;
        }

        @Override
        public void onPress() {
            pressedAt = time();
            super.onPress();
        }

        @Override
        public void playDownSound(SoundManager manager) {
            GuiSkin.play(sound != null ? sound.get() : skin.click(), pitch);
        }
    }
}
