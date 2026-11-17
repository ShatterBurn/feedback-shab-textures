// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.control.data.DataRequest;
import io.github.soundgoodizerfan.feedback.control.data.Reduction;
import io.github.soundgoodizerfan.feedback.control.data.Signals;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * A number source that asks for a reading, not for a block: the {@code signal} it wants (a quantity, or any
 * other signal id a data block declares), the {@code kind} of block it prefers (null for any), the link
 * {@code color} that picks among blocks of one kind (-1 for any) and the {@code reduction} that combines
 * several matches. The Controller the card runs on binds the data blocks and resolves the request each step
 * (see {@link DataRequest}); the card carries no position, so a program works on any Controller whose blocks
 * answer it.
 * <p>
 * {@code signal} is null on a freshly placed card, which reads 0 until the player picks one. A number port is
 * typed by the signal ({@link ProgramGraph#wireError} refuses to compare two different ones).
 */
public record SensorNode(int id, @Nullable ResourceLocation signal, @Nullable ResourceLocation kind, int color,
                         Reduction reduction, int channel) implements ProgramNode {

    public static final String TYPE = "sensor";

    public SensorNode(int id, @Nullable ResourceLocation signal) {
        this(id, signal, null, DataRequest.ANY, Reduction.AVERAGE, 0);
    }

    public SensorNode withSignal(@Nullable ResourceLocation signal) {
        return new SensorNode(id, signal, kind, color, reduction, channel);
    }

    public SensorNode withKind(@Nullable ResourceLocation kind) {
        return new SensorNode(id, signal, kind, color, reduction, channel);
    }

    public SensorNode withColor(int color) {
        return new SensorNode(id, signal, kind, color, reduction, channel);
    }

    public SensorNode withReduction(Reduction reduction) {
        return new SensorNode(id, signal, kind, color, reduction, channel);
    }

    /** Which of a source's readings this card takes, for a source with several (a Driver's Seat's keys, by GLFW key code). */
    public SensorNode withChannel(int channel) {
        return new SensorNode(id, signal, kind, color, reduction, channel);
    }

    public DataRequest request() {
        return new DataRequest(signal, kind, color, reduction);
    }

    /** Whether this card reads a two-state signal, so a program treats it as a boolean. */
    public boolean isState() {
        return signal != null && Signals.isBoolean(signal);
    }

    public static final MapCodec<SensorNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(SensorNode::id),
            ResourceLocation.CODEC.optionalFieldOf("signal").forGetter(n -> Optional.ofNullable(n.signal())),
            ResourceLocation.CODEC.optionalFieldOf("kind").forGetter(n -> Optional.ofNullable(n.kind())),
            Codec.INT.optionalFieldOf("color", DataRequest.ANY).forGetter(SensorNode::color),
            Reduction.CODEC.optionalFieldOf("reduction", Reduction.AVERAGE).forGetter(SensorNode::reduction),
            Codec.INT.optionalFieldOf("channel", 0).forGetter(SensorNode::channel)
    ).apply(instance, (id, signal, kind, color, reduction, channel) ->
            new SensorNode(id, signal.orElse(null), kind.orElse(null), color, reduction, channel)));

    public static final StreamCodec<ByteBuf, SensorNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SensorNode::id,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), n -> Optional.ofNullable(n.signal()),
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), n -> Optional.ofNullable(n.kind()),
            ByteBufCodecs.VAR_INT, SensorNode::color,
            Reduction.STREAM_CODEC, SensorNode::reduction,
            ByteBufCodecs.VAR_INT, SensorNode::channel,
            (id, signal, kind, color, reduction, channel) ->
                    new SensorNode(id, signal.orElse(null), kind.orElse(null), color, reduction, channel));

    @Override
    public String typeKey() {
        return TYPE;
    }
}
