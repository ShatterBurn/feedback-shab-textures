// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.part.ModularBlockItem;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.registry.FBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints a modular block by the materials it was built from. A block model marks the faces of one part with a tint index;
 * {@link #ROLES} says which part role that index is for that block, and the colour is that part's material colour
 * ({@link MaterialColors}), read from the placed block entity's design ({@link FigureHolder}) or from the item's design component.
 * A block with no such part (a creative copy, an addon not fitted) takes {@link #FALLBACK}. To tint a block's model: give its
 * faces a {@code tintindex}, add a row here, and have its block entity implement {@link FigureHolder} and send its update tag.
 * Models draw tinted faces in grey so the colour multiplies cleanly.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FigureTint {

    /** The colour of a part the design lacks: the machines' old fixed metal. */
    private static final ResourceLocation FALLBACK = Feedback.id("copper");

    /** Tint index to the part role it takes its material from, per block. */
    private static Map<Block, String[]> roles() {
        return Map.of(
                FBlocks.LEYDEN_JAR.get(), new String[]{"S", "F"},        // 0 the glass, 1 the foil
                FBlocks.ELECTROMAGNET.get(), new String[]{"S"});         // 0 the core and winding metal
    }

    private FigureTint() {
    }

    private static int colour(ModularDesign design, String[] roles, int tintIndex) {
        ModularDesign.Part part = design == null || tintIndex < 0 || tintIndex >= roles.length ? null : design.part(roles[tintIndex]);
        return 0xFF000000 | MaterialColors.of(part == null ? FALLBACK : part.material());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        roles().forEach((block, roles) -> event.register((state, level, pos, tintIndex) -> {
            ModularDesign design = level != null && pos != null && level.getBlockEntity(pos) instanceof FigureHolder holder
                    ? holder.figures().design() : null;
            return colour(design, roles, tintIndex);
        }, block));
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        Map<Block, String[]> roles = roles();
        for (Item item : BuiltInRegistries.ITEM)
            if (item instanceof ModularBlockItem block && roles.containsKey(block.getBlock())) {
                String[] r = roles.get(block.getBlock());
                event.register((stack, layer) -> colour(ModularStacks.design(stack), r, layer), item);
            }
    }
}
