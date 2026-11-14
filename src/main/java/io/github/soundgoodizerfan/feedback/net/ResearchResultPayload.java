// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: how the research went. {@code score} (0..100) is the quality of the notes; {@code strain} (0..100) how hard the specimen was pushed. */
public record ResearchResultPayload(int container, int score, int strain) implements CustomPacketPayload {

    public static final Type<ResearchResultPayload> TYPE = new Type<>(Feedback.id("research_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResearchResultPayload::container,
            ByteBufCodecs.VAR_INT, ResearchResultPayload::score,
            ByteBufCodecs.VAR_INT, ResearchResultPayload::strain,
            ResearchResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ResearchTableMenu menu
                    && menu.containerId == payload.container())
                menu.finishResearch(player, payload.score(), payload.strain());
        });
    }
}
