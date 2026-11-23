// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.compat.xaero.XaeroWaypoints;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A survey meter reading, to the one player who took it: where each found kind lies, coarsened by the meter, to pin as waypoints on a
 * map mod that takes them. Sent after the chat lines; a client with no such mod ignores it.
 */
public record SurveyPinPayload(List<Pin> pins) implements CustomPacketPayload {

    /** A kind's description key (a lang key or a plain word) and the coarse position of where most of it lies. */
    public record Pin(String key, BlockPos pos) {
        static final StreamCodec<RegistryFriendlyByteBuf, Pin> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Pin::key, BlockPos.STREAM_CODEC, Pin::pos, Pin::new);
    }

    public static final Type<SurveyPinPayload> TYPE = new Type<>(Feedback.id("survey_pin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SurveyPinPayload> STREAM_CODEC = StreamCodec.composite(
            Pin.CODEC.apply(ByteBufCodecs.list()), SurveyPinPayload::pins, SurveyPinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SurveyPinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> XaeroWaypoints.replace(payload.pins().stream()
                .map(p -> new XaeroWaypoints.Pin(p.pos().getX(), p.pos().getY(), p.pos().getZ(), I18n.get(p.key()))).toList()));
    }
}
