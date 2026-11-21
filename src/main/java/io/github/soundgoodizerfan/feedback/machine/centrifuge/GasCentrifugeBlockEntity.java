// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.centrifuge;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Separates uranium by weight, shaft-driven. A batch of natural uranium ingots fluorinated in hydrofluoric acid is spun in a gas
 * cascade: one ingot comes out enriched, the rest depleted. Nothing happens under {@link MachineTuning#CENTRIFUGE_MIN_RPM}; above it the
 * batch advances with the square of the speed (centripetal acceleration), so a faster shaft finishes sooner. The machine holds the
 * feed, the acid and both outputs; it knows only uranium by the material it is made of.
 */
public class GasCentrifugeBlockEntity extends RotationNode implements io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder {

    @Override
    public io.github.soundgoodizerfan.feedback.modular.bake.FigureStore figures() {
        return figures;
    }

    private static final ResourceLocation URANIUM = ResourceLocation.fromNamespaceAndPath("feedback", "uranium_ingot");
    private static final ResourceLocation ENRICHED = ResourceLocation.fromNamespaceAndPath("feedback", "uranium_enriched_ingot");
    private static final ResourceLocation DEPLETED = ResourceLocation.fromNamespaceAndPath("feedback", "uranium_depleted_ingot");

    /** What a built centrifuge keeps of its rotor's design: the speed it starts at, what it loads the shaft with and the speed its rim holds ({@code BlockFigures}). */
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

    private int feed;
    private int enriched;
    private int depleted;
    private float progress;
    private final FluidTank acid = new FluidTank(MachineTuning.CENTRIFUGE_HF_TANK_MB, s -> s.getFluid() == FFluids.HYDROFLUORIC_ACID.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public GasCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GAS_CENTRIFUGE.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return new Su(figures.get("Load", MachineTuning.CENTRIFUGE_LOAD_SU.value()));
    }

    public IFluidHandler handler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return acid.getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return acid.getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return acid.isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return acid.fill(resource, action);
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public int feed() {
        return feed;
    }

    public int acidMb() {
        return acid.getFluidAmount();
    }

    public float progress() {
        return progress;
    }

    /** Takes natural uranium ingots from the stack into the feed; true if any went in. */
    public boolean load(ItemStack stack) {
        if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(URANIUM) || feed >= 4 * MachineTuning.CENTRIFUGE_BATCH_INGOTS)
            return false;
        int moved = Math.min(stack.getCount(), 4 * MachineTuning.CENTRIFUGE_BATCH_INGOTS - feed);
        feed += moved;
        stack.shrink(moved);
        setChanged();
        return true;
    }

    /** Everything finished, as stacks. */
    public java.util.List<ItemStack> unload() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        if (enriched > 0)
            out.add(new ItemStack(BuiltInRegistries.ITEM.get(ENRICHED), enriched));
        if (depleted > 0)
            out.add(new ItemStack(BuiltInRegistries.ITEM.get(DEPLETED), depleted));
        enriched = 0;
        depleted = 0;
        setChanged();
        return out;
    }

    public void tickServer() {
        float rpm = Math.abs(getRpm().value());
        float minRpm = figures.get("MinRpm", MachineTuning.CENTRIFUGE_MIN_RPM), rim = figures.get("TearRpm", 0f);
        // A rotor spun past what its rim holds tears itself apart: a mistake of the shaft's speed against the rotor's metal.
        if (rim > 0f && rpm > rim && level != null) {
            level.destroyBlock(worldPosition, true);
            return;
        }
        if (rpm < minRpm || feed < MachineTuning.CENTRIFUGE_BATCH_INGOTS || acid.getFluidAmount() < MachineTuning.CENTRIFUGE_HF_MB) {
            return;
        }
        float ratio = rpm / minRpm;
        progress += ratio * ratio;
        if (progress >= MachineTuning.CENTRIFUGE_BATCH_TICKS) {
            progress = 0f;
            feed -= MachineTuning.CENTRIFUGE_BATCH_INGOTS;
            acid.drain(MachineTuning.CENTRIFUGE_HF_MB, IFluidHandler.FluidAction.EXECUTE);
            enriched += 1;
            depleted += MachineTuning.CENTRIFUGE_BATCH_INGOTS - 1;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Feed", feed);
        tag.putInt("Enriched", enriched);
        tag.putInt("Depleted", depleted);
        tag.putFloat("Progress", progress);
        figures.save(tag, registries);
        tag.put("Acid", acid.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed = tag.getInt("Feed");
        enriched = tag.getInt("Enriched");
        depleted = tag.getInt("Depleted");
        progress = tag.getFloat("Progress");
        figures.load(tag, registries);
        acid.readFromNBT(registries, tag.getCompound("Acid"));
    }
}
