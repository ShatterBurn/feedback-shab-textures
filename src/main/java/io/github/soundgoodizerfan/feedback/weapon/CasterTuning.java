// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

/**
 * Constants of the fluid caster ({@code modular/bake/CasterSpec}, {@code CasterMechanism}, {@link FluidStream}). Placeholders: safe to
 * retune freely. Speeds are blocks per tick, flows mB per tick, pressures Pu, temperatures Tu.
 */
public final class CasterTuning {

    private CasterTuning() {
    }

    // --- the tank ---------------------------------------------------------------------------------
    /** Inner radius of the tank cylinder, mm: the wall over this against the metal's yield is the pressure it holds. */
    public static final float TANK_INNER_MM = 40f;
    /** mB a cm of tank length holds. */
    public static final float TANK_MB_PER_CM = 60f;
    /** Share of the tank that is always gas headspace in the pressure law (a full tank still has a little to push on). */
    public static final float HEADSPACE_FLOOR = 0.1f;

    // --- the stream -------------------------------------------------------------------------------
    /** Exit speed (blocks per tick) per square root of the pressure over the fluid's density, and its cap. */
    public static final float SPEED_PER_SQRT = 0.12f, SPEED_MAX = 2.5f;
    /** Ticks a jet flies before it breaks up, the bore it is rated at (mm), and the farthest it reaches (blocks). */
    public static final float FLIGHT_TICKS = 14f, REF_BORE_MM = 4f, REACH_MAX = 28f;
    /** mB a tick through a square mm of bore at one block a tick. */
    public static final float FLOW_PER_MM2 = 0.35f;
    /** Below this gauge pressure a stream does not leave the nozzle. */
    public static final float MIN_PRESSURE = 4f;

    // --- drives -----------------------------------------------------------------------------------
    /** A pump's steady head, Pu per unit of its metal's strength, and the share of the tank's danger line it is held under. */
    public static final float PUMP_PU_PER_STRENGTH = 0.2f, PUMP_DANGER_SHARE = 0.8f;
    /** Ticks of flow one stroke of a hand pump primes per cm of stroke, and the most it holds. */
    public static final float PRIME_TICKS_PER_CM = 4f, PRIME_MAX_TICKS = 60f;
    /** Pu one stroke of the tank's own air pump adds, over the headspace share (a fuller tank pressurises faster). */
    public static final float AIR_STROKE_PU = 6f;

    /** An electric pump's head per unit of motor size per unit of its metal's strength (Pu), and the Eu it takes to move a mB against a Pu. */
    public static final float MOTOR_PU_PER_SIZE_STRENGTH = 0.1f, EU_PER_MB_PU = 0.5f;
    /** Ticks a motor takes to come up to its head after the trigger goes down, and the gap (ticks) in pulls after which it has stopped. */
    public static final int MOTOR_SPINUP_TICKS = 10, MOTOR_STOP_TICKS = 4;
    /** Wear (fraction of condition) a tick a motor runs with nothing to move. */
    public static final float WEAR_DRY_MOTOR = 0.0005f;

    // --- effects ----------------------------------------------------------------------------------
    /** A stream hotter than this scalds what it hits; one at or under {@link #FREEZE_TU} chills it. */
    public static final float SCALD_TU = 55f, FREEZE_TU = 0f;
    /** Scald damage a tick per Tu over {@link #SCALD_TU} per mB of flow, and its cap. */
    public static final float SCALD_PER_TU_MB = 0.0015f, SCALD_MAX = 4f;
    /** Ticks of frost a chilling hit adds per mB of flow. */
    public static final int FREEZE_TICKS_PER_MB = 6;
    /** Damage a tick to a creature that water hurts: per mB of its share of the flow, at least what rain does it (1) and at most {@code WATER_HURT_MAX}. The hit's own invulnerability frames space it out as they do rain's. */
    public static final float WATER_HURT_PER_MB = 0.25f, WATER_HURT_MIN = 1f, WATER_HURT_MAX = 4f;
    /** Seconds a burning stream sets what it hits alight. */
    public static final float IGNITE_SECONDS = 5f;
    /** Push on a body, blocks per tick, per unit of the stream's momentum (mass flow times exit speed), and its cap. */
    public static final float SHOVE_PER_MOMENTUM = 0.012f, SHOVE_MAX = 0.6f;
    /** Corrosive fluid wear on armour, durability a tick per mB of flow. */
    public static final float ARMOUR_BITE_PER_MB = 0.08f;
    /** A stream at or over this (Tu) sets a body alight whatever it is made of: hot enough to light cloth and hair. Seconds it burns, and the cap a combustion's burn time is held to. */
    public static final float HOT_IGNITE_TU = 250f, HOT_IGNITE_SECONDS = 4f;
    /** Seconds a burning fuel keeps a body alight: its burn time, clamped to this range. */
    public static final float BURN_SECONDS_MIN = 3f, BURN_SECONDS_MAX = 15f;
    /** A fuel's flame hotter than this (Tu) burns a body worse: damage a tick per Tu over, per mB of flow, and its cap. */
    public static final float FLAME_BASE_TU = 900f, FLAME_PER_TU_MB = 0.0012f, FLAME_MAX = 3f;
    /** An oxidiser on a burning body flares the fire: damage a tick per unit of oxygen fraction per mB, its cap, and the fire ticks it adds (up to {@code FLARE_FIRE_MAX_TICKS}). */
    public static final float FLARE_PER_MB = 0.02f, FLARE_MAX = 2f;
    public static final int FLARE_FIRE_TICKS = 40, FLARE_FIRE_MAX_TICKS = 300;
    /** Colder than this (Tu) a stream freezes flesh outright: damage a tick per Tu under, per mB, and its cap. Frost builds faster the colder: ticks per mB times one plus the Tu under 0 over {@code FREEZE_DEPTH_TU}, at most {@code FREEZE_DEPTH_MAX} times. */
    public static final float FROSTBITE_TU = -60f, FROSTBITE_PER_TU_MB = 0.0008f, FROSTBITE_MAX = 3f;
    public static final float FREEZE_DEPTH_TU = 100f, FREEZE_DEPTH_MAX = 4f;
    /** A fluid thicker than this (Pa s) clings and drags: slowness for these ticks, one level more per ten-fold over it (to level 3). */
    public static final float CLING_VISCOSITY = 5f;
    public static final int CLING_TICKS = 40;
    /** Chemical burn: damage a tick per pH unit off neutral per mB of flow, and its cap. */
    public static final float CHEM_PER_PH_MB = 0.012f, CHEM_MAX = 4f;
    /** pH distance of a strong acid or base: what {@code ARMOUR_BITE_PER_MB} is quoted at. */
    public static final float CORROSIVITY_REF = 7f;
    /** A fluid more toxic than this index poisons what it hits: ticks per index per mB of flow, its cap, and the index from which the poison is the stronger level. */
    public static final float POISON_MIN_TOXICITY = 0.8f, POISON_TICKS_PER_TOX_MB = 8f, POISON_STRONG_TOXICITY = 2f;
    public static final int POISON_MAX_TICKS = 300;
    /** A radioactive fluid withers (and outlines) what it hits: wither ticks per Ru per mB, and its cap. */
    public static final float RADIATION_TICKS_PER_RU_MB = 40f;
    public static final int RADIATION_MAX_TICKS = 200, RADIATION_GLOW_TICKS = 100;
    /** A spatially unstable stream hops what is near its target this often (ticks). */
    public static final int SPATIAL_INTERVAL_TICKS = 10;
    /** Dynamic pressure (density times speed squared, Pu) under which a stream only shoves; over it it cuts: damage a tick per Pu over, and its cap. */
    public static final float CUT_MIN_Q = 2f, CUT_PER_Q = 0.8f, CUT_MAX = 4f;
    /** A gas more toxic than this index leaves a lingering poison cloud where it runs out (strength the index rounded, at most {@code GAS_CLOUD_MAX}). */
    public static final float GAS_CLOUD_MIN_TOXICITY = 1f;
    public static final int GAS_CLOUD_MAX = 5;
    /** mB of water that raise a cauldron one level. */
    public static final float CAULDRON_MB_PER_LEVEL = 333f;
    /** Linger of a mist's vapour source, ticks. */
    public static final int MIST_LINGER_TICKS = 20;
    /** Blocks from the muzzle within which an ignition lets a burning volatile stream flash back, and the bore (mm) from which it can. */
    public static final float FLASHBACK_REACH = 2.5f, FLASHBACK_BORE_MM = 6f;

    // --- the pilot ---------------------------------------------------------------------------------
    /** mB a tick the pilot's orifice bleeds off the tank per square mm of orifice (a fraction: it takes a whole mB with that chance). */
    public static final float PILOT_BLEED_PER_MM2 = 0.022f;
    /** Ticks between the flame tip trying to light the block it points at. */
    public static final int PILOT_LIGHT_INTERVAL_TICKS = 5;
    /** Chance a tick a lit pilot's flame runs back up a wide volatile stream (a standing flame, so a chance, not every tick). */
    public static final float PILOT_FLASHBACK_CHANCE = 0.04f;
    /** Wear (fraction of condition) a tick a pilot books while a fluid hotter than its metal's service temperature washes it. */
    public static final float WEAR_PILOT_HOT = 0.0005f;

    // --- what a design says of itself ------------------------------------------------------------
    /** A stream that reaches less than this (blocks) at the head the design makes is short. */
    public static final float SHORT_REACH_BLOCKS = 3f;
    /** A part whose metal resists corrosion less than this is eaten by an aggressive fluid. */
    public static final float CORRODIBLE_BELOW = 0.3f;

    // --- misuse -----------------------------------------------------------------------------------
    /** Wear (fraction of condition) a dry pump stroke, a weeping tank, a flashback and a burst each book. */
    public static final float WEAR_DRY_PUMP = 0.01f, WEAR_WEEP = 0.02f, WEAR_FLASHBACK = 0.15f, WEAR_BURST = 0.3f;
    /** Hurt to the wielder when the tank lets go, and of a flashback, hp. */
    public static final float BURST_HP = 6f, FLASHBACK_HP = 5f;
}
