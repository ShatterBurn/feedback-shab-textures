// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientTable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships which plain types a first volume's research takes as its specimen, so a recipe browser can list them (later volumes read the types' own schools). */
public record ResearchSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    /** The types (modular type paths) a volume of a school takes as its specimen. */
    public record Entry(String school, int volume, List<String> types) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::school,
                ByteBufCodecs.VAR_INT, Entry::volume,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::types,
                Entry::new);
    }

    public static final Type<ResearchSyncPayload> TYPE = new Type<>(Feedback.id("research_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSyncPayload> STREAM_CODEC =
            Entry.CODEC.apply(ByteBufCodecs.list()).map(ResearchSyncPayload::new, ResearchSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.RESEARCH.set(payload.entries()));
    }
}
