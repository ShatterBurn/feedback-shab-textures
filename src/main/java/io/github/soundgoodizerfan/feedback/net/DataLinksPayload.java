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

/** The links, and the link colors, of some nodes as the server holds them: sent when they change and when a chunk is first watched. */
public record DataLinksPayload(List<Entry> entries) implements CustomPacketPayload {

    /** The color a Controller gave the link to {@code node}. */
    public record Tint(DataNodeRef node, int color) {
    }

    public record Entry(DataNodeRef node, List<DataNodeRef> links, List<Tint> tints) {
    }

    public static final Type<DataLinksPayload> TYPE = new Type<>(Feedback.id("data_links"));

    private static final StreamCodec<RegistryFriendlyByteBuf, Tint> TINT = StreamCodec.composite(
            DataNodeRef.STREAM_CODEC, Tint::node,
            ByteBufCodecs.VAR_INT, Tint::color,
            Tint::new);

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY = StreamCodec.composite(
            DataNodeRef.STREAM_CODEC, Entry::node,
            DataNodeRef.STREAM_CODEC.apply(ByteBufCodecs.list()), Entry::links,
            TINT.apply(ByteBufCodecs.list()), Entry::tints,
            Entry::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, DataLinksPayload> STREAM_CODEC =
            ENTRY.apply(ByteBufCodecs.list()).map(DataLinksPayload::new, DataLinksPayload::entries);

    public static DataLinksPayload of(List<DataNodeRef> nodes, Map<DataNodeRef, Set<DataNodeRef>> links,
                                      Map<DataNodeRef, Map<DataNodeRef, Integer>> colors) {
        List<Entry> entries = new ArrayList<>(nodes.size());
        for (DataNodeRef node : nodes) {
            List<Tint> tints = new ArrayList<>();
            colors.getOrDefault(node, Map.of()).forEach((other, color) -> tints.add(new Tint(other, color)));
            entries.add(new Entry(node, new ArrayList<>(links.getOrDefault(node, Set.of())), tints));
        }
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
