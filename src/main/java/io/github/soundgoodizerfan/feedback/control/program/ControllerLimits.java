// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import java.util.Optional;


/**
 * What a Controller with a given set of fitted upgrades can run: the card cap (Card Racks), the
 * variable cap (Memory Drums) and whether typed delay lengths are allowed (Escapement). The
 * Controller checks a card against its own fitted figures; the Programmer, which does not know
 * which Controller a card will end up in, checks against {@link #MAX} (everything fitted).
 */
public record ControllerLimits(int nodes, int variables, boolean typedDelays) {

    /** A bare Controller. */
    public static final ControllerLimits BASE = of(0, 0, false);

    /** A Controller with every upgrade fitted: the ceiling no card can pass. */
    public static final ControllerLimits MAX = of(ControlTuning.MEMORY_DRUM_MAX, ControlTuning.CARD_RACK_MAX, true,
            io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_MEMORY_MAX);

    /**
     * What a printed card of this medium can hold. A soft medium (Floppy Disk, USB Drive) holds the variables of a bare Controller plus the
     * memory of the circuit seated in it ({@code CIRCUIT_STACK}, as a lattice adds to a Controller's Memory Drums); the cards and nodes it
     * may hold are the host's business, so those stay at the ceiling. Any other medium is held to {@link #MAX}.
     */
    public static ControllerLimits ofMedium(net.minecraft.world.item.ItemStack card) {
        if (!card.is(io.github.soundgoodizerfan.feedback.registry.FTags.SOFT_CARDS))
            return MAX;
        net.minecraft.world.item.ItemStack circuit = card.getOrDefault(io.github.soundgoodizerfan.feedback.registry.FDataComponents.CIRCUIT_STACK.get(),
                net.minecraft.world.item.ItemStack.EMPTY);
        int extra = circuit.isEmpty() ? 0 : io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit).memory();
        return new ControllerLimits(MAX.nodes(), BASE.variables() + extra, true);
    }

    public static ControllerLimits of(int drums, int racks, boolean escapement) {
        return of(drums, racks, escapement, 0);
    }

    /** As above with {@code extraVariables} more, the memory a fitted circuit's sculk lattice gives on top of the Memory Drums. */
    public static ControllerLimits of(int drums, int racks, boolean escapement, int extraVariables) {
        return new ControllerLimits(ControlTuning.CONTROLLER_MAX_NODES + racks * ControlTuning.CARD_RACK_NODES,
                ControlTuning.CONTROLLER_BASE_VARIABLES + drums * ControlTuning.MEMORY_DRUM_VARIABLES + extraVariables, escapement);
    }

    /** Why {@code graph} does not fit these limits (a {@code feedback.programmer.error} key), or empty. */
    public Optional<String> exceededBy(ProgramGraph graph) {
        if (graph.nodes().size() > nodes)
            return Optional.of("too_many_nodes");
        if (graph.variables().size() > variables)
            return Optional.of("too_many_variables");
        if (!typedDelays && graph.nodes().stream().anyMatch(n -> n instanceof DelayNode d && !d.named()))
            return Optional.of("typed_delay_locked");
        return Optional.empty();
    }

    /** Fewest Card Racks a Controller needs to hold {@code graph}'s cards. */
    public static int racksNeeded(ProgramGraph graph) {
        return Math.max(0, Math.ceilDiv(graph.nodes().size() - ControlTuning.CONTROLLER_MAX_NODES, ControlTuning.CARD_RACK_NODES));
    }

    /** Fewest Memory Drums a Controller needs to hold {@code graph}'s variables. */
    public static int drumsNeeded(ProgramGraph graph) {
        return Math.max(0, Math.ceilDiv(graph.variables().size() - ControlTuning.CONTROLLER_BASE_VARIABLES, ControlTuning.MEMORY_DRUM_VARIABLES));
    }
}
