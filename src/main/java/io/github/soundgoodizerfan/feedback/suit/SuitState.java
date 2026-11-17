// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a worn suit remembers between ticks, on its chestplate: the heat the servos have left in it (Tu, 0 meaning it has not been warmed and sits at ambient), which abilities are on (a bit each,
 * {@link Ability#bit}) and whether the heat has cut them out, and the endernet pair the suit's upgrade is bonded under (its scans are shared to the Data Port at the other end). The battery is the vanilla container component beside it.
 */
public record SuitState(float heat, int toggles, boolean cut, java.util.Optional<java.util.UUID> pair) {

    public static final SuitState NEW = new SuitState(0f, 0, false, java.util.Optional.empty());

    public static final Codec<SuitState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.fieldOf("heat").forGetter(SuitState::heat),
            Codec.INT.fieldOf("toggles").forGetter(SuitState::toggles),
            Codec.BOOL.optionalFieldOf("cut", false).forGetter(SuitState::cut),
            net.minecraft.core.UUIDUtil.CODEC.optionalFieldOf("pair").forGetter(SuitState::pair)
    ).apply(i, SuitState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SuitState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SuitState::heat,
            ByteBufCodecs.VAR_INT, SuitState::toggles,
            ByteBufCodecs.BOOL, SuitState::cut,
            ByteBufCodecs.optional(net.minecraft.core.UUIDUtil.STREAM_CODEC), SuitState::pair,
            SuitState::new);

    public boolean on(Ability a) {
        return (toggles & a.bit()) != 0;
    }

    public SuitState with(Ability a, boolean on) {
        return new SuitState(heat, on ? toggles | a.bit() : toggles & ~a.bit(), cut, pair);
    }

    public SuitState withHeat(float h, boolean c) {
        return new SuitState(h, toggles, c, pair);
    }

    /** The state with the run's heat, switches and cut, keeping the bond. */
    public SuitState withRun(float h, int t, boolean c) {
        return new SuitState(h, t, c, pair);
    }

    public SuitState withPair(java.util.UUID id) {
        return new SuitState(heat, toggles, cut, java.util.Optional.of(id));
    }
}
