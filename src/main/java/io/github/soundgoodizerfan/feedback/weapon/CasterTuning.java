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

    // --- effects ----------------------------------------------------------------------------------
    /** A stream hotter than this scalds what it hits; one at or under {@link #FREEZE_TU} chills it. */
    public static final float SCALD_TU = 55f, FREEZE_TU = 0f;
    /** Scald damage a tick per Tu over {@link #SCALD_TU} per mB of flow, and its cap. */
    public static final float SCALD_PER_TU_MB = 0.0015f, SCALD_MAX = 4f;
    /** Ticks of frost a chilling hit adds per mB of flow. */
    public static final int FREEZE_TICKS_PER_MB = 6;
    /** Seconds a burning stream sets what it hits alight. */
    public static final float IGNITE_SECONDS = 5f;
    /** Push on a body, blocks per tick, per unit of the stream's momentum (mass flow times exit speed), and its cap. */
    public static final float SHOVE_PER_MOMENTUM = 0.012f, SHOVE_MAX = 0.6f;
    /** Corrosive fluid wear on armour, durability a tick per mB of flow. */
    public static final float ARMOUR_BITE_PER_MB = 0.08f;
    /** mB of water that raise a cauldron one level. */
    public static final float CAULDRON_MB_PER_LEVEL = 333f;
    /** Linger of a mist's vapour source, ticks. */
    public static final int MIST_LINGER_TICKS = 20;
    /** Blocks from the muzzle within which an ignition lets a burning volatile stream flash back, and the bore (mm) from which it can. */
    public static final float FLASHBACK_REACH = 2.5f, FLASHBACK_BORE_MM = 6f;

    // --- misuse -----------------------------------------------------------------------------------
    /** Wear (fraction of condition) a dry pump stroke, a weeping tank, a flashback and a burst each book. */
    public static final float WEAR_DRY_PUMP = 0.01f, WEAR_WEEP = 0.02f, WEAR_FLASHBACK = 0.15f, WEAR_BURST = 0.3f;
    /** Hurt to the wielder when the tank lets go, and of a flashback, hp. */
    public static final float BURST_HP = 6f, FLASHBACK_HP = 5f;
}
