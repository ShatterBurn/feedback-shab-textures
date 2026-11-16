// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.modular.bake.CasterMechanism;
import io.github.soundgoodizerfan.feedback.modular.bake.CasterSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.weapon.CasterTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;

/**
 * A fluid caster: a gun that throws a stream of whatever its tank holds ({@link CasterSpec}, {@link CasterMechanism}). Held left click
 * opens the valve, release closes it. What the tank holds (species, mB, the temperature stamp) and its gauge pressure are state on the
 * stack, never stats; the tank is a fluid handler, so a bucket, a pipe or a placed tank fills it and drains it. Used on a block that
 * holds fluid it takes from it (a hose from a placed tank, which also pressurises it when the tank is under pressure), sneaking it
 * pours into it.
 */
public class ModularCaster extends GunItem {

    /** Game tick up to which a hand pump's stroke keeps the stream primed. */
    private static final String PRIME_UNTIL = "CasterPrimeUntil";

    public ModularCaster(Properties properties) {
        super(properties);
    }

    @Override
    public GunMechanism mechanism() {
        return CasterMechanism.INSTANCE;
    }

    /** The walls weep and burst on a pressure the player let build, and a fluid the walls cannot take eats them: once a half second, server side. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level instanceof net.minecraft.server.level.ServerLevel server && entity.tickCount % 10 == 0)
            CasterMechanism.walls(server, entity, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        GunCondition.wearLine(stack).ifPresent(tooltip::add);
        FluidStack held = content(stack);
        tooltip.add(held.isEmpty() ? Component.translatable("tooltip.feedback.caster.empty")
                : Component.translatable("tooltip.feedback.caster.holds", held.getHoverName(), held.getAmount()).withStyle(ChatFormatting.GRAY));
        // Words are free; the figure is a gauge's to give (philosophy 10).
        if (pressure(stack) >= CasterTuning.MIN_PRESSURE)
            tooltip.add(Component.translatable("tooltip.feedback.caster.pressurised").withStyle(ChatFormatting.GRAY));
    }

    // --- the tank on the stack -----------------------------------------------------------------

    /** What the tank holds, with its temperature stamp. */
    public static FluidStack content(ItemStack gun) {
        return gun.getOrDefault(FDataComponents.CASTER_CONTENT.get(), SimpleFluidContent.EMPTY).copy();
    }

    public static void setContent(ItemStack gun, FluidStack fluid) {
        if (fluid.isEmpty())
            gun.remove(FDataComponents.CASTER_CONTENT.get());
        else
            gun.set(FDataComponents.CASTER_CONTENT.get(), SimpleFluidContent.copyOf(fluid));
    }

    /** Gauge pressure of the tank, Pu. */
    public static float pressure(ItemStack gun) {
        return gun.getOrDefault(FDataComponents.CASTER_PRESSURE.get(), 0f);
    }

    public static void setPressure(ItemStack gun, float pu) {
        if (pu <= 0.01f)
            gun.remove(FDataComponents.CASTER_PRESSURE.get());
        else
            gun.set(FDataComponents.CASTER_PRESSURE.get(), pu);
    }

    /** The tank's capacity, mB, from its baked figures. */
    public static int capacity(ItemStack gun) {
        ModularFigures f = ModularStacks.figures(gun);
        return f == null ? 0 : (int) f.get(ModularFigures.Fig.CASTER_CAPACITY);
    }

    /** The game tick a hand pump's last stroke keeps the stream primed until, or 0. */
    public static long primeUntil(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0L : data.copyTag().getLong(PRIME_UNTIL);
    }

    public static void setPrimeUntil(ItemStack gun, long tick) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putLong(PRIME_UNTIL, tick));
    }

    public static Optional<CasterSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? Optional.empty() : CasterSpec.of(design, type, MaterialTable.get().materials());
    }

    // --- filling: a source in the world, a bucket, a hose from a placed tank ---------------------------

    /** Used on a fluid source block in the world it draws the fluid in, as a bucket does (the tank is a fluid handler). */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK)
            return net.minecraft.world.InteractionResultHolder.pass(stack);
        var picked = FluidUtil.tryPickUpFluid(stack, player, level, hit.getBlockPos(), hit.getDirection());
        return picked.isSuccess() ? net.minecraft.world.InteractionResultHolder.sidedSuccess(picked.getResult(), level.isClientSide)
                : net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    /**
     * A block that holds fluid is a hose to this caster: used on it, the caster takes what it can from it (and with it the block's
     * pressure when the block is under one); sneaking, it pours what it holds into the block. A bucket, a pipe or any handler does the
     * same through the tank's fluid handler without this.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getPlayer() == null)
            return InteractionResult.PASS;
        Level level = context.getLevel();
        var held = FluidUtil.getFluidHandler(stack);
        var block = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (held.isEmpty() || block == null)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        boolean pour = context.getPlayer().isShiftKeyDown();
        FluidStack moved = pour ? FluidUtil.tryFluidTransfer(block, held.get(), capacity(stack), true)
                : FluidUtil.tryFluidTransfer(held.get(), block, capacity(stack), true);
        if (moved.isEmpty())
            return InteractionResult.FAIL;
        // A tank under pressure pushes its fluid up to its own gauge reading (never past the caster's walls: the walls answer for that).
        if (!pour && level.getBlockEntity(context.getClickedPos()) instanceof PressureHolder holder)
            setPressure(stack, Math.max(pressure(stack), holder.getPressure().value()));
        if (pour && content(stack).isEmpty())
            setPressure(stack, 0f);
        return InteractionResult.sidedSuccess(false);
    }
}
