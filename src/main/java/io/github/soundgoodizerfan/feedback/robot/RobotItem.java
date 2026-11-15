// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.modular.item.ModularItem;
import io.github.soundgoodizerfan.feedback.registry.FEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The robot as a held item; used on a block it stands there as a {@link RobotEntity}, its design, battery, program and cargo kept. */
public class RobotItem extends ModularItem {

    public RobotItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        String capped = RobotCaps.refusal(level, at);
        if (capped != null) {
            if (context.getPlayer() != null)
                context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable(capped), true);
            return InteractionResult.FAIL;
        }
        RobotEntity robot = FEntities.ROBOT.get().create(level);
        if (robot == null)
            return InteractionResult.FAIL;
        ItemStack held = context.getItemInHand();
        robot.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, context.getHorizontalDirection().toYRot(), 0f);
        if (!level.noCollision(robot, robot.getBoundingBox()))
            return InteractionResult.FAIL;
        robot.setup(held);
        level.addFreshEntity(robot);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild)
            held.shrink(1);
        return InteractionResult.CONSUME;
    }
}
