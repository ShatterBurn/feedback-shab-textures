// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ElectricGunSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularMeasures;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.network.chat.Component;

/**
 * The numbers beside the figure bars on the Blueprint Table: what a family's bake gives that is not one of the shared figures (a tank's
 * room, a dynamotor's frame, an engine's displacement). Each is a {@link Use}: an id from {@link ModularMeasures#USES} (or
 * {@code block.<Key>}, {@code axis.<name>}), the line to print and the number to sort the swatches by, so every line sorts, a band by its rank.
 * Which of them a type lists is its {@code measures} ({@link ModularType.Measures}), never decided here.
 */
final class BenchUses {

    private BenchUses() {
    }

    /** One line: what it is ({@code id}), how it reads, and the number it sorts by. */
    record Use(String id, Component line, float value) {
    }

    private static Use num(String id, float value, String text) {
        return new Use(id, Component.translatable(ModularMeasures.langKey(id), text), value);
    }

    private static Use round(String id, float value) {
        return num(id, value, Long.toString(Math.round(value)));
    }

    private static Use fixed(String id, float value, String format) {
        return num(id, value, String.format(Locale.ROOT, format, value));
    }

    private static Use band(String id, VoltageBand b) {
        return new Use(id, Component.translatable(ModularMeasures.langKey(id), Component.translatable(b.key())), b.ordinal());
    }

    private static Use pct(String id, float share) {
        return num(id, share, Long.toString(Math.round(share * 100)));
    }

    /** Every number {@code design} of {@code type} has, in the family's own order, then its parts' as-built axes. */
    static List<Use> of(ModularDesign d, ModularType t) {
        List<Use> out = new ArrayList<>(family(d, t));
        out.addAll(axes(d, t));
        return out;
    }

    private static List<Use> family(ModularDesign d, ModularType t) {
        Map<net.minecraft.resources.ResourceLocation, Material> mats = ClientMaterials.get();
        return switch (t.family()) {
            case CELL -> ModularBake.cell(d, t, mats).<List<Use>>map(c -> List.of(
                    round("cell.cells", c.cells()), band("cell.band", c.caseBand()), round("cell.capacity", c.cells() * c.capacity()))).orElse(List.of());
            case MACHINE -> ModularBake.machine(d, t, mats).<List<Use>>map(m -> List.of(
                    round("machine.frame", m.frameSu()), round("machine.amps", m.ratedAmps()), band("machine.band", m.band()),
                    round("machine.rpm", m.ratedRpm()), round("machine.heat", m.serviceTu()))).orElse(List.of());
            case BLOCK -> io.github.soundgoodizerfan.feedback.modular.bake.BlockFigures.of(d, t, mats).<List<Use>>map(tag -> {
                List<Use> out = new ArrayList<>();
                for (String k : tag.getAllKeys()) {
                    String id = ModularMeasures.BLOCK + k;
                    float v = tag.getFloat(k);
                    out.add(k.equals("Band") ? new Use(id, Component.translatable(ModularMeasures.langKey(id), Component.translatable(VoltageBand.values()[(int) v].key())), v)
                            : fixed(id, v, "%.3g"));
                }
                return out;
            }).orElse(List.of());
            case ELECTRIC_GUN -> ElectricGunSpec.of(d, t, mats).map(BenchUses::egun).orElse(List.of());
            case PRESSURE -> ModularBake.tank(d, t, mats).<List<Use>>map(v -> List.of(
                    round("tank.room", v.volumeMb()), fixed("tank.danger", v.dangerPu(), "%.1f"), fixed("tank.critical", v.criticalPu(), "%.1f"))).orElse(List.of());
            case VESSEL -> ModularBake.xpStore(d, t, mats, null).<List<Use>>map(x -> List.of(
                    num("vessel.points", x.maxPoints(), Integer.toString(x.maxPoints())),
                    fixed("vessel.level", x.maxPoints() / x.perLevel(), "%.0f"))).orElse(List.of());
            case BACKPACK -> ModularBake.bake(d, t, mats, 0).<List<Use>>map(f -> List.of(
                    round("pack.slots", f.get(Fig.SLOTS)), fixed("pack.carry", f.get(Fig.CARRY), "%.1f"),
                    round("pack.points", f.get(Fig.POINTS)), round("pack.stores", f.get(Fig.STORES)))).orElse(List.of());
            case BED -> ModularBake.bake(d, t, mats, 0).<List<Use>>map(f -> List.of(
                    num("bed.comfort", f.get(Fig.COMFORT), Long.toString(Math.round(100 * f.get(Fig.COMFORT)))),
                    num("bed.warmth", f.get(Fig.WARMTH), Long.toString(Math.round(100 * f.get(Fig.WARMTH)))))).orElse(List.of());
            case HEAD -> ModularBake.bake(d, t, mats, 0).<List<Use>>map(f -> List.of(
                    fixed("head.ceiling", f.get(Fig.STRIKE_ST), "%.3g"), fixed("head.hardness", f.get(Fig.HARDNESS), "%.3g"))).orElse(List.of());
            case ENGINE -> ModularBake.engine(d, t, mats).<List<Use>>map(e -> {
                Material m = mats.get(e.material());
                List<Use> out = new ArrayList<>(List.of(round("engine.cylinders", e.cylinders()), fixed("engine.displacement", e.displacementMb(), "%.2f"),
                        round("engine.torque", e.peakTorque().value())));
                if (m != null) {
                    out.add(round("engine.peak", e.peakRpm(m).value()));
                    out.add(round("engine.redline", e.redline(m).value()));
                }
                return out;
            }).orElse(List.of());
            case GEARBOX -> ModularBake.gearbox(d, t, mats).<List<Use>>map(g -> {
                Material m = mats.get(g.material());
                List<Use> out = new ArrayList<>(List.of(round("gearbox.gears", g.gears()), fixed("gearbox.low", g.ratio(1), "%.2f"),
                        fixed("gearbox.top", g.ratio(g.gears()), "%.2f")));
                if (m != null)
                    out.add(fixed("gearbox.wear", g.wearPerRpm(m) * 1000f, "%.3g"));
                return out;
            }).orElse(List.of());
            case PROPELLER -> ModularBake.propeller(d, t, mats).<List<Use>>map(p -> List.of(
                    round("prop.blades", p.blades()), fixed("prop.diameter", (float) p.diameter(), "%.1f"), round("prop.pitch", p.pitch()))).orElse(List.of());
            case BELT -> ModularBake.belt(d, t, mats).<List<Use>>map(b -> List.of(
                    fixed("belt.grip", b.friction(), "%.2f"), round("belt.heat", b.maxTu()))).orElse(List.of());
            default -> List.of();
        };
    }

    /** The numbers a pulse gun's fittings read off their metals, which the shared figures do not carry: only those the build has. */
    private static List<Use> egun(ElectricGunSpec e) {
        List<Use> out = new ArrayList<>();
        out.add(band("egun.band", e.band()));
        if (e.holdCharge())
            out.add(pct("egun.hold", e.holdGain()));
        if (e.pump())
            out.add(pct("egun.pump", e.pumpGain()));
        if (e.zoom())
            out.add(pct("egun.zoom", e.zoomGain()));
        if (e.gauge())
            out.add(pct("egun.gauge", e.gaugeRes()));
        if (e.governor() != ElectricGunSpec.Governor.NONE)
            out.add(pct("egun.sense", e.sense()));
        if (e.coherence())
            out.add(pct("egun.lining", e.lining()));
        if (e.sinkK() > 0)
            out.add(pct("egun.sink", e.sinkK()));
        if (e.lead())
            out.add(pct("egun.lead", e.leadAmp()));
        out.add(pct("egun.loss", e.barrelLoss()));
        int ticks = e.chargeTicks();
        out.add(num("egun.charge", ticks, ticks + " t (" + io.github.soundgoodizerfan.feedback.client.tooltip.Readout.number(ticks / FTuning.TICKS_PER_SECOND) + " s)"));
        out.add(round("egun.shots", e.shotsPerBattery()));
        if (e.coherence())
            out.add(round("egun.bounces", e.bouncesAgainst(ModularTuning.BLOCK_REFLECT_STONE)));
        return out;
    }

    /** What each part's axes are set to, as {@code axis.<name>}: the first part that has the axis names it. */
    private static List<Use> axes(ModularDesign d, ModularType t) {
        List<Use> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ModularDesign.Part p : d.parts()) {
            ModularType.Role r = t.roles().get(p.role());
            if (r == null)
                continue;
            for (Map.Entry<String, ModularType.Axis> a : r.axesFor(p).entrySet()) {
                String name = a.getKey();
                if (!seen.add(name))
                    continue;
                float v = r.axis(p, name);
                String unit = a.getValue().unit();
                out.add(new Use(ModularMeasures.AXIS + name, Component.translatable("modular.feedback.axis." + name)
                        .append(": " + PartPresenter.num(v) + (unit == null || unit.isEmpty() ? "" : " " + unit)), v));
            }
        }
        return out;
    }
}
