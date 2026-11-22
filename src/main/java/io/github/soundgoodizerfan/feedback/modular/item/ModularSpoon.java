// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.prep.DishProfile;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The pot tool. Held use aimed at a stockpot is stirring (every tick, a switch: full pace or none); a use released inside
 * {@link ModularTuning#SPOON_TAP_TICKS} is a skim. The server times everything; no payloads. The bowl's size scales stir and skim; its depth trades the two (a deep bowl scoops, a shallow one stirs), and a ladle's long reach keeps the hand off a hotter pot at the cost of a stir;
 * the handle's material decides how hot a pot it can be held over before the hand burns. A broken spoon does nothing.
 */
public class ModularSpoon extends ModularItem {

    public ModularSpoon(Properties properties) {
        super(properties);
    }

    private static StockpotBlockEntity pot(Player player, Level level) {
        HitResult hit = player.pick(FTuning.SKILLET_LIFT_REACH, 0f, false);
        return hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(block.getBlockPos()) instanceof StockpotBlockEntity pot ? pot : null;
    }

    /** The bowl against the reference spoon: 1 is the reference; a bigger bowl does proportionally more. */
    public static float bowl(ItemStack stack) {
        return bowl(ModularStacks.volumeMb(stack));
    }

    /** The bowl of {@code mb} against the reference spoon. */
    public static float bowl(float mb) {
        return mb <= 0f ? 1f : mb / ModularTuning.SPOON_REF_BOWL_MB;
    }

    /** A figure of the bowl's shape by name ({@code stir}, {@code scoop}, {@code reach}: the variant's) and its depth axis; 1 for an unreadable design. */
    private static float shape(ItemStack stack, String name) {
        return shape(ModularStacks.design(stack), name);
    }

    /** A figure of the bowl of {@code design} by name, as {@link #shape(ItemStack, String)}; the Blueprint Table reads it off a drawing. */
    public static float shape(ModularDesign design, String name) {
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        var bowl = design == null ? null : design.part("B");
        if (type == null || bowl == null)
            return 1f;
        ModularType.Role role = type.roles().get("B");
        float v = role.paramOf(bowl, name, 1f);
        ModularType.Axis depth = role.axesFor(bowl).get("depth");
        if (depth == null || depth.def() <= 0f || name.equals("reach"))
            return v;
        float ratio = role.axis(bowl, "depth") / depth.def();
        // A deep bowl holds what it lifts and a shallow one pushes the pot about: depth is scoop against stir.
        return v * (float) Math.pow(ratio, name.equals("scoop") ? ModularTuning.SPOON_DEPTH_EXPONENT : -ModularTuning.SPOON_DEPTH_EXPONENT);
    }

    /** What a stir of this spoon does, against the reference spoon. */
    public static float stir(ItemStack stack) {
        return bowl(stack) * shape(stack, "stir");
    }

    /** What a skim of this spoon lifts, against the reference spoon. */
    public static float scoop(ItemStack stack) {
        return bowl(stack) * shape(stack, "scoop");
    }

    /** The hottest pot, in Tu, a hand may stir over with the handle of {@code material} at the bowl's reach {@code reach}. */
    public static float holdsTo(net.minecraft.world.level.Level level, net.minecraft.resources.ResourceLocation material, float reach) {
        return Grip.safeUpTo(level, material, ModularTuning.SPOON_EXPOSURE / reach);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModularStacks.broken(stack))
            return InteractionResultHolder.pass(stack);
        // A dish in the other hand is tasted, not a pot stirred.
        ItemStack dish = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (DishProfile.is(dish))
            return taste(level, player, hand, stack, dish);
        if (pot(player, level) == null)
            return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /**
     * Tasting: the dish's profile comes back as words, one line per axis ({@link DishProfile#AXES}, the same figures a villager judges), and as
     * exact figures with an instrument at the truth tier. The spoon's options matter: a bowl under the reference size takes a small mouthful
     * and tells only the two axes that stand furthest from the middle; a dish hotter than the handle can be held over burns the hand (the body
     * is physics too). A tasting wears the spoon a point.
     */
    private static InteractionResultHolder<ItemStack> taste(Level level, Player player, InteractionHand hand, ItemStack spoon, ItemStack dish) {
        if (level.isClientSide)
            return InteractionResultHolder.success(spoon);
        io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures f = DishProfile.read(dish, level);
        if (f == null)
            return InteractionResultHolder.pass(spoon);
        ModularDesign design = ModularStacks.design(spoon);
        var handle = design == null ? null : design.part("S");
        float stands = handle == null ? ThermalTuning.CONTACT_PAIN_TU.value() : holdsTo(level, handle.material(), shape(spoon, "reach"));
        float warm = io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.get(dish, level).value();
        if (warm > stands)
            HotContact.burn(player, new Tu(warm));
        boolean exact = io.github.soundgoodizerfan.feedback.instrument.Instruments.exact(player, io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE);
        java.util.List<DishProfile.Axis> axes = new java.util.ArrayList<>(DishProfile.AXES);
        if (bowl(spoon) < 1f) {
            // A small mouthful: only the two axes furthest from the middle of their range come through.
            axes.sort(java.util.Comparator.comparingDouble((DishProfile.Axis a) ->
                    -Math.abs((f.get(a.fig()) - a.lo()) / Math.max(1e-3f, a.hi() - a.lo()) - 0.5f)));
            axes = new java.util.ArrayList<>(axes.subList(0, Math.min(2, axes.size())));
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tooltip.feedback.tasting.title", dish.getHoverName()), false);
        for (DishProfile.Axis axis : axes) {
            float v = f.get(axis.fig());
            int band = Math.min(3, Math.max(0, (int) ((v - axis.lo()) / Math.max(1e-3f, axis.hi() - axis.lo()) * 4f)));
            String name = axis.fig().name().toLowerCase(java.util.Locale.ROOT);
            var line = net.minecraft.network.chat.Component.translatable("village.feedback.term." + name + "." + band);
            player.displayClientMessage(exact
                    ? net.minecraft.network.chat.Component.translatable("tooltip.feedback.tasting.exact", net.minecraft.network.chat.Component.translatable("village.feedback.noun." + name), line, String.format(java.util.Locale.ROOT, "%.2f", v))
                    : net.minecraft.network.chat.Component.translatable("tooltip.feedback.tasting.line", net.minecraft.network.chat.Component.translatable("village.feedback.noun." + name), line), false);
        }
        spoon.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResultHolder.consume(spoon);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || ModularStacks.broken(stack))
            return;
        int held = getUseDuration(stack, user) - remaining;
        if (held <= ModularTuning.SPOON_TAP_TICKS)
            return;
        StockpotBlockEntity pot = pot(player, level);
        if (pot == null)
            return;
        pot.stir(stir(stack));
        if (held % 20 == 0) {
            ModularDesign design = ModularStacks.design(stack);
            var handle = design == null ? null : design.part("S");
            float stands = handle == null ? ThermalTuning.CONTACT_PAIN_TU.value() : holdsTo(level, handle.material(), shape(stack, "reach"));
            if (pot.temperature() > stands)
                HotContact.burn(player, new Tu(pot.temperature()));
        }
        if (held % ModularTuning.POT_STIR_WEAR_TICKS == 0) {
            stack.hurtAndBreak(1, player, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            ModularStacks.wetUse(stack, player);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining) {
        if (level.isClientSide || !(user instanceof Player player) || ModularStacks.broken(stack))
            return;
        if (getUseDuration(stack, user) - remaining > ModularTuning.SPOON_TAP_TICKS)
            return;
        StockpotBlockEntity pot = pot(player, level);
        if (pot != null && pot.skim(scoop(stack))) {
            stack.hurtAndBreak(1, player, user.getUsedItemHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            ModularStacks.wetUse(stack, player);
        }
    }
}
