// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.attunement.AttunementKnowledge;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.treatise.ResearchTheme;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What the Research Table can research and how a run is laid out. Two kinds of thing: a treatise volume (its subject is the volume's own
 * property, {@link ResearchTheme}) and an attunement the player does not know yet (its subject is a property picked from the effect's id, so one
 * effect always asks about the same thing). Either way the play is the same ranking game over the samples laid on the table.
 */
public final class Researching {

    private Researching() {
    }

    /** The attunements {@code player} has not learned and that are not starters, by id: what the table can offer in attunement mode. */
    public static List<Attunements.Effect> unknown(Player player) {
        List<Attunements.Effect> out = new ArrayList<>();
        for (Attunements.Effect e : Attunements.get().effects().values())
            if (!AttunementKnowledge.knows(player, e))
                out.add(e);
        out.sort(Comparator.comparing(e -> e.id().toString()));
        return out;
    }

    /** The subject the {@code index}-th thing of {@code mode} (0 volumes, 1 attunements) is researched by. */
    public static ResearchTheme.Theme theme(Player player, int mode, int index) {
        if (mode == 0)
            return ResearchTheme.of(index);
        List<Attunements.Effect> list = unknown(player);
        if (list.isEmpty())
            return ResearchTheme.of(0);
        ResourceLocation id = list.get(Math.floorMod(index, list.size())).id();
        return ResearchTheme.of(Math.floorMod(id.toString().hashCode(), ResearchTheme.count()));
    }

    /** How many things there are to pick between in {@code mode}. */
    public static int count(Player player, int mode) {
        return mode == 0 ? ResearchTheme.count() : unknown(player).size();
    }

    /** The samples among {@code stacks} that state the theme's property and differ from each other in it, the first {@link ModularTuning#RESEARCH_SAMPLES}. */
    public static List<ItemStack> samples(List<ItemStack> stacks, ResearchTheme.Theme theme) {
        var materials = MaterialTable.get().materials();
        List<ItemStack> out = new ArrayList<>();
        List<Double> seen = new ArrayList<>();
        for (ItemStack s : stacks) {
            if (s.isEmpty() || out.size() >= ModularTuning.RESEARCH_SAMPLES)
                continue;
            var found = Material.find(materials, s);
            if (found.isEmpty())
                continue;
            var fig = ResearchTheme.figure(theme, found.get().material());
            // Two samples with the same figure cannot be told apart, so only the first counts.
            if (fig.isPresent() && !seen.contains(fig.getAsDouble())) {
                seen.add(fig.getAsDouble());
                out.add(s);
            }
        }
        return out;
    }

    /** The game's data: the index into {@code ResearchTheme} of the subject, then the samples' material ids. */
    public static String data(ResearchTheme.Theme theme, List<ItemStack> samples) {
        var materials = MaterialTable.get().materials();
        int index = 0;
        for (int i = 0; i < ResearchTheme.count(); i++)
            if (ResearchTheme.of(i) == theme)
                index = i;
        StringBuilder sb = new StringBuilder().append(index).append(';');
        for (int i = 0; i < samples.size(); i++) {
            var found = Material.find(materials, samples.get(i));
            if (i > 0)
                sb.append(',');
            sb.append(found.map(f -> f.id().getNamespace().equals("feedback") ? f.id().getPath() : f.id().toString()).orElse(""));
        }
        return sb.toString();
    }
}
