// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client pressed the "Gun option" key: flip option {@code index} of the held weapon's options (the server owns the state). */
public record GunOptionPayload(boolean offHand, int index) implements CustomPacketPayload {

    public static final Type<GunOptionPayload> TYPE = new Type<>(Feedback.id("gun_option"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunOptionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GunOptionPayload::offHand, ByteBufCodecs.VAR_INT, GunOptionPayload::index, GunOptionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunOptionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem gun) || !gun.mechanism().built(stack))
                return;
            var options = gun.mechanism().switches(stack);
            if (payload.index() < 0 || payload.index() >= options.size())
                return;
            var option = options.get(payload.index());
            gun.mechanism().setSwitch(stack, option.id(), !option.on());
            player.displayClientMessage(Component.translatable("message.feedback.option." + option.id(),
                    Component.translatable(option.on() ? "message.feedback.option.off" : "message.feedback.option.on")), true);
        });
    }
}
