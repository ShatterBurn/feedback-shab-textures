// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electrostatic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A hand-turned electrostatic machine. Using it is one turn of the handle: one pulse. */
public class ElectrostaticGeneratorBlock extends Block implements EntityBlock {

    /** Mirrors models/block/electrostatic_generator.json (scripts/electrostatic_generator_model.py): base, two
     *  pillars with balls, the octagonal disc in three pieces, crank with hub, knob. */
    private static final int[][] BOXES = {
            {1, 0, 2, 15, 2, 14}, {1, 2, 11, 3, 12, 13}, {13, 2, 11, 15, 12, 13}, {0, 12, 10, 4, 16, 14}, {12, 12, 10, 16, 16, 14},
            {5, 2, 6, 11, 14, 10}, {3, 4, 6, 5, 12, 10}, {11, 4, 6, 13, 12, 10},
            {7, 7, 4, 9, 9, 6}, {7, 7, 2, 9, 13, 4}, {7, 11, 0, 9, 13, 2}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public ElectrostaticGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ElectrostaticGeneratorBlockEntity generator)
            generator.turn();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectrostaticGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ElectrostaticGeneratorBlockEntity) be).tickServer();
    }
}
