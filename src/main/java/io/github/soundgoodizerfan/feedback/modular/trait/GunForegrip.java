// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.HandlingPhysics;

/**
 * Foregrip: the off hand pulls the muzzle down whatever the shooter is doing (a stock needs a crouch, a still body), so the cut is
 * baked into the gun's figures. The grip holds by its metal's friction and a stiff frame keeps the muzzle down
 * ({@link HandlingPhysics#foregrip}), by the variant's {@code leverage}.
 */
public class GunForegrip implements ModularTrait {
    @Override
    public GunFigures gunBaked(GunFigures f, GunSpec spec, Params p) {
        if (spec.hand().foregrip() == null)
            return f;
        var b = HandlingPhysics.foregrip(spec.hand().leverage(), spec.hand().foregrip());
        return f.withKick(b.kick()).withSpread(b.spread());
    }
}
