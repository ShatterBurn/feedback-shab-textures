// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.WoodCuts;
import io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A saw blade held to a log or plank block in the world: hold use, one stroke every {@link ItemTuning#HAND_STROKE_TICKS} ticks, each
 * delivering the blade's drive over the wood's hardness ({@code St / hardness}, as every stroke does) of the block's work. When the work is done the
 * block is sawn: a log into its planks, a plank block into slabs (the cuts {@link WoodCuts} lists for the bench), and the kerf falls as the blade's
 * shavings ({@link WoodCuts#kerfShavings}). A blade softer than the wood cuts nothing (the hard gate); the blade wears one point per stroke.
 *
 * <p>The stroke count is a pure function of the figures, so nothing is stored while sawing: the target is picked again at each stroke and the cut
 * is abandoned if the look moves off it. The held-use shape is the same as {@link HandWork}'s (read from Create's sandpaper, see there).
 */
public final class SawWork {

    private SawWork() {
    }

    /** Whether {@code stack} is a saw blade that works (not broken). */
    public static boolean isSaw(ItemStack stack) {
        return stack.is(FModular.SAWBLADE.get()) && !ModularStacks.broken(stack);
    }

    /** Use: start sawing the log or plank block in reach, if this blade is hard enough for its wood. */
    public static InteractionResultHolder<ItemStack> begin(Level level, Player player, InteractionHand hand) {
        ItemStack saw = player.getItemInHand(hand);
        if (!isSaw(saw))
            return InteractionResultHolder.pass(saw);
        BlockPos pos = target(level, player);
        if (pos == null || strokes(saw, level.getBlockState(pos)) < 0)
            return InteractionResultHolder.pass(saw);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(saw);
    }

    /** Each use tick: a stroke every {@link ItemTuning#HAND_STROKE_TICKS} ticks, the cut finished on the last. */
    public static void tick(Level level, LivingEntity user, ItemStack saw, int remaining) {
        if (level.isClientSide || !(user instanceof Player player))
            return;
        int used = HandWork.useDuration() - remaining;
        if (used <= 0 || used % ItemTuning.HAND_STROKE_TICKS != 0)
            return;
        BlockPos pos = target(level, player);
        int needed = pos == null ? -1 : strokes(saw, level.getBlockState(pos));
        if (needed < 0 || !isSaw(saw)) {
            player.stopUsingItem();
            return;
        }
        BlockState state = level.getBlockState(pos);
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.5f, 1.3f + level.random.nextFloat() * 0.2f);
        if (level instanceof ServerLevel server)
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.05);
        player.causeFoodExhaustion(ItemTuning.HAND_SWING_EXHAUSTION_PER_ST * ItemTuning.HAND_SAW_ST.value());
        ItemStack worn = saw.getItem().getCraftingRemainingItem(saw);
        if (!worn.isEmpty())
            saw.setDamageValue(worn.getDamageValue());
        if (used / ItemTuning.HAND_STROKE_TICKS >= needed) {
            saw(level, player, saw, pos, state);
            player.stopUsingItem();
        }
    }

    /** The log or plank block the player is looking at, in reach, or null. */
    @Nullable
    private static BlockPos target(Level level, Player player) {
        double reach = player.blockInteractionRange();
        var eye = player.getEyePosition();
        var end = eye.add(player.getViewVector(1f).scale(reach));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK)
            return null;
        BlockState state = level.getBlockState(hit.getBlockPos());
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) ? hit.getBlockPos() : null;
    }

    /** Strokes this blade needs for this wood, or -1 when it cannot cut it (softer than the wood, or no stroke lands). */
    private static int strokes(ItemStack saw, BlockState state) {
        float wood = hardness(state);
        float edge = ModularHandTool.fileHardness(saw).value();
        if (edge < wood || wood <= 0f)
            return -1;
        float drive = ItemTuning.HAND_SAW_ST.value() / thickness(saw);
        return Math.max(1, (int) Math.ceil(ItemTuning.HAND_SAW_WORK * wood / drive));
    }

    /** The hardness of the wood a block is: its material's, else the generic wood's. */
    private static float hardness(BlockState state) {
        Optional<Material.Located> located = MaterialIngredient.locate(state.getBlock().asItem());
        Material material = located.map(Material.Located::material).orElse(MaterialIngredient.materialsNow().get(Feedback.id("wood")));
        return material == null ? 0f : material.hardness().value();
    }

    /** The blade's thickness as a multiple of the default kerf (a thinner blade cuts quicker and bends more); 1 when it has none. */
    private static float thickness(ItemStack saw) {
        var design = ModularStacks.design(saw);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.known(design.type());
        var part = design == null ? null : design.part("B");
        var role = type == null ? null : type.roles().get("B");
        return part == null || role == null ? 1f : Math.max(0.5f, role.axis(part, "thickness"));
    }

    private static void saw(Level level, Player player, ItemStack saw, BlockPos pos, BlockState state) {
        ItemStack block = new ItemStack(state.getBlock().asItem());
        ItemStack out;
        if (state.is(BlockTags.LOGS)) {
            out = WoodCuts.planksOf(level, block).map(p -> new ItemStack(p, WoodCuts.LOG_PLANKS)).orElse(ItemStack.EMPTY);
        } else {
            out = WoodCuts.of(level, block).stream().filter(c -> c.result().is(ItemTags.WOODEN_SLABS) && c.cost() > 0)
                    .findFirst().map(c -> c.result().copyWithCount(Math.max(1, c.result().getCount() / c.cost()))).orElse(ItemStack.EMPTY);
        }
        if (out.isEmpty())
            return;
        level.removeBlock(pos, false);
        drop(level, pos, out);
        int shavings = WoodCuts.kerfShavings(saw, block, 1, level);
        if (shavings > 0)
            WoodCuts.shavingOf(block).ifPresent(s -> drop(level, pos, new ItemStack(s, shavings)));
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
