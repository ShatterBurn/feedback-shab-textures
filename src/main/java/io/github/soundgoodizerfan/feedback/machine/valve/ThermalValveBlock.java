// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.valve;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * A valve that a spring opens, with no electricity and no hand on it. Fit a coil spring (any material) and the valve opens when the
 * fluid behind its input face reaches the temperature the spring's material returns to its set shape at (the material's
 * {@code transition}, as nitinol and void metal do), and shuts again a little under it. A spring of a material that remembers no shape,
 * or none, leaves it shut. It is a {@link ValveBlock} in every other way (same faces, same line, same fittings), so the thermal behaviour
 * is {@link ValveBlockEntity#tickThermal}. Right click with a spring fits one; right click with an empty hand takes it out and reads the
 * state.
 */
public class ThermalValveBlock extends ValveBlock {

    public ThermalValveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof ValveBlockEntity valve))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            ItemStack rest = valve.fitSpring(stack);
            if (rest != stack) {
                player.setItemInHand(hand, rest);
                player.displayClientMessage(Component.translatable(Float.isNaN(valve.opensAt())
                        ? "message.feedback.thermal_valve.no_memory" : "message.feedback.thermal_valve.fitted"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The valve is not worked by hand: an empty hand takes the spring out, or says what holds it shut. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ValveBlockEntity valve))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!valve.spring().isEmpty()) {
                ItemStack out = valve.removeSpring();
                if (!player.getInventory().add(out))
                    player.drop(out, false);
            } else
                player.displayClientMessage(Component.translatable("message.feedback.thermal_valve.no_spring"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ValveBlockEntity) be).tickThermal();
    }
}
