// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.sounds.SoundSource;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Voxel Case: carries one whole cell of finespace as a single item. Used empty on a cell it takes the cell (its voxels, skins, wear, spin
 * and every hosted machine with the item it was made from) into the case and leaves air; used full on a free spot it puts the cell back as it
 * was, or does nothing if the spot is taken. A case holds one cell and does not stack. A cell with a joint or a belt cannot be carried: both
 * hold a link to something outside the cell.
 *
 * <p>Prior art: Chisels & Bits (MIT), design only: a whole chiseled block carried as one data component, placement that checks the target first
 * and commits all or nothing. Rejected: a stack of 64, the rule that no block entity can be carried (a cell hosts machines), naming by the
 * dominant block, and its assets.
 */
public class VoxelCaseItem extends Item {

    private static final String CELL = "Cell";

    public VoxelCaseItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The cell the case holds, or null if it is empty. */
    @Nullable
    public static CompoundTag held(ItemStack stack) {
        CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return data.contains(CELL, Tag.TAG_COMPOUND) ? data.getCompound(CELL) : null;
    }

    private static boolean linked(MicroCellBlockEntity cell) {
        if (cell.holdsBearing())
            return true;
        for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
            if (cell.belt(i) != null)
                return true;
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        CompoundTag held = held(stack);
        if (held == null)
            return take(stack, level, pos, player);
        BlockPos at = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(context.getClickedFace());
        if (!level.getBlockState(at).canBeReplaced() || level.isOutsideBuildHeight(at) || player != null && !level.mayInteract(player, at))
            return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            // The state is a stand-in: the load fills the cell and commit gives it the octants and mass its voxels say.
            level.setBlock(at, MicroCellBlock.stateFor(0xFF, 0), Block.UPDATE_ALL);
            if (level.getBlockEntity(at) instanceof MicroCellBlockEntity cell) {
                cell.loadCustomOnly(held, server.registryAccess());
                cell.commit();
                io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator.queue(server, at);
                io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator.requeue(server, at);
            }
            if (player == null || !player.isCreative())
                stack.remove(DataComponents.CUSTOM_DATA);
            SoundType sound = SoundType.STONE;
            level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult take(ItemStack stack, Level level, BlockPos pos, @Nullable Player player) {
        if (!(level.getBlockEntity(pos) instanceof MicroCellBlockEntity cell))
            return InteractionResult.PASS;
        if (linked(cell)) {
            if (player != null && level.isClientSide)
                player.displayClientMessage(Component.translatable("message.feedback.voxel_case.linked"), true);
            return InteractionResult.FAIL;
        }
        if (player != null && !level.mayInteract(player, pos))
            return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            CompoundTag tag = cell.saveWithoutMetadata(server.registryAccess());
            // The machines are in the case with the items they were made from: let them go from the cell without dropping.
            for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                if (cell.hosted(i) != null)
                    cell.releaseHosted(i);
            level.removeBlock(pos, false);
            CompoundTag data = new CompoundTag();
            data.put(CELL, tag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            level.playSound(null, pos, SoundType.STONE.getBreakSound(), SoundSource.BLOCKS, 1f, 1.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return held(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        CompoundTag held = held(stack);
        if (held == null) {
            lines.add(Component.translatable("tooltip.feedback.voxel_case.empty"));
            return;
        }
        int voxels = 0;
        for (byte id : held.getByteArray("Voxels"))
            if (id != 0)
                voxels++;
        lines.add(Component.translatable("tooltip.feedback.voxel_case.holds", voxels));
        int machines = held.getList("Machines", Tag.TAG_COMPOUND).size();
        if (machines > 0)
            lines.add(Component.translatable("tooltip.feedback.voxel_case.machines", machines));
    }
}
