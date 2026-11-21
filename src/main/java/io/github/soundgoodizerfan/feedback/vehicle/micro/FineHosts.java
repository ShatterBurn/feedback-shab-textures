// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import java.util.Set;

import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlock;
import io.github.soundgoodizerfan.feedback.machine.cable.CableBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.cable.CableGauge;
import io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
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
 * Modular blocks in finespace ({@code docs/specs/finespace_hosting_spec.md}): a machine placed as a voxel is hosted, its own block entity made
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
     * the cell already knows, and whose block state is only its orientation or which is {@link Routed}. A machine that toggles its own
     * state (lit, open, glowing) writes it through {@link #setState}, which gives the write to its voxel; one that writes it any other way would
     * replace the cell with its own block.
     */
    public static boolean hostable(ItemStack stack) {
        if (isFineWire(stack))
            return true;
        if (!(stack.getItem() instanceof BlockItem item) || !(stack.getItem() instanceof Modular))
            return false;
        var block = item.getBlock();
        return block instanceof EntityBlock && !(block instanceof MicroRotation) && !(block instanceof JointPart) && !(block instanceof TireHubBlock)
                && (block instanceof Routed || block.getStateDefinition().getProperties().stream()
                .allMatch(p -> p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty
                        || p.getValueClass() == net.minecraft.core.Direction.Axis.class));
    }

    /**
     * Whether modular type {@code type} (its id is its item's) is one a cell hosts: a blueprint of it may be made fine. Found from the type's
     * own item, never listed by id.
     */
    public static boolean hostableType(net.minecraft.resources.ResourceLocation type) {
        return io.github.soundgoodizerfan.feedback.modular.item.ModularStacks.blank(type).filter(FineHosts::hostable).isPresent();
    }

    /** The counts of a fine build: each role's count in quarters, rounded up, at least one. */
    public static java.util.Map<String, Integer> fineCounts(java.util.Map<String, Integer> counts) {
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        counts.forEach((role, n) -> out.put(role, Math.max(1, (n + 3) / 4)));
        return out;
    }

    /** A fine wire of a conductor: the stock a wire voxel is drawn from, by its form and its material's ampacity, never by id. */
    public static boolean isFineWire(ItemStack stack) {
        return CableRecipe.FINE_WIRE.test(stack);
    }

    /** A hosted machine that is a finespace wire voxel. */
    public static boolean isFineWire(BlockEntity machine) {
        return machine instanceof CableBlockEntity cable && cable.gauge() == CableGauge.FINE;
    }

    /** The cable block's state for a wire voxel: the whole voxel conductor, no arms. */
    public static BlockState fineWireState() {
        return FBlocks.CABLE.get().defaultBlockState().setValue(CableBlock.GAUGE, CableGauge.FINE);
    }

    /**
     * A block whose block entity writes its own state (lit, open, glowing) only through {@link #setState}, so it can be hosted: the cell takes
     * the write as its voxel's new state instead of letting the machine's block replace the cell. A block that writes its state any other way
     * must not carry this.
     */
    public interface Routed {
    }

    /** The cell whose {@link #as} call is running a hosted machine, which stands in the chunk's map in the cell's place for the call. */
    @Nullable
    private static MicroCellBlockEntity running;

    /**
     * A machine's own state write ({@code level.setBlock(worldPosition, state, flags)} for an ordinary block): a hosted machine's voxel takes
     * {@code state} and the machine keeps its place; anything else is the plain write.
     */
    public static void setState(BlockEntity self, BlockState state, int flags) {
        Level level = self.getLevel();
        if (level == null)
            return;
        BlockPos pos = self.getBlockPos();
        MicroCellBlockEntity cell = running != null ? running : level.getBlockEntity(pos) instanceof MicroCellBlockEntity c ? c : null;
        if (cell == null || !cell.hostedState(self, state))
            level.setBlock(pos, state, flags);
    }

    /**
     * {@code Container.stillValidBlockEntity} for a machine that may be hosted: a hosted machine stands in a cell, not in the chunk's map, so
     * it is valid while its cell still holds it and the player is in reach of the cell.
     */
    public static boolean stillValid(BlockEntity machine, net.minecraft.world.entity.player.Player player) {
        Level level = machine.getLevel();
        if (level != null && level.getBlockEntity(machine.getBlockPos()) instanceof MicroCellBlockEntity cell && cell.holds(machine))
            return player.canInteractWithBlock(machine.getBlockPos(), 4.0);
        return net.minecraft.world.Container.stillValidBlockEntity(machine, player);
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
        MicroCellBlockEntity before = running;
        running = cell instanceof MicroCellBlockEntity c ? c : null;
        map.put(pos, machine);
        try {
            return use.get();
        } finally {
            map.put(pos, cell);
            running = before;
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
        // A wire voxel is of the material its fine wire is: found as the cable recipe finds it, not read off a component.
        if (be instanceof CableBlockEntity wire && wire.material() == null)
            Material.find(MaterialIngredient.materialsNow(), stack).map(Material.Located::id).ifPresent(wire::setMaterial);
        // A wire of no material carries nothing: refuse it, and the item is kept.
        if (be instanceof CableBlockEntity wire && wire.material() == null)
            return null;
        return be;
    }
}
