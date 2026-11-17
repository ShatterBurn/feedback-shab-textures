// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.EndernetFitting;
import io.github.soundgoodizerfan.feedback.machine.ender.EnderPortBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * An endernet upgrade: an antenna bonded under a pair id (made from a bonded Data Port, {@code machine/ender/EndernetUpgradeRecipe}).
 * Used on an Electric Controller or Computer it is fitted there, through the host's own say ({@code Fittable.canAdd}). One with no
 * bond (a creative one) does nothing.
 */
public class EndernetUpgradeItem extends Item {

    public EndernetUpgradeItem(Properties properties) {
        super(properties);
    }

    public static UUID pairOf(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(EnderPortBlockEntity.PAIR_KEY) ? data.copyTag().getUUID(EnderPortBlockEntity.PAIR_KEY) : null;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        UUID pair = pairOf(stack);
        if (pair == null || !(be instanceof Fittable fittable))
            return InteractionResult.PASS;
        EndernetFitting upgrade = new EndernetFitting(pair);
        if (!fittable.canAdd(upgrade))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            fittable.addUpgrade(upgrade);
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Used in the air by a wearer of a suit (a modular chestplate with a battery bay): fitted to the suit, bonding its scans under the pair. The Data
     * Port at the other end then hands the last scan to a program as channels ({@code suit/SuitScan}). A suit holds one; a second replaces it.
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID pair = pairOf(stack);
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (pair == null || !io.github.soundgoodizerfan.feedback.suit.ExoSuit.hasBay(chest))
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            chest.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.SUIT.get(),
                    io.github.soundgoodizerfan.feedback.suit.ExoSuit.state(chest).withPair(pair));
            player.displayClientMessage(Component.translatable("message.feedback.suit_endernet_fitted"), true);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(pairOf(stack) == null ? "tooltip.feedback.endernet_upgrade.unbonded" : "tooltip.feedback.endernet_upgrade.bonded")
                .withStyle(ChatFormatting.GRAY));
    }
}
