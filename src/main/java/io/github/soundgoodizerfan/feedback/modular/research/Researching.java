// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

import io.github.soundgoodizerfan.feedback.control.computer.ComputerApps;
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
 * What the Research Table can research and what a run asks of the researcher. Three kinds of thing: a treatise volume, an attunement written
 * onto a rite, and an app written onto a blank disk. A volume wants as its specimen an item the volume before it taught how to build (a type gated by that volume), or for a first volume
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

    /** Every attunement, by id: any of them can be researched into a rite, again and again, for a better one. */
    public static List<Attunements.Effect> effects() {
        List<Attunements.Effect> out = new ArrayList<>(Attunements.known().values());
        out.sort(Comparator.comparing(e -> e.id().toString()));
        return out;
    }

    /** How many things there are to pick between in {@code mode} (0 volumes, 1 attunements, 2 apps). */
    public static int count(int mode) {
        return mode == 0 ? volumes().size() : mode == 1 ? effects().size() : ComputerApps.DISK_APPS.size();
    }

    /** The app the {@code index}-th entry of app mode names. */
    public static String app(int index) {
        return ComputerApps.DISK_APPS.get(Math.floorMod(index, ComputerApps.DISK_APPS.size()));
    }

    /** The plan for the {@code index}-th thing of {@code mode}, or null when there is none loaded. */
    public static ResearchPlan plan(int mode, int index) {
        if (mode == 1) {
            var list = effects();
            return list.isEmpty() ? null : ResearchPlans.get().attunement(list.get(Math.floorMod(index, list.size())).id().getPath());
        }
        if (mode == 2)
            return ResearchPlans.get().app(app(index));
        int[] v = volume(index);
        return ResearchPlans.get().volume(School.values()[v[0]], v[1]);
    }

    /** The figure of the material property called {@code key} that {@code m} states, if it does. */
    private static OptionalDouble figure(String key, io.github.soundgoodizerfan.feedback.process.Material m) {
        return switch (key) {
            case "hardness" -> OptionalDouble.of(m.hardness().value());
            case "toughness" -> m.mechanical().toughness().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "yield" -> m.mechanical().yieldStrength().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "density" -> OptionalDouble.of(m.density().muPerMb());
            case "melting_point" -> m.thermal().meltingPoint().map(x -> OptionalDouble.of(x.value())).orElse(OptionalDouble.empty());
            case "conductivity" -> m.electrical().conductivity().map(x -> OptionalDouble.of(x)).orElse(OptionalDouble.empty());
            case "specific_heat" -> m.thermal().specificHeat().map(x -> OptionalDouble.of(x.workPerMuPerTu())).orElse(OptionalDouble.empty());
            default -> OptionalDouble.empty();
        };
    }

    /**
     * How far up the property {@code key} {@code stack}'s material ranks among all the materials that state it, 0 to 1 (the share of them it matches
     * or beats); -1 when the stack is no material or states none. A rare, hard, hot or dense thing ranks high, so the bar an attunement sets
     * ({@code Effect.target}) is a rank, never an item.
     */
    public static float rank(String key, ItemStack stack) {
        var materials = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials();
        var found = io.github.soundgoodizerfan.feedback.process.Material.find(materials, stack);
        if (found.isEmpty())
            return -1f;
        OptionalDouble mine = figure(key, found.get().material());
        if (mine.isEmpty())
            return -1f;
        int below = 0, all = 0;
        for (var m : materials.values()) {
            OptionalDouble v = figure(key, m);
            if (v.isEmpty())
                continue;
            all++;
            if (v.getAsDouble() <= mine.getAsDouble())
                below++;
        }
        return all == 0 ? -1f : below / (float) all;
    }

    /** The ink an offering earns for {@code effect}: -1 when it does not clear the bar, else 0 to {@link ModularTuning#RESEARCH_OFFER_INK} by how far over it is. */
    public static int offeringInk(Attunements.Effect effect, ItemStack offering) {
        if (offering.isEmpty() || effect.offering().isEmpty())
            return -1;
        float rank = rank(effect.offering().get(), offering);
        if (rank < 0f || rank < effect.target())
            return -1;
        return Math.round(ModularTuning.RESEARCH_OFFER_INK * (rank - effect.target()) / Math.max(0.05f, 1f - effect.target()));
    }

    /** Whether {@code stack} is a brewed potion carrying the effect's reagent effect, still potent. */
    public static boolean reagentFits(Attunements.Effect effect, ItemStack stack, net.minecraft.world.level.Level level) {
        if (effect.reagent().isEmpty() || !(stack.getItem() instanceof io.github.soundgoodizerfan.feedback.potion.PotionItem))
            return false;
        var state = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.POTION_STATE.get());
        return state != null && state.effect().filter(effect.reagent().get()::equals).isPresent() && state.amplifierNow(level) >= 0;
    }

    /**
     * Whether an attunement can be tried: an offering that clears the bar of the property the attunement is about, or the reagent potion it
     * names. The ink is a base, the offering's (more the further it is over the bar) and a flat bonus for the reagent.
     */
    public static int attunementInk(Attunements.Effect effect, ItemStack offering, ItemStack reagent, net.minecraft.world.level.Level level) {
        int offer = offeringInk(effect, offering);
        boolean potion = reagentFits(effect, reagent, level);
        if (offer < 0 && !potion)
            return -1;
        return ModularTuning.RESEARCH_BASE_INK + Math.max(0, offer) + (potion ? ModularTuning.RESEARCH_REAGENT_INK : 0);
    }

    /**
     * The experience points a player has now, from their level and the bar. {@code totalExperience} is only a running score that a level set by
     * command or lost to a vanilla enchant never touches, so it is not what a level is worth; spending points ({@code giveExperiencePoints})
     * does take them from the bar and then from the level below.
     */
    public static long points(Player player) {
        long level = player.experienceLevel;
        long atLevel = level <= 16 ? level * level + 6 * level : level <= 31 ? (long) (2.5 * level * level - 40.5 * level + 360) : (long) (4.5 * level * level - 162.5 * level + 2220);
        return atLevel + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    /** Whether {@code stack} is what the app's plan says the app is about: its subject item, or any item of its subject tag. */
    public static boolean subjectFits(ResearchPlan plan, ItemStack stack) {
        if (plan == null || stack.isEmpty() || plan.subject.isEmpty())
            return false;
        if (plan.subject.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(plan.subject.substring(1));
            return tag != null && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag));
        }
        ResourceLocation id = ResourceLocation.tryParse(plan.subject);
        return id != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    /** Whether {@code stack} is a Floppy Disk with nothing on it: no app, no program. */
    public static boolean blankDisk(ItemStack stack) {
        return stack.is(io.github.soundgoodizerfan.feedback.registry.FItems.FLOPPY_DISK.get())
                && !stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.APP.get())
                && !stack.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.PROGRAM_GRAPH.get());
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
        // a cleaver, maul or spear is a face of the hafted tool: the school is the face's, not the type's
        String gate = type.family() == io.github.soundgoodizerfan.feedback.modular.type.ModularType.Family.HAFTED
                ? io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.school(design, type) : type.school();
        return (school.id() + ":" + (v[1] - 1)).equals(gate);
    }

    /** Whether the earlier volume a volume builds on is in hand (a first volume and an attunement need none). */
    public static boolean builtOn(Player player, net.minecraft.world.inventory.ContainerLevelAccess access, int mode, int index) {
        if (mode >= 1)
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
        return ModularTuning.RESEARCH_BASE_INK + Math.round(ModularTuning.RESEARCH_SPECIMEN_INK * (1f - ModularStacks.worn(specimen))) + eurekaInk(player, plan);
    }

    /** The ink an app's run starts with: the base, and the plan's eureka bonus (what the researcher has done with the subject). It sets the tries and the brackets of {@link AppHack}. */
    public static int appInk(ServerPlayer player, ResearchPlan plan) {
        return ModularTuning.RESEARCH_BASE_INK + eurekaInk(player, plan);
    }

    private static int eurekaInk(ServerPlayer player, ResearchPlan plan) {
        var e = plan.eureka;
        if (e == null)
            return 0;
        ResourceLocation id = ResourceLocation.tryParse(e.item());
        var item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null)
            return 0;
        int done = switch (e.kind()) {
            case "crafted" -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
            case "mined" -> player.getStats().getValue(Stats.BLOCK_MINED.get(net.minecraft.world.level.block.Block.byItem(item)));
            default -> player.getStats().getValue(Stats.ITEM_USED.get(item));
        };
        return done >= e.threshold() ? e.ink() : 0;
    }
}
