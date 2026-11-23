// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.hazard.Coats;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A pot of glaze brushed onto one placed block: the block wears a coat ({@link Coats}) that taint and flux cannot eat through. One block a use,
 * one use a glaze item, and a block already glazed is left alone and the glaze kept. Glazing is the player's choice of what is worth the cost.
 */
public class TaintGlazeItem extends Item {

    public TaintGlazeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.sidedSuccess(true);
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!Coats.get(level).coat(pos, state)) {
            if (context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.feedback.taint_glaze.already"), true);
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
            context.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(false);
    }
}
