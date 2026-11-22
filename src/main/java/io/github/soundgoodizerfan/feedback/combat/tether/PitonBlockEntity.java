// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** One end of a fixed line on a piton (see {@link LineHost}, {@link FixedLines}). */
public class PitonBlockEntity extends BlockEntity implements LineHost {

    private final LineEnd end = new LineEnd();

    public PitonBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PITON.get(), pos, state);
    }

    @Override
    public LineEnd end() {
        return end;
    }

    @Override
    public Vec3 eye() {
        return PitonBlock.eye(getBlockState(), worldPosition);
    }

    @Override
    public BlockPos behind() {
        return worldPosition.relative(getBlockState().getValue(PitonBlock.FACING).getOpposite());
    }

    @Override
    public void lineChanged() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (!level.isClientSide)
                FixedLines.index(level, this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            FixedLines.index(level, this);
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide)
            FixedLines.drop(level, worldPosition);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        end.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        end.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The line runs to the partner, so the box that decides whether the renderer draws covers both ends. */
    public AABB lineBounds() {
        return end.partner() == null ? new AABB(worldPosition) : new AABB(worldPosition).minmax(new AABB(end.partner())).inflate(1);
    }
}
