// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Circuit;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * What a circuit held on a gun does. The circuit is a finished item clipped into a fitting role of the gun (role name {@link #FIRE_CONTROL} on a
 * firearm, {@link #FUSE_CONTROL} on a launcher), read by its figures at the warmth its stack is at: a tubed circuit on a gun has no heater, so it
 * works cold, slower and noisier, never refused. Nothing here names an item; a gun without the fitting, or with a dead circuit in it, has no
 * readout and the base spread.
 *
 * <p>Fire control: noise tightens the group by a bounded factor (the stick machines' {@code oddsFactor} pattern), gain lengthens the rangefinder, and
 * the readout (range, lead, drop) steps coarser as the circuit gets noisier. The ballistic figures are the ones a shot flies by: the muzzle speed,
 * and gravity in {@link GunTuning}; drag is not counted, so a long shot drops a little more than the readout says.
 */
public final class FireControl {

    /** The role names of the circuit fittings, as the type files name them. */
    public static final String FIRE_CONTROL = "fire_control", FUSE_CONTROL = "fuse_control";

    private FireControl() {
    }

    /** The circuit held in the gun's fitting role named {@code role}, at the warmth it is at, or {@link Circuit#NONE}. */
    public static Circuit held(ItemStack gun, String role, Level level) {
        var design = ModularStacks.design(gun);
        var type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return Circuit.NONE;
        for (var p : design.parts()) {
            var r = type.roles().get(p.role());
            if (r != null && r.name().equals(role) && !p.sub().isEmpty())
                return Circuit.of(p.sub(), level);
        }
        return Circuit.NONE;
    }

    /** The multiplier on a group's spread: 1 for none, down by {@code FIRE_CONTROL_CUT} for a silent circuit, less as noise rises. */
    public static float spread(Circuit c) {
        return c.live() ? 1f - GunTuning.FIRE_CONTROL_CUT * (1f - c.noise()) : 1f;
    }

    /** Blocks the rangefinder reaches: its base, longer for the circuit's gain (a saturating gain, never unbounded). */
    public static float reach(Circuit c) {
        float gain = Math.max(0f, c.gain());
        return GunTuning.FIRE_CONTROL_REACH * (1f + GunTuning.FIRE_CONTROL_GAIN * gain / (gain + 1f));
    }

    /** Metres one step of the readout is: a quiet circuit reads fine, a noisy one coarse. */
    public static float step(Circuit c) {
        return 1f + GunTuning.FIRE_CONTROL_STEP * c.noise();
    }

    /**
     * What the readout shows for a target {@code range} blocks away moving at {@code targetVelocity} (blocks a tick) against a round leaving at
     * {@code muzzleSpeed} blocks a tick: the range, the time of flight in ticks, the drop in blocks and the lead in blocks. {@code valid} is false
     * past the rangefinder's reach or for a dead circuit.
     */
    public record Solution(boolean valid, float range, float flightTicks, float drop, float lead) {
        public static final Solution NONE = new Solution(false, 0f, 0f, 0f, 0f);
    }

    public static Solution solve(Circuit c, double range, Vec3 toTarget, Vec3 targetVelocity, float muzzleSpeed) {
        if (!c.live() || range > reach(c) || muzzleSpeed <= 0f)
            return Solution.NONE;
        float t = (float) (range / muzzleSpeed);
        float drop = 0.5f * GunTuning.GUN_GRAVITY_PER_TICK * t * t;
        Vec3 line = toTarget.normalize();
        Vec3 across = targetVelocity.subtract(line.scale(targetVelocity.dot(line)));
        float lead = (float) across.length() * t;
        float step = step(c);
        return new Solution(true, snap(range, step), t, snap(drop, step * 0.1f), snap(lead, step * 0.1f));
    }

    private static float snap(double value, float step) {
        return Math.round(value / step) * step;
    }
}
