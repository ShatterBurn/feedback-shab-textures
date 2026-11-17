// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.wiredrawer;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.HeadedShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Deforming;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Wire Drawer: the hammer's own model, minus the hammer -- one draw per {@link #strokesDue},
 * through the exact same {@link Deforming#apply} every hand and machine tool in the mod already
 * shares. Pulling stock through a die is tension, not a blow, so it always applies
 * {@link Operation#DRAW} -- see that enum's own doc for why a hammer's {@code BLOW} would collide
 * with this on the same input item.
 *
 * <h2>Draw speed is the block's one setting</h2>
 * Fast draws with the seated die's full ceiling and so clears a higher hardness, but risks the wire
 * snapping on each draw that lands; slow draws with {@link MachineTuning#WIRE_DRAWER_SLOW_SHARE} of it,
 * is always safe, and does not reach a metal only the full ceiling clears -- a genuine hard gate rather
 * than patience. A draw that cannot move the metal is absorbed by the die and wears it, the Hammer's
 * rule; with no die the drawer does nothing.
 */
public class WireDrawerBlockEntity extends HeadedShopMachineBlockEntity {

    private static final Operation OPERATION = Operation.DRAW;

    public WireDrawerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.WIRE_DRAWER.get(), pos, state);
    }

    @Override
    protected boolean takesCircuit() {
        return true;
    }

    @Override
    public String headKind() {
        return "wire_die";
    }

    public boolean isFastDraw() {
        return getBlockState().getValue(WireDrawerBlock.FAST);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.WIRE_DRAWER_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || getHead().isEmpty())
            return;
        boolean fast = isFastDraw();
        boolean changed = false;
        for (int due = strokesDue(); due > 0 && !workpiece.isEmpty(); due--) {
            St strength = new St(getHead().stroke(level.random, this).value() * (fast ? 1f : MachineTuning.WIRE_DRAWER_SLOW_SHARE));
            Deforming.Blow blow = Deforming.apply(workpiece, strength, level, OPERATION);
            if (!blow.landed()) {
                wear(strength.value());
                continue;
            }
            // Only a draw that actually pulls metal through the die can snap it.
            if (fast && level.random.nextFloat() < MachineTuning.WIRE_SNAP_CHANCE * spoilFactor()) {
                workpiece = ItemStack.EMPTY;
                level.playSound(null, worldPosition, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.6f, 0.7f);
            } else {
                workpiece = blow.result();
            }
            changed = true;
        }
        if (changed)
            sync();
    }
}
