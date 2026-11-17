// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * A launcher's fuse control: a circuit clipped into the launcher (role {@link FireControl#FUSE_CONTROL}) sets the fuse of the round it fires, in one
 * of a few discrete modes the wielder switches between (sneak and the fire-mode key): the round's own fuse, proximity, a short count or a long count.
 * It is a switch, never a dial: the circuit decides what each mode comes to, the wielder only picks the mode.
 *
 * <ul>
 * <li><b>Proximity</b>: the round blows when a body or block comes within the circuit's sense. The radius is the round's base radius, wider for the
 * circuit's gain, with a per-shot jitter of {@code FUSE_JITTER x noise} (a noisy circuit senses a little short or long). Its arming time is the
 * circuit's own steps: a fast clock arms sooner.</li>
 * <li><b>Short and long count</b>: the round blows after {@code FUSE_STEPS} or {@code FUSE_STEPS_LONG} steps of the circuit's clock, so a slow
 * circuit gives a long fuse and a fast one a short fuse; bounded between {@code FUSE_MIN_TICKS} and {@code FUSE_MAX_TICKS}.</li>
 * </ul>
 * With no circuit, or a dead one, the mode is ignored and the round keeps its own fuse. It works without, only worse: the press's fuse is the base.
 */
public final class SmartFuse {

    /** The modes, in the order the switch cycles them. */
    public enum Mode {
        OWN, PROXIMITY, SHORT_COUNT, LONG_COUNT;

        public String langKey() {
            return "hud.feedback.fuse." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private SmartFuse() {
    }

    public static Mode mode(ItemStack gun) {
        Integer i = gun.get(FWeapons.FUSE_MODE.get());
        return i == null || i < 0 || i >= Mode.values().length ? Mode.OWN : Mode.values()[i];
    }

    public static Mode next(ItemStack gun) {
        Mode next = Mode.values()[(mode(gun).ordinal() + 1) % Mode.values().length];
        gun.set(FWeapons.FUSE_MODE.get(), next.ordinal());
        return next;
    }

    /** Whether the gun carries a live fuse-control circuit. */
    public static boolean fitted(ItemStack gun, Level level) {
        return FireControl.held(gun, FireControl.FUSE_CONTROL, level).live();
    }

    /** What the circuit makes of {@code mode}: the fuse the round flies with and its sense radius in blocks (0 for the base radius), empty for the round's own. */
    public record Setting(Mode mode, Optional<Cartridge.Fuse> fuse, float radius) {
    }

    public static Setting of(ItemStack gun, Level level, net.minecraft.util.RandomSource random) {
        Circuit c = FireControl.held(gun, FireControl.FUSE_CONTROL, level);
        Mode mode = mode(gun);
        if (!c.live() || mode == Mode.OWN)
            return new Setting(Mode.OWN, Optional.empty(), 0f);
        int step = Math.max(1, Math.round(20f / Math.max(1f, c.clockRate())));
        return switch (mode) {
            case PROXIMITY -> new Setting(mode, Optional.of(new Cartridge.Fuse(Cartridge.Kind.PROXIMITY, clamp(Math.round(GunTuning.FUSE_STEPS * step * 0.5f)))),
                    radius(c, random));
            case SHORT_COUNT -> new Setting(mode, Optional.of(new Cartridge.Fuse(Cartridge.Kind.DELAY, clamp(Math.round(GunTuning.FUSE_STEPS * step)))), 0f);
            default -> new Setting(mode, Optional.of(new Cartridge.Fuse(Cartridge.Kind.DELAY, clamp(Math.round(GunTuning.FUSE_STEPS_LONG * step)))), 0f);
        };
    }

    /** The proximity sense radius this circuit gives: wider with gain, jittered by noise, never under half the base. */
    public static float radius(Circuit c, net.minecraft.util.RandomSource random) {
        float gain = Math.max(0f, c.gain());
        float wide = GunTuning.GUN_PROXIMITY_RADIUS * (1f + GunTuning.FUSE_GAIN * gain / (gain + 1f));
        float jitter = 1f + (random.nextFloat() * 2f - 1f) * GunTuning.FUSE_JITTER * c.noise();
        return Math.max(GunTuning.GUN_PROXIMITY_RADIUS * 0.5f, wide * jitter);
    }

    private static int clamp(int ticks) {
        return Mth.clamp(ticks, GunTuning.FUSE_MIN_TICKS, GunTuning.FUSE_MAX_TICKS);
    }
}
