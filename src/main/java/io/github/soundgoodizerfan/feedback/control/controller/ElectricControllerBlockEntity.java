// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Electric Controller: the same {@link ControllerCore} as the mechanical one, powered as an electrical sink instead of off a shaft. It
 * evaluates while the line delivers at least half of what it wants, and takes every program medium: Punch Card, Circuit Board, Floppy
 * Disk and USB Drive.
 */
public class ElectricControllerBlockEntity extends ElectricNode implements ControllerHost {

    private final ControllerCore core = new ControllerCore(this, this::powered, stack -> stack.is(FTags.PUNCH_CARDS)).withCircuitSlot(true);

    public ElectricControllerBlockEntity(BlockPos pos, BlockState state) {
        this(FBlockEntities.ELECTRIC_CONTROLLER.get(), pos, state);
    }

    protected ElectricControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ControllerCore core() {
        return core;
    }

    protected boolean powered() {
        return ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return ControlTuning.CONTROLLER_ELECTRIC_AMPS + core.heaterAmps(ElectricRules.packetEu(ratedBand()));
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    /** The card goes to the client with the chunk and when it changes, so the Programmer can name this controller's variables to another card. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncCard() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        ControllerHost.super.setItem(slot, stack);
        syncCard();
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItem(int slot, int count) {
        net.minecraft.world.item.ItemStack out = ControllerHost.super.removeItem(slot, count);
        syncCard();
        return out;
    }

    @Override
    public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        net.minecraft.world.item.ItemStack out = ControllerHost.super.removeItemNoUpdate(slot);
        syncCard();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        core.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        core.load(tag, registries);
    }
}
