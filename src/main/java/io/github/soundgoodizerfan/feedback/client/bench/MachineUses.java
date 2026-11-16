// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.client.bench.BenchUses.Use;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.hazard.Corrosion;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.logistics.LogisticsTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.BlockFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.HeadWear;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularMeasures;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The derived numbers the Blueprint Table prints for machine parts: what a block's, engine's, gearbox's, propeller's, belt's,
 * dynamotor's, battery's or head's baked figures work out to once a player asks "so what does it do" (steam a tick, a gas bottle's safe
 * fill, how long a jar holds its charge, the speed a propeller tears at). {@link #refine} adds them to the family's own list, replacing a
 * line of the same id; a type's {@code measures} decide which show. Nothing here is stored or read by a block: it is the arithmetic the
 * runtime does, said out loud, from the same constants.
 */
final class MachineUses {

    private MachineUses() {
    }

    /** {@code base} with this class's lines for the design added (a line of the same id replaced). */
    static List<Use> refine(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats, List<Use> base) {
        List<Use> extra = switch (t.family()) {
            case BLOCK -> block(d.type().getPath(), d, t, mats, base);
            case PRESSURE -> ModularBake.tank(d, t, mats).<List<Use>>map(v -> {
                List<Use> out = new ArrayList<>();
                out.add(plain("tank.air", v.volumeMb() * v.dangerPu(), "%s"));
                if (v.reliefMb() > 0f)
                    out.add(plain("tank.relief", v.reliefMb(), "%s"));
                return out;
            }).orElse(List.of());
            case ENGINE -> engine(d, t, mats);
            case GEARBOX -> ModularBake.gearbox(d, t, mats).<List<Use>>map(g -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= g.gears(); i++)
                    sb.append(i > 1 ? ", " : "").append(String.format(Locale.ROOT, "%.2f", g.ratio(i)));
                return List.of(new Use("gearbox.ratios", Component.translatable(ModularMeasures.langKey("gearbox.ratios"), sb.toString()), g.gears()));
            }).orElse(List.of());
            case PROPELLER -> ModularBake.propeller(d, t, mats).<List<Use>>map(p -> propeller(p, mats)).orElse(List.of());
            case BELT -> ModularBake.belt(d, t, mats).<List<Use>>map(b -> {
                float dry = b.friction() > 0f ? LogisticsTuning.INCLINE_TAN / b.friction() : 99f;
                float wet = dry / LogisticsTuning.WET_GRIP;
                return List.of(new Use("belt.slope", Component.translatable(ModularMeasures.langKey("belt.slope"), fmt(dry), fmt(wet)), -dry));
            }).orElse(List.of());
            case MACHINE -> ModularBake.machine(d, t, mats).<List<Use>>map(m -> {
                float loss = m.coreLoss() + m.ratedAmps() * m.windingDrop() / ElectricRules.packetEu(m.band());
                return List.of(plain("machine.loss", -loss * 100f, fmt(loss * 100f)), plain("machine.shed", m.leak(), fmt(m.leak())));
            }).orElse(List.of());
            case CELL -> ModularBake.cell(d, t, mats).<List<Use>>map(c -> List.of(
                    plain("cell.amps", c.capacity() / ElectricMachineTuning.BATTERY_DISCHARGE_TICKS / ElectricRules.packetEu(c.caseBand()),
                            fmt(c.capacity() / ElectricMachineTuning.BATTERY_DISCHARGE_TICKS / ElectricRules.packetEu(c.caseBand()))),
                    plain("cell.efficiency", c.plates().chargeEfficiency(), fmt(c.plates().chargeEfficiency() * 100f)),
                    plain("cell.cold", c.plates().coldFactor(), fmt(c.plates().coldFactor() * 100f)))).orElse(List.of());
            case HEAD -> HeadWear.of(d, t, mats).<List<Use>>map(w -> List.of(
                    new Use("head.wear", Component.translatable(ModularMeasures.langKey("head.wear"), fmt(w.perUnit() * 1000f * 100f),
                            Component.translatable(w.wheel() ? "gui.feedback.blueprint_table.head.wear.wu" : "gui.feedback.blueprint_table.head.wear.st")), -w.perUnit()))).orElse(List.of());
            default -> List.of();
        };
        if (extra.isEmpty() && t.family() != ModularType.Family.BLOCK && t.family() != ModularType.Family.PROPELLER)
            return fix(t, base);
        List<Use> out = new ArrayList<>();
        for (Use u : fix(t, base))
            if (extra.stream().noneMatch(e -> e.id().equals(u.id())))
                out.add(u);
        out.addAll(extra);
        return out;
    }

    /** Lines a family already prints, said better: a propeller's pitch by its name, an engine's torque in the order it is read. */
    private static List<Use> fix(ModularType t, List<Use> base) {
        if (t.family() != ModularType.Family.PROPELLER)
            return base;
        List<Use> out = new ArrayList<>();
        for (Use u : base)
            if (u.id().equals("prop.pitch")) {
                int p = Math.round(u.value());
                out.add(new Use(u.id(), Component.translatable(ModularMeasures.langKey(u.id()),
                        Component.translatable("gui.feedback.blueprint_table.prop.pitch." + Math.max(0, Math.min(2, p)))), u.value()));
            } else
                out.add(u);
        return out;
    }

    // --- engine, propeller --------------------------------------------------------------------

    private static List<Use> engine(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        return ModularBake.engine(d, t, mats).<List<Use>>map(e -> {
            var c = e.compression();
            String window = c.knockBelow <= 0f ? "under " + fmt(c.misfireAbove) : c.misfireAbove >= Float.MAX_VALUE / 2f ? "over " + fmt(c.knockBelow)
                    : fmt(c.knockBelow) + " to " + fmt(c.misfireAbove);
            float fuel = FTuning.ENGINE_FUEL_MB_PER_TICK_PER_L * e.displacementMb();
            float minutes = e.plugLife() / (FTuning.TICKS_PER_SECOND * 60f);
            return List.of(
                    new Use("engine.fuels", Component.translatable(ModularMeasures.langKey("engine.fuels"), window), c.knockBelow),
                    plain("engine.fuel", -fuel, fmt(fuel)),
                    new Use("engine.plug", Component.translatable(ModularMeasures.langKey("engine.plug"), fmt(minutes), fmt(e.plugFoul())), minutes));
        }).orElse(List.of());
    }

    private static List<Use> propeller(io.github.soundgoodizerfan.feedback.vehicle.air.PropDesign p, Map<ResourceLocation, Material> mats) {
        List<Use> out = new ArrayList<>();
        Material m = mats.get(p.material());
        float limit = VehicleTuning.PROP_TIP_SPEED_CAP;
        if (m != null && m.mechanical().yieldStrength().isPresent() && m.density().muPerMb() > 0f)
            limit = Math.min(limit, VehicleTuning.PROP_TIP_SPEED_PER_SQRT_SPECIFIC_YIELD
                    * (float) Math.sqrt(m.mechanical().yieldStrength().get().value() / m.density().muPerMb()));
        float rpm = (float) (limit / (Math.PI * p.diameter()) * 60.0);
        out.add(plain("prop.rpm", rpm, fmt(rpm)));
        out.add(new Use("prop.servo", Component.translatable(ModularMeasures.langKey("prop.servo"),
                Component.translatable(p.servo() ? "gui.feedback.blueprint_table.yes" : "gui.feedback.blueprint_table.no")), p.servo() ? 1f : 0f));
        return out;
    }

    // --- block family ------------------------------------------------------------------------

    private static List<Use> block(String id, ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats, List<Use> base) {
        List<Use> out = new ArrayList<>();
        switch (id) {
            case "boiler" -> {
                float scale = v(base, "Scale");
                out.add(block("Steam", VesselTuning.BOILER_STEAM_PER_TICK_MB * scale));
                out.add(block("Water", VesselTuning.BOILER_WATER_TANK_MB * scale));
            }
            case "gas_tank" -> {
                float safe = Math.min(v(base, "Capacity"), v(base, "Danger") * v(base, "MbPerPu"));
                out.add(block("SafeFill", safe));
            }
            case "heating_coil" -> {
                float power = v(base, "Amps") * ElectricRules.packetEu(VoltageBand.LV) * v(base, "Work");
                out.add(block("CoilPower", power));
                out.add(block("Rise", v(base, "Mass") > 0f ? power / v(base, "Mass") * FTuning.TICKS_PER_SECOND : 0f));
            }
            case "thermal_store" -> {
                float leak = v(base, "Leak");
                out.add(block("Hold", leak > 0f ? (float) Math.log(2) * v(base, "Mass") / leak / FTuning.TICKS_PER_SECOND : 0f));
            }
            case "leyden_jar" -> {
                float seep = v(base, "Seep");
                out.add(block("Holds", seep > 0f && seep < 1f ? (float) (Math.log(2) / -Math.log(1 - seep)) / FTuning.TICKS_PER_SECOND : 0f));
                VoltageBand band = VoltageBand.values()[Math.max(0, Math.min(VoltageBand.values().length - 1, (int) v(base, "Band")))];
                out.add(block("Charge", v(base, "StoresEu") / (ElectricMachineTuning.LEYDEN_JAR_CHARGE_AMPS * ElectricRules.packetEu(band)) / FTuning.TICKS_PER_SECOND));
            }
            case "gas_centrifuge" -> out.add(block("Headroom", v(base, "MinRpm") > 0f ? v(base, "TearRpm") / v(base, "MinRpm") : 0f));
            case "jet" -> {
                float liner = v(base, "LinerTu"), blade = v(base, "BladeTu");
                float safe = liner > 0f && blade > 0f ? Math.min(liner, blade) : Math.max(liner, blade);
                out.add(block("SafeFlame", safe));
                out.add(block("TipFull", VehicleTuning.JET_TIP_SPEED * (1f + VehicleTuning.JET_STAGE_TIP * v(base, "Stages"))));
                out.add(block("Fuel", VehicleTuning.JET_FUEL_MB_PER_TICK_M2 * v(base, "Intake")));
            }
            case "thruster" -> out.add(block("Economy", v(base, "Burn") > 0f ? v(base, "Force") / v(base, "Burn") : 0f));
            case "galvanic_cell" -> {
                var band = BlockFigures.galvanicBand(d, t, mats);
                float packet = ElectricRules.packetEu(band.orElse(VoltageBand.LV));
                band.ifPresent(b -> out.add(new Use("block.Supplies", Component.translatable(ModularMeasures.langKey("block.Supplies"),
                        Component.translatable(b.key())), b.ordinal())));
                float amps = v(base, "Supply");
                out.add(block("Runs", amps > 0f ? v(base, "AnodeEu") / (amps * packet) / FTuning.TICKS_PER_SECOND / 60f : 0f));
            }
            default -> {
            }
        }
        if (base.stream().anyMatch(u -> u.id().equals(ModularMeasures.BLOCK + "Resists"))) {
            float wps = Corrosion.wearPerSecond(v(base, "Resists"));
            Component eaten = wps <= 0f ? Component.translatable("gui.feedback.blueprint_table.never")
                    : Component.translatable("gui.feedback.blueprint_table.minutes", fmt(1f / wps / 60f));
            out.add(new Use("block.Eaten", Component.translatable(ModularMeasures.langKey("block.Eaten"), eaten), wps <= 0f ? 1e9f : 1f / wps));
        }
        return out;
    }

    // --- helpers --------------------------------------------------------------------------------

    /** A baked block figure by key, 0 if the design has none. */
    private static float v(List<Use> base, String key) {
        for (Use u : base)
            if (u.id().equals(ModularMeasures.BLOCK + key))
                return u.value();
        return 0f;
    }

    private static Use block(String key, float value) {
        return plain(ModularMeasures.BLOCK + key, value, fmt(value));
    }

    private static Use plain(String id, float value, String text) {
        return new Use(id, Component.translatable(ModularMeasures.langKey(id), text), value);
    }

    /** Three significant figures, no exponent: 1234 as 1230, 12.34 as 12.3, 0.1234 as 0.123. */
    static String fmt(float v) {
        if (Float.isNaN(v) || Float.isInfinite(v))
            return "-";
        double a = Math.abs(v);
        if (a >= 100)
            return Long.toString(Math.round(v));
        if (a == 0)
            return "0";
        int decimals = a >= 10 ? 1 : a >= 1 ? 2 : a >= 0.1 ? 3 : 4;
        String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }
}
