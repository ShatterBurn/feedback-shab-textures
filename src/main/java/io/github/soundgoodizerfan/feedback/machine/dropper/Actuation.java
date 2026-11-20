// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.dropper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * What a dispenser with an actuator does when its spring releases: the first item in it is used the way a hand would use it, on the block in
 * front (or, when that cell is empty, on the block beyond it, so a block item lands in the empty cell). The item is whatever is in the slot; nothing
 * here knows what it is for. The slot keeps the item as the use left it (a tool is worn, a stack used up shrinks), and an item that does nothing
 * gives the dry click of an empty dispenser.
 */
final class Actuation {

    private Actuation() {
    }

    static void use(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser)) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < dispenser.getContainerSize(); i++)
            if (!dispenser.getItem(i).isEmpty()) {
                slot = i;
                break;
            }
        if (slot < 0) {
            level.levelEvent(1001, pos, 0);
            return;
        }
        Direction facing = state.getValue(DispenserBlock.FACING);
        BlockPos front = pos.relative(facing);
        boolean frontFree = level.getBlockState(front).isAir() || level.getBlockState(front).canBeReplaced();
        BlockPos target = frontFree ? front.relative(facing) : front;
        // Nothing in front to press against: the arm cannot reach further than one cell, so it uses nothing.
        if (frontFree && (level.getBlockState(target).isAir() || level.getBlockState(target).canBeReplaced())) {
            level.levelEvent(1001, pos, 0);
            return;
        }
        var fake = FakePlayerFactory.getMinecraft(level);
        fake.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot(), facing.getAxis() == Direction.Axis.Y ? (facing == Direction.UP ? -90f : 90f) : 0f);
        ItemStack stack = dispenser.getItem(slot);
        fake.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).relative(facing.getOpposite(), 0.5), facing.getOpposite(), target, false);
        InteractionResult result = stack.useOn(new UseOnContext(level, fake, InteractionHand.MAIN_HAND, stack, hit));
        if (!result.consumesAction())
            result = stack.use(level, fake, InteractionHand.MAIN_HAND).getResult();
        ItemStack after = fake.getItemInHand(InteractionHand.MAIN_HAND);
        fake.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        dispenser.setItem(slot, after);
        if (!result.consumesAction())
            level.levelEvent(1001, pos, 0);
    }
}
