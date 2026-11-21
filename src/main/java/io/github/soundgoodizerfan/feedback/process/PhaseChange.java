// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * A fluid changing phase, authored as the {@code phase} block of a {@code thermal_process} row: the liquid boils to the gas above its
 * boiling point, spending {@code latent} Work per mB, and the gas condenses back below it, giving
 * the same Work back. The fluid ids say only the phase; the heat rides on the stack. Boiler and
 * condenser read this and name neither water nor steam.
 *
 * @param liquid       what boils, by tag or id.
 * @param gas          what it boils to (and what condenses), by tag or id.
 * @param liquidFluid  the fluid made when the gas condenses.
 * @param gasFluid     the fluid made when the liquid boils.
 * @param boil         the boiling point.
 * @param latent       Work per mB of the change.
 * @param lightShare   for a crude's cut: the share of the feed's light fraction this cut carries. Present
 *                     marks the boil as a distillation cut rather than a pure liquid's.
 */
public record PhaseChange(FluidIngredient liquid, FluidIngredient gas, ResourceLocation liquidFluid,
                          ResourceLocation gasFluid, Tu boil, float latent, Optional<Float> lightShare) {

    /** The {@code phase} block of a thermal process row: what boils, what it becomes, the Work per mB, and a distillation share. */
    public record Phase(FluidIngredient liquid, FluidIngredient gas, ResourceLocation liquidFluid, ResourceLocation gasFluid,
                        Optional<ResourceLocation> material, Optional<Float> latent, Optional<Float> lightShare) {
        public static final Codec<Phase> CODEC = RecordCodecBuilder.create(i -> i.group(
                FluidIngredient.CODEC.fieldOf("liquid").forGetter(Phase::liquid),
                FluidIngredient.CODEC.fieldOf("gas").forGetter(Phase::gas),
                ResourceLocation.CODEC.fieldOf("liquid_fluid").forGetter(Phase::liquidFluid),
                ResourceLocation.CODEC.fieldOf("gas_fluid").forGetter(Phase::gasFluid),
                ResourceLocation.CODEC.optionalFieldOf("material").forGetter(Phase::material),
                Codec.FLOAT.optionalFieldOf("latent").forGetter(Phase::latent),
                Codec.FLOAT.optionalFieldOf("light_share").forGetter(Phase::lightShare)
        ).apply(i, Phase::new));
    }

    /**
     * The view of a thermal process row's phase block, with its boiling point (the row's {@code min_temperature}). The Work per mB is the
     * row's {@code material}'s latent heat of vaporisation times its density times
     * {@link io.github.soundgoodizerfan.feedback.core.FTuning#BOIL_LATENT_SCALE}; a row that names no material (or one the table lacks)
     * carries its own {@code latent}.
     */
    public static PhaseChange of(ThermalProcess row, Phase p, java.util.Map<ResourceLocation, Material> materials) {
        float latent = p.material().map(materials::get).flatMap(m -> m.thermal().latentHeatVaporisation().map(h ->
                h.workPerMu() * m.density().muPerMb() * io.github.soundgoodizerfan.feedback.core.FTuning.BOIL_LATENT_SCALE))
                .orElse(p.latent().orElse(0f));
        return new PhaseChange(p.liquid(), p.gas(), p.liquidFluid(), p.gasFluid(), row.minTemperature(), latent, p.lightShare());
    }

    public static final Codec<PhaseChange> CODEC = RecordCodecBuilder.create(i -> i.group(
            FluidIngredient.CODEC.fieldOf("liquid").forGetter(PhaseChange::liquid),
            FluidIngredient.CODEC.fieldOf("gas").forGetter(PhaseChange::gas),
            ResourceLocation.CODEC.fieldOf("liquid_fluid").forGetter(PhaseChange::liquidFluid),
            ResourceLocation.CODEC.fieldOf("gas_fluid").forGetter(PhaseChange::gasFluid),
            Units.codec(Tu::new).fieldOf("boil").forGetter(PhaseChange::boil),
            Codec.FLOAT.fieldOf("latent").forGetter(PhaseChange::latent),
            Codec.FLOAT.optionalFieldOf("light_share").forGetter(PhaseChange::lightShare)
    ).apply(i, PhaseChange::new));

    public boolean boils(FluidStack stack) {
        return liquid.test(stack);
    }

    public boolean condenses(FluidStack stack) {
        return gas.test(stack);
    }
}
