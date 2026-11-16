// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.combat.RangedDispatch;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * The recurve bow's snap (spec 5.4): a bow drawn all the way ({@code AbstractArrow.isCritArrow},
 * which is vanilla's own full-draw flag) shoots an arrow that does {@code 1 + bonus} damage and
 * passes through {@code pierce} mob. Cost: a part draw gets neither, and the limbs draw
 * {@code draw_mult} slower, so the bonus is for the patient.
 */
public final class Recurve implements ModularTrait {

    @Override
    public float shotDamage(Params params, boolean fullDraw) {
        return fullDraw ? 1 + params.get("bonus", 0.25f) : 1f;
    }

    @Override
    public int shotPierce(Params params, boolean fullDraw) {
        return fullDraw ? (int) params.get("pierce", 1f) : 0;
    }

    @Override
    public void onArrow(AbstractArrow arrow, ItemStack weapon, Params params) {
        if (!arrow.isCritArrow())
            return;
        arrow.setBaseDamage(arrow.getBaseDamage() * shotDamage(params, true));
        RangedDispatch.pierce(arrow, shotPierce(params, true));
    }
}
