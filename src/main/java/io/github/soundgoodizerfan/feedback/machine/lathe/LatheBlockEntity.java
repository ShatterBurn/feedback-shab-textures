// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.lathe;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.HeadedShopMachineBlockEntity;
import io.github.soundgoodizerfan.feedback.process.Operation;
import io.github.soundgoodizerfan.feedback.process.Removing;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stock chucked and spun by the network; the seated lathe bit removes material continuously toward
 * whatever the workpiece's {@code Removal} entry calls its target. Same removal primitive as the
 * Drill Press, cutting continuously by speed ({@code passesDue}) rather than on discrete pushes --
 * a lathe's cut is continuous over the whole pass, not a series of separate feeds.
 *
 * <h2>Calipers' real second job</h2>
 * Nothing new is built for reading a live diameter. Calipers already read {@link Quantity#WORK}
 * off a workpiece's accumulated-Wu components ({@code WorkpieceTooltip}, {@code CalipersItem}) --
 * a Lathe's workpiece is no different from a Hammer's, so it is already "readable" the moment it
 * exists. What is genuinely new is exposing that same number to a *controller* -- this class
 * implements {@link SensorFitting} directly (an unsided sensor, the same shape a Clutch implements
 * {@code DataNode} directly rather than through a fitting slot) so a {@code Input} card can
 * wire it straight to a comparator, which can drive a Clutch on the same network for auto-stop.
 * Nothing new was added to {@code control/}.
 *
 * <h2>The tool bit wears; the machine does not</h2>
 * {@link #getCondition} is the seated bit's, which lives on the {@link ItemStack} (see
 * {@code FDataComponents#TOOL_CONDITION}), because philosophy 5 calls the bit itself the physical
 * component you point at -- a machine-wide condition figure was explicitly the wrong shape here.
 * A worn bit cuts less of each pass; it never stops clearing what its rating clears.
 */
public class LatheBlockEntity extends HeadedShopMachineBlockEntity implements SensorFitting {


    /** Wu turned off the piece now chucked -- what the sensor reads. Unlike the workpiece's own
     * WORK it does not reset when a stage completes, so done and not-started read differently. */
    private float turnedWu;

    public LatheBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LATHE.get(), pos, state);
    }

    /** The chuck of models/block/lathe.json is the table the workpiece lies on. */
    @Override
    public float workpieceSurface() {
        return 0.51f;
    }

    @Override
    public boolean insert(ItemStack stack) {
        boolean inserted = super.insert(stack);
        if (inserted)
            turnedWu = 0;
        return inserted;
    }

    @Override
    public String headKind() {
        return "lathe_bit";
    }

    public boolean isHeavyPass() {
        return getBlockState().getValue(LatheBlock.HEAVY);
    }

    @Override
    public Su getLoadSu() {
        return MachineTuning.LATHE_LOAD_SU;
    }

    public void tickServer() {
        if (level == null || level.isClientSide || workpiece.isEmpty() || getHead().isEmpty())
            return;
        float passes = passesDue();
        if (passes <= 0)
            return;

        // A blunt bit cuts less of every pass; it never stops clearing what its rating clears,
        // which would have turned wear into a hard gate on steel.
        St driveForce = new St(Math.min((isHeavyPass() ? MachineTuning.LATHE_ST_HEAVY : MachineTuning.LATHE_ST_LIGHT).value() * getCondition(),
                getHead().ceiling().value()));
        Removing.Cut cut = Removing.apply(workpiece, driveForce, getHead().hardness(), Operation.TURN,
                passes, carriedWu, level);
        carriedWu = cut.carried();

        if (!cut.landed()) {
            wear(driveForce.value() * passes);
            return;
        }

        turnedWu += cut.delivered();
        workpiece = cut.result();
        maybeDropSwarf(cut);
        sync();
    }

    // --- Instrument / SensorFitting ------------------------------------------------------------

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.WORK;
    }

    @Override
    public float resolution(Quantity quantity) {
        return 1f;   // Wu is whole numbers, same as the Hand Calipers reading Quantity.WORK.
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.lathe");
    }

    @Override
    public float readRaw() {
        return Instruments.quantise(turnedWu, resolution(Quantity.WORK));
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.readout.work_raw", Math.round(readRaw())));
    }

    /** Not a removable fitting -- this is the whole block implementing the sensor contract
     * directly (the same shape a Clutch implements {@code DataNode} directly), so there is
     * nothing to hand back on removal. */
    @Override
    public ItemStack getPickItem() {
        return ItemStack.EMPTY;
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("TurnedWu", turnedWu);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        turnedWu = tag.getFloat("TurnedWu");
    }

}
