// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.sorter;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

/**
 * Inventory Sorter (cpw, GPL-3.0) soft compat. Read at its IMC handler ({@code InventorySorter.handleimcmessage}, 1.21 line):
 * a {@code containerblacklist} message carries a menu type id, and the sorter then neither sorts nor scroll-moves in that
 * menu. Taken: the message protocol only, no code. Rejected: a mixin into its stack-merge key (brittle across its releases,
 * and food that merges by our tolerance already merges where the work happens, in the vessel that finishes it).
 *
 * <p>A sort rewrites every slot of a container by item, so a screen whose slot positions are roles (an input here, a fuel
 * there, a result in the corner, a recipe grid) must be left alone: sorting would scramble the work in progress. Storage
 * screens (packs, depots) stay sortable. Without the sorter installed the messages go nowhere.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SorterCompat {

    /** Menus whose slots are roles, not storage. */
    static final List<String> ROLE_MENUS = List.of("engineering_table", "reloading_press", "blueprint_table", "research_table",
            "material_probe", "enchanting", "casting_basin", "mixer", "brewing_stand", "station", "programmer", "prep_table",
            "thermal_vessel", "arc_hearth", "crucible");

    private SorterCompat() {
    }

    @SubscribeEvent
    public static void onEnqueue(InterModEnqueueEvent event) {
        for (String menu : ROLE_MENUS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, menu);
            InterModComms.sendTo("inventorysorter", "containerblacklist", () -> id);
        }
    }
}
