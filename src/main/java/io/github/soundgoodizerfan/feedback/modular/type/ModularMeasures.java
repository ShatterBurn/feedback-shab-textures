// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;

import net.minecraft.resources.ResourceLocation;

/**
 * Reads a type's {@link ModularType.Measures}: the one place that says which figures matter for a type (and for a part of it), in what
 * order, and which of them the Blueprint Table's swatches may be sorted by. The table, its sort and a blueprint's tooltip read only this.
 * <p>
 * A measure id is a figure (the lower-case {@link Fig} name), a number a family's bake gives beside the figures (one of {@link #USES},
 * or {@code block.<Key>} for a placed block's figure), or a part's as-built axis ({@code axis.<name>}). A type that declares none lists
 * every figure it bakes, as before ({@link #figs}); one that declares some lists exactly those, in that order, that it bakes.
 */
public final class ModularMeasures {

    private ModularMeasures() {
    }

    /** The numbers beside the figures that the Blueprint Table reads off a family's spec: id to the lang key of its line. */
    public static final Set<String> USES = Set.of(
            "cell.cells", "cell.band", "cell.capacity",
            "machine.frame", "machine.amps", "machine.band", "machine.rpm", "machine.heat",
            "tank.room", "tank.danger", "tank.critical",
            "vessel.points", "vessel.level",
            "pack.slots", "pack.carry", "pack.points", "pack.stores",
            "bed.comfort", "bed.warmth",
            "head.ceiling", "head.hardness",
            "egun.band", "egun.hold", "egun.pump", "egun.zoom", "egun.gauge", "egun.sense", "egun.lining", "egun.sink", "egun.lead", "egun.loss", "egun.charge", "egun.shots", "egun.bounces",
            "engine.cylinders", "engine.displacement", "engine.torque", "engine.peak", "engine.redline",
            "gearbox.gears", "gearbox.low", "gearbox.top", "gearbox.wear",
            "prop.blades", "prop.diameter", "prop.pitch",
            "belt.grip", "belt.heat",
            "suit.feeds", "suit.optic", "suit.spring", "suit.actuator", "suit.tread", "suit.filter", "suit.thermal", "suit.thruster", "suit.plate", "suit.coil", "suit.dispenser", "suit.cost", "suit.abilities",
            "robot.speed", "robot.dig", "robot.lift", "robot.step", "robot.drift",
            "pack.free", "pack.fire", "resp.airtime", "resp.filtertime", "bed.heal", "bed.nights",
            "dish.buffs", "dish.eat", "dish.seal", "dish.spill");

    /** The prefixes of ids that name a figure outside the two lists above: a placed block's, and a part's axis. */
    public static final String BLOCK = "block.", AXIS = "axis.";

    /** The lang key of a use line's label, which the board translates ({@code gui.feedback.blueprint_table.<id>}; a block's is {@code figure.feedback.block.<Key>}). */
    public static String langKey(String id) {
        return id.startsWith(BLOCK) ? "figure.feedback.block." + id.substring(BLOCK.length()) : id.startsWith(AXIS) ? "modular.feedback.axis." + id.substring(AXIS.length())
                : "gui.feedback.blueprint_table." + id;
    }

    /** The figure an id names, or empty for a use or an axis. */
    public static Optional<Fig> fig(String id) {
        if (id.indexOf('.') >= 0)
            return Optional.empty();
        try {
            return Optional.of(Fig.valueOf(id.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** The id of a figure. */
    public static String id(Fig fig) {
        return fig.name().toLowerCase(Locale.ROOT);
    }

    /** Whether {@code id} names something the table can read: a figure, a family number, a block figure or an axis. */
    public static boolean known(String id) {
        return fig(id).isPresent() || USES.contains(id) || id.startsWith(BLOCK) && id.length() > BLOCK.length() || id.startsWith(AXIS) && id.length() > AXIS.length();
    }

    /** Whether the type states what matters. A type that does not shows everything it bakes. */
    public static boolean declared(ModularType type) {
        return type != null && !type.measures().show().isEmpty();
    }

    /**
     * The ids the figure panel lists for {@code type} while part {@code role} is the one picked (null for none), in order: the part's
     * own measures first, then the rest of the type's. Empty when the type declares none.
     */
    public static List<String> show(ModularType type, String role, ModularDesign design) {
        if (!declared(type))
            return List.of();
        Set<String> out = new LinkedHashSet<>();
        if (role != null)
            out.addAll(type.measures().roles().getOrDefault(role, List.of()));
        out.addAll(type.measures().show());
        out.addAll(added(type, design));
        return List.copyOf(out);
    }

    /** What the fitted addons and chosen variants of {@code design} add: each fitted addon's, then each held variant's, in part order. */
    public static List<String> added(ModularType type, ModularDesign design) {
        if (type == null || design == null)
            return List.of();
        List<String> out = new ArrayList<>();
        for (ModularDesign.Part p : design.parts()) {
            out.addAll(type.measures().addons().getOrDefault(p.role(), List.of()));
            if (p.variant() != null && !p.variant().isEmpty())
                out.addAll(type.measures().variants().getOrDefault(p.variant(), List.of()));
        }
        return out;
    }

    /**
     * The ids the swatches of part {@code role} may be sorted by: the part's own, else the type's {@code sort}, else everything it shows.
     * Null when the type declares none (every listed figure sorts).
     */
    public static Set<String> sortKeys(ModularType type, String role, ModularDesign design) {
        if (!declared(type))
            return null;
        Set<String> out = new LinkedHashSet<>(added(type, design));
        List<String> own = role == null ? List.of() : type.measures().roles().getOrDefault(role, List.of());
        if (!own.isEmpty())
            out.addAll(own);
        else
            out.addAll(type.measures().sort().isEmpty() ? type.measures().show() : type.measures().sort());
        return out;
    }

    /**
     * The figures to list for a build with {@code figures}: those the type names, in its order (with the picked part's first), that the
     * build has; every figure it has when the type names none, or none of the named is baked, so the list is never blank.
     */
    public static List<Fig> figs(ModularType type, String role, ModularDesign design, ModularFigures figures) {
        List<Fig> out = new ArrayList<>();
        for (String id : show(type, role, design))
            fig(id).filter(f -> figures.get(f) != 0).ifPresent(out::add);
        if (out.isEmpty() && !hasUses(type))
            for (Fig f : Fig.values())
                if (figures.get(f) != 0)
                    out.add(f);
        return List.copyOf(out);
    }

    /** Whether the type names any measure that is not a figure: a type that lists only numbers is not given every figure as well. */
    private static boolean hasUses(ModularType type) {
        return show(type, null, null).stream().anyMatch(id -> fig(id).isEmpty());
    }

    /** Ids a type names that nothing can read, for the load log. */
    public static List<String> problems(Map<ResourceLocation, ModularType> types) {
        List<String> out = new ArrayList<>();
        types.forEach((id, t) -> {
            List<String> all = new ArrayList<>(t.measures().show());
            all.addAll(t.measures().sort());
            t.measures().roles().forEach((role, ids) -> {
                if (!t.roles().containsKey(role))
                    out.add(id + " measures: role '" + role + "' is not a role of the type");
                all.addAll(ids);
            });
            t.measures().addons().forEach((role, ids) -> {
                if (!t.roles().containsKey(role))
                    out.add(id + " measures: addon '" + role + "' is not a role of the type");
                all.addAll(ids);
            });
            Set<String> names = new java.util.HashSet<>();
            t.roles().values().forEach(r -> r.part().ifPresent(sh -> sh.variants().forEach(v -> names.add(v.name()))));
            t.measures().variants().forEach((name, ids) -> {
                if (!names.contains(name))
                    out.add(id + " measures: '" + name + "' is no variant of the type");
                all.addAll(ids);
            });
            for (String m : all)
                if (!known(m))
                    out.add(id + " measures: '" + m + "' is no figure, family number or axis");
        });
        return out;
    }
}
