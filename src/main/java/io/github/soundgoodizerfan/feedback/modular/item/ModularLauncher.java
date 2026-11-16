// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import io.github.soundgoodizerfan.feedback.modular.bake.LauncherMechanism;
import io.github.soundgoodizerfan.feedback.modular.bake.LauncherSpec;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunMechanism;

/**
 * A launcher: a gun that drives a shell with work stored in a seat instead of burning powder ({@link LauncherSpec}). The seat's state
 * lives on the stack, never in a stat: a spring is cocked by the reload key, taking the spring's energy over the wielder's strength
 * ({@link LauncherMechanism}). The round is a {@link io.github.soundgoodizerfan.feedback.weapon.Cartridge} with no charge: the seat gives
 * the push.
 */
public class ModularLauncher extends GunItem {

    /** Game tick the seat is cocked at (0 when it is not being cocked), and whether the stroke is done. */
    private static final String COCK_AT = "LaunchCockAt", COCKED = "LaunchCocked";

    public ModularLauncher(Properties properties) {
        super(properties);
    }

    @Override
    public GunMechanism mechanism() {
        return LauncherMechanism.INSTANCE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        GunCondition.wearLine(stack).ifPresent(tooltip::add);
        if (cocked(stack))
            tooltip.add(Component.translatable("tooltip.feedback.launcher.cocked").withStyle(ChatFormatting.GRAY));
        spec(stack).ifPresent(sp -> {
            switch (sp.seat()) {
                case RESERVOIR -> tooltip.add(Component.translatable("tooltip.feedback.launcher.pressure", Math.round(pressure(stack) * 100f)).withStyle(ChatFormatting.GRAY));
                case STEAM -> tooltip.add(Component.translatable("tooltip.feedback.launcher.water", Math.round(water(stack) * 100f)).withStyle(ChatFormatting.GRAY));
                case SOLENOID -> tooltip.add(ModularElectricGun.cell(stack).isEmpty()
                        ? Component.translatable("tooltip.feedback.launcher.no_cell").withStyle(ChatFormatting.GRAY)
                        : Component.translatable("tooltip.feedback.launcher.cell", Math.round(io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack
                                .stateOfCharge(io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.state(ModularElectricGun.cell(stack))) * 100f)).withStyle(ChatFormatting.GRAY));
                default -> { }
            }
        });
        int rounds = GunAmmo.count(stack);
        if (rounds > 0)
            tooltip.add(Component.translatable("tooltip.feedback.launcher.chambered", rounds).withStyle(ChatFormatting.GRAY));
    }

    // --- the seat's state on the stack ---------------------------------------------------------

    public static boolean cocked(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(COCKED);
    }

    public static void setCocked(ItemStack gun, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putBoolean(COCKED, on));
    }

    /** The game tick a cocking in progress ends, or 0 for none. */
    public static long cockAt(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0L : data.copyTag().getLong(COCK_AT);
    }

    public static void setCockAt(ItemStack gun, long tick) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putLong(COCK_AT, tick));
    }

    private static final String PRESSURE = "LaunchPressure", WATER = "LaunchWater";

    /** A reservoir seat's pressure as a share of the pressure it is rated for (0 flat, 1 the walls' limit, over it the walls weep). */
    public static float pressure(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0f : data.copyTag().getFloat(PRESSURE);
    }

    public static void setPressure(ItemStack gun, float share) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putFloat(PRESSURE, Math.max(0f, share)));
    }

    /** A steam seat's water, as a share of a full boiler. */
    public static float water(ItemStack gun) {
        CustomData data = gun.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0f : data.copyTag().getFloat(WATER);
    }

    public static void setWater(ItemStack gun, float share) {
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> tag.putFloat(WATER, Math.max(0f, Math.min(1f, share))));
    }

    /** A solenoid seat takes a battery or Leyden jar on the cursor, as an electric gun does; an empty cursor takes it out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack gun, ItemStack other, net.minecraft.world.inventory.Slot slot,
                                            net.minecraft.world.inventory.ClickAction action, net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.entity.SlotAccess access) {
        if (action != net.minecraft.world.inventory.ClickAction.SECONDARY || !slot.allowModification(player)
                || spec(gun).map(sp -> sp.seat() != LauncherSpec.Seat.SOLENOID).orElse(true))
            return false;
        if (other.isEmpty()) {
            ItemStack held = ModularElectricGun.cell(gun);
            if (held.isEmpty())
                return false;
            ModularElectricGun.setCell(gun, ItemStack.EMPTY);
            access.set(held);
            return true;
        }
        if (!io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack.is(other))
            return false;
        ItemStack old = ModularElectricGun.cell(gun);
        ModularElectricGun.setCell(gun, other);
        access.set(old);
        return true;
    }

    public static Optional<LauncherSpec> spec(ItemStack gun) {
        ModularDesign design = ModularStacks.design(gun);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? Optional.empty() : LauncherSpec.of(design, type, MaterialTable.get().materials());
    }
}
