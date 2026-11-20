// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A tire hub: the part that makes a turned body a wheel ({@link MicroWheels}). It sits on the axle of a Rotary Bearing's body, in coarse
 * space as a block or in fine space as a voxel, and names the wheel's radius; it comes with no tire. A tire (the {@code tire} form of a
 * rubber, see {@code Material.Trait.TIRE}) used on it fits the tire: the hub then rolls on that material's grip, softness and rolling
 * resistance, drawn as a round tire about the axle. Sneak-use with an empty hand takes the tire off. The hub carries no suspension: the
 * wheel is where it is mounted, and a slider joint is the suspension.
 *
 * <p>Three sizes ({@link #SIZES}) in each space, so six in all. The fitted tire is the {@link #TIRE} state, an index into the tire materials
 * in id order, so it travels with the voxel wherever the voxel is moved.
 */
public class TireHubBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** 0 no tire; n the (n-1)th tire material in id order. */
    public static final IntegerProperty TIRE = IntegerProperty.create("tire", 0, 7);

    /** Tread radius in blocks of each size, in coarse space and in fine space. */
    private static final double[] COARSE = {0.75, 1.0, 1.5}, FINE = {0.25, 0.375, 0.5};
    public static final int SIZES = 3;

    private final int size;

    public TireHubBlock(int size, Properties properties) {
        super(properties);
        this.size = size;
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(TIRE, 0));
    }

    /** Tread radius in blocks, in fine space (a voxel) or coarse space (a block). */
    public double radius(boolean fine) {
        return (fine ? FINE : COARSE)[size];
    }

    private static java.util.Map<ResourceLocation, Material> tiresFor;
    private static List<ResourceLocation> tiresMemo = List.of();

    /**
     * The tire materials a hub can carry, in id order: every material with the {@code tire} trait, at most seven (more are left out and
     * logged). Memoised per material table, since a renderer asks every frame.
     */
    public static List<ResourceLocation> tires() {
        var now = MaterialIngredient.materialsNow();
        if (now != tiresFor) {
            List<ResourceLocation> all = now.entrySet().stream().filter(e -> e.getValue().traits().contains(Material.Trait.TIRE))
                    .map(java.util.Map.Entry::getKey).sorted(Comparator.comparing(ResourceLocation::toString)).toList();
            if (all.size() > 7)
                org.slf4j.LoggerFactory.getLogger("feedback.tire").warn("[feedback] {} tire materials, a hub carries seven: {} are left out", all.size(), all.subList(7, all.size()));
            tiresMemo = all.size() > 7 ? all.subList(0, 7) : all;
            tiresFor = now;
        }
        return tiresMemo;
    }

    /** The tire on a hub state, or null. */
    @Nullable
    public static Material tire(BlockState state) {
        ResourceLocation id = tireId(state);
        return id == null ? null : MaterialIngredient.materialsNow().get(id);
    }

    @Nullable
    public static ResourceLocation tireId(BlockState state) {
        int t = state.getValue(TIRE);
        List<ResourceLocation> all = tires();
        return t == 0 || t > all.size() ? null : all.get(t - 1);
    }

    /** The tire material {@code stack} is a tire of, or null: any item of the {@code tire} form of a material that has the trait. */
    @Nullable
    public static ResourceLocation tireOf(ItemStack stack) {
        var found = Material.find(MaterialIngredient.materialsNow(), stack);
        return found.isPresent() && found.get().form().equals("tire") && tires().contains(found.get().id()) ? found.get().id() : null;
    }

    /** The state with {@code material}'s tire fitted, or the bare hub for null. */
    public static BlockState withTire(BlockState hub, @Nullable ResourceLocation material) {
        MicroCellBlockEntity.touched();
        return hub.setValue(TIRE, material == null ? 0 : tires().indexOf(material) + 1);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        MicroCellBlockEntity.touched();
        super.onPlace(state, level, pos, oldState, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, TIRE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Mirrors models/block/tire_hub_*.json: the flange (taken as its bounding square) and the stub axle through it.
        return switch (state.getValue(AXIS)) {
            case X -> net.minecraft.world.phys.shapes.Shapes.or(box(7, 2, 2, 9, 14, 14), box(2, 6, 6, 14, 10, 10));
            case Y -> net.minecraft.world.phys.shapes.Shapes.or(box(2, 7, 2, 14, 9, 14), box(6, 2, 6, 10, 14, 10));
            case Z -> net.minecraft.world.phys.shapes.Shapes.or(box(2, 2, 7, 14, 14, 9), box(6, 6, 2, 10, 10, 14));
        };
    }

    /** A tire in hand fits it (the one on it comes off first). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        ResourceLocation tire = tireOf(stack);
        if (tire == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            giveBack(player, state);
            level.setBlock(pos, withTire(state, tire), Block.UPDATE_ALL);
            if (!player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sneak with an empty hand takes the tire off. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || state.getValue(TIRE) == 0)
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            giveBack(player, state);
            level.setBlock(pos, withTire(state, null), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The tire a hub state carries, handed to the player (or dropped). */
    public static void giveBack(Player player, BlockState state) {
        ResourceLocation id = tireId(state);
        if (id == null || player.getAbilities().instabuild)
            return;
        var mat = MaterialIngredient.materialsNow().get(id);
        if (mat == null)
            return;
        mat.stack(id, "tire").ifPresent(back -> {
            if (!player.getInventory().add(back))
                player.drop(back, false);
        });
    }

    /** Broken, the hub gives up its tire. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        ResourceLocation id = tireId(state);
        MicroCellBlockEntity.touched();
        if (id != null && !state.is(newState.getBlock()) && !moved && !level.isClientSide) {
            var mat = MaterialIngredient.materialsNow().get(id);
            if (mat != null)
                mat.stack(id, "tire").ifPresent(tire -> Block.popResource(level, pos, tire));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TireHubBlockEntity(pos, state);
    }
}
