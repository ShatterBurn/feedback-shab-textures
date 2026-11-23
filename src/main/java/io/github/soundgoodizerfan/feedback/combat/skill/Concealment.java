// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.WeaponProfile;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * Veil's concealed carry: a player holding a weapon that scores on stealth ({@link WeaponProfile#stealth}) is noticed
 * from nearer, in proportion to the share. A held item above the burn line glows and is seen: no help from it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Concealment {

    private Concealment() {
    }

    /** Stealth share a weapon needs to hide, and the most visibility it takes off at a full share ([OPEN]). */
    public static final float MIN_STEALTH = 0.6f, MAX_HIDE = 0.4f;

    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p))
            return;
        ItemStack held = p.getMainHandItem();
        // Cheapest test first: every mob that can see the player asks this each tick, and the skill lookup walks every skill tree.
        if (held.isEmpty())
            return;
        long now = p.level().getGameTime();
        if (now != askedTick || !p.getUUID().equals(askedPlayer)) {
            askedTick = now;
            askedPlayer = p.getUUID();
            double hide = 1.0;
            if (Skills.unlocked(p, "concealed_carry") && !ItemHeat.isHot(held, p.level())) {
                float stealth = WeaponProfile.of(held).stealth();
                if (stealth >= MIN_STEALTH)
                    hide = 1.0 - MAX_HIDE * stealth;
            }
            askedHide = hide;
        }
        if (askedHide != 1.0)
            event.modifyVisibility(askedHide);
    }

    // The answer holds for the tick: what a player holds and knows does not change between one mob's look and the next.
    private static java.util.UUID askedPlayer;
    private static long askedTick = Long.MIN_VALUE;
    private static double askedHide = 1.0;
}
