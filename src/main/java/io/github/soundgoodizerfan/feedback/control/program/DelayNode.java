// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 1 in, 1 out of the same {@link ValueType}: the output is the input as it was {@code ticks}
 * ago. A state node: its output is history, so a wire cycle through it is legal (a latch, an
 * oscillator). {@code ticks} is one of {@link ControlTuning#DELAY_NAMED_TICKS} on any Controller;
 * any other count from {@link ControlTuning#DELAY_TYPED_MIN_TICKS} up is a typed delay and needs an
 * Escapement fitted to the Controller the card runs in.
 * <p>
 * {@code ticksInput} is an optional second port taking the count from any number: wired, the count is
 * that number rounded and clamped to {@code 1..DELAY_MAX_TICKS} each step and {@code ticks} is only the
 * fallback the editor shows; unwired, {@code ticks} is the count. The wire is a dependency (the count
 * is read before the history is), while {@code input} is not, so a cycle may still pass through
 * {@code input}. A wired count is always a typed delay.
 */
public record DelayNode(int id, int input, ValueType type, int ticks, int ticksInput) implements ProgramNode {

    /** A delay with the constant count only: nothing feeds its tick-count port. */
    public DelayNode(int id, int input, ValueType type, int ticks) {
        this(id, input, type, ticks, UNWIRED);
    }

    public static final String TYPE = "delay";

    public static final MapCodec<DelayNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(DelayNode::id),
            Codec.INT.fieldOf("input").forGetter(DelayNode::input),
            ValueType.CODEC.fieldOf("value_type").forGetter(DelayNode::type),
            Codec.intRange(1, ControlTuning.DELAY_MAX_TICKS).fieldOf("ticks").forGetter(DelayNode::ticks),
            Codec.INT.optionalFieldOf("ticks_input", UNWIRED).forGetter(DelayNode::ticksInput)
    ).apply(instance, DelayNode::new));

    public static final StreamCodec<ByteBuf, DelayNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DelayNode::id,
            ByteBufCodecs.VAR_INT, DelayNode::input,
            ValueType.STREAM_CODEC, DelayNode::type,
            ByteBufCodecs.VAR_INT, DelayNode::ticks,
            ByteBufCodecs.VAR_INT, DelayNode::ticksInput,
            DelayNode::new);

    /** Whether a number feeds the tick count. */
    public boolean countWired() {
        return ticksInput != UNWIRED;
    }

    /** The count a number on the tick-count port asks for: rounded, clamped to what a delay can hold. */
    public static int clampTicks(float requested) {
        if (!(requested > 1f))
            return 1;
        return requested >= ControlTuning.DELAY_MAX_TICKS ? ControlTuning.DELAY_MAX_TICKS : Math.round(requested);
    }

    /** Whether {@link #ticks} is on the named list (any Controller) rather than a typed count; a wired count never is. */
    public boolean named() {
        if (countWired())
            return false;
        for (int t : ControlTuning.DELAY_NAMED_TICKS)
            if (t == ticks)
                return true;
        return false;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }
}
