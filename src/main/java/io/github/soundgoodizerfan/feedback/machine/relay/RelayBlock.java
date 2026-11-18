// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.relay;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Contacts moved by an adjacent electromagnet. See {@link RelayBlockEntity}. */
public class RelayBlock extends Block implements EntityBlock {

    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");

    /**
     * Mirrors models/block/relay.json and relay_closed.json (scripts/relay_model.py): base, yoke, pole stub, contact post, then the
     * armature, which lies flat on pole and contact when closed and is raised two units when open.
     */
    private static final int[][] FIXED = {{2, 0, 2, 14, 2, 14}, {2, 2, 6, 4, 10, 10}, {6, 2, 6, 10, 6, 10}, {12, 2, 6, 14, 6, 10}};
    private static final VoxelShape OPEN = shape(new int[]{4, 8, 6, 14, 10, 10});
    private static final VoxelShape CLOSED_SHAPE = shape(new int[]{4, 6, 6, 14, 8, 10});

    private static VoxelShape shape(int[] armature) {
        VoxelShape s = Block.box(armature[0], armature[1], armature[2], armature[3], armature[4], armature[5]);
        for (int[] b : FIXED) {
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s;
    }

    public RelayBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CLOSED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CLOSED) ? CLOSED_SHAPE : OPEN;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RelayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RelayBlockEntity) be).tickServer();
    }
}
