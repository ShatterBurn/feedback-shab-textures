// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.electromagnet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A coil on an iron core: the first thing that runs on what the resolver gives it. */
public class ElectromagnetBlock extends Block implements EntityBlock, io.github.soundgoodizerfan.feedback.vehicle.micro.FineHosts.Routed {

    /** True while the field is above zero (set by the block entity): the coil glows and the pole faces show the field. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Mirrors models/block/electromagnet.json (scripts/electromagnet_model.py): pole plates, coil, the two lugs as one box. */
    private static final int[][] BOXES = {{2, 0, 2, 14, 2, 14}, {3, 2, 3, 13, 14, 13}, {2, 14, 2, 14, 16, 14}, {13, 6, 5, 15, 10, 11}};
    private static final VoxelShape SHAPE = shape();

    private static VoxelShape shape() {
        VoxelShape s = Shapes.empty();
        for (int[] b : BOXES) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public ElectromagnetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectromagnetBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ElectromagnetBlockEntity) be).tickServer();
    }
}
