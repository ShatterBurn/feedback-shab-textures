// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.tether.FixedLines;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A hook on a line. Use throws the hook; holding use winches the line in and sneaking lets it go. What the
 * hook does is {@link GrappleHook}. A cast wears the grapple one point; a line that parts wears it
 * {@link ModularTuning#GRAPPLE_BREAK_WEAR}.
 */
public class GrappleItem extends Item {

    public GrappleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.sidedSuccess(stack, true);
        int fittings = stack.getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0);
        // A line is out: holding use winches it (client/GrappleSwing), sneaking lets go; a second head throws a second line.
        if (GrappleHook.allOf(level, player).size() >= ((fittings & GrappleHook.MULTI) != 0 ? 2 : 1))
            return InteractionResultHolder.sidedSuccess(stack, false);
        GrappleHook hook = new GrappleHook(level, player);
        hook.setFittings(fittings);
        var line = stack.get(FDataComponents.GRAPPLE_LINE.get());
        if (line != null)
            hook.setLine(line);
        hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, ModularTuning.GRAPPLE_SPEED, 0f);
        level.addFreshEntity(hook);
        level.playSound(null, player, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 0.9f);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * Used on a piton instead of thrown: the first piton is remembered, the second ties a fixed line between them, paid out of the grapple's line
     * (one point of wear per {@link ModularTuning#FIXED_LINE_BLOCKS_PER_WEAR} blocks, in the grapple's line material). Sneaking cuts the line the
     * piton holds.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (FixedLines.host(level, pos) == null)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.sidedSuccess(true);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null)
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            FixedLines.cut(level, pos);
            stack.remove(FDataComponents.GRAPPLE_TIE.get());
            player.displayClientMessage(Component.translatable("feedback.grapple.tie.cut"), true);
            return InteractionResult.CONSUME;
        }
        BlockPos first = stack.get(FDataComponents.GRAPPLE_TIE.get());
        if (first == null || first.equals(pos) || FixedLines.host(level, first) == null) {
            stack.set(FDataComponents.GRAPPLE_TIE.get(), pos);
            player.displayClientMessage(Component.translatable("feedback.grapple.tie.first"), true);
            return InteractionResult.CONSUME;
        }
        int fittings = stack.getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0);
        float span = FixedLines.span((fittings & GrappleHook.ROPE) != 0 ? ModularTuning.GRAPPLE_ROPE_LONG : ModularTuning.GRAPPLE_ROPE);
        double gap = Math.sqrt(first.distSqr(pos));
        if (gap > span) {
            player.displayClientMessage(Component.translatable("feedback.grapple.tie.far"), true);
            return InteractionResult.CONSUME;
        }
        var line = stack.get(FDataComponents.GRAPPLE_LINE.get());
        float paid = FixedLines.tie(level, first, pos, line != null ? line : Feedback.id("wool"));
        stack.remove(FDataComponents.GRAPPLE_TIE.get());
        if (paid >= 0f) {
            stack.hurtAndBreak(Math.max(1, Math.round(paid / ModularTuning.FIXED_LINE_BLOCKS_PER_WEAR)), player, LivingEntity.getSlotForHand(context.getHand()));
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 1f);
            player.displayClientMessage(Component.translatable("feedback.grapple.tie.done"), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int fittings = stack.getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0);
        var line = stack.get(FDataComponents.GRAPPLE_LINE.get());
        if (line != null)
            tooltip.add(Component.translatable("tooltip.feedback.grapple.line",
                    Component.translatable("material." + line.getNamespace() + "." + line.getPath())).withStyle(ChatFormatting.GRAY));
        for (GrappleUpgradeRecipe.Fitting f : GrappleUpgradeRecipe.FITTINGS)
            if ((fittings & f.bit()) != 0)
                tooltip.add(Component.translatable("tooltip.feedback.grapple." + f.name()).withStyle(ChatFormatting.GRAY));
    }

    /** Wears whichever grapple {@code player} holds. */
    public static void wear(Player player, int amount) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.getItem() instanceof GrappleItem) {
                s.hurtAndBreak(amount, player, LivingEntity.getSlotForHand(hand));
                return;
            }
        }
    }
}
