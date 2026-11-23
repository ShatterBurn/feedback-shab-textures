// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import java.lang.reflect.Method;

import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * The season's shift of an Overworld room's temperature, when Serene Seasons is installed; zero without it.
 *
 * <p>Serene Seasons (all rights reserved, public API only, no code read) does not move vanilla's biome temperature: its season only
 * decides snow, rain and freezing, so a kiln reads no winter unless this adds one. The season is read through its public
 * {@code SeasonHelper.getSeasonState(level).getSeason()} by reflection (no compile dependency, nothing of its signatures in ours);
 * a failed lookup turns the shift off for good. The shift is the same everywhere in the Overworld (the table keys dimensions,
 * not places), so winter makes every vessel there lose heat to a colder room and summer a little less. It is mapped from the
 * season's name to the figures below, which are ours.
 */
public final class SeasonalAmbient {

    /** Invented. The Tu a room is colder in winter and autumn and warmer in summer than its table figure; spring is the table's own. */
    private static final float WINTER = -15f, AUTUMN = -4f, SPRING = 0f, SUMMER = 4f;

    private static boolean looked;
    private static boolean usable;
    private static Method getState;
    private static Method getSeason;

    private SeasonalAmbient() {
    }

    /** Tu to add to the table's ambient for this level: nonzero only in the Overworld with Serene Seasons running. */
    public static float shift(Level level) {
        if (level == null || !level.dimension().equals(Level.OVERWORLD))
            return 0f;
        if (!looked) {
            looked = true;
            try {
                if (ModList.get().isLoaded("sereneseasons")) {
                    Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
                    getState = helper.getMethod("getSeasonState", Level.class);
                    getSeason = Class.forName("sereneseasons.api.season.ISeasonState").getMethod("getSeason");
                    usable = true;
                }
            } catch (Throwable t) {
                usable = false;
            }
        }
        if (!usable)
            return 0f;
        try {
            Object state = getState.invoke(null, level);
            Object season = getSeason.invoke(state);
            return switch (String.valueOf(season)) {
                case "WINTER" -> WINTER;
                case "AUTUMN" -> AUTUMN;
                case "SUMMER" -> SUMMER;
                default -> SPRING;
            };
        } catch (Throwable t) {
            usable = false;
            return 0f;
        }
    }
}
