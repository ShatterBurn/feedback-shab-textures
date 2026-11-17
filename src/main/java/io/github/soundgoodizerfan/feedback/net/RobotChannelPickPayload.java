// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.robot.RobotWorker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the channel picked from the list, selected for a Programmer card's Link as the connector would. The server asks for the connector in hand. */
public record RobotChannelPickPayload(int channel) implements CustomPacketPayload {

    public static final Type<RobotChannelPickPayload> TYPE = new Type<>(Feedback.id("robot_channel_pick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RobotChannelPickPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RobotChannelPickPayload::channel, RobotChannelPickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RobotChannelPickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> RobotWorker.select(context.player(), payload.channel()));
    }
}
