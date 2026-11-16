// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.grindingwheel;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.HeadedShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same removal primitive as the Drill Press and the Lathe, cut by the seated abrasive wheel: its
 * grit's hardness is the wheel's rating, so a diamond-gritted wheel is the route through a material
 * like hardened tool steel or quartz, and its grit axis sets how hard it can be driven.
 *
 * <h2>The one real exception to wear-only-from-misuse</h2>
 * The Hammer and the Lathe's tool bit both wear <em>only</em> from misuse -- force or a cut the
 * tool cannot use, absorbed rather than wasted for free. This wheel is different on purpose: grit
 * is genuinely consumed by correct use, the same way a real abrasive wheel wears down whether or
 * not the operator made a mistake. So {@link #tickServer} costs condition on every <em>landed</em>
 * grind, not only on a failed one, and its floor is zero rather than the Hammer's 0.5 -- a bald
 * wheel's effective hardness is also zero, which is a genuine hard gate (§7) rather than a machine
 * silently refusing to run.
 *
 * <h2>The thermal tie-in is the differentiator landing a third time</h2>
 * Grinding produces real interface heat. {@link #tickServer} bumps the workpiece's own {@code
 * ItemHeat} proportionally to Wu removed -- and keeps heating whatever it rubs and cannot cut, so
 * overrun grinding heats too -- no new mechanic, since a workpiece is hot wherever it
 * is regardless of what heated it (philosophy 9). Overrun grinding a part that was deliberately
 * hardened can walk its temperature back into a tempering-relevant range and soften it, which
 * needed nothing new to be written for it to happen.
 */
public class GrindingWheelBlockEntity extends HeadedShopMachineBlockEntity {

    public GrindingWheelBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GRINDING_WHEEL.get(), pos, state);
    }

    @Override
    public String headKind() {
        return "abrasive_wheel";
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.GRINDING_WHEEL_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || getHead().isEmpty())
            return;
        float passes = passesDue();
        if (passes <= 0)
            return;

        // The wheel's grit sets both how hard it can be driven and how hard it cuts; as the grit goes the ceiling falls with it, and a bald wheel drives nothing.
        St driveForce = getHead().ceiling();
        Removing.Cut cut = Removing.apply(workpiece, driveForce, getHead().hardness(), Operation.GRIND,
                passes, carriedWu, level);
        carriedWu = cut.carried();

        // A wheel rubbing something it cannot cut still heats it -- as if every pass were Wu.
        float wu = cut.landed() ? cut.delivered() : driveForce.value() * passes;
        if (cut.landed()) {
            workpiece = cut.result();
            if (getHead().wear(wu, MachineTuning.GRINDING_WHEEL_WEAR_PER_WU, MachineTuning.GRINDING_WHEEL_CONDITION_FLOOR))
                sync();
            setChanged();
            maybeDropSwarf(cut);
        }

        Tu heated = ItemHeat.get(workpiece, level);
        ItemHeat.set(workpiece, new Tu(heated.value() + wu * MachineTuning.GRINDING_HEAT_PER_WU), level);
        sync();
    }
}
