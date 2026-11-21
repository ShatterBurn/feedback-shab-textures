// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.column;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.multiblock.Multiblocks;
import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * One cut point (spec 6.4). The reboiler hands it the liquid that condensed at its height; it holds
 * that in a draw tank and offers it to any fluid handler beside it (the draw port is a pipe). Coke
 * builds on it only from over-temperature, and takes both its efficiency and its flow with it.
 */
public class TrayBlockEntity extends BlockEntity implements Part, io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder {

    @Override
    public io.github.soundgoodizerfan.feedback.modular.bake.FigureStore figures() {
        return figures;
    }

    /** The design rides to the client so the deck takes its metal's colour. */
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    private Structure structure;
    private final HeatedTank draw = new HeatedTank(PetrochemTuning.TRAY_DRAW_TANK_MB, this::setChanged);
    private float foul;

    public TrayBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.TRAY.get(), pos, state);
    }

    /** What a built tray keeps of its deck's design: share of vapour held back, mB a tick it takes, and how fast it cokes ({@code BlockFigures}); a sieve's constants for a tray built from nothing. */
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

    /** Fraction of the tray still clean, 0 to 1 (shown as machine condition). */
    public float condition() {
        return 1f - foul;
    }

    public float efficiency() {
        return figures.get("Efficiency", PetrochemTuning.TRAY_SIEVE_EFFICIENCY) * condition();
    }

    public HeatedTank getDraw() {
        return draw;
    }

    /** What a pipe sees: the draw can be taken from but never filled from outside, or a pipe would hand it straight back. */
    public IFluidHandler getDrawPort() {
        return drawPort;
    }

    private final IFluidHandler drawPort = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return draw.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return draw.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return draw.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return draw.drain(maxDrain, action);
        }
    };

    /** Takes up to a clean tray's flow of {@code stack} (the liquid that condensed here); returns how much it took. */
    public int take(FluidStack stack, float tu) {
        int amount = Math.min(Math.min(Math.round(figures.get("Flow", PetrochemTuning.TRAY_SIEVE_FLOW) * condition()), draw.getSpace()), stack.getAmount());
        if (amount <= 0)
            return 0;
        return draw.fill(FluidHeat.stamp(stack.copyWithAmount(amount), tu), IFluidHandler.FluidAction.EXECUTE);
    }

    /** Over-temperature coats the tray, faster with a heavier feed; misuse wear only, never uptime. */
    public void coke(float tu, float heaviness) {
        if (tu <= PetrochemTuning.COLUMN_COKE_TU)
            return;
        foul = Math.min(1f, foul + PetrochemTuning.COLUMN_FOUL_PER_TICK * figures.get("Foul", PetrochemTuning.TRAY_SIEVE_FOUL) * heaviness
                * (tu - PetrochemTuning.COLUMN_COKE_TU) / PetrochemTuning.COLUMN_COKE_TU);
        setChanged();
    }

    /** Steam through a cold column lifts the coke. */
    public void decoke(float amount) {
        foul = Math.max(0f, foul - amount);
        setChanged();
    }

    public void tickServer() {
        if (level == null || draw.isEmpty())
            return;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d),
                    d.getOpposite());
            if (target == null)
                continue;
            int fits = target.fill(draw.drain(PetrochemTuning.TRAY_DRAW_PUSH_MB, IFluidHandler.FluidAction.SIMULATE),
                    IFluidHandler.FluidAction.SIMULATE);
            if (fits > 0)
                target.fill(draw.drain(fits, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    // --- Part ------------------------------------------------------------------------------

    @Override
    public String role() {
        return ColumnStructure.roleOf(getBlockState(), ColumnStructure.TRAY);
    }

    @Override
    public boolean connects(Direction face) {
        return face.getAxis() == Direction.Axis.Y;
    }

    @Override
    public RoleRule rule() {
        return ColumnStructure.RULE;
    }

    @Nullable
    @Override
    public Structure structure() {
        return structure;
    }

    @Override
    public void setStructure(@Nullable Structure structure) {
        this.structure = structure;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Multiblocks.load(this);
    }

    @Override
    public void setRemoved() {
        Multiblocks.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Foul", foul);
        figures.save(tag, registries);
        tag.put("Draw", draw.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        foul = tag.getFloat("Foul");
        figures.load(tag, registries);
        if (tag.contains("Draw"))
            draw.readFromNBT(registries, tag.getCompound("Draw"));
    }
}
