// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.control.data.DataRequest;
import io.github.soundgoodizerfan.feedback.control.data.Reduction;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * 1 boolean in, nothing out -- a terminal card. In a vehicle the input may be a number instead, and then it
 * drives an {@link io.github.soundgoodizerfan.feedback.control.Analog} part. It asks for blocks to drive by
 * {@code kind} (null until picked, which drives nothing: unlike a reading, an actuator never falls back to
 * "anything switchable") and link {@code color} (-1 for any), and drives every match. {@code input} is
 * {@link ProgramNode#UNWIRED} until wired to whatever feeds its condition.
 */
public record ActuatorNode(int id, int input, @Nullable ResourceLocation kind, int color, int channel) implements ProgramNode {

    public static final String TYPE = "actuator";

    public ActuatorNode(int id, int input) {
        this(id, input, null, DataRequest.ANY, 0);
    }

    public ActuatorNode withKind(@Nullable ResourceLocation kind) {
        return new ActuatorNode(id, input, kind, color, channel);
    }

    public ActuatorNode withColor(int color) {
        return new ActuatorNode(id, input, kind, color, channel);
    }

    /** Which of an {@link io.github.soundgoodizerfan.feedback.control.Analog} target's inputs a number drives (a chassis's steering or brakes); zero for a switch. */
    public ActuatorNode withChannel(int channel) {
        return new ActuatorNode(id, input, kind, color, channel);
    }

    public DataRequest request() {
        return new DataRequest(null, kind, color, Reduction.AVERAGE);
    }

    public static final MapCodec<ActuatorNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(ActuatorNode::id),
            Codec.INT.fieldOf("input").forGetter(ActuatorNode::input),
            ResourceLocation.CODEC.optionalFieldOf("kind").forGetter(n -> Optional.ofNullable(n.kind())),
            Codec.INT.optionalFieldOf("color", DataRequest.ANY).forGetter(ActuatorNode::color),
            Codec.INT.optionalFieldOf("channel", 0).forGetter(ActuatorNode::channel)
    ).apply(instance, (id, input, kind, color, channel) -> new ActuatorNode(id, input, kind.orElse(null), color, channel)));

    public static final StreamCodec<ByteBuf, ActuatorNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActuatorNode::id,
            ByteBufCodecs.VAR_INT, ActuatorNode::input,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), n -> Optional.ofNullable(n.kind()),
            ByteBufCodecs.VAR_INT, ActuatorNode::color,
            ByteBufCodecs.VAR_INT, ActuatorNode::channel,
            (id, input, kind, color, channel) -> new ActuatorNode(id, input, kind.orElse(null), color, channel));

    @Override
    public String typeKey() {
        return TYPE;
    }
}
