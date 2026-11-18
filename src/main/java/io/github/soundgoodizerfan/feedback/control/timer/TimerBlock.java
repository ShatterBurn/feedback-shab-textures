// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.timer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Runs a machine for a while and then stops it. The first automation in the game.
 *
 * <h2>What it automates, and what it does not</h2>
 * It does not run a process. It <em>ends</em> one — and only because the player guessed how long
 * the process takes. No part of this device can tell whether the job is done, which is precisely
 * philosophy 10's point that timing is a crude form of control.
 * <p>
 * So it is a bet, and a losing one some of the time. A water wheel's speed wanders with how much
 * water is against it, the shaft run's friction rises with speed, and the same wind of the timer
 * therefore buys a different number of blows on different days. Mostly plates. Sometimes foil.
 * <p>
 * It is still a real improvement, and the player should feel that before they are handed a single
 * instrument: <strong>automation arrives before precision does.</strong> Slice 2's controller
 * replaces this block without replacing the idea, because both of them do the one thing 13
 * allows — switch a supply on or off.
 */
public class TimerBlock extends Block implements EntityBlock {

    public static final BooleanProperty RUNNING = BlockStateProperties.LIT;
    /** Which way the dial faces: toward the player who placed it. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TimerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RUNNING, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUNNING, FACING);
    }

    /** Mirrors models/block/timer.json (scripts/timer_model.py): the copper casing, the winding cog and its bronze key. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 10, 14), Block.box(5, 10, 5, 11, 12, 11), Block.box(7, 12, 7, 9, 13, 9));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Right click winds it and lets go. Sneak to change how long a wind lasts. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TimerBlockEntity timer) {
            if (player.isShiftKeyDown())
                timer.cycleSetting(player);
            else
                timer.toggle();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((TimerBlockEntity) be).tickServer();
    }
}
