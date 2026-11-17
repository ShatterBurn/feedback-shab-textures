// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkStore;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The links of some nodes, as the server holds them: sent when they change and when a chunk is first watched. */
public record DataLinksPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(DataNodeRef node, List<DataNodeRef> links) {
    }

    public static final Type<DataLinksPayload> TYPE = new Type<>(Feedback.id("data_links"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY = StreamCodec.composite(
            DataNodeRef.STREAM_CODEC, Entry::node,
            DataNodeRef.STREAM_CODEC.apply(ByteBufCodecs.list()), Entry::links,
            Entry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, DataLinksPayload> STREAM_CODEC =
            ENTRY.apply(ByteBufCodecs.list()).map(DataLinksPayload::new, DataLinksPayload::entries);

    public static DataLinksPayload of(List<DataNodeRef> nodes, Map<DataNodeRef, Set<DataNodeRef>> links) {
        List<Entry> entries = new ArrayList<>(nodes.size());
        for (DataNodeRef node : nodes)
            entries.add(new Entry(node, new ArrayList<>(links.getOrDefault(node, Set.of()))));
        return new DataLinksPayload(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DataLinksPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DataLinkStore.receive(payload));
    }
}
