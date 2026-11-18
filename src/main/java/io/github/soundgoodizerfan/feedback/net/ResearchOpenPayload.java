// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: the Research Table's trial wants playing. {@code plan} is the research plan as authored ({@code ResearchPlan}), {@code ink}
 * what the specimen and the researcher's past deeds have earned the researcher to spend on links, {@code seed} lays the board out the same way
 * every time it is asked for. {@code kind} is the Research Table's mode (0 treatise, 1 attunement, 2 app), which picks the game; {@code tier} is the
 * treatise volume's number (1 for the rest); {@code jokers} are the held volumes' trait lists ({@code TreatiseHand.encode}, empty for the other games). The client answers with one {@link ResearchResultPayload}.
 */
public record ResearchOpenPayload(int container, String plan, int ink, int seed, int kind, int tier, String jokers) implements CustomPacketPayload {

    public static final Type<ResearchOpenPayload> TYPE = new Type<>(Feedback.id("research_open"));

    /** Written by hand: seven fields are more than {@code StreamCodec.composite} takes. */
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchOpenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.container);
                buf.writeUtf(p.plan, 32767);
                buf.writeVarInt(p.ink);
                buf.writeVarInt(p.seed);
                buf.writeVarInt(p.kind);
                buf.writeVarInt(p.tier);
                buf.writeUtf(p.jokers, 2048);
            },
            buf -> new ResearchOpenPayload(buf.readVarInt(), buf.readUtf(32767), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(2048)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.ClientPayloads.openResearch(payload));
    }
}
