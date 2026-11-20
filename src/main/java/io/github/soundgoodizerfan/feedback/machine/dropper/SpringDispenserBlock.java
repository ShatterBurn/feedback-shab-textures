// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.dropper;

import net.minecraft.world.level.block.DispenserBlock;

/**
 * The dispenser, driven by a spring ({@link SpringFeed}): a signal releases a cocked spring that pushes the item out, and the spring stays
 * spent until it is cocked by hand or by a turning shaft. Redstone alone moves nothing. Vanilla's block is swapped for it
 * ({@code VanillaVesselSwap}); it keeps vanilla's inventory and dispense behaviours.
 */
public class SpringDispenserBlock extends DispenserBlock {

    public static final net.minecraft.world.level.block.state.properties.BooleanProperty COCKED = SpringFeed.COCKED;
    /** A dispenser actuator is fitted: the released spring uses the first item on the block in front instead of throwing it. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTUATED = net.minecraft.world.level.block.state.properties.BooleanProperty.create("actuated");

    public SpringDispenserBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COCKED, false).setValue(ACTUATED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,
            net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COCKED, ACTUATED);
    }

    /** Releases only a cocked spring; the spring is then spent. */
    @Override
    protected void dispenseFrom(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.block.state.BlockState state,
                                net.minecraft.core.BlockPos pos) {
        if (!state.getValue(COCKED)) {
            level.levelEvent(1001, pos, 0); // the dry click of a dispenser with nothing to push
            return;
        }
        if (state.getValue(ACTUATED))
            Actuation.use(level, pos, state);
        else
            super.dispenseFrom(level, state, pos);
        SpringFeed.release(level, pos);
    }

    /** An actuator in hand is fitted once; the fitting comes back out as an item when the dispenser is broken. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.DISPENSER_ACTUATOR.get()) && !state.getValue(ACTUATED)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(ACTUATED, true), net.minecraft.world.level.block.Block.UPDATE_ALL);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                            net.minecraft.world.level.block.state.BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && state.getValue(ACTUATED))
            net.minecraft.world.level.block.Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.DISPENSER_ACTUATOR.get()));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (player.isShiftKeyDown() && SpringFeed.cockByHand(level, pos, state))
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onPlace(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                           net.minecraft.world.level.block.state.BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!old.is(this) && !level.isClientSide)
            level.scheduleTick(pos, this, SpringFeed.POLL);
    }

    /** A tick is either a signal's release (the state is triggered) or a poll for a turning shaft that recocks the spring. */
    @Override
    protected void tick(net.minecraft.world.level.block.state.BlockState state, net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
                        net.minecraft.util.RandomSource random) {
        if (state.getValue(TRIGGERED))
            super.tick(state, level, pos, random);
        SpringFeed.poll(level, pos, this);
    }
}
