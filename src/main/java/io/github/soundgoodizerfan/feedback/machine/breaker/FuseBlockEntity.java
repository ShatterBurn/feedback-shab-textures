// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.breaker;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricPropagator;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fuse: a holder for one fine wire, sacrificed to protect what it guards -- the third accepted
 * wear-with-correct-use exception, because a fuse is a consumable by nature. The
 * holder never wears; the wire is spent.
 *
 * <h2>Both its figures are its wire's metal</h2>
 * It is a cable's physics with a thin wire: over its rating the wire warms, and
 * when it passes its metal's melting point it is gone and the holder conducts nothing. The rating
 * is the metal's {@code ampacity} times {@link ElectricMachineTuning#FUSE_AMPS_PER_AMPACITY}; how long an
 * overload is tolerated is how far its melting point sits above the room. Tin melts at 232 Tu and
 * blows fast and low; copper carries more and blows slowly. Which fuse fits a line is read off the
 * material table, and nothing names a fuse.
 * <p>
 * A blown holder is rewired by hand with a fine wire of any metal.
 */
public class FuseBlockEntity extends ElectricNode implements ThermalBody {

    /** The wire's metal, or null when there is none -- placed empty, or blown. */
    @Nullable
    private ResourceLocation wire;
    private float temperature = ThermalTuning.AMBIENT_TU.value();

    public FuseBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.FUSE.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.WIRE;
    }

    @Override
    public boolean conducting() {
        return wire != null;
    }

    @Nullable
    private Material material() {
        return wire == null ? null : MaterialTable.get().materials().get(wire);
    }

    public float getRatingAmps() {
        Material m = material();
        return m == null ? 0f : m.ampacity().orElse(0f) * ElectricMachineTuning.FUSE_AMPS_PER_AMPACITY;
    }

    @Override
    public float lossPerPacket() {
        Material m = material();
        return m == null ? 0f : m.electrical().drop().orElse(0f) * ElectricMachineTuning.CABLE_LOSS_EU_PER_DROP;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        super.onResolved(band, eu, amps, wanted, blocked);
        Tu ambient = AmbientTable.at(level);
        float over = amps - getRatingAmps();
        if (wire != null && over > 0f)
            Heat.warm(this, ElectricMachineTuning.FUSE_OVERLOAD_WORK_PER_AMP * over);
        Heat.tick(this, ambient, ambient, false);

        Material m = material();
        if (m != null && temperature > m.thermal().meltingPoint().map(Tu::value).orElse(Float.MAX_VALUE))
            blow();
    }

    private void blow() {
        wire = null;
        setChanged();
        showWire();
        if (level != null && !level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
    }

    /** Puts the block's {@link FuseBlock#WIRED} state in step with the wire, so the model shows it or the blown holder. */
    private void showWire() {
        if (level == null || level.isClientSide)
            return;
        BlockState shown = getBlockState();
        if (shown.hasProperty(FuseBlock.WIRED) && shown.getValue(FuseBlock.WIRED) != (wire != null))
            level.setBlock(worldPosition, shown.setValue(FuseBlock.WIRED, wire != null), 3);
    }

    /** The wire's metal rides to the client so the wire in the glass takes its colour. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Whether the holder has a wire in it. */
    public boolean isIntact() {
        return wire != null;
    }

    @Nullable
    public ResourceLocation getWire() {
        return wire;
    }

    /** Fits one fine wire from {@code stack} into an empty holder. Returns whether it took one. */
    public boolean rewire(ItemStack stack) {
        if (wire != null || level == null)
            return false;
        Optional<Material.Located> found = Material.find(MaterialTable.get().materials(), stack.getItem());
        if (found.isEmpty() || !found.get().form().equals("fine_wire") || found.get().material().ampacity().isEmpty())
            return false;
        stack.shrink(1);
        wire = found.get().id();
        temperature = AmbientTable.at(level).value();
        setChanged();
        showWire();
        if (!level.isClientSide)
            ElectricPropagator.reconnect(level, worldPosition);
        return true;
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
        return ElectricMachineTuning.WIRE_MASS;
    }

    @Override
    public Conductance getLeak() {
        return ElectricMachineTuning.WIRE_LEAK;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (wire != null)
            tag.putString("Wire", wire.toString());
        tag.putFloat("Temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation before = wire;
        wire = tag.contains("Wire") ? ResourceLocation.tryParse(tag.getString("Wire")) : null;
        // The wire's colour is read when the chunk meshes: when the wire changes on the client, ask for a new mesh.
        if (level != null && level.isClientSide && !java.util.Objects.equals(before, wire))
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
    }
}
