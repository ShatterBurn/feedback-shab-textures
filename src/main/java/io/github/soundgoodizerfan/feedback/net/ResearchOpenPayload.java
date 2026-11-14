// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: the Research Table's trial wants playing. {@code plan} is the research plan as authored ({@code ResearchPlan}), {@code ink}
 * what the specimen and the researcher's past deeds have earned the researcher to spend on links, {@code seed} lays the board out the same way
 * every time it is asked for. The client answers with one {@link ResearchResultPayload}.
 */
public record ResearchOpenPayload(int container, String plan, int ink, int seed) implements CustomPacketPayload {

    public static final Type<ResearchOpenPayload> TYPE = new Type<>(Feedback.id("research_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResearchOpenPayload::container,
            ByteBufCodecs.stringUtf8(32767), ResearchOpenPayload::plan,
            ByteBufCodecs.VAR_INT, ResearchOpenPayload::ink,
            ByteBufCodecs.VAR_INT, ResearchOpenPayload::seed,
            ResearchOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.ClientPayloads.openResearch(payload));
    }
}
