// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.core.tube.TubeTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An electric sink asking {@link TubeTuning#LINEAR_MOTOR_AMPS} at MV. {@link #powered} is true while the
 * run feeds it at least half of what it wants; tube blocks beside it read that when they time a hop.
 * Only a capsule with a magnetic plate answers to it (the same property again, spec 6.1).
 */
public class LinearMotorBlockEntity extends ElectricNode implements io.github.soundgoodizerfan.feedback.machine.Wearing {

    private boolean powered;
    /** How sound the winding is, 1 as built: current past what its wire carries wears it, to a floor, and it pushes by it. A ring never breaks. */
    private float condition = 1f;
    /** What a built ring keeps of its winding's design: the amps it draws and the push it gives a magnetic capsule ({@code BlockFigures}). */
    private final io.github.soundgoodizerfan.feedback.modular.bake.FigureStore figures = new io.github.soundgoodizerfan.feedback.modular.bake.FigureStore();

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.BLOCK_FIGURES.get()),
                input.get(io.github.soundgoodizerfan.feedback.registry.FModular.DESIGN.get()), input.get(io.github.soundgoodizerfan.feedback.registry.FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }

    /** The multiple of a magnetic capsule's drive this ring gives while it is fed. */
    public float push() {
        return 1f + (figures.get("Push", TubeTuning.TUBE_LINEAR_FACTOR) - 1f) * condition;
    }

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return powered;
    }

    public LinearMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LINEAR_MOTOR.get(), pos, state);
    }

    public boolean powered() {
        return powered;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return figures.get("Amps", TubeTuning.LINEAR_MOTOR_AMPS);
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        powered = ElectricRules.rate(eu, amps, wanted, ratedBand()) >= 0.5f;
        float over = ElectricRules.overCurrent(amps, eu, ratedBand(), figures.get("Ampacity", 0f));
        if (over > 0f && condition > ElectricMachineTuning.LINEAR_MOTOR_CONDITION_FLOOR) {
            condition = Math.max(ElectricMachineTuning.LINEAR_MOTOR_CONDITION_FLOOR, condition - ElectricMachineTuning.LINEAR_MOTOR_WEAR_PER_OVERCURRENT * over
                    * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(this));
            setChanged();
        }
    }
}
