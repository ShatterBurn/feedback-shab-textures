// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.FireControl;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fire control: a circuit clipped into the gun reads the shot. A quiet one tightens the group by a bounded share ({@link FireControl#spread});
 * the rangefinder, lead and drop readout is drawn on the gun's card by the client. A gun with no circuit, or a dead one, keeps its base spread.
 */
public class GunFireControl implements ModularTrait {

    @Override
    public GunFigures gunAimed(Player shooter, ItemStack gun, GunFigures f, Params p) {
        float cut = FireControl.spread(FireControl.held(gun, FireControl.FIRE_CONTROL, shooter.level()));
        return cut >= 1f ? f : f.withSpread(cut);
    }
}
