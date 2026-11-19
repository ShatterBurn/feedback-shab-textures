// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A hosted machine's rotation node as a member of a rotation network ({@code docs/specs/finespace_hosting_spec.md}). The machine stands in a
 * voxel of its cell, and the cell is the block entity at its position, so the network is shown this stand-in for it: it finds the machine
 * by voxel like a {@link MicroRotor}, and every figure and callback is the machine's own. Its figures are those the machine was given at
 * placement ({@link FineHosts#scaled}); nothing is scaled again here.
 */
public final class HostedRotor implements FineMember {

    private final MicroCellBlockEntity cell;
    private final int index;
    private final RotationNode node;

    HostedRotor(MicroCellBlockEntity cell, int index, RotationNode node) {
        this.cell = cell;
        this.index = index;
        this.node = node;
    }

    public RotationNode node() {
        return node;
    }

    @Override
    public MicroCellBlockEntity cell() {
        return cell;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public BlockState state() {
        return node.getBlockState();
    }

    @Override
    public float getRatio() {
        return node.getRatio();
    }

    @Override
    public void setRatio(float ratio) {
        node.setRatio(ratio);
    }

    @Nullable
    @Override
    public RotationNetwork getNetwork() {
        return node.getNetwork();
    }

    @Override
    public void setNetwork(@Nullable RotationNetwork network) {
        node.setNetwork(network);
    }

    @Override
    public Rpm getGeneratedRpm() {
        return node.getGeneratedRpm();
    }

    @Override
    public Su getCapacitySu() {
        return node.getCapacitySu();
    }

    @Override
    public Su getLoadSu() {
        return node.getLoadSu();
    }

    @Override
    public Drag getDragSuPerRpm() {
        return node.getDragSuPerRpm();
    }

    @Override
    public Inertia getInertia() {
        return node.getInertia();
    }

    @Override
    public Rpm getRpm() {
        return node.getRpm();
    }

    @Override
    public void onNetworkChanged(Su capacitySu, Su loadSu) {
        node.onNetworkChanged(capacitySu, loadSu);
    }

    @Override
    public void onNetworkSpeedChanged() {
        node.onNetworkSpeedChanged();
    }

    @Override
    public void syncSpeedNow() {
        node.syncSpeedNow();
    }

    @Nullable
    @Override
    public Level getLevel() {
        return cell.getLevel();
    }

    @Override
    public BlockPos getBlockPos() {
        return cell.getBlockPos();
    }

    /** The node's own test (it is the block entity at its position) is false in a cell, so this is the cell's. */
    @Override
    public boolean isLive() {
        Level level = cell.getLevel();
        return level != null && !cell.isRemoved() && cell.member(index) == this && level.getBlockEntity(cell.getBlockPos()) == cell;
    }
}
