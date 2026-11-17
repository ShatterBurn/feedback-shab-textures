// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.CasterTuning;

/**
 * The physics of a fluid caster, read from its design. The tank's wall holds the pressure ({@code danger}: the yield of its metal times
 * the wall over the inner radius), its length sets what it holds; the nozzle's bore trades range and push against flow and spread, its
 * variant sets how the stream fans; a drive part makes it a pump (steady head while worked) and no drive makes it a pressurised tank
 * (pressure falls as the tank empties). The valve and nozzle metals are what a wrong fluid eats.
 *
 * @param capacityMb  mB the tank holds.
 * @param danger      Pu the walls hold; they weep over it.
 * @param critical    Pu at which they let go.
 * @param boreMm      the nozzle's bore.
 * @param spreadDeg   half-angle of the stream's cone.
 * @param atomiser    the nozzle breaks the stream to a mist that mixes with air.
 * @param pump        a drive part is fitted: steady head while worked.
 * @param pumpPu      the pump's head, Pu (0 without a pump).
 * @param strokeTicks ticks of flow one stroke of the pump primes.
 * @param tankResist  corrosion resistance of the tank's metal; valve and nozzle likewise.
 * @param pilotReach  blocks from the muzzle the pilot's flame stands out (0 without a pilot).
 * @param pilotBleed  mB a tick its orifice bleeds off the tank while lit.
 * @param pilotServiceTu the hottest fluid (Tu) the pilot's metal takes washing over it.
 * @param reachMult   the frame's multiple of the stream's reach (a long frame keeps the pipe true).
 * @param lineLoss    the share of pressure that reaches the nozzle (a hose from a pack loses some).
 * @param pumpWantPu the head the pump would make before the walls hold it back (0 without a pump).
 * @param electric    the drive is a motor: it draws charge from a seated cell, and the reload key does nothing.
 * @param tankServiceTu the hottest fluid (Tu) the tank walls hold before they soften, and {@code tankMeltTu} where they run.
 */
public record CasterSpec(float capacityMb, float danger, float critical, float boreMm, float spreadDeg, boolean atomiser, boolean pump,
                         float pumpPu, float strokeTicks, float tankResist, float valveResist, float nozzleResist, float pilotReach, float pilotBleed, float pilotServiceTu, float tankServiceTu, float tankMeltTu, float massKg, float reachMult, float lineLoss, boolean electric, float pumpWantPu) {

    public static final String TANK = "tank", NOZZLE = "nozzle", RECEIVER = "receiver", VALVE = "valve", DRIVE = "drive", PILOT = "pilot";

    /** The spec of {@code design}, or empty if it lacks a tank, a nozzle, a receiver or a valve, or a metal is unknown or states no yield. */
    public static Optional<CasterSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        float capacity = 0, danger = 0, bore = 0, spread = 0, pumpPu = 0, stroke = 0, mass = 0;
        float pilotReach = 0, pilotBleed = 0, pilotService = Float.MAX_VALUE, tankR = 1f, valveR = 1f, nozzleR = 1f, service = Float.MAX_VALUE, melt = Float.MAX_VALUE;
        float reachMult = 1f, spreadMult = 1f, capacityMult = 1f, line = 1f;
        boolean electric = false;
        boolean tank = false, nozzle = false, receiver = false, valve = false, atomise = false, pump = false;
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                return Optional.empty();
            mass += ModularBake.mass(role, p, m);
            switch (role.name()) {
                case TANK -> {
                    if (m.mechanical().yieldStrength().isEmpty())
                        return Optional.empty();
                    float wall = role.axis(p, "wall"), length = role.axis(p, "length");
                    danger = ModularTuning.TANK_PU_PER_STRESS * m.mechanical().yieldStrength().get().value() * wall / CasterTuning.TANK_INNER_MM;
                    capacity = CasterTuning.TANK_MB_PER_CM * length;
                    tankR = m.corrosionResistance().orElse(0.5f);
                    service = m.thermal().serviceTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE);
                    melt = m.thermal().meltingPoint().map(t -> t.value()).orElse(Float.MAX_VALUE);
                    tank = true;
                }
                case NOZZLE -> {
                    bore = role.axis(p, "bore");
                    spread = role.paramOf(p, "spread", 1f);
                    atomise = role.paramOf(p, "atomise", 0f) > 0.5f;
                    nozzleR = m.corrosionResistance().orElse(0.5f);
                    nozzle = true;
                }
                case RECEIVER -> {
                    // The frame: how it carries the tank and holds the pipe (variant params).
                    reachMult = role.paramOf(p, "reach", 1f);
                    spreadMult = role.paramOf(p, "spread", 1f);
                    capacityMult = role.paramOf(p, "capacity", 1f);
                    line = role.paramOf(p, "line", 1f);
                    receiver = true;
                }
                case VALVE -> {
                    valveR = m.corrosionResistance().orElse(0.5f);
                    valve = true;
                }
                case DRIVE -> {
                    electric = role.paramOf(p, "electric", 0f) > 0.5f;
                    if (electric) // a motor's head is its size times its metal's strength; it needs no stroke
                        pumpPu = CasterTuning.MOTOR_PU_PER_SIZE_STRENGTH * role.axis(p, "motor") * ModularBake.strength(m);
                    else {
                        stroke = CasterTuning.PRIME_TICKS_PER_CM * role.axis(p, "stroke");
                        pumpPu = CasterTuning.PUMP_PU_PER_STRENGTH * ModularBake.strength(m);
                    }
                    pump = true;
                }
                case PILOT -> {
                    float orifice = role.axis(p, "orifice");
                    pilotReach = role.paramOf(p, "reach", 0.4f);
                    pilotBleed = CasterTuning.PILOT_BLEED_PER_MM2 * orifice * orifice;
                    pilotService = m.thermal().serviceTemperature().map(t -> t.value()).orElse(Float.MAX_VALUE);
                }
                default -> { }
            }
        }
        if (!(tank && nozzle && receiver && valve) || bore <= 0 || capacity <= 0)
            return Optional.empty();
        // A pump never drives harder than the walls hold.
        float pumpWant = pumpPu;
        pumpPu = Math.min(pumpPu, danger * CasterTuning.PUMP_DANGER_SHARE);
        return Optional.of(new CasterSpec(capacity * capacityMult, danger, danger * ModularTuning.TANK_CRITICAL_OVER_DANGER, bore, spread * spreadMult, atomise, pump,
                pumpPu, stroke, tankR, valveR, nozzleR, pilotReach, pilotBleed, pilotService, service, melt, mass, reachMult, line, electric, pump ? pumpWant : 0f));
    }

    /** Exit speed in blocks a tick of a fluid of {@code density} (Mu per mB) at gauge pressure {@code pu}. */
    public float speed(float pu, float density) {
        if (pu < CasterTuning.MIN_PRESSURE)
            return 0f;
        return Math.min(CasterTuning.SPEED_MAX, CasterTuning.SPEED_PER_SQRT * (float) Math.sqrt(pu * lineLoss / Math.max(0.05f, density)));
    }

    /** Blocks the stream reaches at exit speed {@code speed}: a narrow bore holds together farther. */
    public float reach(float speed) {
        return Math.min(CasterTuning.REACH_MAX, reachMult * speed * CasterTuning.FLIGHT_TICKS * (float) Math.sqrt(CasterTuning.REF_BORE_MM / boreMm));
    }

    /** mB a tick leaving at exit speed {@code speed}: a wide bore passes more. */
    public float flow(float speed) {
        return CasterTuning.FLOW_PER_MM2 * boreMm * boreMm * speed;
    }

    /** The steady head the stream is made at: a pump's, or what the walls hold. */
    public float head() {
        return pump ? pumpPu : danger;
    }

    /** The pump's own head over what the walls hold; above {@link CasterTuning#PUMP_DANGER_SHARE} the walls hold it back. */
    public float pumpOverWalls() {
        return pumpWantPu / Math.max(0.001f, danger);
    }

    /** A pilot is fitted: its flame lights a stream that burns. */
    public boolean piloted() {
        return pilotReach > 0f;
    }

    /** The worst corrosion resistance among the parts a fluid wets: what a corrosive stream eats first. */
    public float weakestResist() {
        return Math.min(tankResist, Math.min(valveResist, nozzleResist));
    }
}
