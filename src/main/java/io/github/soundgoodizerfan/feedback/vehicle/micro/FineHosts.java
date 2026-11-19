// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Modular blocks in fine space ({@code docs/specs/fine_space_hosting_spec.md}): a machine placed as a voxel is hosted, its own block entity made
 * by its block, given the item's components and ticked by the cell. This holds what decides which machines are hosted and how their figures
 * scale to a voxel; the cell ({@link MicroCellBlockEntity}) holds the entities.
 */
public final class FineHosts {

    /** A voxel's share of a block's volume. */
    public static final float VOLUME = 1f / MicroCellBlockEntity.VOXELS;
    /** A rotational inertia: mass by volume (1/64) and radius squared (1/16), as {@code VehicleTuning.MICRO_ROTATION_INERTIA_SCALE}. */
    public static final float INERTIA = 1f / 1024f;

    /** Figures that grow with how much machine there is: a smaller machine has a share of them. */
    private static final Set<String> BY_VOLUME = Set.of("Mass", "Power", "Force", "Burn", "Capacity", "Tank", "Flow", "Push", "Pull", "Lift",
            "Amps", "BurstAmps", "Load", "Supply", "StoresEu", "AnodeEu", "Conductance", "Heat", "Work", "Intake", "MbPerRpm", "MbPerPu", "Seep", "Leak");
    private static final Set<String> BY_INERTIA = Set.of("Inertia");

    private FineHosts() {
    }

    /**
     * Whether {@code stack} is a modular machine a cell hosts: a modular block item whose block has a block entity of its own, is not a part
     * the cell already knows, and whose block state is only its orientation. A machine that toggles its own state (lit, open, glowing)
     * writes it to its position, which in a cell is the cell's, and would replace the cell; routing such writes is later work
     * ({@code docs/specs/fine_space_hosting_spec.md}).
     */
    public static boolean hostable(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(stack.getItem() instanceof Modular))
            return false;
        var block = item.getBlock();
        return block instanceof EntityBlock && !(block instanceof MicroRotation) && !(block instanceof JointPart) && !(block instanceof TireHubBlock)
                && block.getStateDefinition().getProperties().stream()
                .allMatch(p -> p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty
                        || p.getValueClass() == net.minecraft.core.Direction.Axis.class);
    }

    /** The stack with its figures back at full size: what a machine taken out of a voxel is, with whatever condition it has kept. */
    public static ItemStack unscaled(ItemStack stack) {
        CompoundTag figures = stack.get(FDataComponents.BLOCK_FIGURES.get());
        if (figures == null)
            return stack;
        CompoundTag full = figures.copy();
        for (String key : figures.getAllKeys())
            if (BY_VOLUME.contains(key))
                full.putFloat(key, figures.getFloat(key) / VOLUME);
            else if (BY_INERTIA.contains(key))
                full.putFloat(key, figures.getFloat(key) / INERTIA);
        stack.set(FDataComponents.BLOCK_FIGURES.get(), full);
        return stack;
    }

    /** The stack with its figures at a voxel's scale ({@code block_figures} only; the design and the rest are the machine's own). */
    public static ItemStack scaled(ItemStack stack) {
        ItemStack out = stack.copyWithCount(1);
        CompoundTag figures = out.get(FDataComponents.BLOCK_FIGURES.get());
        if (figures == null)
            return out;
        CompoundTag scaled = figures.copy();
        for (String key : figures.getAllKeys())
            if (BY_VOLUME.contains(key))
                scaled.putFloat(key, figures.getFloat(key) * VOLUME);
            else if (BY_INERTIA.contains(key))
                scaled.putFloat(key, figures.getFloat(key) * INERTIA);
        out.set(FDataComponents.BLOCK_FIGURES.get(), scaled);
        return out;
    }

    /**
     * Runs {@code use} as if the machine were the block entity at the cell's position (which it shares: a hosted machine is made at the cell's
     * place), by standing it in the chunk's block entity map for the call and putting the cell back after, so the machine's own use code
     * finds itself with {@code level.getBlockEntity(pos)}. Single-threaded with the level, restored whatever happens.
     */
    private static <R> R as(Level level, BlockPos pos, BlockEntity machine, java.util.function.Supplier<R> use) {
        var map = level.getChunkAt(pos).getBlockEntities();
        BlockEntity cell = map.get(pos);
        map.put(pos, machine);
        try {
            return use.get();
        } finally {
            map.put(pos, cell);
        }
    }

    /** The click as the machine's own block would see it: the point in the voxel's unit cube, the voxel's state, the cell's position. */
    private static net.minecraft.world.phys.BlockHitResult inVoxel(BlockPos pos, int voxel, net.minecraft.world.phys.BlockHitResult hit) {
        int n = MicroCellBlockEntity.N;
        var local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()).scale(n)
                .subtract(voxel & 3, voxel >> 2 & 3, voxel >> 4);
        return new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX() + local.x, pos.getY() + local.y, pos.getZ() + local.z),
                hit.getDirection(), pos, hit.isInside());
    }

    /** A use with an item on the machine in voxel {@code voxel}: its own block decides. */
    public static net.minecraft.world.ItemInteractionResult useItemOn(MicroCellBlockEntity cell, int voxel, ItemStack stack, Level level,
                                                                      net.minecraft.world.entity.player.Player player,
                                                                      net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockEntity machine = cell.hosted(voxel);
        BlockState state = machine.getBlockState();
        BlockPos pos = cell.getBlockPos();
        var result = as(level, pos, machine, () -> state.useItemOn(stack, level, player, hand, inVoxel(pos, voxel, hit)));
        cell.synced();
        return result;
    }

    /** A use with an empty hand on the machine in voxel {@code voxel}: its own block decides. */
    public static net.minecraft.world.InteractionResult useWithoutItem(MicroCellBlockEntity cell, int voxel, Level level,
                                                                       net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.phys.BlockHitResult hit) {
        BlockEntity machine = cell.hosted(voxel);
        BlockState state = machine.getBlockState();
        BlockPos pos = cell.getBlockPos();
        var result = as(level, pos, machine, () -> state.useWithoutItem(level, player, inVoxel(pos, voxel, hit)));
        cell.synced();
        return result;
    }

    /** A hosted machine's block entity for {@code state} in the cell at {@code cell}, made from {@code stack}; null if the block makes none. */
    @Nullable
    public static BlockEntity make(Level level, BlockPos cell, BlockState state, ItemStack stack) {
        if (!(state.getBlock() instanceof EntityBlock block))
            return null;
        BlockEntity be = block.newBlockEntity(cell, state);
        if (be == null)
            return null;
        be.setLevel(level);
        be.applyComponentsFromItemStack(scaled(stack));
        return be;
    }
}
