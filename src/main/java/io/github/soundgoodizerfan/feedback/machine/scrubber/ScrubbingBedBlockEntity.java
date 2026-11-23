// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.scrubber;

import io.github.soundgoodizerfan.feedback.compat.thaumaturge.ThaumaturgeFlux;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A packed bed that takes Thaumaturge's flux out of its chunk. The bed is any item whose material has a {@code catalyst} figure (charcoal,
 * a molecular sieve), seated by hand: the figure's activity sets how fast it takes flux and how much it holds, its regeneration temperature
 * is the fire that burns the load off, and its sinter temperature is the fire that ruins it for good. Nothing here names an item.
 *
 * <p>The overrun: a saturated bed takes nothing and lets a trickle back into the chunk. The fire is a {@link HeatSource} directly beneath the
 * bed, read at its flame temperature; a bed held in a fire past its sinter line loses activity permanently, which is misuse, never uptime.
 * With Thaumaturge absent there is no flux to take and the bed idles. Once a second, server side only.
 */
public class ScrubbingBedBlockEntity extends BlockEntity {

    private static final int PERIOD_TICKS = 20;

    private ItemStack bed = ItemStack.EMPTY;
    /** How much flux the bed holds, 0 to its capacity. */
    private float load;
    /** The share of the bed's activity burnt away by too hot a fire, 0 to 1. */
    private float sintered;

    public ScrubbingBedBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.SCRUBBING_BED.get(), pos, state);
    }

    // --- the bed -----------------------------------------------------------------------------------

    /** The catalyst figure of the seated item's material, or null when it has none (or nothing is seated). */
    private Material.Catalyst catalyst() {
        return catalystOf(bed);
    }

    /** The catalyst figure of the material {@code stack} is, or null. */
    public static Material.Catalyst catalystOf(ItemStack stack) {
        if (stack.isEmpty())
            return null;
        return Material.find(MaterialTable.get().materials(), stack.getItem()).flatMap(l -> l.material().catalyst()).orElse(null);
    }

    public static boolean isBed(ItemStack stack) {
        return catalystOf(stack) != null;
    }

    public ItemStack bed() {
        return bed;
    }

    /** Seats one item as the bed; returns what was there (the load goes with the bed's own figures, so a new bed starts empty). */
    public ItemStack seat(ItemStack stack) {
        ItemStack old = bed;
        bed = stack.split(1);
        load = 0f;
        sintered = 0f;
        setChanged();
        return old;
    }

    public ItemStack take() {
        ItemStack out = bed;
        bed = ItemStack.EMPTY;
        load = 0f;
        sintered = 0f;
        setChanged();
        return out;
    }

    /** Activity left, 0 to 1: the figure of the bed's material less what the fire took. */
    public float activity() {
        Material.Catalyst c = catalyst();
        return c == null ? 0f : c.activity() * (1f - sintered);
    }

    public float capacity() {
        return MachineTuning.SCRUBBER_CAPACITY * activity();
    }

    /** How full the bed is, 0 to 1 (1 with no capacity left). */
    public float fill() {
        float cap = capacity();
        return cap <= 0f ? (bed.isEmpty() ? 0f : 1f) : Math.min(1f, load / cap);
    }

    public boolean saturated() {
        return !bed.isEmpty() && fill() >= 1f;
    }

    // --- the second -----------------------------------------------------------------------------------

    public void tickServer() {
        if (level == null || bed.isEmpty() || level.getGameTime() % PERIOD_TICKS != 0)
            return;
        Material.Catalyst c = catalyst();
        if (c == null)
            return;
        boolean changed = false;
        float fire = fireBelow();
        if (fire >= c.sinterTemperature().value() && sintered < 1f) {
            sintered = Math.min(1f, sintered + MachineTuning.SCRUBBER_SINTER_PER_SECOND);
            changed = true;
        }
        if (fire >= c.regenerationTemperature().value() && load > 0f) {
            load = Math.max(0f, load - capacity() * MachineTuning.SCRUBBER_REGEN_PER_SECOND);
            changed = true;
        } else if (ThaumaturgeFlux.present()) {
            float room = capacity() - load;
            if (room > 0f) {
                float took = ThaumaturgeFlux.drain(level, worldPosition, Math.min(room, MachineTuning.SCRUBBER_FLUX_PER_SECOND * activity()), false);
                if (took > 0f) {
                    load += took;
                    changed = true;
                }
            } else {
                // Saturated: the bed takes nothing and lets some of what it holds back out.
                ThaumaturgeFlux.add(level, worldPosition, MachineTuning.SCRUBBER_BREAKTHROUGH_PER_SECOND);
                load = Math.max(0f, load - MachineTuning.SCRUBBER_BREAKTHROUGH_PER_SECOND);
                changed = true;
            }
        }
        if (changed)
            setChanged();
    }

    /** The flame temperature of a fire directly beneath that faces up into the bed, 0 when none. */
    private float fireBelow() {
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        return below instanceof HeatSource source && source.heatFacing() == Direction.UP ? source.getFireTu().value() : 0f;
    }

    // --- persistence --------------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!bed.isEmpty())
            tag.put("Bed", bed.save(registries));
        tag.putFloat("Load", load);
        tag.putFloat("Sintered", sintered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bed = tag.contains("Bed") ? ItemStack.parseOptional(registries, tag.getCompound("Bed")) : ItemStack.EMPTY;
        load = tag.getFloat("Load");
        sintered = tag.getFloat("Sintered");
    }
}
