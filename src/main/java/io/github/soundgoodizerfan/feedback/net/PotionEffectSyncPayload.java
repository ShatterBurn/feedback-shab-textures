// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.potion.PotionFeature;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the effect nodes of the brewing map (effect, length, top level) so the creative tab and a recipe browser can list one bottle per effect; never a position or a route. */
public record PotionEffectSyncPayload(List<PotionFeature.Effect> effects) implements CustomPacketPayload {

    public static final Type<PotionEffectSyncPayload> TYPE = new Type<>(Feedback.id("potion_effect_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotionEffectSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(PotionFeature.Effect.CODEC.listOf()), PotionEffectSyncPayload::effects,
            PotionEffectSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PotionEffectSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.POTION_EFFECTS.set(payload.effects()));
    }
}
