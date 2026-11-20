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

    /** Mirrors models/block/orb_absorber.json: the shrieker core, four iron corner plates, two crystals, the lever, the wire outlet. The other taps are cubes. */
    private static final net.minecraft.world.phys.shapes.VoxelShape ORB_SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(2, 0, 2, 14, 10, 14), Block.box(0, 0, 0, 4, 12, 4), Block.box(12, 0, 0, 16, 12, 4),
            Block.box(0, 0, 12, 4, 12, 16), Block.box(12, 0, 12, 16, 12, 16), Block.box(4, 10, 7, 6, 14, 9),
            Block.box(10, 10, 7, 12, 14, 9), Block.box(6, 10, 11, 10, 12, 13), Block.box(7, 12, 11, 9, 15, 13),
            Block.box(6, 0, 0, 10, 4, 2));

    /** Mirrors models/block/ore_tap.json (and trade, catalyst and smelter): the iron body, the front part, the lever and the wire outlet at the back. */
    public static final net.minecraft.world.phys.shapes.VoxelShape TAP_SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(1, 0, 2, 15, 12, 14), Block.box(3, 2, 0, 13, 10, 2), Block.box(6, 12, 10, 10, 14, 12),
            Block.box(7, 14, 10, 9, 16, 12), Block.box(6, 2, 14, 10, 6, 16));

    private final Kind kind;

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return kind == Kind.ORB ? ORB_SHAPE : TAP_SHAPE;
    }

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
