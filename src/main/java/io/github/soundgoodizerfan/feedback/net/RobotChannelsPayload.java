// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server to client: a Data Connector was used on a robot or arm, so open the list of its channels. {@code selected} is the channel already held (-1 for none). */
public record RobotChannelsPayload(int selected) implements CustomPacketPayload {

    public static final Type<RobotChannelsPayload> TYPE = new Type<>(Feedback.id("robot_channels"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RobotChannelsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RobotChannelsPayload::selected, RobotChannelsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RobotChannelsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.screen.RobotChannelScreen.open(payload.selected()));
    }
}
