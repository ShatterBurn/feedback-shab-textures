// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.space.SpaceTravel;
import io.github.soundgoodizerfan.feedback.space.SpaceTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Pushing off a wall in the Gap: a person who is floating beside a hull or any solid face and presses jump is sent away from it, as a jump
 * from a hull plate sends them off it ({@code space/ZeroG}). Weightless, there is nothing else to push against, so a wall is the only way to
 * start or change a drift. Runs on the player's own client, which decides their movement; the server follows.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class WallPush {

    private static boolean wasDown;

    private WallPush() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer p) || p != mc.player)
            return;
        boolean down = mc.options.keyJump.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || !p.level().dimension().equals(SpaceTravel.GAP) || p.onGround() || p.getAbilities().flying || p.isFallFlying()
                || p.isPassenger() || p.isInWater())
            return;
        // Which of the four sides the body touches: its box grown a hair toward that side meets something solid.
        AABB box = p.getBoundingBox();
        double dx = 0, dz = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            AABB grown = box.expandTowards(d.getStepX() * 0.06, 0, d.getStepZ() * 0.06);
            if (!p.level().noCollision(p, grown)) {
                dx -= d.getStepX();
                dz -= d.getStepZ();
            }
        }
        Vec3 away = new Vec3(dx, 0, dz);
        if (away.lengthSqr() < 1e-6)
            return;
        away = away.normalize().scale(SpaceTuning.WALL_PUSH_OFF);
        p.setDeltaMovement(p.getDeltaMovement().add(away));
        p.hasImpulse = true;
    }
}
