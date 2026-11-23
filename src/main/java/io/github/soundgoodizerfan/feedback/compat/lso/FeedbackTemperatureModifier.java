// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import javax.annotation.Nullable;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import sfiomn.legendarysurvivaloverhaul.api.temperature.ModifierBase;
import sfiomn.legendarysurvivaloverhaul.registry.TemperatureModifierRegistry;

/**
 * Feedback's world temperature as an LSO body-temperature influence.
 *
 * <p>LSO's target is the sum of its modifiers: the dimension's figure, the biome's lean either side of it, altitude, night,
 * weather, and a seasons modifier from Serene Seasons. Feedback owns the dimension and the season (its ambient table, with its
 * own Serene Seasons shift), so this one adds the difference between Feedback's room and what LSO's dimension and seasons
 * modifiers would add; the rest of LSO's modifiers still lean on top. Then it adds the warmth or chill of Feedback machines
 * standing near (see {@link LsoHeat}).
 */
final class FeedbackTemperatureModifier extends ModifierBase {

    @Override
    public float getWorldInfluence(@Nullable Player player, Level level, BlockPos pos) {
        float theirs = TemperatureModifierRegistry.DIMENSION.get().getWorldInfluence(player, level, pos)
                + TemperatureModifierRegistry.SERENE_SEASONS.get().getWorldInfluence(player, level, pos);
        return AmbientTable.at(level).value() - theirs + LsoHeat.near(level, pos);
    }
}
