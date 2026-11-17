// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Steppable;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * What a data block says about itself, and everything a program, the Programmer, the connector and the node
 * drawing need to know about it: the {@code kinds} it belongs to, the numbers it {@code reads} and whether it
 * can be driven ({@code writes}). A block entity declares by overriding {@link DataNode#dataSpec()}; the default
 * {@link #derive} reads the interfaces blocks already implement, so existing blocks declare nothing.
 * <p>
 * Kinds are block tags under {@code data/<ns>/tags/block/data_kind/}: a datapack, or another mod, puts a block in
 * a kind (the crucible and the arc hearth both in {@code feedback:data_kind/furnace}) and a tag can include
 * another tag. Nothing here tests a block id.
 */
public record DataSpec(Set<ResourceLocation> kinds, Set<ResourceLocation> reads, boolean writes) {

    public static final String KIND_PREFIX = "data_kind/";

    public static final DataSpec NONE = new DataSpec(Set.of(), Set.of(), false);

    public boolean reads(ResourceLocation signal) {
        return reads.contains(signal);
    }

    public boolean isOfKind(ResourceLocation kind) {
        return kinds.contains(kind);
    }

    /** The spec of a node whose block entity declares nothing itself: its block's kind tags plus what its interfaces offer. */
    public static DataSpec derive(DataNode node) {
        Set<ResourceLocation> reads = new HashSet<>();
        boolean writes = false;
        if (node instanceof ChannelSource source && source.offersChannels())
            reads.add(Signals.CONTROL);
        if (node instanceof Switchable) {
            reads.add(Signals.STATE);
            writes = true;
        }
        if (node instanceof Analog || node instanceof Steppable)
            writes = true;
        if (node instanceof SensorFitting fitting)
            for (Quantity quantity : Quantity.values())
                if (fitting.canRead(quantity)) {
                    reads.add(Signals.of(quantity));
                    break;
                }
        return new DataSpec(kindsOf(node.getNodeOwner().getBlockState().getBlock()), reads, writes);
    }

    /** The kind tags a block is in. */
    public static Set<ResourceLocation> kindsOf(Block block) {
        Set<ResourceLocation> kinds = new HashSet<>();
        block.builtInRegistryHolder().tags().forEach(tag -> {
            if (tag.location().getPath().startsWith(KIND_PREFIX))
                kinds.add(tag.location());
        });
        return kinds;
    }

    /** Every kind that exists, for the Programmer's list; read from the tags the client holds. */
    public static Set<ResourceLocation> allKinds() {
        Set<ResourceLocation> kinds = new java.util.TreeSet<>();
        BuiltInRegistries.BLOCK.getTagNames().map(TagKey::location).forEach(id -> {
            if (id.getPath().startsWith(KIND_PREFIX))
                kinds.add(id);
        });
        return kinds;
    }
}
