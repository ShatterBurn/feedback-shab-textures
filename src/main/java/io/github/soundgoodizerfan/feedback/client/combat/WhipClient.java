// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.punch.Punching;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.combat.whip.WhipPayload;
import io.github.soundgoodizerfan.feedback.combat.whip.WhipTuning;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Whiplash's key. A press throws the spear, or retrieves the one that is out; the line reels on its own once the spear lands. While the
 * line it holds is a swing (a block, or a creature too heavy to drag), {@link #winching} makes {@link GrappleSwing} draw the wearer in
 * along it, as holding use does for a grapple in hand. The wearer's momentum stays when the line ends.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class WhipClient {

    public static final KeyMapping WHIP = new KeyMapping("key.feedback.whip", InputConstants.KEY_B, "key.categories.feedback");

    private static boolean winching;

    private WhipClient() {
    }

    /** Whether the key has come up on a line that holds, and the reel is drawing the wearer in. */
    public static boolean winching() {
        return winching;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null)
            return;
        boolean pressed = false;
        while (WHIP.consumeClick())
            pressed = true;
        boolean active = mc.screen == null && !mc.isPaused() && !player.isSpectator()
                && !CombatGear.granting(player, WhipTuning.WHIPLASH).isEmpty();
        var hooks = GrappleHook.allOf(mc.level, player).stream().filter(h -> h.has(GrappleHook.WHIP)).toList();
        if (pressed && active) {
            // A spear out is retrieved; none out, one is thrown.
            PacketDistributor.sendToServer(new WhipPayload());
            if (hooks.isEmpty())
                player.swing(Punching.hand(player));
        }
        // A spear that holds a swing anchor draws the wearer in at once.
        winching = hooks.stream().anyMatch(GrappleHook::anchored);
    }
}
