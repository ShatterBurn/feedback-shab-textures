// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.diagram;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientPayloads;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Cause;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Sample;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: the forces on a sensor's body, in the sensor body's plot frame, with the mass in
 * Mu and the centre of mass. {@code bodies} lists every body the figures cover (the sensor's own first;
 * more in assembly mode). {@code onBody} is false when the sensor is not on a Sable body.
 */
public record DiagramDataPayload(BlockPos sensor, boolean onBody, boolean assembly, float massMu,
                                 float comX, float comY, float comZ, List<UUID> bodies, List<Sample> samples)
        implements CustomPacketPayload {

    public static final Type<DiagramDataPayload> TYPE = new Type<>(Feedback.id("diagram_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiagramDataPayload> STREAM_CODEC = StreamCodec.of(
            DiagramDataPayload::write, DiagramDataPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, DiagramDataPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.sensor);
        buf.writeBoolean(p.onBody);
        buf.writeBoolean(p.assembly);
        buf.writeFloat(p.massMu);
        buf.writeFloat(p.comX);
        buf.writeFloat(p.comY);
        buf.writeFloat(p.comZ);
        buf.writeVarInt(p.bodies.size());
        for (UUID id : p.bodies)
            buf.writeUUID(id);
        buf.writeVarInt(p.samples.size());
        for (Sample s : p.samples) {
            buf.writeByte(s.cause().ordinal());
            buf.writeFloat(s.x());
            buf.writeFloat(s.y());
            buf.writeFloat(s.z());
            buf.writeFloat(s.fx());
            buf.writeFloat(s.fy());
            buf.writeFloat(s.fz());
        }
    }

    private static DiagramDataPayload read(RegistryFriendlyByteBuf buf) {
        BlockPos sensor = BlockPos.STREAM_CODEC.decode(buf);
        boolean onBody = buf.readBoolean(), assembly = buf.readBoolean();
        float mass = buf.readFloat(), cx = buf.readFloat(), cy = buf.readFloat(), cz = buf.readFloat();
        int nb = buf.readVarInt();
        List<UUID> bodies = new ArrayList<>(nb);
        for (int i = 0; i < nb; i++)
            bodies.add(buf.readUUID());
        int ns = buf.readVarInt();
        List<Sample> samples = new ArrayList<>(ns);
        Cause[] causes = Cause.values();
        for (int i = 0; i < ns; i++)
            samples.add(new Sample(causes[Math.floorMod(buf.readByte(), causes.length)],
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        return new DiagramDataPayload(sensor, onBody, assembly, mass, cx, cy, cz, bodies, samples);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DiagramDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloads.diagramData(payload));
    }
}
