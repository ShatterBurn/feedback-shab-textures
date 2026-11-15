// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.space.ShelfMapView;
import io.github.soundgoodizerfan.feedback.space.MapData;
import io.github.soundgoodizerfan.feedback.space.SpaceTravel;
import io.github.soundgoodizerfan.feedback.space.SpaceTuning;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Shelf's map below the craft: the client asks for a square of chunks around it, the server answers with the packed colours. */
public final class ShelfMapPayloads {
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ShelfMapPayloads() {
    }

    /** Client to server: the centre chunk of the square wanted. */
    public record Request(int cx, int cz) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Feedback.id("shelf_map_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Request::cx, ByteBufCodecs.VAR_INT, Request::cz, Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request r, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (!(ctx.player() instanceof ServerPlayer p) || !p.level().dimension().equals(SpaceTravel.SHELF))
                    return;
                long now = p.level().getGameTime();
                if (now - LAST.getOrDefault(p.getUUID(), -1000L) < SpaceTuning.MAP_REQUEST_INTERVAL_TICKS)
                    return;
                LAST.put(p.getUUID(), now);
                int size = 2 * SpaceTuning.MAP_RADIUS_CHUNKS + 1;
                int x0 = r.cx() - SpaceTuning.MAP_RADIUS_CHUNKS, z0 = r.cz() - SpaceTuning.MAP_RADIUS_CHUNKS;
                PacketDistributor.sendToPlayer(p, new Tile(x0, z0, size, MapData.of(p.getServer().overworld()).tile(x0, z0, size)));
            });
        }
    }

    /** Server to client: {@code size} by {@code size} packed colours from chunk ({@code x0}, {@code z0}). */
    public record Tile(int x0, int z0, int size, byte[] colours) implements CustomPacketPayload {
        public static final Type<Tile> TYPE = new Type<>(Feedback.id("shelf_map_tile"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Tile> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Tile::x0, ByteBufCodecs.VAR_INT, Tile::z0, ByteBufCodecs.VAR_INT, Tile::size,
                ByteBufCodecs.BYTE_ARRAY, Tile::colours, Tile::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Tile t, IPayloadContext ctx) {
            ctx.enqueueWork(() -> ShelfMapView.accept(t.x0(), t.z0(), t.size(), t.colours()));
        }
    }
}
