// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.diagram;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a Flight Sensor was used; open the Contraption Diagram of the body it sits on. */
public record DiagramOpenPayload(BlockPos sensor) implements CustomPacketPayload {

    public static final Type<DiagramOpenPayload> TYPE = new Type<>(Feedback.id("diagram_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramOpenPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, DiagramOpenPayload::sensor, DiagramOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on the client; the screen is in {@code client/}. */
    public static void handle(DiagramOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloads.openDiagram(payload));
    }
}
