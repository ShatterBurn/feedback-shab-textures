// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.StringRepresentable;

/**
 * What a sealed vacuum tube is to a board: its figures at working warmth, read off the finished tube
 * (the modular type {@code vacuum_tube}, {@code modular/bake/TubeDesign}) when it is mounted and kept
 * in the board's part, so a circuit needs no material lookup.
 *
 * <p>Design from PowerGrid (Apache-2.0, design only, no code or assets): taken, a tube's emission
 * follows its heater's temperature (cold is slow and noisy, never refused), heater power follows the
 * emitter's size, and a tube's character (diode, triode, pentode, thyratron) is computed from built
 * geometry and materials, never an id. Rejected: typed continuous knobs on the part, an MNA solver,
 * overheat destroying the part, id-keyed assembly, voltage and watt displays.
 *
 * @param kind     the character, from the grid count and whether the seal holds gas
 * @param vacuum   0-1, how good the seal's vacuum came out (a dryer's depth, bake and the envelope's tightness)
 * @param gain     what the tube adds as an amplifier (0 for a diode)
 * @param speed    0-1, at working warmth
 * @param noise    0-1, at working warmth
 * @param power    standing heater power, Eu a tick, which the host pays
 * @param warmTu   the bulb temperature at which the cathode works
 * @param warmRate Tu a tick the heater raises the bulb by at its full power
 */
public record Tube(Kind kind, float vacuum, float gain, float speed, float noise, float power, float warmTu, float warmRate) {

    /** The character of a tube, computed and never chosen. */
    public enum Kind implements StringRepresentable {
        /** No grid: rectifies, amplifies nothing. */
        DIODE("diode"),
        /** One grid. */
        TRIODE("triode"),
        /** Two grids: a screen against the plate's pull on the first. */
        TETRODE("tetrode"),
        /** Three grids: a suppressor besides, quieter and faster. */
        PENTODE("pentode"),
        /** A grid and a seal that holds gas: it conducts in a flash when the grid lets it, and is slow to let go. */
        THYRATRON("thyratron");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String key;

        Kind(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "feedback.tube.kind." + key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final Codec<Tube> CODEC = RecordCodecBuilder.create(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(Tube::kind),
            Codec.FLOAT.fieldOf("vacuum").forGetter(Tube::vacuum),
            Codec.FLOAT.fieldOf("gain").forGetter(Tube::gain),
            Codec.FLOAT.fieldOf("speed").forGetter(Tube::speed),
            Codec.FLOAT.fieldOf("noise").forGetter(Tube::noise),
            Codec.FLOAT.fieldOf("power").forGetter(Tube::power),
            Codec.FLOAT.fieldOf("warm_tu").forGetter(Tube::warmTu),
            Codec.FLOAT.fieldOf("warm_rate").forGetter(Tube::warmRate)).apply(i, Tube::new));

    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Tube> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(Tube.CODEC);

    /** The character a grid count and a seal make: a seal that holds gas turns any gridded tube into a thyratron. */
    public static Kind kindOf(int grids, float vacuum) {
        if (grids <= 0)
            return Kind.DIODE;
        if (vacuum < ProcessTuning.TUBE_GAS_BELOW)
            return Kind.THYRATRON;
        return switch (Math.min(3, grids)) {
            case 1 -> Kind.TRIODE;
            case 2 -> Kind.TETRODE;
            default -> Kind.PENTODE;
        };
    }

    /** How far a bulb at {@code tu} has come toward its working temperature, 0 (ambient or cooler) to 1. */
    public static float warmth(float tu, float warmTu, float ambientTu) {
        if (warmTu <= ambientTu)
            return 1f;
        return Math.max(0f, Math.min(1f, (tu - ambientTu) / (warmTu - ambientTu)));
    }
}
