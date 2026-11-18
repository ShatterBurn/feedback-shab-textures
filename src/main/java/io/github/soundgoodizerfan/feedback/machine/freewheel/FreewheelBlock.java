// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.freewheel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationMember;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationSplit;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * An overrunning clutch (freewheel): a shaft in at the {@link #FACING} face and a shaft out the far face. The propagator couples only the input,
 * so the output is a run of its own; this block drives it ({@link RotationSplit}) at the input's speed only while the input is the faster, in the
 * same sense. When the output outruns the input (a rotor spun by the air, a flywheel coasting) it is let go and keeps its own speed:
 * the output never drives the input back.
 */
public class FreewheelBlock extends Block implements EntityBlock, Rotatable {
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return io.github.soundgoodizerfan.feedback.machine.gearbox.GearboxBlock.SHAPE;
    }


    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FreewheelBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The input is on the face the player clicked against, the shaft they are joining; the output is the far face. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /** Only the input: the output is fed, not coupled. */
    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    /** One step: drives the run across the output face at the input's speed while the input is the faster. */
    public static void step(Level level, RotationMember self, BlockState state) {
        RotationNetwork in = self.getNetwork();
        if (in == null || self.getRpm().value() == 0f)
            return;
        RotationMember out = RotationSplit.across(level, self, state.getValue(FACING).getOpposite());
        if (out == null || out.getNetwork() == null)
            return;
        float want = self.getRpm().value(), have = out.getRpm().value();
        if (want == 0f)
            return;
        // engaged: the input turns the output's way and faster; otherwise the output is free
        if (want == 0f || Math.signum(want) != Math.signum(have) && have != 0f || Math.abs(want) < Math.abs(have))
            return;
        RotationSplit.drive(in, List.of(out), new float[] {want}, level.getGameTime());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((FreewheelBlockEntity) be).tickServer();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FreewheelBlockEntity(pos, state);
    }
}
