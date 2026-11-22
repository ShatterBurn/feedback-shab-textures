// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.winch;

import io.github.soundgoodizerfan.feedback.combat.tether.FixedLines;
import io.github.soundgoodizerfan.feedback.combat.tether.LineEnd;
import io.github.soundgoodizerfan.feedback.combat.tether.LineHost;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The drum of a winch on the rotation network, and one end of a fixed line.
 *
 * <ul>
 * <li><b>Reel.</b> Blocks per tick the line draws in is {@code RPM x 2 pi x radius / (60 x 20)} with the drum radius
 * {@link MachineTuning#WINCH_DRUM_RADIUS}; a positive speed draws toward the winch, a negative one pays out.
 * <li><b>Load.</b> What hangs on the line puts a torque on the drum: {@code hung mass x radius x }{@link MachineTuning#WINCH_SU_PER_MU_BLOCK}
 * Su on the shaft, nothing while nothing hangs. When the network cannot carry it the whole run reads zero speed (the ledger's answer), so the drum
 * stalls and holds: the line stays where it is and takes the weight, over its rating it strains (see {@link FixedLines}). The stall torque is the load limit.
 * <li><b>Hold.</b> The winch is bolted to its own frame, so unlike a piton it holds any load.
 * </ul>
 */
public class WinchBlockEntity extends RotationNode implements LineHost {

    private final LineEnd end = new LineEnd();
    /** Mass in Mu hanging on the line this tick. */
    private float hung;

    public WinchBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WINCH.get(), pos, state);
    }

    @Override
    public LineEnd end() {
        return end;
    }

    @Override
    public Vec3 eye() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public BlockPos behind() {
        return null;
    }

    @Override
    public float reel() {
        return (float) (getRpm().value() * 2 * Math.PI * MachineTuning.WINCH_DRUM_RADIUS / (60 * 20));
    }

    @Override
    public void hung(float mass) {
        boolean wasZero = hung <= 0f;
        // A load that moved by a little does not rebuild the ledger; one that comes on or goes off, or doubles, does.
        if (Math.abs(mass - hung) > Math.max(1f, hung * 0.25f) || wasZero != (mass <= 0f)) {
            hung = mass;
            if (getNetwork() != null)
                getNetwork().recalculate();
        }
    }

    @Override
    public boolean stalled() {
        return hung > 0f && isOverstressed();
    }

    @Override
    public Su getLoadSu() {
        return new Su(hung * MachineTuning.WINCH_DRUM_RADIUS * MachineTuning.WINCH_SU_PER_MU_BLOCK);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return MachineTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return MachineTuning.GEARBOX_INERTIA;
    }

    @Override
    public void lineChanged() {
        sync();
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
        end.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        end.load(tag);
    }

    /** The line runs to the partner, so the box that decides whether the renderer draws covers both ends. */
    public AABB lineBounds() {
        return end.partner() == null ? new AABB(worldPosition) : new AABB(worldPosition).minmax(new AABB(end.partner())).inflate(1);
    }
}
