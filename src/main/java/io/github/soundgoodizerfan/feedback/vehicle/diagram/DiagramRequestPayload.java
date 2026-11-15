// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.diagram;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: an open diagram (or a desktop window) asks for the forces on the body a sensor sits
 * on, now and again every ten ticks while it is open. Each request keeps the body's recording alive.
 */
public record DiagramRequestPayload(BlockPos sensor, boolean assembly) implements CustomPacketPayload {

    public static final Type<DiagramRequestPayload> TYPE = new Type<>(Feedback.id("diagram_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DiagramRequestPayload::sensor,
            ByteBufCodecs.BOOL, DiagramRequestPayload::assembly,
            DiagramRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiagramRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                DiagramService.respond(player, payload.sensor(), payload.assembly());
        });
    }
}
