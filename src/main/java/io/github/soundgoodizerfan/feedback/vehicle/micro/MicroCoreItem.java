/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The fine-space building gauge ({@code docs/dev/vehicle.md}): held in either hand while placing or mining motes it sets how they go (a
 * line, a box, mirrored, or as a wedge); sneak-use steps its mode. It places nothing itself: a mote starts fine space ({@link MicroPlacer}).
 */
public class MicroCoreItem extends Item {

    private static final int N = MicroCellBlockEntity.N;

    public MicroCoreItem(Properties properties) {
        super(properties);
    }

    /** Sneaking with the core in the air steps its mode, as on a block. */
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player,
                                                                                           net.minecraft.world.InteractionHand hand) {
        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !level.isClientSide)
            showMode(player, stack, MicroTool.cycle(stack));
        return player.isShiftKeyDown() ? net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
                : super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (!level.isClientSide)
                showMode(player, context.getItemInHand(), MicroTool.cycle(context.getItemInHand()));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // In mirror mode a click on a build sets a plane through the face clicked, or takes it away.
        if (player != null && MicroTool.mode(context.getItemInHand()) == MicroTool.Mode.MIRROR
                && level.getBlockEntity(context.getClickedPos()) instanceof MicroCellBlockEntity cell) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            int hit = cell.pick(context.getClickLocation(), context.getClickedFace());
            if (hit < 0)
                return InteractionResult.FAIL;
            BlockPos c = context.getClickedPos();
            BlockPos voxel = new BlockPos(c.getX() * N + (hit & 3), c.getY() * N + (hit >> 2 & 3), c.getZ() * N + (hit >> 4));
            boolean ok = MicroTool.togglePlane(context.getItemInHand(), voxel, context.getClickedFace());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    ok ? "message.feedback.micro.planes" : "message.feedback.micro.planes_full",
                    MicroTool.planes(context.getItemInHand()).size()), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** The build modes as a strip, the new one lit, with what it does beneath. */
    private static void showMode(Player player, net.minecraft.world.item.ItemStack stack, MicroTool.Mode mode) {
        String name = mode.name().toLowerCase(java.util.Locale.ROOT);
        io.github.soundgoodizerfan.feedback.net.AimPayloads.modes(player, stack.getHoverName(),
                io.github.soundgoodizerfan.feedback.net.AimPayloads.keys("message.feedback.micro.mode.short.", "single", "line", "box", "mirror", "wedge"),
                mode.ordinal(), net.minecraft.network.chat.Component.translatable("message.feedback.micro.mode." + name));
    }
}
