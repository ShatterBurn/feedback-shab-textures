// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.drillpress;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.HeadedShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.linkage.Reciprocating;
import io.github.soundgoodizerfan.feedback.machine.linkage.Throw;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The removal primitive's cheapest, first real test. The bit rotates for as long as the network
 * drives it -- that alone does not cut anything, exactly as a real drill press's spindle can spin
 * all day against a workpiece nobody is feeding in. Cutting only happens on a {@link #push()},
 * fed by hand with a right click.
 * <p>
 * <b>Automatic feed is a Crank Linkage.</b> A linkage placed in front of the press delivers one
 * stroke per turn of its crank and each stroke is one {@link #push()}, at whatever feed (slow or
 * fast) the press is set to. A Clutch upstream of the linkage starts and stops the feeding. The
 * throw and gearing do not matter here: a push's force is the feed's, not the stroke's. The
 * linkage borrows the press's load, as it does for every machine it drives, so a fed press costs
 * its spindle's Su on one shaft and the feed's on the other.
 * <p>
 * Overrun is the removal table's business: keep pushing on a drilled plate and its overrun entry
 * takes the rest of it to swarf. A bound, fast-fed bit -- see {@link MachineTuning#BIT_SNAP_CHANCE} --
 * costs the push; a push that cannot cut costs the machine (see {@code ShopMachineBlockEntity}).
 */
public class DrillPressBlockEntity extends HeadedShopMachineBlockEntity implements Reciprocating {

    public DrillPressBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DRILL_PRESS.get(), pos, state);
    }

    /** The base plate of models/block/drill_press.json is the table the workpiece lies on. */
    @Override
    public float workpieceSurface() {
        return 0.26f;
    }

    @Override
    protected boolean takesCircuit() {
        return true;
    }

    @Override
    public String headKind() {
        return "drill_bit";
    }

    public boolean isFastFeed() {
        return getBlockState().getValue(DrillPressBlock.FAST);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.DRILL_PRESS_LOAD_SU;
    }

    @Override
    public void onStroke(Throw installed, float gearAdvantage) {
        push();
    }

    /**
     * One push: one pass of the bit. The bit must actually be spinning -- a stalled or
     * unpowered spindle biting into anything is a hazard in reality and a no-op here.
     */
    public void push() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || getHead().isEmpty() || !isTurning())
            return;

        boolean fast = isFastFeed();
        if (fast && level.random.nextFloat() < MachineTuning.BIT_SNAP_CHANCE * spoilFactor()) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.2f, 0.5f);
            return;
        }

        St driveForce = new St(Math.min((fast ? MachineTuning.DRILL_PRESS_ST_FAST : MachineTuning.DRILL_PRESS_ST_SLOW).value() * getCondition(),
                getHead().ceiling().value()));
        Removing.Cut cut = Removing.apply(workpiece, driveForce, getHead().hardness(), Operation.DRILL,
                1f, carriedWu, level);
        carriedWu = cut.carried();
        if (!cut.landed()) {
            // A bit that cannot cut, or nothing left to cut: the push goes into the machine.
            wear(driveForce.value());
            return;
        }

        workpiece = cut.result();
        maybeDropSwarf(cut);
        sync();
    }
}
