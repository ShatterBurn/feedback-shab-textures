// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.freewheel;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The input side of a freewheel on the network; the output is fed by {@link FreewheelBlock#step}. Cost: one speed read and at most one drive a tick. */
public class FreewheelBlockEntity extends RotationNode {

    public FreewheelBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FREEWHEEL.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return MachineTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return MachineTuning.GEARBOX_INERTIA;
    }

    public void tickServer() {
        if (level != null && !level.isClientSide)
            FreewheelBlock.step(level, this, getBlockState());
    }
}
