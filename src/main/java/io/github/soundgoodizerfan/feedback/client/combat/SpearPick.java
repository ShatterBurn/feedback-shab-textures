// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/**
 * A spear's own pick of what it strikes. Vanilla's crosshair stops at any block with an outline (grass, a flower, a vine) and takes an
 * entity's hitbox as it is; a spear is a long thin weapon, so its pick looks through blocks that have no collision shape (found by
 * the shape, never the block) and grows each target's hitbox by {@link ModularTuning#SPEAR_PICK_MARGIN}. The client picks, as vanilla's
 * does, and the blow is vanilla's attack on the entity found; the server's own reach check, which allows a block of margin, decides it.
 */
final class SpearPick {

    private SpearPick() {
    }

    /** The entity the held spear strikes now, or null for no spear or nothing there (the caller then takes vanilla's pick). */
    @Nullable
    static Entity pick(Minecraft mc, Player player) {
        if (mc.level == null || SpearCharge.of(player.getMainHandItem()).isEmpty())
            return null;
        double reach = player.entityInteractionRange();
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1f), end = eye.add(look.scale(reach));
        // Only a block with a collision shape stops the spear: the clip in collider mode passes grass and flowers by.
        var block = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS)
            end = block.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(mc.level, player, eye, end, new AABB(eye, end).inflate(1),
                e -> !e.isSpectator() && e.isPickable() && e.isAlive(), (float) ModularTuning.SPEAR_PICK_MARGIN);
        return hit == null ? null : hit.getEntity();
    }
}
