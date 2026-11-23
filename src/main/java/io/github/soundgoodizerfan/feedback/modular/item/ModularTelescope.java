// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A held telescope: an objective lens, an eyepiece lens and a tube. Held in use (as a spyglass is) the view narrows by its magnification
 * and softens by its blur; both are figures baked from the parts ({@code ModularBake}, family {@code TELESCOPE}). The view itself is a client
 * effect ({@code client/TelescopeView}); the server only knows the item is being used.
 */
public class ModularTelescope extends ModularItem {

    public static final String OBJECTIVE = "O";
    public static final String EYEPIECE = "E";
    public static final String TUBE = "T";

    public ModularTelescope(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModularStacks.broken(stack))
            return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
