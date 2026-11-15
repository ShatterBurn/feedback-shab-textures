// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.diagram;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.joml.Vector3d;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Cause;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger.Sample;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import io.github.soundgoodizerfan.feedback.vehicle.diagram.DiagramDataPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Contraption Diagram's page: the body drawn from a chosen angle with an arrow for each force the
 * server recorded, coloured by cause, from where it acts. Shared by the screen and the Computer's window,
 * which only add controls around it. Arrow length is the force against the biggest shown; the figures a
 * hover prints are quantised to the sensor's own resolution (digest: numbers cost an instrument).
 * <p>
 * Merged arrows: forces of one cause that point within 0.4 rad of one another are summed into one arrow
 * at their force-weighted centre. The same idea as Create Simulated's cluster finder (design only); the
 * grouping here is its own greedy pass.
 */
public final class DiagramPage implements AutoCloseable {

    private static final double MERGE_COS = Math.cos(0.4);

    /** One arrow to draw: a summed force at a point of the body's plot frame. */
    private static final class Arrow {
        final Cause cause;
        double x, y, z, fx, fy, fz;
        double weight;
        int count;

        Arrow(Cause cause) {
            this.cause = cause;
        }

        double magnitude() {
            return Math.sqrt(fx * fx + fy * fy + fz * fz);
        }
    }

    public final BlockPos sensor;
    public float yaw = 0.8f, pitch = -0.5f;
    public final EnumSet<Cause> shown = EnumSet.allOf(Cause.class);
    public boolean merge = true, centre = true, assembly;

    private final BodyRender body = new BodyRender();
    private int sinceRequest = 100;

    public DiagramPage(BlockPos sensor) {
        this.sensor = sensor;
    }

    public void tick() {
        if (++sinceRequest >= 10) {
            sinceRequest = 0;
            DiagramFeed.request(sensor, assembly);
        }
    }

    public DiagramDataPayload data() {
        return DiagramFeed.latest(sensor);
    }

    public void turn(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(-1.4f, Math.min(1.4f, pitch + dPitch));
    }

    public Component massText() {
        DiagramDataPayload d = data();
        if (d == null || !d.onBody())
            return Component.empty();
        return Component.translatable("feedback.diagram.mass",
                Component.translatable("feedback.unit.mu", Instruments.format(new io.github.soundgoodizerfan.feedback.core.unit.Mu(d.massMu()).raw(), VehicleTuning.FLIGHT_MASS_RESOLUTION)));
    }

    /**
     * Draws the page into the {@code size} square at {@code x, y} and returns the hover text for the
     * arrow under the pointer, or null.
     */
    public Component draw(GuiGraphics g, GuiSkin skin, Font font, int x, int y, int size, int mx, int my, float partial) {
        skin.inset(g, x - 1, y - 1, size + 2, size + 2);
        DiagramDataPayload d = data();
        Minecraft mc = Minecraft.getInstance();
        if (d == null || DiagramFeed.age() > 3000) {
            g.drawCenteredString(font, Component.translatable("feedback.diagram.reading"), x + size / 2, y + size / 2 - 4, skin.dim());
            return null;
        }
        if (!d.onBody()) {
            g.drawCenteredString(font, Component.translatable("feedback.diagram.not_on_vehicle"), x + size / 2, y + size / 2 - 4, skin.warn());
            return null;
        }
        ClientLevel level = mc.level;
        if (level == null || !(SableCompanion.INSTANCE.getContainingClient(sensor) instanceof ClientSubLevel root))
            return null;
        List<ClientSubLevel> group = new ArrayList<>();
        group.add(root);
        float extent = extent(root);
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        for (UUID id : d.bodies())
            if (container.getSubLevel(id) instanceof ClientSubLevel other && other != root) {
                group.add(other);
                extent += extent(other);
            }
        body.render(root, group, yaw, pitch, extent, partial);
        body.blit(g, x, y, size);

        Vector3d right = body.view.transform(new Vector3d(1, 0, 0));
        Vector3d up = body.view.transform(new Vector3d(0, 1, 0));
        Vector3d toward = body.view.transform(new Vector3d(0, 0, 1));
        double half = size / 2.0, scale = half / body.radius;
        List<Arrow> arrows = arrows(d);
        double biggest = 1e-6;
        for (Arrow a : arrows)
            biggest = Math.max(biggest, a.magnitude());

        Component hover = null;
        double best = 7 * 7;
        g.enableScissor(x, y, x + size, y + size);
        for (Arrow a : arrows) {
            double rx = a.x - body.centre.x, ry = a.y - body.centre.y, rz = a.z - body.centre.z;
            int sx = (int) Math.round(x + half + (rx * right.x + ry * right.y + rz * right.z) * scale);
            int sy = (int) Math.round(y + half - (rx * up.x + ry * up.y + rz * up.z) * scale);
            double mag = a.magnitude();
            double vx = a.fx * right.x + a.fy * right.y + a.fz * right.z;
            double vy = a.fx * up.x + a.fy * up.y + a.fz * up.z;
            double along = (a.fx * toward.x + a.fy * toward.y + a.fz * toward.z) / mag;
            int colour = 0xFF000000 | a.cause.colour();
            double reach = Math.max(10, mag / biggest * size * 0.4);
            double flat = Math.sqrt(vx * vx + vy * vy) / mag;
            double d2;
            if (Math.abs(along) > 0.85 || flat < 1e-3) {
                // Pointing at or away from the viewer: a dot in a ring (toward) or a cross (away).
                GuiStyle.ring(g, sx, sy, 4, 1, colour);
                if (along > 0)
                    GuiStyle.disc(g, sx, sy, 1, colour);
                else {
                    GuiStyle.line(g, sx - 2, sy - 2, sx + 2, sy + 2, colour);
                    GuiStyle.line(g, sx - 2, sy + 2, sx + 2, sy - 2, colour);
                }
                d2 = (mx - sx) * (mx - sx) + (my - sy) * (my - sy);
            } else {
                double len = reach * Math.max(0.35, flat);
                double ux = vx / Math.sqrt(vx * vx + vy * vy), uy = -vy / Math.sqrt(vx * vx + vy * vy);
                int ex = (int) Math.round(sx + ux * len), ey = (int) Math.round(sy + uy * len);
                GuiStyle.line(g, sx, sy, ex, ey, colour);
                GuiStyle.line(g, ex, ey, (int) Math.round(ex - ux * 5 + uy * 3), (int) Math.round(ey - uy * 5 - ux * 3), colour);
                GuiStyle.line(g, ex, ey, (int) Math.round(ex - ux * 5 - uy * 3), (int) Math.round(ey - uy * 5 + ux * 3), colour);
                d2 = segment(mx, my, sx, sy, ex, ey);
            }
            if (mx >= x && mx < x + size && my >= y && my < y + size && d2 < best) {
                best = d2;
                Component figure = Component.translatable("feedback.unit.fu", Instruments.format(new io.github.soundgoodizerfan.feedback.core.unit.Fu((float) mag).raw(), VehicleTuning.FLIGHT_FORCE_RESOLUTION));
                hover = a.count > 1
                        ? Component.translatable("feedback.diagram.force_many", Component.translatable(a.cause.key()), figure, a.count)
                        : Component.translatable("feedback.diagram.force", Component.translatable(a.cause.key()), figure);
            }
        }
        if (centre) {
            double rx = d.comX() - body.centre.x, ry = d.comY() - body.centre.y, rz = d.comZ() - body.centre.z;
            int sx = (int) Math.round(x + half + (rx * right.x + ry * right.y + rz * right.z) * scale);
            int sy = (int) Math.round(y + half - (rx * up.x + ry * up.y + rz * up.z) * scale);
            GuiStyle.ring(g, sx, sy, 4, 1, skin.accent());
            GuiStyle.line(g, sx - 6, sy, sx + 6, sy, skin.accent());
            GuiStyle.line(g, sx, sy - 6, sx, sy + 6, skin.accent());
        }
        g.disableScissor();
        return hover;
    }

    private static float extent(ClientSubLevel sub) {
        var b = sub.getPlot().getBoundingBox();
        return Math.max(Math.max(b.maxX() - b.minX(), b.maxY() - b.minY()), b.maxZ() - b.minZ()) + 1;
    }

    /** Squared distance from a point to a segment. */
    private static double segment(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax, dy = by - ay, l = dx * dx + dy * dy;
        double t = l == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / l));
        double cx = ax + t * dx - px, cy = ay + t * dy - py;
        return cx * cx + cy * cy;
    }

    private List<Arrow> arrows(DiagramDataPayload d) {
        List<Arrow> out = new ArrayList<>();
        for (Sample s : d.samples()) {
            if (!shown.contains(s.cause()))
                continue;
            double m = Math.sqrt(s.fx() * s.fx() + s.fy() * s.fy() + s.fz() * s.fz());
            if (m < 1e-6)
                continue;
            Arrow home = null;
            if (merge)
                for (Arrow a : out)
                    if (a.cause == s.cause() && (a.fx * s.fx() + a.fy * s.fy() + a.fz * s.fz()) / (a.magnitude() * m) > MERGE_COS) {
                        home = a;
                        break;
                    }
            if (home == null) {
                home = new Arrow(s.cause());
                home.x = s.x();
                home.y = s.y();
                home.z = s.z();
                home.weight = m;
                out.add(home);
            } else {
                // Anchor at the force-weighted centre of what it sums.
                double w = home.weight + m;
                home.x = (home.x * home.weight + s.x() * m) / w;
                home.y = (home.y * home.weight + s.y() * m) / w;
                home.z = (home.z * home.weight + s.z() * m) / w;
                home.weight = w;
            }
            home.fx += s.fx();
            home.fy += s.fy();
            home.fz += s.fz();
            home.count++;
        }
        out.removeIf(a -> a.magnitude() < 1e-6);
        return out;
    }

    @Override
    public void close() {
        body.close();
    }
}
