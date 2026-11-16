// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.client.bench.BenchUses.Use;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryStats;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.LauncherSpec;
import io.github.soundgoodizerfan.feedback.modular.combat.RangedDispatch;
import io.github.soundgoodizerfan.feedback.modular.combat.ShieldHeat;
import io.github.soundgoodizerfan.feedback.modular.combat.Throwing;
import io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.prep.PrepWork;
import io.github.soundgoodizerfan.feedback.modular.trait.Mail;
import io.github.soundgoodizerfan.feedback.modular.trait.ModularTraits;
import io.github.soundgoodizerfan.feedback.modular.trait.Rigid;
import io.github.soundgoodizerfan.feedback.weapon.BulletShape;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.modular.item.ModularShield;
import io.github.soundgoodizerfan.feedback.modular.item.ModularSpoon;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularFaces;
import io.github.soundgoodizerfan.feedback.modular.type.ModularHeads;
import io.github.soundgoodizerfan.feedback.modular.type.ModularMeasures;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.weapon.Blast;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The numbers the Blueprint Table reads for weapons, tools and hand types beside their figures: what a hafted build can harvest and
 * throw, a gun's rounds and heat, a charge's fragments and fuse, a shield's slow and heat, a spoon's stir. {@link BenchUses} calls
 * {@link #of} for every type; a number a build has no use for (a gun fed from a magazine holds no rounds of its own) is left out, and
 * which of them a type lists is its {@code measures}, never decided here.
 */
final class WeaponUses {

    private WeaponUses() {
    }

    static List<Use> of(ModularDesign d, ModularType t) {
        Map<ResourceLocation, Material> mats = ClientMaterials.get();
        return switch (t.family()) {
            case HAFTED -> hafted(d, t, mats);
            case GUN -> gun(d, t, mats);
            case CHARGE -> charge(d, t, mats);
            case SHIELD -> shield(d, t, mats);
            case BOW, CROSSBOW -> bow(d, t, mats);
            case ARMOUR -> armour(d, t);
            case AIR_GUN -> spring(d, t, mats);
            case HAND -> spoon(d, t, mats);
            default -> List.of();
        };
    }

    // --- helpers: the table's own line shapes, kept here so BenchUses stays another agent's ---------------------

    private static Use num(String id, float value, String text) {
        return new Use(id, Component.translatable(ModularMeasures.langKey(id), text), value);
    }

    private static Use fixed(String id, float value, String format) {
        return num(id, value, String.format(Locale.ROOT, format, value));
    }

    private static Use pct(String id, float share) {
        return num(id, share, Long.toString(Math.round(share * 100)));
    }

    // --- hafted: harvest class, hammer blow, throw speed -------------------------------------------------------

    private static List<Use> hafted(ModularDesign d, ModularType plan, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = ModularBake.bake(d, plan, mats, 0);
        if (baked.isEmpty())
            return List.of();
        ModularFigures f = baked.get();
        ModularType type = ModularFaces.view(plan, d);
        List<Use> out = new ArrayList<>();
        // What a blow of the head carries: a share of an ordinary hand-forging blow, capped at all of it (ModularHandTool.of).
        if (f.get(Fig.STRIKE_ST) > 0f)
            out.add(pct("hammer.blow", Math.min(1f, f.get(Fig.STRIKE_ST) / FTuning.HAND_SWING_BLOW_ST.value())));
        // What a thrown build leaves the hand at: one arm's throw over its mass (Throwing.launchSpeed).
        if (f.get(Fig.MASS) > 0f)
            out.add(fixed("throw.speed", Throwing.launchSpeed(f.get(Fig.MASS)), "%.2f"));
        Harvest harvest = harvest(d, plan, type, f);
        if (harvest.rank() >= 0)
            out.add(new Use("dig.class", Component.translatable(ModularMeasures.langKey("dig.class"),
                    Component.translatable(ModularMeasures.langKey("dig.class") + "." + harvest.rank())), harvest.rank()));
        // The hardest block the edge cuts without chipping: the rule ModularStacks.misuse runs, read for the edge that digs.
        out.add(fixed("chip.limit", ModularStacks.cleanHardness(harvest.toughness(), d.edge()), "%.3g"));
        // What a blow makes of a parry: window, recovery, cone and the Work the weapon takes clean, from the one profile the runtime reads.
        if (ParryStats.melee(d, plan)) {
            ParryTuning.Profile parry = ParryTuning.profile(f.get(Fig.MASS), f.get(Fig.REACH), f.get(Fig.BALANCE));
            out.add(num("parry.window", parry.window(), Integer.toString(parry.window())));
            out.add(num("parry.recover", parry.recovery(), Integer.toString(parry.recovery())));
            out.add(num("parry.cone", parry.coneHalfDeg(), Integer.toString(Math.round(parry.coneHalfDeg()))));
            out.add(num("parry.rating", parry.rating(), Integer.toString(Math.round(parry.rating()))));
        }
        out.addAll(handWork(d, plan, mats));
        return out;
    }

    /** What a hafted build's file, bit or knife does at the bench, in words (a grain, a tip, a slide has no number worth a bar). */
    private static List<Use> handWork(ModularDesign d, ModularType plan, Map<ResourceLocation, Material> mats) {
        List<Use> out = new ArrayList<>();
        ToolAbilities.Face file = ToolAbilities.face(d, plan, "file_head");
        if (file != null) {
            float grains = file.param("grains", 1f), marks = file.param("marks", 0f);
            int cut = grains >= 1.5f ? 0 : grains >= 0.75f ? 1 : 2;
            out.add(word("file.cut", cut, new String[] {"coarse", "medium", "fine"}[cut]));
            out.add(word("file.finish", marks < 0f ? 1 : 0, marks < 0f ? "polishes" : "marks"));
        }
        ToolAbilities.Face bit = ToolAbilities.face(d, plan, "driver_bit");
        if (bit != null) {
            String best = "none";
            float top = 0f;
            for (String form : List.of("screw", "machine_bolt", "rivet"))
                if (bit.param(form, 0f) > top) {
                    top = bit.param(form, 0f);
                    best = form;
                }
            out.add(word("driver.fit", List.of("none", "screw", "machine_bolt", "rivet").indexOf(best), best));
            boolean ratchet = ToolAbilities.ratchet(d, plan, mats) > 0f;
            out.add(word("driver.ratchet", ratchet ? 1 : 0, ratchet ? "yes" : "no"));
        }
        for (ModularDesign.Part p : ModularFaces.faces(d)) {
            ModularType.Role role = plan.roles().get(p.role());
            if (role == null || ModularFaces.spent(p) || !ModularHeads.kindOf(p, role).equals("knife_blade"))
                continue;
            PrepWork.Edge edge = PrepWork.Edge.of(d, plan);
            String cut = edge.slide() >= 1.3f ? "slicing" : edge.grain() >= 1.1f ? "boning" : "plain";
            out.add(word("knife.cut", edge.slide(), cut));
            break;
        }
        return out;
    }

    /** A text row: the line is the id's template over the word {@code <id>.<word>}; {@code rank} sorts it. */
    private static Use word(String id, float rank, String word) {
        return new Use(id, Component.translatable(ModularMeasures.langKey(id), Component.translatable(ModularMeasures.langKey(id) + "." + word)), rank);
    }

    /**
     * What the build's faces can drop, 0 (soft blocks only) to 3 (diamond class), or -1 for a build that digs nothing: the same reading
     * as {@code ModularStacks.tool} (the head that digs stone sets it, an edge too brittle for its hardness is held back to stone).
     */
    private static Harvest harvest(ModularDesign d, ModularType plan, ModularType type, ModularFigures f) {
        List<ModularHeads> heads = new ArrayList<>();
        for (String letter : ModularFaces.LETTERS) {
            ModularDesign.Part p = d.part(letter);
            ModularType.Role role = plan.roles().get(letter);
            if (p == null || role == null)
                continue;
            ModularHeads.of(ModularHeads.kindOf(p, role)).ifPresent(h -> heads.add(ModularFaces.spent(p)
                    ? new ModularHeads(List.of(), h.strike(), h.cells(), h.shaping(), h.combat(), h.wearLitres(), List.of(), h.role(), h.school(), h.trait(), h.traitParams()) : h));
        }
        float toughness = f.get(Fig.EDGE_TOUGHNESS);
        if (heads.isEmpty() || heads.stream().allMatch(h -> h.mines().isEmpty()))
            return new Harvest(-1, toughness);
        // A head shaped to dig nothing (a war pick) is a weapon, and no tool.
        ModularDesign.Part wearing = d.part(type.wearRole());
        if (heads.size() == 1 && wearing != null
                && type.roles().get(type.wearRole()).variant(wearing).map(v -> v.params().getOrDefault("digs", 1f) <= 0f).orElse(false))
            return new Harvest(-1, toughness);
        float hardness = f.get(Fig.HARDNESS);
        for (int i = heads.size() - 1; i >= 0; i--)
            if (heads.get(i).mines().stream().anyMatch(m -> m.tag().getPath().equals("mineable/pickaxe"))) {
                hardness = face(f, i, Fig.HARDNESS);
                toughness = face(f, i, Fig.EDGE_TOUGHNESS);
            }
        if (hardness < ModularTuning.STONE_CLASS_HARDNESS)
            return new Harvest(0, toughness);
        if (hardness < ModularTuning.IRON_CLASS_HARDNESS
                || (toughness < ModularTuning.IRON_CLASS_TOUGHNESS && hardness < ModularTuning.DIAMOND_CLASS_HARDNESS))
            return new Harvest(1, toughness);
        return new Harvest(hardness < ModularTuning.DIAMOND_CLASS_HARDNESS ? 2 : 3, toughness);
    }

    /** What the digging edge takes: the class (-1 for none) and the toughness of the face that does it, which the chip rule reads. */
    private record Harvest(int rank, float toughness) {
    }

    /** A figure of face {@code index}: the second and third faces have figures of their own. */
    private static float face(ModularFigures f, int index, Fig fig) {
        if (index == 0)
            return f.get(fig);
        boolean third = index == 2;
        return f.get(switch (fig) {
            case HARDNESS -> third ? Fig.HARDNESS_THIRD : Fig.HARDNESS_OFF;
            default -> third ? Fig.EDGE_TOUGHNESS_THIRD : Fig.EDGE_TOUGHNESS_OFF;
        });
    }

    // --- gun: rounds, heat, aim ---------------------------------------------------------------------------------

    private static List<Use> gun(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<GunSpec> spec = GunSpec.of(d, t, mats);
        if (spec.isEmpty())
            return List.of();
        List<Use> out = new ArrayList<>();
        spec.get().internalCapacity().ifPresent(n -> out.add(num("gun.rounds", n, Integer.toString(n))));
        Optional<GunFigures> g = spec.get().bake(mats);
        if (g.isPresent()) {
            GunFigures gf = g.get();
            // Heat a shot leaves in the barrel over its thermal mass, and what it sheds a second: sustained fire is the one against the other.
            out.add(fixed("gun.heat", gf.muzzleEnergy() * GunTuning.GUN_HEAT_PER_WORK * gf.heatFactor() / gf.thermalMass().workPerTu(), "%.3g"));
            out.add(fixed("gun.cool", gf.coolingRate().tuPerTick() * FTuning.TICKS_PER_SECOND, "%.3g"));
            // Shots of a plain round nose to the bore's floor: the barrel's hardness against the work the shot does (GunCondition).
            float shots = GunCondition.shotsToFloor(gf.muzzleEnergy(), BulletShape.ROUND_NOSE.wear(), mats.get(spec.get().material()));
            if (shots < Float.MAX_VALUE)
                out.add(num("gun.wear", shots, Long.toString(Math.round(shots))));
        }
        int ms = spec.get().sight().raiseMs();
        out.add(num("gun.aim", ms, Integer.toString(ms)));
        return out;
    }

    // --- charge: fragments, fuse --------------------------------------------------------------------------------

    private static List<Use> charge(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<Blast.Packed> read = Blast.read(d, t, mats);
        if (read.isEmpty())
            return List.of();
        Blast.Packed p = read.get();
        List<Use> out = new ArrayList<>();
        out.add(fixed("charge.frag", p.fragDamage(), "%.3g"));
        if (p.impact())
            out.add(new Use("charge.fuse", Component.translatable(ModularMeasures.langKey("charge.fuse"),
                    Component.translatable(ModularMeasures.langKey("charge.fuse") + ".impact")), 0f));
        else
            out.add(num("charge.fuse", p.fuseTicks(), p.fuseTicks() + " t (" + Readout.number(p.fuseTicks() / FTuning.TICKS_PER_SECOND) + " s)"));
        return out;
    }

    // --- shield: slow, heat -------------------------------------------------------------------------------------

    private static List<Use> shield(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = ModularBake.bake(d, t, mats, 0);
        if (baked.isEmpty())
            return List.of();
        List<Use> out = new ArrayList<>();
        ModularFigures sf = baked.get();
        out.add(pct("shield.slow", ModularShield.slow(sf.get(Fig.MASS))));
        // What the face's hardness and edge toughness do (ShieldHeat): hard takes less of a blow as wear, a brittle edge chips under a heavy one.
        out.add(pct("shield.wear", 1f / ShieldHeat.resist(sf.get(Fig.HARDNESS))));
        out.add(fixed("shield.chip", ShieldHeat.chipDamage(sf.get(Fig.EDGE_TOUGHNESS)), "%.3g"));
        ModularDesign.Part facePart = d.part(t.primary());
        Material face = facePart == null ? null : mats.get(facePart.material());
        if (face != null) {
            Level level = Minecraft.getInstance().level;
            float ambient = AmbientTable.at(level).value();
            Optional<Float> stands = Grip.stands(face);
            // Capacity holds in full to SHIELD_SOFTEN_ONSET of the way from the room to the face's softening point (ShieldHeat.hold).
            if (stands.isPresent() && stands.get() > ambient)
                out.add(num("shield.hot", ShieldHeat.holdsFullTo(ambient, stands.get(), sf.get(Fig.HARDNESS)),
                        Readout.number(ShieldHeat.holdsFullTo(ambient, stands.get(), sf.get(Fig.HARDNESS)))));
            else
                out.add(new Use("shield.hot", Component.translatable(ModularMeasures.langKey("shield.hot"),
                        Component.translatable(ModularMeasures.langKey("shield.hot") + ".never")), Float.MAX_VALUE));
        }
        return out;
    }

    // --- bow and crossbow: the shot as the traits shape it ------------------------------------------------------

    private static List<Use> bow(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = ModularBake.bake(d, t, mats, 0);
        if (baked.isEmpty())
            return List.of();
        // Limb length, recurve, rest, stabiliser and magazine all act through the traits: the same fold the draw, launch and arrow read.
        RangedDispatch.Shot shot = RangedDispatch.shot(ModularTraits.actives(d, t), baked.get());
        List<Use> out = new ArrayList<>();
        out.add(fixed("bow.draw", shot.drawTicks(), "%.3g"));
        out.add(fixed("bow.speed", shot.speed(), "%.2f"));
        out.add(fixed("bow.damage", shot.damage(), "%.2f"));
        out.add(fixed("bow.spread", shot.spread(), "%.2f"));
        out.add(num("bow.pierce", shot.pierce(), Integer.toString(shot.pierce())));
        out.add(pct("bow.slow", shot.slow()));
        return out;
    }

    /** The share of a slash, a blunt blow and a fall or blast the plates turn, from the mail and rigid traits' own multiples. */
    private static List<Use> armour(ModularDesign d, ModularType t) {
        List<Use> out = new ArrayList<>();
        for (ModularTraits.Active a : ModularTraits.actives(d, t)) {
            if (a.trait() instanceof Mail) {
                out.add(pct("armour.slash", 1f - Mail.slashMult(a.params())));
                out.add(pct("armour.blunt", 1f - Mail.bluntMult(a.params())));
            } else if (a.trait() instanceof Rigid)
                out.add(pct("armour.shock", 1f - Rigid.absorbMult(a.params())));
        }
        return out;
    }

    // --- spring gun: pellet -------------------------------------------------------------------------------------

    private static List<Use> spring(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        return LauncherSpec.of(d, t, mats)
                .<List<Use>>map(s -> List.of(fixed("spring.damage", s.shot(ModularTuning.AIR_REF_DENSITY, 0f).damage(), "%.2f")))
                .orElse(List.of());
    }

    // --- spoon: stir, skim, hold --------------------------------------------------------------------------------

    private static List<Use> spoon(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        ModularType.Role bowl = t.roles().get("B");
        ModularDesign.Part bowlPart = d.part("B");
        if (bowl == null || !bowl.name().equals("bowl") || bowlPart == null)
            return List.of();
        Optional<ModularFigures> baked = ModularBake.bake(d, t, mats, 0);
        Material wear = mats.get(bowlPart.material());
        if (baked.isEmpty() || wear == null || wear.density().muPerMb() <= 0f)
            return List.of();
        // The bowl's size as the stack reads it: the baked mass over the density of the wearing part (ModularStacks.volumeMb).
        float bowlSize = ModularSpoon.bowl(baked.get().get(Fig.MASS) / wear.density().muPerMb());
        List<Use> out = new ArrayList<>();
        out.add(fixed("spoon.stir", bowlSize * ModularSpoon.shape(d, "stir"), "%.2f"));
        out.add(fixed("spoon.skim", bowlSize * ModularSpoon.shape(d, "scoop"), "%.2f"));
        ModularDesign.Part handle = d.part("S");
        Level level = Minecraft.getInstance().level;
        if (handle != null && level != null) {
            float hold = ModularSpoon.holdsTo(level, handle.material(), ModularSpoon.shape(d, "reach"));
            out.add(num("spoon.hold", hold, Readout.number(hold)));
        }
        return out;
    }
}
