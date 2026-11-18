// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.whip;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The whip key went down: throw the spear, or retrieve the one that is out. The server decides everything. */
public record WhipPayload() implements CustomPacketPayload {

    public static final Type<WhipPayload> TYPE = new Type<>(Feedback.id("whip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhipPayload> STREAM_CODEC = StreamCodec.unit(new WhipPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhipPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Whipping.press((ServerPlayer) context.player());
        });
    }
}
