// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.blueprint;

import io.github.soundgoodizerfan.feedback.control.FacedShape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Where a modular item is drawn before it is made. Like
 * a crafting grid it keeps nothing: the drawing lives in the screen until it is printed, and the
 * paper comes back when the screen closes. See {@link BlueprintTableMenu}.
 */
public class BlueprintTableBlock extends Block {

    /**
     * Mirrors models/block/blueprint_table.json (scripts/blueprint_table_model.py), front on north: four legs, the shelf and the paper stack, the drawer
     * apron, the front lip, the back ledge with its rolls and pencil, the tilted board as its bounding box, and the sheet hanging off the east end.
     */
    private static final java.util.Map<Direction, VoxelShape> SHAPES = FacedShape.of(new int[][]{
            {0, 0, 0, 2, 10, 2}, {14, 0, 0, 16, 10, 2}, {0, 0, 14, 2, 10, 16}, {14, 0, 14, 16, 10, 16}, {2, 1, 2, 14, 3, 14}, {4, 3, 4, 12, 5, 12},
            {2, 5, 1, 14, 9, 15}, {2, 9, 0, 14, 12, 2}, {0, 10, 13, 16, 14, 16}, {1, 14, 13, 15, 16, 15}, {0, 10, 2, 14, 16, 13}, {14, 3, 3, 16, 12, 13}});
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }


    /** Which way the table faces: the drawer toward the player who placed it, the hanging blueprint on its east side. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlueprintTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                    new BlueprintTableMenu(id, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("block.feedback.blueprint_table")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
