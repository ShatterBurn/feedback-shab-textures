// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.endernet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Endernet layer 3, addressable packets. A Computer with an endernet upgrade is a {@link Node}: it has a {@code channel} and an {@code address}
 * (both numbers tuned in the Network app; address 0 is none and a node with none neither sends nor is listed). A node sends a MESSAGE (short
 * text) or a FILE (a program, up to {@link ControlTuning#FILE_MAX_NODES} nodes) to one row of its directory: the live nodes on its channel in
 * its dimension. Everything is checked here, on the server; a client names only a row and the row's key.
 *
 * <p>Cost, all deterministic: {@code signals = max(1, ceil(bytes / PACKET_BYTES_PER_SIGNAL))}, each taken from the channel's shared bandwidth
 * ({@link EndernetBand}) and each paid through the sender's pair as any crossing is (teleportant scaled by the distance to the destination, a
 * unit of the paying port's bandwidth). A packet that cannot be paid for, finds a full inbox or a file with no blank medium to land on is
 * <em>lost</em>, and the sender is told which ({@link Result}); nothing arrives changed, and what was already paid stays paid. Everything else
 * on the channel overhears a message (its text, marked overheard) and notes a file's size, for free: the channel is open to all tuned to it.
 */
public final class Packets {

    /** Why a send did or did not go. */
    public enum Result {
        SENT, NO_ADDRESS, NO_PAIR, NO_ROW, EMPTY, TOO_BIG, LOST_BAND, LOST_PAY, LOST_FULL, LOST_NO_ROOM;

        public boolean ok() {
            return this == SENT;
        }

        public String key() {
            return "net_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** One mail in an inbox. {@code from} and {@code to} are addresses; an overheard mail was not for this node. */
    public record Mail(long time, int from, int to, boolean overheard, boolean file, String text) {
    }

    /** One row of a directory. */
    public record Row(int address, int distance, BlockPos pos) {
        /** The row's key: its index in the low byte and a hash of its place above, so a list that changed under a click is refused. */
        public static int key(int index, BlockPos pos) {
            return (index & 0xFF) | ((pos.hashCode() & 0x7FFFFF) << 8);
        }
    }

    /** A Computer on the net. */
    public interface Node extends Endernet.End {
        int channel();

        int address();

        /** Puts {@code mail} in the inbox; false when it is full (and an overheard copy is simply not kept). */
        boolean deliver(Mail mail);

        /** Lands {@code program} on a blank soft medium in a drive bay; false when there is none. */
        boolean land(ProgramGraph program);

        /** A line in this node's console. */
        void note(String key, String arg);
    }

    private Packets() {
    }

    private static List<Node> nodes(Node from) {
        Level level = from.getNodeLevel();
        List<Node> out = new ArrayList<>();
        if (level == null)
            return out;
        for (Endernet.End e : Endernet.live(level))
            if (e instanceof Node n && n != from && n.live() && n.address() > 0 && n.channel() == from.channel())
                out.add(n);
        out.sort(Comparator.<Node>comparingInt(Node::address).thenComparingInt(n -> n.getNodePos().getX()).thenComparingInt(n -> n.getNodePos().getY())
                .thenComparingInt(n -> n.getNodePos().getZ()));
        return out;
    }

    /** The rows {@code from} may send to: live addressed nodes on its channel, nearest sorted by address, at most {@link ControlTuning#DIRECTORY_MAX}. */
    public static List<Row> directory(Node from) {
        List<Row> out = new ArrayList<>();
        if (from.address() <= 0 || !from.live())
            return out;
        for (Node n : nodes(from)) {
            if (out.size() >= ControlTuning.DIRECTORY_MAX)
                break;
            out.add(new Row(n.address(), (int) Math.sqrt(from.getNodePos().distSqr(n.getNodePos())), n.getNodePos()));
        }
        return out;
    }

    /** Text cleaned of control characters, cut to {@link ControlTuning#MESSAGE_MAX} characters. */
    public static String clean(String text) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length() && b.length() < ControlTuning.MESSAGE_MAX; i++) {
            char c = text.charAt(i);
            if (c >= 32 && c != 127)
                b.append(c);
        }
        return b.toString().strip();
    }

    public static Result sendMessage(Node from, int index, int key, String text) {
        String body = clean(text);
        if (body.isEmpty())
            return Result.EMPTY;
        return send(from, index, key, body, null);
    }

    public static Result sendFile(Node from, int index, int key, ProgramGraph file) {
        if (file == null || file.nodes().isEmpty())
            return Result.EMPTY;
        if (file.nodes().size() > ControlTuning.FILE_MAX_NODES)
            return Result.TOO_BIG;
        return send(from, index, key, "", file);
    }

    private static Result send(Node from, int index, int key, String text, ProgramGraph file) {
        if (from.address() <= 0)
            return Result.NO_ADDRESS;
        Endernet.End far = Endernet.partner(from).orElse(null);
        if (far == null || !from.live())
            return Result.NO_PAIR;
        List<Row> rows = directory(from);
        if (index < 0 || index >= rows.size() || Row.key(index, rows.get(index).pos()) != key)
            return Result.NO_ROW;
        Node to = null;
        for (Node n : nodes(from))
            if (n.getNodePos().equals(rows.get(index).pos())) {
                to = n;
                break;
            }
        if (to == null)
            return Result.NO_ROW;
        int bytes = file == null ? text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length : file.nodes().size() * ControlTuning.FILE_BYTES_PER_NODE;
        int signals = Math.max(1, (bytes + ControlTuning.PACKET_BYTES_PER_SIGNAL - 1) / ControlTuning.PACKET_BYTES_PER_SIGNAL);
        Level level = from.getNodeLevel();
        if (!EndernetBand.take(level, from.channel(), signals))
            return Result.LOST_BAND;
        float blocks = (float) Math.sqrt(from.getNodePos().distSqr(to.getNodePos()));
        for (int i = 0; i < signals; i++)
            if (!(from.pay(blocks) || far.pay(blocks)))
                return Result.LOST_PAY;
        long now = level.getGameTime();
        if (file != null) {
            if (!to.land(file))
                return Result.LOST_NO_ROOM;
            String label = file.nodes().size() + "";
            if (!to.deliver(new Mail(now, from.address(), to.address(), false, true, label)))
                return Result.LOST_FULL;
        } else if (!to.deliver(new Mail(now, from.address(), to.address(), false, false, text)))
            return Result.LOST_FULL;
        for (Node other : nodes(from))
            if (other != to)
                other.deliver(new Mail(now, from.address(), to.address(), true, file != null, file == null ? text : String.valueOf(file.nodes().size())));
        return Result.SENT;
    }
}
