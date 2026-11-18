// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * How much ore one dimension holds and how patchy it is ({@code data/<ns>/ore_dimension/*.json}). A dimension with no
 * file holds no veins.
 *
 * @param dimension     the dimension's id.
 * @param slots         veins drawn per region (3 by 3 chunks); more is a richer world. Invented.
 * @param provinceScale how strongly provinces swing a deposit's weight here: 0 none, 1 the full range ({@link Provinces}).
 */
public record OreDimension(ResourceLocation dimension, int slots, float provinceScale) {

    public static final Codec<OreDimension> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(OreDimension::dimension),
            Codec.intRange(1, 64).fieldOf("slots").forGetter(OreDimension::slots),
            Codec.floatRange(0f, 1f).optionalFieldOf("province_scale", 1f).forGetter(OreDimension::provinceScale)
    ).apply(instance, OreDimension::new));
}
