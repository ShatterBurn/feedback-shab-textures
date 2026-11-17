// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.ControlTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: a Network app action ({@code ComputerBlockEntity.NET_*}). It names a directory row (its index {@code a} and key {@code b}) and,
 * for a file, the bay {@code c}; a message carries its text, cut and cleaned on the server. Only honoured while that player has that
 * Computer's desktop open; the Computer checks everything again.
 */
public record ComputerNetPayload(BlockPos pos, int op, int a, int b, int c, String text) implements CustomPacketPayload {

    public static final Type<ComputerNetPayload> TYPE = new Type<>(Feedback.id("computer_net"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerNetPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerNetPayload::pos,
            ByteBufCodecs.VAR_INT, ComputerNetPayload::op,
            ByteBufCodecs.VAR_INT, ComputerNetPayload::a,
            ByteBufCodecs.VAR_INT, ComputerNetPayload::b,
            ByteBufCodecs.VAR_INT, ComputerNetPayload::c,
            ByteBufCodecs.stringUtf8(ControlTuning.MESSAGE_MAX * 4), ComputerNetPayload::text,
            ComputerNetPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ComputerNetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ComputerMenu menu)
                    || !menu.getPos().equals(payload.pos()) || menu.getComputer() == null)
                return;
            menu.getComputer().netAction(payload.op(), payload.a(), payload.b(), payload.c(), payload.text(), player);
        });
    }
}
