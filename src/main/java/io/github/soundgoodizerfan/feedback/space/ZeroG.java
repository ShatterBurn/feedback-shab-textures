// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A person in the Gap has no weight and no air to slow them: gravity is cancelled, and the vanilla air drag on a
 * body in the air (0.91 across, 0.98 along) is undone each tick, so a person keeps the speed they have and drifts
 * until something stops them. A jump from a hull plate is the push-off. Runs on both sides: a player's movement is
 * decided on their own client, and the server follows.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ZeroG {
    private static final net.minecraft.resources.ResourceLocation MODIFIER = Feedback.id("gap_zero_g");

    private ZeroG() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        var gravity = p.getAttribute(Attributes.GRAVITY);
        if (gravity == null)
            return;
        boolean gap = p.level().dimension().equals(SpaceTravel.GAP);
        boolean has = gravity.hasModifier(MODIFIER);
        if (gap && !has)
            gravity.addTransientModifier(new AttributeModifier(MODIFIER, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!gap && has)
            gravity.removeModifier(MODIFIER);
        if (gap && !p.onGround() && !p.isInWater() && !p.getAbilities().flying && !p.isFallFlying() && !p.isPassenger()) {
            var v = p.getDeltaMovement();
            p.setDeltaMovement(v.x / 0.91, v.y / 0.98, v.z / 0.91);
        }
    }
}
