// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.Element;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;

import net.minecraft.resources.ResourceLocation;

/**
 * What an amethyst resonator or a sculk lattice design is, read off the parts and their materials, never an id. Both are circuit elements
 * ({@code Board.Kind.RESONATOR} and {@code LATTICE}) with fixed figures: nothing in them grows, drifts or wears.
 *
 * <h2>Resonator</h2>
 * A cut crystal between two electrodes. Its coupling is the crystal's own {@code piezo_coupling} (amethyst rings, calcite does not), its
 * {@code size} (mm) and {@code cut} (per cent; a narrow cut) are measurements set when the part is shaped. A bigger crystal is steadier and
 * slower in bandwidth; a narrower cut separates more channels and carries less. The electrodes' conductivity sets how well the crystal is
 * coupled to its wire.
 * <ul><li>{@code stability = piezo x size / (size + RESONATOR_STABLE_HALF_MM)}</li>
 * <li>{@code selectivity = 1 + round(piezo x cut x RESONATOR_SELECT_MAX)} channels</li>
 * <li>{@code bandwidth = piezo x electrode coupling x (1 - RESONATOR_NARROW x cut) x min(1, RESONATOR_WIDE_MM / size)}</li></ul>
 * The character is clock (few channels), filter (many) or band.
 *
 * <h2>Lattice</h2>
 * A living mesh in a frame. The body's material states what it can hold ({@code xp_store}: capacitance, conductance, leak: sculk, echo
 * shard); its {@code cells} and {@code mesh} (per cent) are set when it is shaped: a fine mesh holds more and learns less.
 * <ul><li>{@code memory = round(cells x (LATTICE_MESH_FLOOR + (1 - LATTICE_MESH_FLOOR) x mesh) x capacitance / LATTICE_CAPACITANCE_REF)} extra variables, at most {@link ProcessTuning#CIRCUIT_MEMORY_MAX}</li>
 * <li>{@code adaptation = (1 - LATTICE_FINE x mesh) x min(1, conductance / LATTICE_CONDUCTANCE_REF)}</li>
 * <li>{@code retention = (1 - leak / LATTICE_LEAK_REF) x LATTICE_RETENTION_WEIGHT + (1 - weight) x frame insulation share} (the frame's band over the top band): at {@link ProcessTuning#LATTICE_HOLDS_AT} the memory persists with the host unpowered.</li></ul>
 * The character is store (memory share well over adaptation), learner (the reverse) or mixed.
 */
public final class ElementDesign {

    private ElementDesign() {
    }

    /** The part names of the two types. */
    public static final String CRYSTAL = "resonator_crystal", ELECTRODE = "resonator_electrode", BODY = "lattice_body", FRAME = "lattice_frame";

    /** Whether {@code material} can do the job of the part named {@code role}: what the bench offers. */
    public static boolean suits(String role, Material m) {
        return switch (role) {
            case CRYSTAL -> m.electrical().piezoCoupling().isPresent();
            case ELECTRODE -> m.electrical().conductivity().isPresent();
            case BODY -> m.xpStore().isPresent();
            case FRAME -> m.electrical().insulation().isPresent();
            default -> true;
        };
    }

    /** The element a design is, or empty if a part is missing or made of something its role cannot use. */
    public static Optional<Element> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part crystal = partNamed(design, type, CRYSTAL), body = partNamed(design, type, BODY);
        if (crystal != null)
            return resonator(design, type, materials, crystal);
        if (body != null)
            return lattice(design, type, materials, body);
        return Optional.empty();
    }

    private static Part partNamed(ModularDesign design, ModularType type, String name) {
        for (Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            if (role != null && role.name().equals(name))
                return p;
        }
        return null;
    }

    private static Optional<Element> resonator(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials, Part crystal) {
        Part electrode = partNamed(design, type, ELECTRODE);
        Material cm = materials.get(crystal.material()), em = electrode == null ? null : materials.get(electrode.material());
        if (cm == null || em == null || !suits(CRYSTAL, cm) || !suits(ELECTRODE, em))
            return Optional.empty();
        float piezo = Math.max(0f, Math.min(1f, cm.electrical().piezoCoupling().get()));
        ModularType.Role role = type.roles().get(crystal.role());
        float size = role.axis(crystal, "size"), cut = role.axis(crystal, "cut") / 100f;
        float coupling = 0.5f + 0.5f * Math.min(1f, em.electrical().conductivity().get());
        float stability = piezo * size / (size + ProcessTuning.RESONATOR_STABLE_HALF_MM);
        int channels = 1 + Math.round(piezo * cut * ProcessTuning.RESONATOR_SELECT_MAX);
        float bandwidth = piezo * coupling * (1f - ProcessTuning.RESONATOR_NARROW * cut) * Math.min(1f, ProcessTuning.RESONATOR_WIDE_MM / size);
        Element.Character c = channels <= ProcessTuning.RESONATOR_CLOCK_MAX_CHANNELS ? Element.Character.CLOCK
                : channels >= ProcessTuning.RESONATOR_FILTER_MIN_CHANNELS ? Element.Character.FILTER : Element.Character.BAND;
        return Optional.of(new Element(Board.Kind.RESONATOR, c, new Circuit.Line(stability, channels, bandwidth, 0, 0f, 0f)));
    }

    private static Optional<Element> lattice(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials, Part body) {
        Part frame = partNamed(design, type, FRAME);
        Material bm = materials.get(body.material()), fm = frame == null ? null : materials.get(frame.material());
        if (bm == null || fm == null || !suits(BODY, bm) || !suits(FRAME, fm))
            return Optional.empty();
        Material.XpCapacity xp = bm.xpStore().get();
        ModularType.Role role = type.roles().get(body.role());
        float cells = role.axis(body, "cells"), mesh = role.axis(body, "mesh") / 100f;
        int memory = Math.min(ProcessTuning.CIRCUIT_MEMORY_MAX, Math.round(cells
                * (ProcessTuning.LATTICE_MESH_FLOOR + (1f - ProcessTuning.LATTICE_MESH_FLOOR) * mesh) * xp.capacitance() / ProcessTuning.LATTICE_CAPACITANCE_REF));
        float adaptation = (1f - ProcessTuning.LATTICE_FINE * mesh) * Math.min(1f, xp.conductance() / ProcessTuning.LATTICE_CONDUCTANCE_REF);
        io.github.soundgoodizerfan.feedback.core.unit.VoltageBand[] bands = io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.values();
        float shield = (fm.electrical().insulation().get().ordinal() + 1f) / bands.length;
        float retention = Math.max(0f, Math.min(1f, (1f - xp.leak() / ProcessTuning.LATTICE_LEAK_REF) * ProcessTuning.LATTICE_RETENTION_WEIGHT
                + (1f - ProcessTuning.LATTICE_RETENTION_WEIGHT) * shield));
        float held = memory / (float) ProcessTuning.CIRCUIT_MEMORY_MAX;
        Element.Character c = held >= adaptation + 0.2f ? Element.Character.STORE : adaptation >= held + 0.2f ? Element.Character.LEARNER : Element.Character.MIXED;
        return Optional.of(new Element(Board.Kind.LATTICE, c, new Circuit.Line(0f, 0, 0f, memory, retention, Math.max(0f, Math.min(1f, adaptation)))));
    }
}
