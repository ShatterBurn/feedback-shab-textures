// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The part of a {@link ModularPistonBlock} that is out in the world: the head at the end of the stroke (a plate across the front) or a length
 * of rod behind it. It holds no state of its own beyond which way it faces and whether it is rod or head; it stands only while the cell behind it
 * is its cylinder or more rod, so a broken cylinder or rod takes the rest with it. It cannot be pushed.
 */
public class ModularPistonHeadBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** Rod rather than head. */
    public static final BooleanProperty ROD = BooleanProperty.create("rod");
    /** The cylinder had a slime face. */
    public static final BooleanProperty STICKY = BooleanProperty.create("sticky");

    private static final VoxelShape ROD_NORTH = Block.box(6, 6, 0, 10, 10, 16);
    private static final VoxelShape HEAD_NORTH = Shapes.or(Block.box(0, 0, 0, 16, 16, 4), Block.box(6, 6, 4, 10, 10, 16));

    public ModularPistonHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROD, false).setValue(STICKY, false));
    }

    public static BlockState state(Direction facing, boolean rod, boolean sticky) {
        return io.github.soundgoodizerfan.feedback.registry.FBlocks.MODULAR_PISTON_HEAD.get().defaultBlockState()
                .setValue(FACING, facing).setValue(ROD, rod).setValue(STICKY, sticky);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROD, STICKY);
    }

    /** Shapes by facing ordinal and rod (index {@code facing * 2 + rod}), built once. */
    private static final VoxelShape[] SHAPES = new VoxelShape[12];

    static {
        for (Direction d : Direction.values())
            for (int rod = 0; rod < 2; rod++) {
                VoxelShape north = rod == 1 ? ROD_NORTH : HEAD_NORTH;
                SHAPES[d.ordinal() * 2 + rod] = switch (d) {
                    case NORTH -> north;
                    case SOUTH -> turn(north, 2, false);
                    case EAST -> turn(north, 1, false);
                    case WEST -> turn(north, 3, false);
                    case UP -> turn(north, 0, true);
                    case DOWN -> turn(north, 0, false);
                };
            }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal() * 2 + (state.getValue(ROD) ? 1 : 0)];
    }

    /** {@code north} turned about Y by {@code quarters} (then flat to up or down when {@code vertical} or the quarters are 0 and it is down). */
    private static VoxelShape turn(VoxelShape north, int quarters, boolean up) {
        if (quarters == 0) {
            // Up and down: the same shape laid along Y.
            VoxelShape[] out = {Shapes.empty()};
            north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> out[0] = Shapes.or(out[0], up
                    ? Shapes.box(x1, 1 - z2, y1, x2, 1 - z1, y2)
                    : Shapes.box(x1, z1, y1, x2, z2, y2)));
            return out[0];
        }
        VoxelShape[] out = {Shapes.empty()};
        north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            double a1 = x1, a2 = x2, b1 = z1, b2 = z2;
            for (int i = 0; i < quarters; i++) {
                double nx1 = 1 - b2, nx2 = 1 - b1, nz1 = a1, nz2 = a2;
                a1 = nx1;
                a2 = nx2;
                b1 = nz1;
                b2 = nz2;
            }
            out[0] = Shapes.or(out[0], Shapes.box(a1, y1, b1, a2, y2, b2));
        });
        return out[0];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState behind = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
        Direction f = state.getValue(FACING);
        if (behind.getBlock() instanceof ModularPistonHeadBlock)
            return behind.getValue(FACING) == f && behind.getValue(ROD);
        return behind.getBlock() instanceof ModularPistonBlock && behind.getValue(ModularPistonBlock.FACING) == f
                && behind.getValue(ModularPistonBlock.EXTENDED);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return dir == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }
}
