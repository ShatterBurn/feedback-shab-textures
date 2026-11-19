// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The four world-facing taps on the XP wire; one class, the kind says which source it drinks. */
public class XpTapBlock extends Block implements EntityBlock {

    public enum Kind {
        /** Loose orbs in reach. */
        ORB,
        /** XP of ore broken in reach. */
        ORE,
        /** XP of a villager's trade in reach. */
        TRADE,
        /** XP banked by a vessel beside it. */
        SMELTER
    }

    /** The valve: a tap draws only while its lever is open and no redstone signal holds it shut. */
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    private final Kind kind;

    public XpTapBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(OPEN, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    /** Whether the tap at {@code pos} (this block or the Catalyst Tap) draws: its lever open and no redstone signal on it. */
    public static boolean drawing(Level level, BlockPos pos, BlockState state) {
        return state.hasProperty(OPEN) && state.getValue(OPEN) && !level.hasNeighborSignal(pos);
    }

    /** The lever: an empty hand opens or shuts the valve. */
    public static InteractionResult flip(BlockState state, Level level, BlockPos pos) {
        level.setBlock(pos, state.cycle(OPEN), Block.UPDATE_ALL);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LEVER_CLICK, net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, state.getValue(OPEN) ? 0.5f : 0.6f);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return flip(state, level, pos);
    }

    public Kind kind() {
        return kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpTapBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != FBlockEntities.XP_TAP.get() || (kind != Kind.ORB && kind != Kind.SMELTER))
            return null;
        return (lvl, pos, st, be) -> XpTapBlockEntity.serverTick(lvl, pos, st, (XpTapBlockEntity) be);
    }
}
