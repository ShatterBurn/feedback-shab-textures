// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.rub;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Friction electricity by hand: a rod of any good insulator (amber is the classical one) is seated, and each rub of it (a right-click) puts
 * charge on it that the rod holds because it is an insulator: the better the rod's {@code electrical.insulation} band, the more a rub leaves.
 * The charge leaks away on its own and is drawn off as a low-voltage source into whatever line the generator is wired to, a small burst
 * rather than a supply: it charges a Leyden jar, and that is the point of it.
 */
public class RubGeneratorBlockEntity extends ElectricNode {

    private ItemStack rod = ItemStack.EMPTY;
    /** Eu on the rod now. */
    private float charge;

    public RubGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.RUB_GENERATOR.get(), pos, state);
    }

    // --- the rod -------------------------------------------------------------------------------------

    /** The insulation band of the material of {@code stack}, or null when it is not an insulator. */
    public static VoltageBand insulationOf(ItemStack stack) {
        if (stack.isEmpty())
            return null;
        return Material.find(MaterialTable.get().materials(), stack.getItem())
                .flatMap(l -> l.material().electrical().insulation()).orElse(null);
    }

    public static boolean isRod(ItemStack stack) {
        VoltageBand band = insulationOf(stack);
        return band != null && band.compareTo(VoltageBand.LV) > 0;
    }

    public ItemStack rod() {
        return rod;
    }

    public ItemStack seat(ItemStack stack) {
        ItemStack old = rod;
        rod = stack.split(1);
        charge = 0f;
        setChanged();
        return old;
    }

    public ItemStack take() {
        ItemStack out = rod;
        rod = ItemStack.EMPTY;
        charge = 0f;
        setChanged();
        return out;
    }

    /** One rub: the rod takes its insulator's share of charge, up to what it can hold. Returns whether anything went on. */
    public boolean rub() {
        VoltageBand band = insulationOf(rod);
        if (band == null)
            return false;
        float cap = ElectricMachineTuning.RUB_GENERATOR_CAPACITY_EU;
        if (charge >= cap)
            return false;
        charge = Math.min(cap, charge + ElectricMachineTuning.RUB_EU_PER_BAND_STEP * (band.ordinal() + 1));
        setChanged();
        return true;
    }

    public float charge() {
        return charge;
    }

    // --- source --------------------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SOURCE;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public VoltageBand sourceBand() {
        return VoltageBand.LV;
    }

    @Override
    public float sourceAmps() {
        return charge > ElectricMachineTuning.LEYDEN_JAR_EMPTY_EU
                ? Math.min(ElectricMachineTuning.RUB_GENERATOR_AMPS, charge / ElectricRules.packetEu(VoltageBand.LV)) : 0f;
    }

    public void tickServer() {
        if (level == null || charge <= 0f)
            return;
        float before = charge;
        // Last tick's resolution: the packets drawn off, then the slow leak of a charge that holds on a rod, not a battery.
        charge -= getAmps() * getPacketEu();
        charge = Math.max(0f, charge * (1f - ElectricMachineTuning.RUB_GENERATOR_LEAK_PER_TICK));
        if (charge != before)
            setChanged();
    }

    // --- persistence ------------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!rod.isEmpty())
            tag.put("Rod", rod.save(registries));
        tag.putFloat("Charge", charge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        rod = tag.contains("Rod") ? ItemStack.parseOptional(registries, tag.getCompound("Rod")) : ItemStack.EMPTY;
        charge = tag.getFloat("Charge");
    }
}
