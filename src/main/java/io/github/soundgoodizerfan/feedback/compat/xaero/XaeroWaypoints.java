// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.xaero;

import java.lang.reflect.Method;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.neoforged.fml.ModList;

/**
 * Pins a survey meter reading as waypoints in Xaero's Minimap, when it is installed; does nothing without it. Client side only.
 *
 * <p>Xaero's Minimap (all rights reserved, closed source) publishes its jar on its own Maven server for other mods; no source was read.
 * The chain used was read from that jar's public signatures with {@code javap}: {@code BuiltInHudModules.MINIMAP} (a {@code HudModule}),
 * {@code getCurrentSession()}, {@code MinimapSession.getWorldManager()}, {@code MinimapWorldManager.getCurrentWorld()},
 * {@code MinimapWorld.getWaypointSet(String)} / {@code addWaypointSet(String)}, {@code WaypointSet.clear()} and {@code add(Waypoint)},
 * and {@code Waypoint(int x, int y, int z, String name, String symbol, WaypointColor)}. It is reached by reflection (no compile
 * dependency, nothing of its types in a signature here); a missing class or changed signature turns the pin off with one log line.
 *
 * <p>The pins go in their own set, {@value #SET}, which every survey reading clears and refills, so the reading is the only thing that
 * puts a waypoint on the map: nothing omniscient, and old readings do not pile up. Positions are the meter's own (coarsened by the
 * caller), never the exact block.
 */
public final class XaeroWaypoints {

    public static final String SET = "Feedback survey";

    private static boolean ready;
    private static boolean usable;
    private static Object module;
    private static Method getSession, getWorldManager, getCurrentWorld, getSet, addSet, clear, add;
    private static java.lang.reflect.Constructor<?> waypoint;
    private static Object colour;

    /** A pin: a block position and the name to show. */
    public record Pin(int x, int y, int z, String name) {
    }

    private XaeroWaypoints() {
    }

    /** Replaces the {@value #SET} waypoints with {@code pins}. Quiet when Xaero's is absent or not yet in a world. */
    public static void replace(List<Pin> pins) {
        init();
        if (!usable)
            return;
        try {
            Object session = getSession.invoke(module);
            if (session == null)
                return;
            Object world = getCurrentWorld.invoke(getWorldManager.invoke(session));
            if (world == null)
                return;
            Object set = getSet.invoke(world, SET);
            if (set == null) {
                addSet.invoke(world, SET);
                set = getSet.invoke(world, SET);
            }
            clear.invoke(set);
            for (Pin pin : pins) {
                String symbol = pin.name().isEmpty() ? "S" : pin.name().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
                add.invoke(set, waypoint.newInstance(pin.x(), pin.y(), pin.z(), pin.name(), symbol, colour));
            }
        } catch (Throwable t) {
            Feedback.LOGGER.warn("Xaero's Minimap pin failed, turning it off: {}", t.toString());
            usable = false;
        }
    }

    private static synchronized void init() {
        if (ready)
            return;
        ready = true;
        if (!ModList.get().isLoaded("xaerominimap"))
            return;
        try {
            Class<?> modules = Class.forName("xaero.hud.minimap.BuiltInHudModules");
            module = modules.getField("MINIMAP").get(null);
            getSession = Class.forName("xaero.hud.module.HudModule").getMethod("getCurrentSession");
            Class<?> session = Class.forName("xaero.hud.minimap.module.MinimapSession");
            getWorldManager = session.getMethod("getWorldManager");
            getCurrentWorld = Class.forName("xaero.hud.minimap.world.MinimapWorldManager").getMethod("getCurrentWorld");
            Class<?> world = Class.forName("xaero.hud.minimap.world.MinimapWorld");
            getSet = world.getMethod("getWaypointSet", String.class);
            addSet = world.getMethod("addWaypointSet", String.class);
            Class<?> set = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
            Class<?> wp = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            clear = set.getMethod("clear");
            add = set.getMethod("add", wp);
            Class<?> colours = Class.forName("xaero.hud.minimap.waypoint.WaypointColor");
            colour = colours.getField("YELLOW").get(null);
            waypoint = wp.getConstructor(int.class, int.class, int.class, String.class, String.class, colours);
            usable = true;
        } catch (ReflectiveOperationException | LinkageError e) {
            Feedback.LOGGER.warn("Xaero's Minimap is installed but its waypoint API did not match, survey pins are off: {}", e.toString());
        }
    }
}
