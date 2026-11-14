// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchBoard;
import io.github.soundgoodizerfan.feedback.modular.research.ResearchTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: how the research went, as what was done and never as a grade. {@code moves} are the placements in order ({@code concept@q,r});
 * {@code answer} the option chosen after the replay (-1 for none). The server replays them over the plan, seed and ink it sent
 * ({@link ResearchBoard}) and grades that.
 */
public record ResearchResultPayload(int container, List<String> moves, int answer) implements CustomPacketPayload {

    public static final Type<ResearchResultPayload> TYPE = new Type<>(Feedback.id("research_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ResearchResultPayload::container,
            ByteBufCodecs.stringUtf8(48).apply(ByteBufCodecs.list(ResearchBoard.MAX_MOVES)), ResearchResultPayload::moves,
            ByteBufCodecs.VAR_INT, ResearchResultPayload::answer,
            ResearchResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ResearchTableMenu menu
                    && menu.containerId == payload.container())
                menu.finishResearch(player, payload.moves(), payload.answer());
        });
    }
}
