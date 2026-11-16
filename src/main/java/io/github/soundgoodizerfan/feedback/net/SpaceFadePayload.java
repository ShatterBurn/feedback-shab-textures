// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.hud.SpaceFade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A person aboard a body crossing between the Overworld and the Gap: the screen clears from black going up, from a hot white coming down. */
public record SpaceFadePayload(boolean up) implements CustomPacketPayload {

    public static final Type<SpaceFadePayload> TYPE = new Type<>(Feedback.id("space_fade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpaceFadePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SpaceFadePayload::up, SpaceFadePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpaceFadePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpaceFade.start(payload.up()));
    }
}
