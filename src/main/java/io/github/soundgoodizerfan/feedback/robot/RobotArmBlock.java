// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * A robot arm on a mount: a fixed robot run off an electrical line, working the space it faces. Right-click with a Punch Card to load a
 * program, with a Data Connector to step through its channels for a Programmer card, with any other item to put it in its cargo; empty-handed
 * and sneaking takes the last stack out. See {@link RobotArmBlockEntity}.
 */
public class RobotArmBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    /**
     * The boxes of the arm (front on north), as {x1, y1, z1, x2, y2, z2} in sixteenths: base, pedestal, then the turning head from
     * models/block/robot_arm_head.json (turret, upper arm, forearm, wrist, two fingers). Keep in step with scripts/robot_models.py.
     */
    private static final int[][] BOXES = {
            {2, 0, 2, 14, 2, 14}, {5, 2, 5, 11, 6, 11},
            {4, 6, 4, 12, 8, 12}, {6, 8, 6, 10, 14, 10}, {6, 12, 0, 10, 14, 6}, {5, 10, 0, 11, 12, 2}, {5, 6, 0, 7, 10, 2}, {9, 6, 0, 11, 10, 2}};

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

    public RobotArmBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof RobotArmBlockEntity arm))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof DataConnectorItem)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;
        if (io.github.soundgoodizerfan.feedback.process.Circuit.of(stack).elements() > 0 && stack.getCount() == 1) {
            ItemStack old = arm.brain();
            arm.setBrain(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
            if (!old.isEmpty())
                player.getInventory().placeItemBackInInventory(old);
        } else if (stack.is(FTags.PUNCH_CARDS) && player.isShiftKeyDown()) {
            ItemStack out = arm.core().getItem(0);
            if (!out.isEmpty()) {
                arm.core().setItem(0, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(out);
            }
        } else if (stack.is(FTags.PUNCH_CARDS)) {
            var refusal = arm.core().refusalFor(stack);
            if (refusal.isPresent()) {
                player.displayClientMessage(Component.translatable("feedback.programmer.error." + refusal.get()), true);
            } else {
                ItemStack old = arm.core().getItem(0);
                arm.core().setItem(0, stack.copyWithCount(1));
                if (!player.getAbilities().instabuild)
                    stack.shrink(1);
                if (!old.isEmpty())
                    player.getInventory().placeItemBackInInventory(old);
            }
        } else {
            ItemStack left = arm.cargo().addItem(stack.copy());
            if (!player.getAbilities().instabuild)
                player.setItemInHand(hand, left);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RobotArmBlockEntity arm))
            return InteractionResult.PASS;
        if (!level.isClientSide && player.isShiftKeyDown()) {
            for (int i = arm.cargo().getContainerSize() - 1; i >= 0; i--) {
                ItemStack s = arm.cargo().getItem(i);
                if (!s.isEmpty()) {
                    arm.cargo().setItem(i, ItemStack.EMPTY);
                    player.getInventory().placeItemBackInInventory(s);
                    break;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RobotArmBlockEntity arm)
            for (ItemStack s : arm.contents())
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RobotArmBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RobotArmBlockEntity) be).tickServer();
    }
}
