// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.treatise;

import io.github.soundgoodizerfan.feedback.process.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * What researching a treatise volume asks of the researcher: the one property of a material the volume is about, which samples are put in
 * a row by in the research game ({@code ResearchScreen}). A volume is a subject; the way to learn it without a specimen is to find out, by
 * trials on stuff, how that subject's own property orders the stuff. Every volume of every {@link School} has one, in the order the schools
 * and their volumes are laid end to end ({@link School#bit}).
 */
public final class ResearchTheme {

    /** One volume's subject: {@code key} names the property (lang {@code gui.feedback.research.property.<key>}) and {@code figure} reads it. */
    public record Theme(School school, int volume, String key, Function<Material, OptionalDouble> figure) {
    }

    private static final List<Theme> THEMES = new ArrayList<>();

    static {
        add(School.WEAPONS, 1, "hardness", m -> OptionalDouble.of(m.hardness().value()));
        add(School.WEAPONS, 2, "yield", m -> m.mechanical().yieldStrength().map(s -> OptionalDouble.of(s.value())).orElse(OptionalDouble.empty()));
        add(School.WEAPONS, 3, "toughness", m -> m.mechanical().toughness().map(s -> OptionalDouble.of(s.value())).orElse(OptionalDouble.empty()));
        add(School.MECHANICAL, 1, "yield", m -> m.mechanical().yieldStrength().map(s -> OptionalDouble.of(s.value())).orElse(OptionalDouble.empty()));
        add(School.MECHANICAL, 2, "density", m -> OptionalDouble.of(m.density().muPerMb()));
        add(School.AEROSPACE, 1, "density", m -> OptionalDouble.of(m.density().muPerMb()));
        add(School.AEROSPACE, 2, "specific_heat", m -> m.thermal().specificHeat().map(c -> OptionalDouble.of(c.workPerMuPerTu())).orElse(OptionalDouble.empty()));
        add(School.ROBOTICS, 1, "conductivity", m -> m.electrical().conductivity().map(OptionalDouble::of).orElse(OptionalDouble.empty()));
        add(School.CHEMICAL, 1, "melting_point", m -> m.thermal().meltingPoint().map(t -> OptionalDouble.of(t.value())).orElse(OptionalDouble.empty()));
        add(School.CHEMICAL, 2, "density", m -> OptionalDouble.of(m.density().muPerMb()));
        add(School.CHEMICAL, 3, "specific_heat", m -> m.thermal().specificHeat().map(c -> OptionalDouble.of(c.workPerMuPerTu())).orElse(OptionalDouble.empty()));
        add(School.MATERIALS, 1, "hardness", m -> OptionalDouble.of(m.hardness().value()));
    }

    private ResearchTheme() {
    }

    private static void add(School school, int volume, String key, Function<Material, OptionalDouble> figure) {
        THEMES.add(new Theme(school, volume, key, figure));
    }

    /** How many volumes can be researched. */
    public static int count() {
        return THEMES.size();
    }

    /** The subject of the {@code index}-th volume (wrapping). */
    public static Theme of(int index) {
        return THEMES.get(Math.floorMod(index, THEMES.size()));
    }

    /** The figure {@code m} has for {@code theme}, or empty when it states none. */
    public static OptionalDouble figure(Theme theme, Material m) {
        return theme.figure().apply(m);
    }
}
