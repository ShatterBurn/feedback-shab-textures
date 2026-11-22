// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * What a block holds of a fixed line it is one end of: the block at the other end, the line's material, and the strain on the line and on
 * this end's hold. A {@link LineHost} block entity keeps one and saves it. The end with the lower position is the primary.
 */
public final class LineEnd {

    private BlockPos partner;
    private ResourceLocation line = Feedback.id("wool");
    /** Strain on the line (0 to 1; parts at 1). Read from the primary end. */
    private float strain;
    /** Strain on this end's hold in the block behind it (0 to 1; it works loose at 1). */
    private float holdStrain;

    /** Load on the line over its rating this tick (not saved, set by {@link FixedLines}; read by a tension gauge). */
    private float tension;

    public float tension() {
        return tension;
    }

    void setTension(float value) {
        tension = value;
    }

    public BlockPos partner() {
        return partner;
    }

    public ResourceLocation line() {
        return line;
    }

    public float strain() {
        return strain;
    }

    public float holdStrain() {
        return holdStrain;
    }

    public boolean primary(BlockPos self) {
        return partner != null && self.asLong() < partner.asLong();
    }

    void link(BlockPos other, ResourceLocation material) {
        partner = other;
        line = material;
        strain = 0f;
        holdStrain = 0f;
    }

    void unlink() {
        partner = null;
        strain = 0f;
    }

    void setStrain(float value) {
        strain = value;
    }

    void setHoldStrain(float value) {
        holdStrain = value;
    }

    public void save(CompoundTag tag) {
        if (partner != null) {
            tag.putLong("partner", partner.asLong());
            tag.putString("line", line.toString());
            tag.putFloat("strain", strain);
        }
        tag.putFloat("hold_strain", holdStrain);
    }

    public void load(CompoundTag tag) {
        partner = tag.contains("partner") ? BlockPos.of(tag.getLong("partner")) : null;
        if (tag.contains("line"))
            line = ResourceLocation.parse(tag.getString("line"));
        strain = tag.getFloat("strain");
        holdStrain = tag.getFloat("hold_strain");
    }
}
