// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.data;

import java.util.concurrent.CompletableFuture;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.Tags;

/**
 * Publishes every Feedback fluid and gas under the conventional {@code c:} fluid tags so foreign pumps, tanks,
 * buckets and recipes can find it without knowing an id of ours.
 *
 * <p>Two kinds of tag, both read off the registered fluids, so a new metal or gas needs nothing here:
 * <ul>
 * <li>{@code c:<name>} for the fluid's own name, flowing variants folded in ({@code c:molten_copper},
 * {@code c:crude_oil}, {@code c:hydrogen}).</li>
 * <li>{@link Tags.Fluids#GASEOUS} for every fluid whose type is lighter than air (negative density, the same
 * figure the tanks and the fume rules already read), a property test and not a list.</li>
 * </ul>
 */
public final class FluidTagsGen extends FluidTagsProvider {

    public FluidTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, Feedback.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (var entry : BuiltInRegistries.FLUID.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!Feedback.MOD_ID.equals(id.getNamespace())) continue;
            Fluid fluid = entry.getValue();
            String name = id.getPath().endsWith("_flowing")
                    ? id.getPath().substring(0, id.getPath().length() - "_flowing".length())
                    : id.getPath();
            tag(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", name))).add(fluid);
            if (fluid.getFluidType().getDensity() < 0) tag(Tags.Fluids.GASEOUS).add(fluid);
        }
    }
}
