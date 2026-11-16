// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.item.ModularHafted;
import io.github.soundgoodizerfan.feedback.modular.item.ModularMachineHead;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * How fast a machine head (roll, wire die, lathe bit, drill bit, abrasive wheel, and the hammer head a Hammer or Press takes) loses condition to the misuse it absorbs, read off its
 * own material. The machines give the head a quantity to take ({@code HeadSlot#wear}: St a blow the head could not carry, Wu a wheel
 * grinds); this says what share of it the head is worn by. A head is measured against an iron head of the same shape and size: the
 * tougher its edge (it absorbs a blow without chipping), the longer-lived its material (durability), the heavier it is (it spreads the
 * shock) and the better it resists corrosion (no pitted edge to start a crack), the less condition each unit of misuse costs. Wear is
 * still only from misuse (a head that is never overdriven absorbs nothing); the abrasive wheel's grit is the consumable exception.
 * <p>
 * Pure: the bench prints {@link #of} and the machine multiplies by {@link #factor(ItemStack)}.
 *
 * @param factor  the head's wear per unit of misuse against an iron head of the same shape (1 is iron's).
 * @param perUnit condition lost per unit of misuse (St, or Wu for the abrasive wheel) on the machine that takes the head.
 * @param wheel   whether the unit is Wu ground (an abrasive wheel) rather than St of overload.
 */
public record HeadWear(float factor, float perUnit, boolean wheel) {

    /**
     * Whether a design is a head a machine takes: a machine head type, or a hafted tool with a bench tool's striking face (the hammer head
     * seated in a Hammer or Press, with or without its haft).
     */
    public static boolean machineHead(ModularDesign design, ModularType type) {
        if (type.family() == ModularType.Family.HEAD)
            return true;
        if (type.family() != ModularType.Family.HAFTED)
            return false;
        for (ModularDesign.Part p : ModularFaces.faces(design))
            if (!ModularFaces.spent(p) && ModularFaces.kind(p, type).filter(h -> h.combat() == ModularType.Family.HAND && h.strike() > 0f).isPresent())
                return true;
        return false;
    }

    /** The type id of the abrasive wheel, whose wear runs per Wu ground. */
    public static final String WHEEL = "abrasive_wheel";

    /** Wear per unit of the head a design makes, or empty if it cannot be baked. */
    public static Optional<HeadWear> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Optional<ModularFigures> own = ModularBake.bake(design, type, materials, 0);
        if (own.isEmpty())
            return Optional.empty();
        float factor = factor(own.get(), reference(design, type, materials));
        boolean wheel = design.type().getPath().equals(WHEEL);
        return Optional.of(new HeadWear(factor, (wheel ? MachineTuning.GRINDING_WHEEL_WEAR_PER_WU : MachineTuning.SHOP_MACHINE_WEAR_PER_ST) * factor, wheel));
    }

    /** The factor of a head stack in a machine: its design's, 1 for a head with none or one that is not a machine head. */
    public static float factor(ItemStack head) {
        if (!(head.getItem() instanceof ModularMachineHead || head.getItem() instanceof ModularHafted))
            return 1f;
        ModularDesign design = ModularStacks.design(head);
        if (design == null)
            return 1f;
        ModularType type = ModularTypes.known().get(design.type());
        if (type == null || !machineHead(design, type))
            return 1f;
        int generation = MaterialTable.generation();
        if (generation != cacheGeneration) {
            CACHE.clear();
            cacheGeneration = generation;
        }
        return CACHE.computeIfAbsent(design, d -> of(d, type, MaterialTable.get().materials()).map(w -> w.factor).orElse(1f));
    }

    private static final Map<ModularDesign, Float> CACHE = new ConcurrentHashMap<>();
    private static volatile int cacheGeneration = -1;

    /** The same head cut from the reference metal, or empty if there is none to measure against. */
    private static Optional<ModularFigures> reference(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        ResourceLocation iron = ResourceLocation.parse(ModularTuning.HEAD_WEAR_REFERENCE);
        if (!materials.containsKey(iron))
            return Optional.empty();
        // A head set on a haft is judged on its own faces: the haft stays what it is.
        java.util.Set<String> faces = new java.util.HashSet<>();
        if (type.family() == ModularType.Family.HAFTED)
            for (ModularDesign.Part p : ModularFaces.faces(design))
                faces.add(p.role());
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (ModularDesign.Part p : design.parts())
            parts.add(type.family() == ModularType.Family.HAFTED && !faces.contains(p.role()) ? p
                    : new ModularDesign.Part(p.role(), iron, p.form(), p.count(), p.kind(), p.variant(), p.flaw(), p.measures(), p.sub()));
        return ModularBake.bake(design.withParts(parts), type, materials, 0);
    }

    private static float factor(ModularFigures own, Optional<ModularFigures> iron) {
        if (iron.isEmpty())
            return 1f;
        ModularFigures ref = iron.get();
        float f = ratio(ref.get(Fig.EDGE_TOUGHNESS), own.get(Fig.EDGE_TOUGHNESS), ModularTuning.HEAD_WEAR_TOUGHNESS_EXP)
                * ratio(ref.get(Fig.DURABILITY), own.get(Fig.DURABILITY), ModularTuning.HEAD_WEAR_DURABILITY_EXP)
                * ratio(ref.get(Fig.MASS), own.get(Fig.MASS), ModularTuning.HEAD_WEAR_MASS_EXP)
                * (1f + ModularTuning.HEAD_WEAR_CORROSION * (1f - own.get(Fig.CORROSION)))
                / (1f + ModularTuning.HEAD_WEAR_CORROSION * (1f - ref.get(Fig.CORROSION)));
        return Math.max(ModularTuning.HEAD_WEAR_RANGE[0], Math.min(ModularTuning.HEAD_WEAR_RANGE[1], f));
    }

    /** (reference over own) to {@code exp}; 1 when either is missing, so a head without the figure is judged on the others. */
    private static float ratio(float reference, float own, float exp) {
        return reference <= 0f || own <= 0f ? 1f : (float) Math.pow(reference / own, exp);
    }
}
