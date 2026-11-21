// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bench;

import io.github.soundgoodizerfan.feedback.registry.FMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * The part-making benches' menu: {@link PartBenchMenu} on the Parts tab (shaping, forging, cuts; assembly and rework are the Engineering
 * Table's), with its {@link Station} filtering the stock. The stonecutter adds the vanilla cut as a second tab.
 */
public class StationMenu extends PartBenchMenu {

    /** Client-side open: the open packet carries the position and which bench it is (the client's copy of the block may be stale). */
    public StationMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), station(buf.readUtf()));
    }

    private static Station station(String name) {
        for (Station station : Station.values())
            if (station.getSerializedName().equals(name))
                return station;
        return Station.TOOL;
    }

    private StationMenu(int id, Inventory inventory, BlockPos pos, Station station) {
        super(FMenus.STATION.get(), ModularAssembly.Kind.TOOL, id, inventory, new SimpleContainer(PartBenchBlockEntity.SIZE), pos, station);
    }

    /** Server-side open. */
    public StationMenu(int id, Inventory inventory, StationBlockEntity table, Station station) {
        super(FMenus.STATION.get(), ModularAssembly.Kind.TOOL, id, inventory, table, table.getBlockPos(), station);
    }
}
