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
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.SpringGunSpec;
import io.github.soundgoodizerfan.feedback.modular.combat.Throwing;
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
        int rank = harvest(d, plan, type, f);
        if (rank >= 0)
            out.add(new Use("dig.class", Component.translatable(ModularMeasures.langKey("dig.class"),
                    Component.translatable(ModularMeasures.langKey("dig.class") + "." + rank)), rank));
        return out;
    }

    /**
     * What the build's faces can drop, 0 (soft blocks only) to 3 (diamond class), or -1 for a build that digs nothing: the same reading
     * as {@code ModularStacks.tool} (the head that digs stone sets it, an edge too brittle for its hardness is held back to stone).
     */
    private static int harvest(ModularDesign d, ModularType plan, ModularType type, ModularFigures f) {
        List<ModularHeads> heads = new ArrayList<>();
        for (String letter : ModularFaces.LETTERS) {
            ModularDesign.Part p = d.part(letter);
            ModularType.Role role = plan.roles().get(letter);
            if (p == null || role == null)
                continue;
            ModularHeads.of(ModularHeads.kindOf(p, role)).ifPresent(h -> heads.add(ModularFaces.spent(p)
                    ? new ModularHeads(List.of(), h.strike(), h.cells(), h.shaping(), h.combat(), h.wearLitres(), List.of(), h.role(), h.school(), h.trait(), h.traitParams()) : h));
        }
        if (heads.isEmpty() || heads.stream().allMatch(h -> h.mines().isEmpty()))
            return -1;
        // A head shaped to dig nothing (a war pick) is a weapon, and no tool.
        ModularDesign.Part wearing = d.part(type.wearRole());
        if (heads.size() == 1 && wearing != null
                && type.roles().get(type.wearRole()).variant(wearing).map(v -> v.params().getOrDefault("digs", 1f) <= 0f).orElse(false))
            return -1;
        float hardness = f.get(Fig.HARDNESS), toughness = f.get(Fig.EDGE_TOUGHNESS);
        for (int i = heads.size() - 1; i >= 0; i--)
            if (heads.get(i).mines().stream().anyMatch(m -> m.tag().getPath().equals("mineable/pickaxe"))) {
                hardness = face(f, i, Fig.HARDNESS);
                toughness = face(f, i, Fig.EDGE_TOUGHNESS);
            }
        if (hardness < ModularTuning.STONE_CLASS_HARDNESS)
            return 0;
        if (hardness < ModularTuning.IRON_CLASS_HARDNESS
                || (toughness < ModularTuning.IRON_CLASS_TOUGHNESS && hardness < ModularTuning.DIAMOND_CLASS_HARDNESS))
            return 1;
        return hardness < ModularTuning.DIAMOND_CLASS_HARDNESS ? 2 : 3;
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
        out.add(pct("shield.slow", ModularShield.slow(baked.get().get(Fig.MASS))));
        ModularDesign.Part facePart = d.part(t.primary());
        Material face = facePart == null ? null : mats.get(facePart.material());
        if (face != null) {
            Level level = Minecraft.getInstance().level;
            float ambient = AmbientTable.at(level).value();
            Optional<Float> stands = Grip.stands(face);
            // Capacity holds in full to SHIELD_SOFTEN_ONSET of the way from the room to the face's softening point (ShieldHeat.hold).
            if (stands.isPresent() && stands.get() > ambient)
                out.add(num("shield.hot", ambient + ModularTuning.SHIELD_SOFTEN_ONSET * (stands.get() - ambient),
                        Readout.number(ambient + ModularTuning.SHIELD_SOFTEN_ONSET * (stands.get() - ambient))));
            else
                out.add(new Use("shield.hot", Component.translatable(ModularMeasures.langKey("shield.hot"),
                        Component.translatable(ModularMeasures.langKey("shield.hot") + ".never")), Float.MAX_VALUE));
        }
        return out;
    }

    // --- spring gun: pellet -------------------------------------------------------------------------------------

    private static List<Use> spring(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        return SpringGunSpec.of(d, t, mats)
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
