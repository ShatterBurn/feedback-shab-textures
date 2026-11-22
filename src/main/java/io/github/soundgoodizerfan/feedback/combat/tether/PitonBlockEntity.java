// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * One end of a fixed line: the other piton it is tied to, the material of the line and the strain it has taken. Both ends hold the same
 * line; the end with the lower position is the primary, the one {@link FixedLines} indexes and the client draws from.
 */
public class PitonBlockEntity extends BlockEntity {

    /** The piton at the other end of the line, or null when this one holds none. */
    private BlockPos partner;
    private ResourceLocation line = Feedback.id("wool");
    /** Strain on the line (0 to 1; parts at 1). Kept on the primary end. */
    private float strain;
    /** Strain on this piton's hold in the block behind it (0 to 1; it works loose at 1). */
    private float holdStrain;

    public PitonBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.PITON.get(), pos, state);
    }

    public BlockPos partner() {
        return partner;
    }

    public ResourceLocation line() {
        return line;
    }

    public float strain() {
        return strain;
    }

    public float holdStrain() {
        return holdStrain;
    }

    /** True for the end that owns the line: the line is drawn and indexed from here. */
    public boolean primary() {
        return partner != null && worldPosition.asLong() < partner.asLong();
    }

    void link(BlockPos other, ResourceLocation material) {
        partner = other;
        line = material;
        strain = 0f;
        holdStrain = 0f;
        sync();
    }

    void unlink() {
        partner = null;
        strain = 0f;
        sync();
    }

    void setStrain(float value) {
        strain = value;
        setChanged();
    }

    void setHoldStrain(float value) {
        holdStrain = value;
        setChanged();
    }

    private void sync() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        if (level != null && !level.isClientSide)
            FixedLines.index(level, this);
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
        if (partner != null) {
            tag.putLong("partner", partner.asLong());
            tag.putString("line", line.toString());
            tag.putFloat("strain", strain);
        }
        tag.putFloat("hold_strain", holdStrain);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        partner = tag.contains("partner") ? BlockPos.of(tag.getLong("partner")) : null;
        if (tag.contains("line"))
            line = ResourceLocation.parse(tag.getString("line"));
        strain = tag.getFloat("strain");
        holdStrain = tag.getFloat("hold_strain");
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
        return partner == null ? new AABB(worldPosition) : new AABB(worldPosition).minmax(new AABB(partner)).inflate(1);
    }
}
