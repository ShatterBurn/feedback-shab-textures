// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.control.controller.ControllerHands;
import io.github.soundgoodizerfan.feedback.control.controller.ControllerHost;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Computer block. A medium in hand goes into the Controller slot; an empty hand opens the Programmer editor; sneaking with an empty
 * hand takes the medium back out of the Controller slot.
 */
public class ComputerBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ComputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * The boxes of models/block/computer.json (front on north), as {x1, y1, z1, x2, y2, z2} in sixteenths: case, stand, monitor frame,
     * body, shoulder, tube, keyboard with its cable, mouse. The bezel opening is filled; keep in step with scripts/computer_model.py.
     */
    private static final int[][] BOXES = {
            {1, 0, 6, 15, 3, 15}, {4, 3, 7, 12, 4, 13}, {6, 4, 9, 10, 5, 12},
            {2, 5, 6, 14, 15, 8}, {3, 5, 8, 13, 14, 11}, {4, 6, 11, 12, 14, 13}, {5, 7, 13, 11, 13, 15},
            {6, 0, 1, 15, 1, 5}, {10, 0, 5, 11, 1, 6}, {2, 0, 2, 5, 2, 6}};

    private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int[] b : BOXES) {
                // blockstate y rotation is clockwise from above: north -> east maps (x, z) to (16 - z, x)
                int x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                int[] r = switch (d) {
                    case EAST -> new int[]{16 - z2, x1, 16 - z1, x2};
                    case SOUTH -> new int[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case WEST -> new int[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new int[]{x1, z1, x2, z2};
                };
                shape = Shapes.or(shape, Block.box(r[0], b[1], r[1], r[2], b[4], r[3]));
            }
            SHAPES.put(d, shape);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The screen faces the player who placed it. */
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return ControllerHands.insert(stack, level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown())
            return openDriveWay(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof ComputerBlockEntity computer))
            return InteractionResult.PASS;
        if (!level.isClientSide)
            player.openMenu(computer, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sneak-use: the physical bays, the running slot and the inventory as an ordinary slot menu. */
    private static boolean openDriveWay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ComputerBlockEntity computer))
            return false;
        if (!level.isClientSide)
            player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) ->
                    new ComputerDriveMenu(id, inventory, pos, computer), net.minecraft.network.chat.Component.translatable("gui.feedback.computer.drive_way")), pos);
        return true;
    }

    /** A player's break drops the Computer with its disk id (not in creative): only then does the disk travel as an item. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.isCreative() && level.getBlockEntity(pos) instanceof ComputerBlockEntity computer)
            computer.markCarried();
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer)
            computer.markCarried();
        super.onBlockExploded(state, level, pos, explosion);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ComputerBlockEntity computer) {
                if (ComputerFiles.isTransient(computer.programmer().getItem(0)))
                    computer.programmer().removeItemNoUpdate(0);
                Containers.dropContents(level, pos, computer.programmer());
                Containers.dropContents(level, pos, computer.bays());
            }
            ControllerHands.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ControllerHost) be).tickServer();
    }
}
