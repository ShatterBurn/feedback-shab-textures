// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: something done to a file on the Computer's internal disk. {@code index} is the file's place in the list the server last
 * sent, {@code drive} a bay, and the server bounds-checks both and refuses what would shift the list while a file is open in the editor; only honoured while that player has that Computer's desktop open.
 */
public record ComputerFileActionPayload(BlockPos pos, int op, int index, int drive) implements CustomPacketPayload {

    /** {@code SAVE}: the file in bay {@code drive} to the internal disk. {@code COPY} and {@code MOVE}: internal file {@code index} onto the blank medium in bay {@code drive}. */
    public static final int SAVE = 0, COPY = 1, MOVE = 2, DELETE = 3, RUN = 4, EDIT = 5;

    public static final Type<ComputerFileActionPayload> TYPE = new Type<>(Feedback.id("computer_file_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerFileActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerFileActionPayload::pos,
            ByteBufCodecs.VAR_INT, ComputerFileActionPayload::op,
            ByteBufCodecs.VAR_INT, ComputerFileActionPayload::index,
            ByteBufCodecs.VAR_INT, ComputerFileActionPayload::drive,
            ComputerFileActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ComputerFileActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ComputerMenu menu)
                    || !menu.getPos().equals(payload.pos()) || menu.getComputer() == null)
                return;
            ComputerBlockEntity computer = menu.getComputer();
            switch (payload.op()) {
                case SAVE -> computer.saveToInternal(payload.drive(), player);
                case COPY -> computer.writeFromInternal(payload.index(), payload.drive(), false, player);
                case MOVE -> computer.writeFromInternal(payload.index(), payload.drive(), true, player);
                case DELETE -> computer.deleteInternal(payload.index());
                case RUN -> computer.runInternal(payload.index(), player);
                case EDIT -> computer.editInternal(payload.index(), player);
                default -> { }
            }
        });
    }
}
