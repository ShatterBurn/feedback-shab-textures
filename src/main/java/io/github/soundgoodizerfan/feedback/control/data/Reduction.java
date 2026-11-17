// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** How a Sensor card combines several matching readings; one match is just its reading under any of them. */
public enum Reduction implements StringRepresentable {
    AVERAGE("average"), MIN("min"), MAX("max");

    public static final Codec<Reduction> CODEC = StringRepresentable.fromEnum(Reduction::values);
    public static final StreamCodec<ByteBuf, Reduction> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Reduction::ordinal);

    private final String name;

    Reduction(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public float combine(float[] values, int count) {
        if (count == 0)
            return 0f;
        float result = values[0];
        for (int i = 1; i < count; i++)
            result = switch (this) {
                case MIN -> Math.min(result, values[i]);
                case MAX -> Math.max(result, values[i]);
                case AVERAGE -> result + values[i];
            };
        return this == AVERAGE ? result / count : result;
    }
}
