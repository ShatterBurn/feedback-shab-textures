// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.damper;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A {@link DataNode} as well as a {@link Switchable}, the same pairing {@code ClutchBlockEntity}
 * uses and for the same reason (spec {@code docs/specs/feedback_controller_spec.md} §3): a Punch Card's
 * generic actuator card needs a real {@link DataNodeRef} to resolve to. Unsided, standalone --
 * not a {@code RotationNode}, it carries no drag or inertia and belongs to no mechanical network.
 */
public class DamperBlockEntity extends BlockEntity implements Switchable, DataNode {



    public DamperBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DAMPER.get(), pos, state);
    }

    @Override
    public boolean isEngaged() {
        return getBlockState().getValue(DamperBlock.OPEN);
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (level == null || level.isClientSide || isEngaged() == engaged)
            return;

        io.github.soundgoodizerfan.feedback.vehicle.micro.FineHosts.setState(this, getBlockState().setValue(DamperBlock.OPEN, engaged), 3);
        level.playSound(null, worldPosition,
                engaged ? SoundEvents.WOODEN_TRAPDOOR_OPEN : SoundEvents.WOODEN_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, 0.4f, 0.8f);
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }
}
