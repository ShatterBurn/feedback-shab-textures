// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A worn item (in the Curios {@code face} slot) that can give its wearer air to breathe where the air is too thin. */
public interface BreathingGear {

    /**
     * Whether {@code worn} gives {@code wearer} breathable air this tick, where a lung would find {@code breathable}
     * (1 at sea level). Called once a tick, on the server, only while the air outside is too thin; the item spends
     * itself (a filter wears, a tank draws) when it does.
     */
    boolean supply(ItemStack worn, ServerPlayer wearer, float breathable);
}
