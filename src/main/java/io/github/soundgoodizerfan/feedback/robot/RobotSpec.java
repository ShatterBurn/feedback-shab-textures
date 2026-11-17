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
 * @param radio   whether a radio part is fitted.
 * @param dexterity how steadily its hand lands, 0 to 1, from the circuit in its brain: speed times quiet, halved by a cold joint; 0 with no circuit.
 * @param brain   the brain circuit's figures as the robot reads them ({@code Circuit.NONE} with none): a tubed brain works at its working figures while the pack pays its heater. It sets the program size, the sense reach and fidelity, the leash and the places the robot remembers.
 */
public record RobotSpec(float mass, float speed, float headSt, String sensor, String drive, boolean loader, boolean radio, float dexterity,
                        io.github.soundgoodizerfan.feedback.process.Circuit brain) {

    /** A robot with no brain figures beyond its dexterity. */
    public RobotSpec(float mass, float speed, float headSt, String sensor, String drive, boolean loader, boolean radio, float dexterity) {
        this(mass, speed, headSt, sensor, drive, loader, radio, dexterity, io.github.soundgoodizerfan.feedback.process.Circuit.NONE);
    }

    public static final String PROBE = "probe", RANGEFINDER = "rangefinder", INERTIAL = "inertial";
    public static final String WHEELS = "wheels", LEGS = "legs", ROTOR = "rotor", GAS_CELL = "gas_cell";
    public static final RobotSpec NONE = new RobotSpec(0f, 0f, 0f, "", WHEELS, false, false, 0f);

    public static RobotSpec of(ItemStack stack) {
        return of(ModularStacks.design(stack), ModularStacks.figures(stack), MaterialTable.get().materials());
    }

    /** From a design and its baked figures: what the stack overload reads, and what the Blueprint Table predicts for a drawing. */
    public static RobotSpec of(ModularDesign design, io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures figures,
                               java.util.Map<net.minecraft.resources.ResourceLocation, Material> materials) {
        if (design == null || figures == null)
            return NONE;
        float mass = figures.get(Fig.MASS);
        ModularDesign.Part driveGear = design.part("D");
        String drive = driveGear == null || driveGear.variant() == null || driveGear.variant().isEmpty() ? WHEELS : driveGear.variant();
        float factor = drive.equals(LEGS) ? RobotTuning.LEGS_SPEED : drive.equals(GAS_CELL) ? RobotTuning.GAS_CELL_SPEED : drive.equals(ROTOR) ? RobotTuning.ROTOR_SPEED : 1f;
        // a rotor too heavy to lift has nothing to roll on: it stays where it was set down
        float speed = (drive.equals(ROTOR) || drive.equals(GAS_CELL)) && mass > liftOf(drive) ? 0f
                : RobotTuning.SPEED_MAX * factor * RobotTuning.SPEED_HALF_MASS / (RobotTuning.SPEED_HALF_MASS + mass);
        ModularDesign.Part head = design.part("T");
        Material m = head == null ? null : materials.get(head.material());
        float st = m == null ? 0f : m.mechanical().yieldStrength().map(s -> s.value()).orElse(0f);
        ModularDesign.Part sensor = design.part("S");
        ModularDesign.Part brain = design.part("K");
        return new RobotSpec(mass, speed, st, sensor == null || sensor.variant() == null ? "" : sensor.variant(), drive,
                design.part("L") != null, design.part("R") != null, brain == null ? 0f : dexterity(brain.sub()),
                brain == null ? io.github.soundgoodizerfan.feedback.process.Circuit.NONE : io.github.soundgoodizerfan.feedback.process.Circuit.of(brain.sub()));
    }

    /** How steadily a hand lands, from a circuit: its speed times how quiet it is, halved by a cold joint; a dead or missing one reads 0. */
    public static float dexterity(ItemStack circuit) {
        return dexterity(io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit));
    }

    /** As above, for a circuit read at the warmth its stack is at now (a tubed brain that is cold is slow and noisy). */
    public static float dexterity(ItemStack circuit, net.minecraft.world.level.Level level) {
        return dexterity(io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit, level));
    }

    private static float dexterity(io.github.soundgoodizerfan.feedback.process.Circuit c) {
        if (c.elements() <= 0)
            return 0f;
        float d = c.speed() * (1f - c.noise());
        return c.joint() == io.github.soundgoodizerfan.feedback.process.Board.Joint.COLD ? d * 0.5f : d;
    }

    /** The same figures with a brain fitted: the arm takes its circuit by hand. */
    public RobotSpec withBrain(ItemStack circuit, net.minecraft.world.level.Level level) {
        return new RobotSpec(mass, speed, headSt, sensor, drive, loader, radio, dexterity(circuit, level),
                io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit, level));
    }

    public RobotSpec withBrain(ItemStack circuit) {
        return new RobotSpec(mass, speed, headSt, sensor, drive, loader, radio, dexterity(circuit),
                io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit));
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

    private static float liftOf(String drive) {
        return drive.equals(GAS_CELL) ? RobotTuning.GAS_CELL_LIFT_MU : RobotTuning.ROTOR_LIFT_MU;
    }

    /** A rotor or gas-cell robot light enough to lift itself: a drone. */
    public boolean flies() {
        return (drive.equals(ROTOR) || drive.equals(GAS_CELL)) && mass <= liftOf(drive);
    }

    /** A gas-cell drone: it floats, holds its height with no power and drifts nowhere when its power dies (a rotor falls). */
    public boolean floats() {
        return drive.equals(GAS_CELL) && flies();
    }

    /** Eu a tick it draws to hold its height: a rotor's hover, nothing for a gas cell. */
    public float hoverEu() {
        return drive.equals(GAS_CELL) ? 0f : RobotTuning.EU_HOVER_PER_TICK;
    }

    // --- what the brain decides ---------------------------------------------------------------------------

    /** Cards its program may hold: a small base with no brain, more for each element on the circuit, never over the Controller ceiling. */
    public int cards() {
        return Math.min(io.github.soundgoodizerfan.feedback.control.program.ControllerLimits.MAX.nodes(),
                RobotTuning.CARDS_BASE + RobotTuning.CARDS_PER_ELEMENT * Math.max(0, brain.elements()));
    }

    /** A sense reach (blocks) lengthened by the brain's gain; the base with none. */
    public int senseReach(int base) {
        return Math.round(base * (1f + RobotTuning.SENSE_GAIN * Math.max(0f, brain.gain())));
    }

    /** A numeric sense as the brain gives it: wobbled by its noise times the read jitter (a robot with no brain, at full noise, most). */
    public float senseJitter(float value, net.minecraft.util.RandomSource random) {
        if (value == 0f)
            return 0f;
        return value * (1f + (random.nextFloat() * 2f - 1f) * brain.noise() * io.github.soundgoodizerfan.feedback.control.ControlTuning.CONTROLLER_READ_JITTER);
    }

    /** Places it remembers: the lattice's memory. */
    public int places() {
        return Math.max(0, brain.memory());
    }

    /** Whether a leash can carry at all: a live brain, never none. */
    public boolean leashes() {
        return brain.live();
    }

    /** Channels of senses and actions the leash carries: a base, times the brain's selectivity (at least once). */
    public int leashChannels() {
        return RobotTuning.LEASH_CHANNELS_BASE * Math.max(1, brain.selectivity());
    }

    /** Eu a tick the brain's heater draws from the pack. */
    public float heaterEu() {
        return brain.heaterPower();
    }

    /** The height of ledge it steps over without a program: legs a block, wheels a slab, a rotor none. */
    public float stepUp() {
        return drive.equals(LEGS) ? 1.1f : drive.equals(WHEELS) ? 0.6f : 0f;
    }

    /** The hardest block (hardness) the head can break. */
    public float hardest() {
        return headSt / RobotTuning.HEAD_ST_PER_HARDNESS;
    }

    /** Mu of mass left before a rotor cannot lift (negative: it is grounded). */
    public float liftMargin() {
        return liftOf(drive) - mass;
    }
}
