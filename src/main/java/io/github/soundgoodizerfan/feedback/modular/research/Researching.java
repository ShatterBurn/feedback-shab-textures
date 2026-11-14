// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.attunement.AttunementKnowledge;
import io.github.soundgoodizerfan.feedback.modular.attunement.Attunements;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What the Research Table can research and what a run asks of the researcher. Two kinds of thing: a treatise volume and an attunement not yet
 * known. A volume wants as its specimen an item the volume before it taught how to build (a type gated by that volume), or for a first volume
 * one of the plain things its plan names; it also wants that earlier treatise in hand. Either way the play is the research board
 * ({@code ResearchBoardScreen}) over the plan authored for it ({@link ResearchPlan}).
 */
public final class Researching {

    private Researching() {
    }

    /** The volumes that can be researched, school by school, volume by volume. */
    public static List<int[]> volumes() {
        List<int[]> out = new ArrayList<>();
        for (School school : School.values())
            for (int v = 1; v <= school.volumes(); v++)
                out.add(new int[] {school.ordinal(), v});
        return out;
    }

    /** The school and volume of the {@code index}-th thing in volume mode. */
    public static int[] volume(int index) {
        List<int[]> all = volumes();
        return all.get(Math.floorMod(index, all.size()));
    }

    /** The attunements {@code player} has not learned and that are not starters, by id. */
    public static List<Attunements.Effect> unknown(Player player) {
        List<Attunements.Effect> out = new ArrayList<>();
        for (Attunements.Effect e : Attunements.get().effects().values())
            if (!AttunementKnowledge.knows(player, e))
                out.add(e);
        out.sort(Comparator.comparing(e -> e.id().toString()));
        return out;
    }

    /** How many things there are to pick between in {@code mode} (0 volumes, 1 attunements). */
    public static int count(Player player, int mode) {
        return mode == 0 ? volumes().size() : unknown(player).size();
    }

    /** The plan for the {@code index}-th thing of {@code mode}, or null when there is none loaded. */
    public static ResearchPlan plan(int mode, int index) {
        if (mode == 1)
            return ResearchPlans.get().attunement();
        int[] v = volume(index);
        return ResearchPlans.get().volume(School.values()[v[0]], v[1]);
    }

    /** Whether {@code specimen} is what research of the {@code index}-th thing of {@code mode} takes. */
    public static boolean specimenFits(ItemStack specimen, int mode, int index) {
        var design = ModularStacks.design(specimen);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return false;
        if (mode == 1)
            return true;
        int[] v = volume(index);
        School school = School.values()[v[0]];
        if (v[1] <= 1) {
            ResearchPlan plan = plan(0, index);
            return plan != null && plan.specimens.contains(design.type().getPath());
        }
        return (school.id() + ":" + (v[1] - 1)).equals(type.school());
    }

    /** Whether the earlier volume a volume builds on is in hand (a first volume and an attunement need none). */
    public static boolean builtOn(Player player, net.minecraft.world.inventory.ContainerLevelAccess access, int mode, int index) {
        if (mode == 1)
            return true;
        int[] v = volume(index);
        return v[1] <= 1 || io.github.soundgoodizerfan.feedback.modular.treatise.Treatises.has(player, access, School.values()[v[0]], v[1] - 1);
    }

    /**
     * How forgiving the specimen is of being pushed, 0 to 1: the toughness of the material it wears with, against the reference. A tougher
     * specimen takes more fizzles before it is worn.
     */
    public static float forgiving(ItemStack specimen) {
        var design = ModularStacks.design(specimen);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        var part = type == null ? null : design.part(type.wearRole());
        var m = part == null ? null : io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(part.material());
        if (m == null)
            return 0f;
        float t = m.mechanical().toughness().map(x -> x.value()).orElse(0f);
        return t / (t + ModularTuning.RESEARCH_TOUGH_HALF);
    }

    /**
     * The ink a run starts with: a base, more from a specimen that is in good shape, and the plan's eureka bonus when the researcher has already
     * done the real thing enough times.
     */
    public static int ink(ServerPlayer player, ItemStack specimen, ResearchPlan plan) {
        int ink = ModularTuning.RESEARCH_BASE_INK + Math.round(ModularTuning.RESEARCH_SPECIMEN_INK * (1f - ModularStacks.worn(specimen)));
        var e = plan.eureka;
        if (e != null) {
            ResourceLocation id = ResourceLocation.tryParse(e.item());
            var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null) {
                int done = switch (e.kind()) {
                    case "crafted" -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
                    case "mined" -> player.getStats().getValue(Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Block.byItem(item)));
                    default -> player.getStats().getValue(Stats.ITEM_USED.get(item));
                };
                if (done >= e.threshold())
                    ink += e.ink();
            }
        }
        return ink;
    }
}
