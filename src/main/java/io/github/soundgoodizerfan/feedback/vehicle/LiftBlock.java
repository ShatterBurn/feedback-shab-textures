// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import com.mojang.serialization.MapCodec;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The stand a vehicle is built on ({@code docs/specs/feedback_vehicles_spec.md} section 4), after Scrap
 * Mechanic's lift: build on top of it, use it, and everything resting on it becomes one Sable
 * body held still where it stands. Use again to raise it a step, sneak-use to lower it, and
 * sneak-use at the bottom to turn it back into blocks; break the lift and the vehicle drops into
 * physics.
 *
 * <p>The lift is never part of the vehicle. It is what bounds Sable's floodfill, which otherwise
 * takes every block touching the build -- the ground included.
 */
public class LiftBlock extends BaseEntityBlock {

    public static final MapCodec<LiftBlock> CODEC = simpleCodec(LiftBlock::new);

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    /** True while the lift stands above its rest: the platform and its brackets then ride up with the vehicle (drawn by LiftRenderer), so the block's own shape drops them. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty RAISED =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("raised");

    /** Mirrors models/block/lift.json (scripts/lift_model.py), facing north: base, body, four corner blocks, console, cylinder. */
    private static final int[][] BOXES = {{0, 0, 2, 16, 4, 16}, {3, 4, 2, 13, 8, 16}, {0, 4, 2, 3, 8, 5}, {13, 4, 2, 16, 8, 5},
            {0, 4, 13, 3, 8, 16}, {13, 4, 13, 16, 8, 16}, {4, 2, 0, 12, 6, 2}, {5, 8, 5, 11, 12, 11}};
    /** The platform at rest (models/block/lift_top.json): where the vehicle sits. */
    private static final int[] PLATFORM = {0, 12, 0, 16, 16, 16};
    /** Shapes by horizontal facing ({@code get2DDataValue}) and raised (index {@code facing * 2 + raised}), built once. */
    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = shapes();

    private static net.minecraft.world.phys.shapes.VoxelShape[] shapes() {
        var out = new net.minecraft.world.phys.shapes.VoxelShape[8];
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int quarter = switch (d) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
            for (int raised = 0; raised < 2; raised++) {
                var shape = net.minecraft.world.phys.shapes.Shapes.empty();
                java.util.List<int[]> boxes = new java.util.ArrayList<>(java.util.Arrays.asList(BOXES));
                if (raised == 0)
                    boxes.add(PLATFORM);
                for (int[] b : boxes) {
                    double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                    for (int i = 0; i < quarter; i++) { // clockwise from above: (x, z) -> (16 - z, x)
                        double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
                        x0 = nx0; x1 = nx1; z0 = nz0; z1 = nz1;
                    }
                    shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(x0, b[1], z0, x1, b[4], z1));
                }
                out[d.get2DDataValue() * 2 + raised] = shape;
            }
        }
        return out;
    }

    public LiftBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(RAISED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RAISED);
    }

    /** The console faces the player who places it. */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).get2DDataValue() * 2 + (state.getValue(RAISED) ? 1 : 0)];
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof LiftBlockEntity lift))
            return InteractionResult.SUCCESS;
        // Sneak-use at the bottom puts the vehicle back into the world as blocks.
        String message = !lift.holding() ? lift.lift(server)
                : player.isShiftKeyDown() && lift.height() == 0 ? lift.lower(server)
                : lift.step(server, player.isShiftKeyDown() ? -1 : 1);
        Component balance = lift.holding() ? lift.balance(server) : null;
        Component said = Component.translatable("message.feedback.lift." + message, lift.height());
        if (message.equals("lifted") && lift.jointLoops() > 0)
            said = said.copy().append(" ").append(Component.translatable("message.feedback.lift.loops", lift.jointLoops()));
        player.displayClientMessage(balance == null ? said : said.copy().append(" ").append(balance), true);
        return InteractionResult.CONSUME;
    }

    /** Breaking the lift lets go of the vehicle; unloading it does not. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof LiftBlockEntity lift)
            lift.release(server);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, FVehicles.LIFT_ENTITY.get(), LiftBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LiftBlockEntity(pos, state);
    }
}
