// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularBake;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.prep.DishBake;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularMeasures;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Compound;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;
import io.github.soundgoodizerfan.feedback.process.FoodPerk;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import io.github.soundgoodizerfan.feedback.robot.RobotSpec;
import io.github.soundgoodizerfan.feedback.suit.Ability;
import io.github.soundgoodizerfan.feedback.suit.SuitModules;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * The Blueprint Table's numbers for the misc modular types: a suit module (what each attachment buys, what the module costs, what the wheel
 * gets), a robot (speed, dig, lift, step, drift), a respirator (air and filter as time), a backpack (free carry, fire), a bed (the heal a night
 * gives, its nights) and a dish (the effects it gives, eating time, seal, spill). Each is a {@link BenchUses.Use}; which of them a type lists is
 * its {@code measures}. Everything is read off the drawing's baked figures (a drawing bakes with {@link ModularBake#drawing} on) and the
 * shared helpers of the module, robot and dish code, so the table and the item agree.
 */
final class MiscUses {

    private MiscUses() {
    }

    static List<BenchUses.Use> of(ModularDesign d, ModularType t) {
        Map<ResourceLocation, Material> mats = ClientMaterials.get();
        return switch (t.family()) {
            case SUIT_MODULE -> suit(d, t, mats);
            case ROBOT -> robot(d, t, mats);
            case RESPIRATOR -> respirator(d, t, mats);
            case BACKPACK -> pack(d, t, mats);
            case BED -> bed(d, t, mats);
            case DISH -> dish(d, t, mats);
            default -> List.of();
        };
    }

    // --- helpers ------------------------------------------------------------------------------------------------

    private static BenchUses.Use line(String id, float value, Component line, List<Component> tip) {
        return new BenchUses.Use(id, line, value, tip);
    }

    private static Component key(String id, Object... args) {
        return Component.translatable(ModularMeasures.langKey(id), args);
    }

    private static Component tip(String key, Object... args) {
        return Component.translatable("gui.feedback.blueprint_table." + key, args);
    }

    private static Component yes(boolean on) {
        return tip(on ? "suit.yes" : "suit.no");
    }

    private static String n(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String pct(float share) {
        return Long.toString(Math.round(share * 100f));
    }

    /** Ticks as seconds, or minutes and seconds past a minute. */
    private static String time(float ticks) {
        long s = Math.round(ticks / 20f);
        return s >= 60 ? (s / 60) + " min " + (s % 60) + " s" : s + " s";
    }

    /** The figures of {@code d} as a drawing: the table draws no components, so a held part reads at the middling grade the prediction does. */
    private static Optional<ModularFigures> bake(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        boolean was = ModularBake.drawing();
        ModularBake.drawing(true);
        try {
            return ModularBake.bake(d, t, mats, 0);
        } finally {
            ModularBake.drawing(was);
        }
    }

    // --- suit module --------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> suit(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = bake(d, t, mats);
        if (baked.isEmpty())
            return List.of();
        ModularFigures f = baked.get();
        List<BenchUses.Use> out = new ArrayList<>();
        int feeds = ModularBake.suitFeeds(d, t, mats);
        out.add(line("suit.feeds", feeds, key("suit.feeds", feeds, Fig.ATTACHMENTS), List.of(tip("suit.feeds.tip"))));
        float draw = 0f;
        int wheel = 0;
        for (int i = 0; i < Fig.ATTACHMENTS; i++) {
            int id = Math.round(f.get(Fig.att(i)));
            String name = SuitModules.key(id);
            if (id <= 0 || name.isEmpty())
                continue;
            float p = f.get(Fig.pow(i)), a = f.get(Fig.arg(i));
            draw += SuitModules.draw(id, p);
            wheel |= SuitModules.gives(id, Math.round(f.get(Fig.sup(i))));
            out.add(attachment("suit." + name, id, p, a, t));
        }
        float mass = f.get(Fig.MASS);
        out.add(line("suit.cost", draw, key("suit.cost", n(draw), n(mass)), List.of(tip("suit.cost.tip"))));
        if (wheel != 0) {
            MutableComponent names = Component.empty();
            boolean first = true;
            for (Ability ab : Ability.values())
                if ((wheel & ab.bit()) != 0) {
                    if (!first)
                        names.append(", ");
                    first = false;
                    names.append(Component.translatable("gui.feedback.suit.ability." + ab.key()));
                }
            out.add(line("suit.abilities", Integer.bitCount(wheel), key("suit.abilities", names), List.of(tip("suit.abilities.tip"))));
        }
        return out;
    }

    /**
     * One attachment: its strength and the headline it buys at its tuned share {@code a}, with the other end of its trade-off and what it costs on hover.
     * The formulas are {@link SuitModules}'s own.
     */
    private static BenchUses.Use attachment(String uid, int id, float p, float a, ModularType t) {
        String strength = pct(p);
        Component head;
        Component tip;
        switch (id) {
            case SuitModules.OPTIC -> {
                head = key(uid, strength, n(SuitModules.reach(p, a)));
                tip = tip(uid + ".tip", n(SuitModules.sight(p, a)), n(SuitModules.perUse(id)), yes(SuitModules.keepsGlare(p)));
            }
            case SuitModules.SPRING -> {
                head = key(uid, strength, pct(SuitModules.fallCut(p, a)));
                tip = tip(uid + ".tip", pct(SuitModules.jump(p, a)), n(SuitModules.stomp(p, a)));
            }
            case SuitModules.ACTUATOR -> {
                head = key(uid, strength, pct(SuitModules.stride(p, a)));
                tip = tip(uid + ".tip", n(SuitModules.step(p, a)), n(SuitModules.draw(id, p)));
            }
            case SuitModules.TREAD -> {
                head = key(uid, strength, pct(SuitModules.groundGrip(p, a)));
                tip = tip(uid + ".tip", pct(SuitModules.waterSpeed(p, a)));
            }
            case SuitModules.FILTER -> {
                head = key(uid, strength, n(SuitModules.breath(p, a)));
                tip = tip(uid + ".tip", yes(SuitModules.scrubs(p, a)), n(SuitModules.perUse(id)));
            }
            case SuitModules.THERMAL -> {
                head = key(uid, strength, pct(SuitModules.fireCut(p, a)));
                tip = tip(uid + ".tip", n(SuitModules.cooling(p, a)), yes(SuitModules.lines(p, a)), n(SuitModules.perUse(id)));
            }
            case SuitModules.THRUSTER -> {
                head = key(uid, strength, n(SuitModules.throatOf(a)));
                tip = tip(uid + ".tip", n(SuitModules.throatOf(a)));
            }
            case SuitModules.PLATE -> {
                head = key(uid, strength, pct(SuitModules.blastCut(p, a)));
                tip = tip(uid + ".tip", pct(SuitModules.shotCut(p, a)), pct(SuitModules.knockResist(p, a)));
            }
            case SuitModules.COIL -> {
                head = key(uid, strength, n(SuitModules.magnetRadius(p, a)));
                tip = tip(uid + ".tip", n(SuitModules.draw(id, p)));
            }
            default -> {
                // the dispenser: the dose its tuned axis sets, read off the axis's range at the tuned share
                ModularType.Axis dose = t.roles().values().stream().map(r -> r.axes().get("dose")).filter(java.util.Objects::nonNull).findFirst().orElse(null);
                float mb = dose == null ? 0f : dose.min() + a * (dose.max() - dose.min());
                head = key(uid, strength, Math.round(mb));
                tip = tip(uid + ".tip");
            }
        }
        return line(uid, p, head, List.of(tip));
    }

    // --- robot --------------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> robot(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = bake(d, t, mats);
        if (baked.isEmpty())
            return List.of();
        RobotSpec spec = RobotSpec.of(d, baked.get(), mats);
        List<BenchUses.Use> out = new ArrayList<>();
        float perSecond = spec.speed() * 20f;
        out.add(line("robot.speed", perSecond, key("robot.speed", n(perSecond)), List.of()));
        if (spec.hardest() > 0f)
            out.add(line("robot.dig", spec.hardest(), key("robot.dig", n(spec.hardest())), List.of()));
        if (spec.drive().equals(RobotSpec.ROTOR) || spec.drive().equals(RobotSpec.GAS_CELL))
            out.add(line("robot.lift", spec.liftMargin(), key("robot.lift", n(spec.liftMargin())), List.of()));
        else
            out.add(line("robot.step", spec.stepUp(), key("robot.step", n(spec.stepUp())), List.of()));
        if (spec.sensor().equals(RobotSpec.INERTIAL))
            out.add(line("robot.drift", spec.drift(), key("robot.drift", pct(spec.drift())), List.of()));
        // the brain decides the rest: a small program with none, more for each element, and what its figures add
        out.add(line("robot.cards", spec.cards(), key("robot.cards", n(spec.cards())), List.of()));
        if (spec.brain().gain() > 0f)
            out.add(line("robot.sense", spec.senseReach(1), key("robot.sense", n(spec.senseReach(io.github.soundgoodizerfan.feedback.robot.RobotTuning.RANGE_MAX))), List.of()));
        if (spec.places() > 0)
            out.add(line("robot.places", spec.places(), key("robot.places", n(spec.places())), List.of()));
        if (spec.leashes())
            out.add(line("robot.leash", spec.leashChannels(), key("robot.leash", n(spec.leashChannels())), List.of()));
        return out;
    }

    // --- respirator -----------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> respirator(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = bake(d, t, mats);
        if (baked.isEmpty())
            return List.of();
        ModularFigures f = baked.get();
        List<BenchUses.Use> out = new ArrayList<>();
        float life = f.get(Fig.FILTER_LIFE);
        if (life > 0f)
            out.add(line("resp.filtertime", life, key("resp.filtertime", time(life)), List.of()));
        float air = f.get(Fig.AIR_CAPACITY);
        if (air > 0f) {
            float ticks = air / ModularTuning.RESPIRATOR_DRAW_MB_PER_TICK;
            out.add(line("resp.airtime", ticks, key("resp.airtime", time(ticks)), List.of()));
        }
        return out;
    }

    // --- backpack ---------------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> pack(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = bake(d, t, mats);
        if (baked.isEmpty())
            return List.of();
        ModularFigures f = baked.get();
        List<BenchUses.Use> out = new ArrayList<>();
        float free = f.get(Fig.CARRY) - f.get(Fig.MASS);
        out.add(line("pack.free", free, key("pack.free", n(free)), List.of(tip("pack.free.tip"))));
        float c = f.get(Fig.FIRE_CONDUCT);
        if (c > 0f) {
            float chance = c / (c + ModularTuning.PACK_PATCH_HALF_CONDUCT);
            out.add(line("pack.fire", chance, key("pack.fire", pct(chance)), List.of()));
        }
        return out;
    }

    // --- bed ----------------------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> bed(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        Optional<ModularFigures> baked = bake(d, t, mats);
        if (baked.isEmpty())
            return List.of();
        ModularFigures f = baked.get();
        List<BenchUses.Use> out = new ArrayList<>();
        // what a night gives back of the sleeper's health, as ModularBed.soothe works it out; in snow country less by the padding's warmth
        float heal = ModularTuning.BED_HEAL * f.get(Fig.COMFORT);
        out.add(line("bed.heal", heal, key("bed.heal", pct(heal), pct(heal * f.get(Fig.WARMTH))), List.of(tip("bed.heal.tip"))));
        float nights = f.get(Fig.DURABILITY) / ModularTuning.BED_WEAR;
        out.add(line("bed.nights", nights, key("bed.nights", Math.round(nights)), List.of()));
        return out;
    }

    // --- dish -----------------------------------------------------------------------------------------------------------

    private static List<BenchUses.Use> dish(ModularDesign d, ModularType t, Map<ResourceLocation, Material> mats) {
        boolean was = ModularBake.drawing();
        ModularBake.drawing(true);
        Optional<DishBake.Dish> baked;
        try {
            baked = DishBake.of(d, t, mats);
        } finally {
            ModularBake.drawing(was);
        }
        if (baked.isEmpty())
            return List.of();
        DishBake.Dish dish = baked.get();
        List<BenchUses.Use> out = new ArrayList<>();
        // each effect a compound gives, as BuffEvents doses it: how long, after how long (fat and fibre slow the absorbing)
        float absorb = dish.fat() + dish.fibre();
        for (Map.Entry<String, Float> e : new java.util.TreeMap<>(dish.compounds()).entrySet()) {
            if (e.getValue() < ProcessTuning.FOOD_BUFF_MIN_STRENGTH || e.getKey().startsWith(FoodPerk.PREFIX))
                continue;
            Compound c = CompoundTable.get().find(e.getKey()).orElse(null);
            if (c == null || c.effect().isEmpty())
                continue;
            var effect = BuiltInRegistries.MOB_EFFECT.getOptional(c.effect().get()).orElse(null);
            if (effect == null)
                continue;
            float ticks = e.getValue() * c.ticksPerStrength() * (1f + absorb);
            float delay = absorb * ProcessTuning.FOOD_BUFF_DELAY_TICKS;
            MutableComponent name = effect.getDisplayName().copy();
            if (c.amplifier() > 0)
                name.append(" ").append(Component.translatable("potion.potency." + c.amplifier()));
            out.add(line("dish.buffs", ticks, delay >= 20f
                    ? Component.translatable("gui.feedback.blueprint_table.dish.buffs.late", name, time(ticks), time(delay)) : key("dish.buffs", name, time(ticks)),
                    List.of(tip("dish.buffs.tip"))));
        }
        float seconds = (ModularTuning.DISH_EAT_BASE_SECONDS + ModularTuning.DISH_EAT_SECONDS_PER_LITRE * dish.litres()) * (1f - ModularTuning.DISH_EAT_CUT_SCALE * dish.cut());
        out.add(line("dish.eat", seconds, key("dish.eat", n(seconds)), List.of()));
        out.add(line("dish.seal", dish.seal(), key("dish.seal", tip("dish.seal." + Math.max(0, Math.min(2, dish.seal())))), List.of(tip("dish.seal.tip"))));
        if (dish.overflow() > 0f)
            out.add(line("dish.spill", dish.overflow(), key("dish.spill", Math.round(dish.overflow())), List.of(tip("dish.spill.tip"))));
        return out;
    }
}
