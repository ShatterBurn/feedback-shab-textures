// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/** A thruster: pushes along {@link #FACING}. A bucket or a pipe fills it; use toggles it. */
public class ThrusterBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /** The nozzle shape the part was made in ({@code nozzle_*} variants of the thruster_nozzle part), kept on the state so the model can differ. */
    public enum Nozzle implements net.minecraft.util.StringRepresentable {
        BELL("bell"), CONE("cone"), THROAT("throat");

        private final String id;

        Nozzle(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /** The nozzle for a part variant name; the base shape (bell) when the name is empty or unknown. */
        static Nozzle of(String variant) {
            for (Nozzle n : values())
                if (variant.equals("nozzle_" + n.id))
                    return n;
            return BELL;
        }
    }

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Nozzle> NOZZLE =
            net.minecraft.world.level.block.state.properties.EnumProperty.create("nozzle", Nozzle.class);

    /** Mirrors models/block/thruster*.json (scripts/thruster_models.py), drawn thrusting up: chamber, jacket, inlet, then the nozzle. */
    private static final int[][] CHAMBER = {{4, 6, 4, 12, 14, 12}, {3, 8, 3, 13, 10, 13}, {6, 14, 6, 10, 16, 10}};
    private static final int[][][] NOZZLE_BOXES = {
            {{6, 4, 6, 10, 6, 10}, {3, 2, 3, 13, 4, 13}, {2, 0, 2, 14, 2, 14}},
            {{5, 4, 5, 11, 6, 11}, {4, 2, 4, 12, 4, 12}, {3, 0, 3, 13, 2, 13}},
            {{5, 3, 5, 11, 6, 11}, {6, 0, 6, 10, 3, 10}}};
    private static final net.minecraft.world.phys.shapes.VoxelShape[][] SHAPES = shapes();

    /** One shape per facing and nozzle: the boxes turned the way the blockstate turns the model (x first, then y). */
    private static net.minecraft.world.phys.shapes.VoxelShape[][] shapes() {
        net.minecraft.world.phys.shapes.VoxelShape[][] out = new net.minecraft.world.phys.shapes.VoxelShape[6][Nozzle.values().length];
        for (Direction facing : Direction.values()) {
            int x = switch (facing) { case UP -> 0; case DOWN -> 180; default -> 90; };
            int y = switch (facing) { case SOUTH -> 180; case WEST -> 270; case EAST -> 90; default -> 0; };
            for (Nozzle n : Nozzle.values()) {
                net.minecraft.world.phys.shapes.VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
                for (int[][] set : new int[][][] {CHAMBER, NOZZLE_BOXES[n.ordinal()]})
                    for (int[] b : set) {
                        double[] p = turn(b[0], b[1], b[2], x, y), q = turn(b[3], b[4], b[5], x, y);
                        shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(Math.min(p[0], q[0]), Math.min(p[1], q[1]),
                                Math.min(p[2], q[2]), Math.max(p[0], q[0]), Math.max(p[1], q[1]), Math.max(p[2], q[2])));
                    }
                out[facing.ordinal()][n.ordinal()] = shape;
            }
        }
        return out;
    }

    private static double[] turn(double px, double py, double pz, int xDeg, int yDeg) {
        double x = px - 8, y = py - 8, z = pz - 8, t;
        for (int i = 0; i < xDeg / 90; i++) { // up goes to north: (x, y, z) -> (x, z, -y)
            t = y;
            y = z;
            z = -t;
        }
        for (int i = 0; i < yDeg / 90; i++) { // clockwise from above: (x, y, z) -> (-z, y, x)
            t = x;
            x = -z;
            z = t;
        }
        return new double[] {x + 8, y + 8, z + 8};
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPES[state.getValue(FACING).ordinal()][state.getValue(NOZZLE).ordinal()];
    }

    public ThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(NOZZLE, Nozzle.BELL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NOZZLE);
    }

    /** Thrust is along the way the player looks the block, so placing it against a face pushes the body away from it; the nozzle is the one the item was built with. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var design = context.getItemInHand().get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get());
        var nozzle = design == null ? null : design.part("S");
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite())
                .setValue(NOZZLE, Nozzle.of(nozzle == null ? "" : nozzle.variant()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ThrusterBlockEntity thruster) {
            thruster.setRunning(!thruster.running());
            player.displayClientMessage(Component.translatable(thruster.running() ? "message.feedback.thruster.on"
                    : "message.feedback.thruster.off", Math.round(thruster.getFuel().getFluidInTank(0).getAmount())), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThrusterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((ThrusterBlockEntity) be).tickServer();
    }
}
