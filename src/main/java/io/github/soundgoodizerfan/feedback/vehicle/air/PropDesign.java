// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A propeller as baked from its modular design ({@code ModularBake#propeller}): how many blades, how wide the
 * disc is, how coarse the pitch, the blade metal (which sets the tip speed it survives) and a mass
 * multiplier. Every figure the prop block has is derived from these and the material at the moment it is
 * needed -- nothing stored that could disagree.
 *
 * @param blades     blades on the hub.
 * @param diameterCm disc diameter in cm (a block is a metre).
 * @param pitch      0 fine, 1 medium, 2 coarse: fine pulls hard from a standstill and tops out slow, coarse is the reverse.
 * @param material   the blades' metal.
 * @param massMu     what the propeller weighs, in Mu ({@link #massMu(int, int, float, float)}); the bench's mass figure and the vehicle's weight are this one number.
 * @param servo      a pitch linkage is fitted: a controller card sets the pitch as a level, reverse through fine to coarse.
 */
public record PropDesign(int blades, int diameterCm, int pitch, ResourceLocation material, float massMu, boolean servo) {

    public static final int MIN_BLADES = 2, MAX_BLADES = 6, MIN_CM = 80, MAX_CM = 600;

    public static final Codec<PropDesign> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(MIN_BLADES, MAX_BLADES).fieldOf("blades").forGetter(PropDesign::blades),
            Codec.intRange(MIN_CM, MAX_CM).fieldOf("diameter").forGetter(PropDesign::diameterCm),
            Codec.intRange(0, 2).fieldOf("pitch").forGetter(PropDesign::pitch),
            ResourceLocation.CODEC.fieldOf("material").forGetter(PropDesign::material),
            Codec.FLOAT.optionalFieldOf("mass", 0f).forGetter(PropDesign::massMu),
            Codec.BOOL.optionalFieldOf("servo", false).forGetter(PropDesign::servo)).apply(i, PropDesign::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PropDesign> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PropDesign::blades,
            ByteBufCodecs.VAR_INT, PropDesign::diameterCm,
            ByteBufCodecs.VAR_INT, PropDesign::pitch,
            ResourceLocation.STREAM_CODEC, PropDesign::material,
            ByteBufCodecs.FLOAT, PropDesign::massMu,
            ByteBufCodecs.BOOL, PropDesign::servo,
            PropDesign::new);

    /** The settings pulled back into range, so a design is always buildable. */
    public PropDesign {
        blades = Mth.clamp(blades, MIN_BLADES, MAX_BLADES);
        diameterCm = Mth.clamp(diameterCm, MIN_CM, MAX_CM);
        pitch = Mth.clamp(pitch, 0, 2);
    }

    /**
     * The one weight formula of a propeller, in Mu: a hub and, per blade, its length, at the mean density of hub and blades against
     * {@link io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning#PROP_REFERENCE_DENSITY}, the hub scaled by {@code hubMass}
     * (more blades, a bigger hub). {@code ModularBake#propeller} calls it.
     */
    public static float massMu(int blades, int diameterCm, float meanDensity, float hubMass) {
        return hubMass * meanDensity / io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.PROP_REFERENCE_DENSITY
                * (io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.PROP_HUB_MU
                + blades * diameterCm / 100f / 2f * io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning.PROP_BLADE_MU_PER_M);
    }

    /** What the propeller weighs: the one number the Blueprint Table's mass bar prints and the vehicle carries. */
    public io.github.soundgoodizerfan.feedback.core.unit.Mu mass() {
        return new io.github.soundgoodizerfan.feedback.core.unit.Mu(massMu);
    }

    /** Disc diameter in blocks (metres). */
    public double diameter() {
        return diameterCm / 100.0;
    }
}
