// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.world.item.ItemStack;

/**
 * What a robot's parts make of it: how fast it moves (its mass against its drive), what its tool head can break (the head material's
 * yield strength against a block's hardness, the digest's St over hardness), which sensor it carries and whether it holds its chunk
 * loaded. Read once when the robot is set down or loaded, never per tick.
 *
 * @param mass    Mu, from the baked figures.
 * @param speed   blocks per tick.
 * @param headSt  yield strength of the tool head's material; 0 for a head that states none.
 * @param sensor  the sensor part's variant ({@link #PROBE}, {@link #RANGEFINDER}, {@link #INERTIAL}), or empty for none.
 * @param drive   the drive part's variant ({@link #WHEELS}, {@link #LEGS}, {@link #ROTOR}).
 * @param loader  whether a chunk-loading part is fitted.
 */
public record RobotSpec(float mass, float speed, float headSt, String sensor, String drive, boolean loader) {

    public static final String PROBE = "probe", RANGEFINDER = "rangefinder", INERTIAL = "inertial";
    public static final String WHEELS = "wheels", LEGS = "legs", ROTOR = "rotor";
    public static final RobotSpec NONE = new RobotSpec(0f, 0f, 0f, "", WHEELS, false);

    public static RobotSpec of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        var figures = ModularStacks.figures(stack);
        if (design == null || figures == null)
            return NONE;
        float mass = figures.get(Fig.MASS);
        ModularDesign.Part driveGear = design.part("D");
        String drive = driveGear == null || driveGear.variant() == null || driveGear.variant().isEmpty() ? WHEELS : driveGear.variant();
        float factor = drive.equals(LEGS) ? RobotTuning.LEGS_SPEED : drive.equals(ROTOR) ? RobotTuning.ROTOR_SPEED : 1f;
        // a rotor too heavy to lift has nothing to roll on: it stays where it was set down
        float speed = drive.equals(ROTOR) && mass > RobotTuning.ROTOR_LIFT_MU ? 0f
                : RobotTuning.SPEED_MAX * factor * RobotTuning.SPEED_HALF_MASS / (RobotTuning.SPEED_HALF_MASS + mass);
        ModularDesign.Part head = design.part("T");
        Material m = head == null ? null : MaterialTable.get().materials().get(head.material());
        float st = m == null ? 0f : m.mechanical().yieldStrength().map(s -> s.value()).orElse(0f);
        ModularDesign.Part sensor = design.part("S");
        return new RobotSpec(mass, speed, st, sensor == null || sensor.variant() == null ? "" : sensor.variant(), drive,
                design.part("L") != null);
    }

    /** Whether the head can break a block of this hardness at all. */
    public boolean canBreak(float hardness) {
        return hardness >= 0f && headSt >= hardness * RobotTuning.HEAD_ST_PER_HARDNESS;
    }

    /** Ticks to break a block: harder rock takes longer, a stronger head shorter. */
    public int digTicks(float hardness) {
        float rate = headSt <= 0f ? 1f : headSt / RobotTuning.HEAD_ST_REF;
        return Math.max(4, Math.round(hardness * RobotTuning.DIG_TICKS_PER_HARDNESS / Math.max(0.25f, rate)));
    }

    /** Share of its wheel error that survives: an inertial unit holds a heading, so little of it. */
    public float drift() {
        return sensor.equals(INERTIAL) ? RobotTuning.INERTIAL_DRIFT_SHARE : 1f;
    }

    /** A rotor robot light enough to lift itself. */
    public boolean flies() {
        return drive.equals(ROTOR) && mass <= RobotTuning.ROTOR_LIFT_MU;
    }

    /** The height of ledge it steps over without a program: legs a block, wheels a slab, a rotor none. */
    public float stepUp() {
        return drive.equals(LEGS) ? 1.1f : drive.equals(WHEELS) ? 0.6f : 0f;
    }
}
