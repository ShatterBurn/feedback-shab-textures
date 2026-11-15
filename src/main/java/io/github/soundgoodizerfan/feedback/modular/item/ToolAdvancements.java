// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.EventTrigger;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Advancement witness for hafted tools: a block that needs the right tool, broken with one. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class ToolAdvancements {

    private ToolAdvancements() {
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || !(p.getMainHandItem().getItem() instanceof ModularHafted))
            return;
        if (event.getState().requiresCorrectToolForDrops() && p.getMainHandItem().isCorrectToolForDrops(event.getState()))
            EventTrigger.fire(p, "progress/survival/right_tool");
    }
}
