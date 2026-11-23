// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.dryer;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.potion.PotionState;
import io.github.soundgoodizerfan.feedback.potion.PotionTuning;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.modular.bake.TubeDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.item.ModularTube;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The vacuum dryer applies one physical operation: take the water out of a potion bottle. It does so while the chamber is both hot enough
 * ({@link PotionTuning#DRYER_MIN_TU}) and thin enough ({@link PotionTuning#DRYER_MAX_PU}, gauge, negative is vacuum), for {@link PotionTuning#DRYER_TICKS}
 * ticks; low pressure is why 60 Tu is enough, water boils off in a thin chamber long before a hundred.
 *
 * <h2>Where the vacuum comes from</h2>
 * Pressure exists as {@code Pu} (the Pressure Vessel, {@link PressureHolder}) and a sealed phosphor envelope reaches a partial vacuum by hot-sealing, but nothing
 * pumps one down. So the dryer is a {@link RotationNode}, a shaft-driven vacuum pump bolted to the chamber: its target gauge pressure falls with RPM
 * ({@link PotionTuning#DRYER_PU_PER_RPM}, floor {@link PotionTuning#DRYER_FULL_PU}) and the chamber follows at {@link PotionTuning#DRYER_RELAX_PER_TICK},
 * and leaks back toward zero when the shaft stops. No new unit, no new energy type. The chamber is a {@link ThermalBody} on a fire like the
 * stand and the phosphor screen.
 *
 * <h2>Misuse</h2>
 * Over {@link PotionTuning#DRYER_SCORCH_TU} the drying salt scorches: potency lost per tick grows with the excess, and the loss is fixed in the salts.
 * Over {@link PotionTuning#DRYER_CRACK_TU} under vacuum the bottle can crack (it is lost). A correct run costs nothing; the machine itself takes no wear.
 *
 * <h2>Sealing a vacuum tube</h2>
 * The same chamber evacuates and seals a built, still-open vacuum tube ({@link ModularTube}): held hot and thin for the same
 * {@link PotionTuning#DRYER_TICKS}, it comes out sealed, its vacuum set by how deep the pump had pulled, how hot the chamber had baked
 * the envelope and how tight the envelope is ({@link TubeDesign#vacuum}). A shallow, cool seal leaves gas in a gridded tube, which makes it
 * a thyratron; an envelope taken past its service temperature slumps and holds none. Nothing is refused or destroyed.
 */
public class DryerBlockEntity extends RotationNode implements ThermalBody, PressureHolder {

    private float temperature = ThermalTuning.AMBIENT_TU.value();
    private float pressure;
    private final Catchup offline = new Catchup();
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int ticks;
    private float scorch;

    public DryerBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.VACUUM_DRYER.get(), pos, state);
    }

    @Override
    public Su getLoadSu() {
        return PotionTuning.DRYER_LOAD_SU;
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return PotionTuning.DRYER_MASS;
    }

    @Override
    public Conductance getLeak() {
        return PotionTuning.DRYER_LEAK;
    }

    @Override
    public Pu getPressure() {
        return new Pu(pressure);
    }

    public float progress() {
        return ticks / (float) PotionTuning.DRYER_TICKS;
    }

    /** A real potion bottle goes in when the chamber holds nothing; a dud draught has no effect to keep. Returns whether it was taken. */
    public boolean insert(ItemStack stack, boolean simulate) {
        if (stack.getItem() instanceof ModularTube) {
            if (stack.has(FDataComponents.TUBE.get()) || !input.isEmpty() || !output.isEmpty() || level == null || tubeBuild(stack, level).isEmpty())
                return false;
            if (!simulate) {
                input = stack.copyWithCount(1);
                stack.shrink(1);
                ticks = 0;
                scorch = 0f;
                setChanged();
            }
            return true;
        }
        PotionState state = stack.get(FDataComponents.POTION_STATE.get());
        if (state == null || state.dud() || !input.isEmpty() || !output.isEmpty() || stack.is(FItems.POTION_SALTS.get()))
            return false;
        if (!simulate) {
            input = stack.copyWithCount(1);
            stack.shrink(1);
            ticks = 0;
            scorch = 0f;
            setChanged();
        }
        return true;
    }

    /** The salts if dried, otherwise the bottle back. */
    public ItemStack take() {
        ItemStack out = output.isEmpty() ? input : output;
        input = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        ticks = 0;
        setChanged();
        return out;
    }

    public void dropContents() {
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), input);
        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), output);
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.at(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());

        // Thin air does the pump's work: the chamber never sits above the gauge the world's own air has (an Overworld
        // atmosphere is 0, the End's near-vacuum is under the full-vacuum floor, so a dryer there needs no shaft).
        float ambientGauge = -(1f - Math.min(1f, io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable.at(level, worldPosition.getY())));
        float pumped = -Math.min(Math.abs(getRpm().value()) * PotionTuning.DRYER_PU_PER_RPM, -PotionTuning.DRYER_FULL_PU.value());
        float target = Math.max(PotionTuning.DRYER_FULL_PU.value(), Math.min(pumped, ambientGauge));
        pressure += (target - pressure) * PotionTuning.DRYER_RELAX_PER_TICK;

        boolean evacuated = pressure <= PotionTuning.DRYER_MAX_PU.value();
        if (input.getItem() instanceof ModularTube) {
            if (evacuated && temperature >= PotionTuning.DRYER_MIN_TU.value() && ++ticks >= PotionTuning.DRYER_TICKS)
                sealTube();
        } else if (!input.isEmpty()) {
            if (evacuated && temperature > PotionTuning.DRYER_CRACK_TU.value() && level.random.nextFloat() < PotionTuning.DRYER_CRACK_CHANCE) {
                input = ItemStack.EMPTY;
                ticks = 0;
                level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
            } else if (evacuated && temperature >= PotionTuning.DRYER_MIN_TU.value()) {
                scorch += Math.max(0f, temperature - PotionTuning.DRYER_SCORCH_TU.value()) * PotionTuning.DRYER_SCORCH_PER_TU_TICK;
                if (++ticks >= PotionTuning.DRYER_TICKS)
                    finish();
            }
        }
        setChanged();
    }

    /** What a tube stack is before its seal, read on whichever side asks; empty if it is not a buildable design. */
    private static java.util.Optional<TubeDesign.Build> tubeBuild(ItemStack stack, net.minecraft.world.level.Level level) {
        var design = ModularStacks.design(stack);
        var type = design == null ? null : ModularTypes.known(design.type());
        return type == null ? java.util.Optional.empty() : TubeDesign.of(design, type, ClientMaterials.on(level));
    }

    /** The tube comes out sealed: the chamber's depth now, its heat now and the envelope's tightness decide the vacuum. */
    private void sealTube() {
        var build = tubeBuild(input, level);
        if (build.isPresent()) {
            float depth = (pressure - PotionTuning.DRYER_MAX_PU.value()) / (PotionTuning.DRYER_FULL_PU.value() - PotionTuning.DRYER_MAX_PU.value());
            output = input.copy();
            output.set(FDataComponents.TUBE.get(), TubeDesign.seal(build.get(), TubeDesign.vacuum(build.get(), depth, temperature)));
        } else
            output = input.copy();
        input = ItemStack.EMPTY;
        ticks = 0;
    }

    private void finish() {
        PotionState state = input.get(FDataComponents.POTION_STATE.get());
        float potency = state.potencyNow(level) * (1f - Mth.clamp(scorch, 0f, PotionTuning.DRYER_SCORCH_MAX));
        // stamp 0 is the lazy clock's "no time passes": salts never fade.
        PotionState dry = new PotionState(state.effect(), state.amplifier(), state.duration(), potency, "", state.taste(), 0L, state.envTu(), state.tint());
        output = new ItemStack(FItems.POTION_SALTS.get());
        output.set(FDataComponents.POTION_STATE.get(), dry);
        input = ItemStack.EMPTY;
        ticks = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Pressure", pressure);
        tag.putInt("Ticks", ticks);
        tag.putFloat("Scorch", scorch);
        if (!input.isEmpty())
            tag.put("Input", input.save(registries));
        if (!output.isEmpty())
            tag.put("Output", output.save(registries));
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : ThermalTuning.AMBIENT_TU.value();
        pressure = tag.getFloat("Pressure");
        ticks = tag.getInt("Ticks");
        scorch = tag.getFloat("Scorch");
        input = tag.contains("Input") ? ItemStack.parseOptional(registries, tag.getCompound("Input")) : ItemStack.EMPTY;
        output = tag.contains("Output") ? ItemStack.parseOptional(registries, tag.getCompound("Output")) : ItemStack.EMPTY;
        offline.load(tag);
    }
}
