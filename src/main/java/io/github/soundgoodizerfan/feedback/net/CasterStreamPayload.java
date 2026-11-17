// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.StreamFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One tick of a fluid caster's stream for the clients to draw as particles: where it starts, which way it runs, how far, how wide it fans,
 * its colour and what kind it is. Sent only on a tick the valve is open; the server decides every hit and the client only draws.
 *
 * @param kind  {@link #LIQUID}, {@link #BURNING} or {@link #MIST}.
 * @param flowMb mB that left this tick: how thick the stream is drawn.
 * @param rgb   the colour, or -1 when the fluid has none of its own (the client takes its sprite's average, by {@code fluid}).
 * @param flame blocks from the start the nozzle's pilot flame stands out, or 0 if it is not lit.
 */
public record CasterStreamPayload(double x, double y, double z, float dx, float dy, float dz, float length, float spread, int rgb, byte kind,
                                  int flowMb, float flame, net.minecraft.resources.ResourceLocation fluid) implements CustomPacketPayload {

    public static final byte LIQUID = 0, BURNING = 1, MIST = 2;

    public static final Type<CasterStreamPayload> TYPE = new Type<>(Feedback.id("caster_stream"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CasterStreamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.dx);
                buf.writeFloat(p.dy);
                buf.writeFloat(p.dz);
                buf.writeFloat(p.length);
                buf.writeFloat(p.spread);
                buf.writeInt(p.rgb);
                buf.writeByte(p.kind);
                buf.writeVarInt(p.flowMb);
                buf.writeFloat(p.flame);
                buf.writeResourceLocation(p.fluid);
            },
            buf -> new CasterStreamPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readByte(), buf.readVarInt(), buf.readFloat(), buf.readResourceLocation()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, Vec3 from, Vec3 dir, float length, float spread, int rgb, byte kind, int flowMb, float flame,
                            net.minecraft.resources.ResourceLocation fluid) {
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, 96,
                new CasterStreamPayload(from.x, from.y, from.z, (float) dir.x, (float) dir.y, (float) dir.z, length, spread, rgb, kind, flowMb, flame, fluid));
    }

    public static void handle(CasterStreamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StreamFx.draw(payload));
    }
}
