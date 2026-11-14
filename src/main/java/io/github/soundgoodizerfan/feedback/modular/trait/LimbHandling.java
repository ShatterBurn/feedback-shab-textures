// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

import java.util.HashMap;
import java.util.Map;

/**
 * How a bow's limbs or a crossbow's prod handle by their length, with no named archetypes: against the role's default length
 * ({@code r = length / default}), the draw or load time scales as {@code r^draw_exp}, the arrow's speed as {@code r^velocity_exp}, its
 * damage as {@code r^damage_exp}; a longer limb than the default also slows the wielder by {@code slow_per} per unit of {@code r - 1} and
 * lets the projectile through {@code floor((r - 1) * pierce_per)} mobs. The role names this trait and its exponents; {@link Handling}
 * runs the result, so a short limb is quick and light and a long one slow and hard, one continuous trade.
 */
public final class LimbHandling {

    private LimbHandling() {
    }

    /** The trait's params for {@code part} of {@code role}: the exponents applied to its length axis, as {@link Handling} reads them. */
    public static ModularTrait.Params derive(ModularType.Role role, ModularDesign.Part part) {
        Map<String, Float> base = role.params();
        Map<String, Float> out = new HashMap<>();
        var axes = role.axesFor(part);
        if (axes.isEmpty())
            return new ModularTrait.Params(out);
        String name = axes.keySet().iterator().next();
        float r = role.axis(part, name) / axes.get(name).def();
        out.put("draw_mult", (float) Math.pow(r, base.getOrDefault("draw_exp", 1f)));
        out.put("velocity_mult", (float) Math.pow(r, base.getOrDefault("velocity_exp", 0.5f)));
        out.put("damage_mult", (float) Math.pow(r, base.getOrDefault("damage_exp", 0f)));
        out.put("slow", Math.max(0f, (r - 1f) * base.getOrDefault("slow_per", 0.45f)));
        out.put("pierce", (float) Math.floor(Math.max(0f, (r - 1f) * base.getOrDefault("pierce_per", 0f))));
        return new ModularTrait.Params(out);
    }
}
