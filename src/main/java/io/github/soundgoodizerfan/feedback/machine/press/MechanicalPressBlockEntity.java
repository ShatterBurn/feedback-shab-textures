// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.press;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.HeadedShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One large stamping stroke rather than many light blows -- same {@code Wu}/hardness gate as the
 * Hammer, through the same {@link Operation#BLOW} (a press is not a new physical action on the
 * material, only a different way of applying one, so it shares the Hammer's own deformation
 * chains rather than needing its own). One stroke per {@link #strokesDue}, so it presses as often
 * as it is turned, and a stroke that lands nothing wears the press like a wasted hammer blow.
 * <p>
 * Exists for the same reason the Rolling Mill does: unattended, network-driven throughput against
 * the Hammer's hands-on crank (philosophy §6).
 *
 * <h2>The head is the press</h2>
 * Each stroke lands the seated head's full St ceiling (a press has no crank setting to pick within it), and a
 * stroke that lands nothing wears the head, not the body. With no head it does nothing and wears nothing.
 */
public class MechanicalPressBlockEntity extends HeadedShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.BLOW;

    public MechanicalPressBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MECHANICAL_PRESS.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.MECHANICAL_PRESS_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || getHead().isEmpty())
            return;
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            St strength = getHead().stroke(level.random, this);
            Deforming.Blow blow = Deforming.apply(workpiece, strength, level, OPERATION);
            if (!blow.landed()) {
                wear(strength.value());
                continue;
            }
            workpiece = blow.result();
            changed = true;
        }
        if (changed)
            sync();
    }
}
