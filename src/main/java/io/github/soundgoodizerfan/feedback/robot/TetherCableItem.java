// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.registry.FBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import org.jetbrains.annotations.Nullable;

/**
 * The cable between a tether reel and a robot, carrying its power. Use it on a reel to take that reel's end, then on a robot to tie the
 * robot to the reel; sneak-use on a robot unties it. The robot may roam no further than {@link RobotTuning#TETHER_LENGTH} blocks from the
 * reel, and a removed reel leaves it untied.
 */
public class TetherCableItem extends Item {

    private static final String REEL = "Reel";

    public TetherCableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getBlockState(pos).is(FBlocks.ROBOT_REEL.get()))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            CompoundTag tag = new CompoundTag();
            tag.putLong(REEL, pos.asLong());
            context.getItemInHand().set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("robot.feedback.tether_taken"), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** The reel this cable has been taken to, or null. */
    @Nullable
    public static BlockPos reel(ItemStack stack) {
        var custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || !custom.contains(REEL))
            return null;
        return BlockPos.of(custom.copyTag().getLong(REEL));
    }
}
