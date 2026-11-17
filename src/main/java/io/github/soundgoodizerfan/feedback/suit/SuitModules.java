// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The suit's upgrade modules, one per piece of armour, each an {@code upgrade} part held whole. An attachment is either a part of one
 * mechanism class (ids {@link #OPTIC} and up: a lens, spring, actuator, tread, filter), tuned by an axis whose share of range is
 * {@link #arg} and read off its material, or one of the named shapes below (the {@code module} param of its variant) not yet converted.
 * A module works only while the suit is powered and not cut out, except the plain physics of the plate and the springs, treads and
 * filter beds ({@link #passive}). Each reads {@code mod[id]}, its power (0.25 to 1.5), or 0 when not fitted; the array is null when none is.
 *
 * <p>Attribute modules are transient modifiers set when the fit or power changes; per-tick ones run in {@link #tick}; damage and landing
 * ones are events here. Whatever they cost is Eu added to the battery's pending draw.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SuitModules {

    private SuitModules() {
    }

    // mechanism classes: a part of one kind of mechanism, tuned by its axis, whatever the figures make of it
    public static final int OPTIC = 1, SPRING = 2, ACTUATOR = 3, TREAD = 4, FILTER = 5, THERMAL = 6, THRUSTER = 7, PLATE = 8, COIL = 9, DISPENSER = 10;
    /** The name of each class, which is also its lang key suffix; index 0 is none. */
    private static final String[] KEYS = {"", "optic", "spring", "actuator", "tread", "filter", "thermal", "thruster", "plate", "coil", "dispenser"};

    /** The name key of upgrade {@code id} (translate {@code modular.feedback.variant.<key>}), or empty for none. */
    public static String key(int id) {
        return id > 0 && id < KEYS.length ? KEYS[id] : "";
    }

    /** Ids are 1-based; a module array holds each id's power at {@code [id]} and its tuned-axis share at {@code [COUNT + id]}. */
    public static final int COUNT = 11;

    /** A module array's length: powers, then tuned-axis shares. */
    static final int SLOTS = 2 * COUNT + 3;

    /** Where the held circuit's figures sit in a module array, after the powers and shares: its gain, its noise and its heater's draw (Eu a tick). All 0 with no live circuit. */
    private static final int GAIN = 2 * COUNT, NOISE = 2 * COUNT + 1, HEATER = 2 * COUNT + 2;

    /** Writes the held circuit's figures into module array {@code m}. */
    static void circuit(float[] m, io.github.soundgoodizerfan.feedback.process.Circuit c) {
        if (m == null || !c.live())
            return;
        m[GAIN] = Math.max(0f, c.gain());
        m[NOISE] = Math.min(1f, Math.max(0f, c.noise()));
        m[HEATER] = c.heaterPower();
    }

    /** The multiplier on a reach (a scan's, the lens's) from the held circuit's gain: 1 with none, up to {@code 1 + CIRCUIT_REACH}. */
    static float reachGain(float[] m) {
        float g = m == null ? 0f : m[GAIN];
        return 1f + SuitTuning.CIRCUIT_REACH * g / (g + 1f);
    }

    /** The held circuit's noise, 0 to 1 (a scan module always holds one: the optic's scan needs a controller). */
    static float noise(float[] m) {
        return m == null ? 1f : m[NOISE];
    }

    /** Eu a tick the held tubed circuit's heater draws from the pack while the suit runs. */
    static float heater(float[] m) {
        return m == null ? 0f : m[HEATER];
    }

    /** The tuned-axis share (0 to 1) of module {@code id}: where its part's axis sits in its range, 0 for a module with none. */
    static float arg(float[] m, int id) {
        return m == null ? 0f : m[COUNT + id];
    }

    /**
     * The axis a mechanism part of class {@code id} is tuned by (the bake reads its share of range into the module's {@code arg}), or empty
     * for a class with none. A class is the kind of mechanism (a lens, a spring), not a named upgrade: what it does follows from the figures.
     */
    public static String argAxis(int id) {
        return switch (id) {
            case OPTIC -> "focal_length";
            case SPRING, ACTUATOR -> "stroke";
            case TREAD -> "span";
            case FILTER -> "bed";
            case THERMAL -> "thickness";
            case THRUSTER -> "bore";
            case PLATE -> "thickness";
            case COIL -> "core";
            case DISPENSER -> "dose";
            default -> "";
        };
    }

    /** Bits of the wheel segments module {@code id} gives when it holds the supports {@code sup} ({@code Fig.SUP_*}): what a part can do is what it gets a segment for. */
    public static int gives(int id, int sup) {
        if (id == ACTUATOR)
            return Ability.STRIDE.bit() | Ability.SHOCK.bit();
        if (id == OPTIC)
            return Ability.LENS.bit() | ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_CONTROLLER) != 0 ? Ability.SCAN.bit() : 0);
        // a nozzle lifts by holding the jet key and fires a single lump of fuel from the wheel
        if (id == THRUSTER)
            return Ability.JET.bit() | Ability.BURST.bit();
        // a pump on a held motor can vent the suit's heat; a held circuit lets the plate follow the ambient, a thermal mask
        if (id == THERMAL)
            return ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_MOTOR) != 0 ? Ability.VENT.bit() : 0)
                    | ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_CONTROLLER) != 0 ? Ability.CLOAK.bit() : 0);
        // an electromagnet pulls magnetic things in while switched on; a capacitor held beside the coil lets it dump its field as a pulse
        if (id == COIL)
            return Ability.MAGNET.bit() | ((sup & io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig.SUP_CELL) != 0 ? Ability.EMP.bit() : 0);
        // a metered pump that doses what the wearer carries
        return id == DISPENSER ? Ability.HEAL.bit() : 0;
    }

    /** The switch segment that must be on for module {@code id} to work, 0 for one that is passive or only pulses. */
    static int gates(int id) {
        if (id == OPTIC)
            return Ability.LENS.bit();
        if (id == ACTUATOR)
            return Ability.STRIDE.bit();
        return id == COIL ? Ability.MAGNET.bit() : 0;
    }

    /** The module that sets how strongly {@code a} works (the first that gives it), or 0 for a built-in segment. */
    static int moduleOf(Ability a) {
        for (int id = 1; id < COUNT; id++)
            if ((gives(id, 7) & a.bit()) != 0)
                return id;
        return 0;
    }

    /** How strongly {@code a} works in {@code m}, 0 when no fitted module gives it. */
    static float power(float[] m, Ability a) {
        int id = moduleOf(a);
        if (id == 0 || m == null)
            return 0f;
        return switch (a) {
            // a scan's flash reaches as far as the lens is long-focus
            case SCAN -> m[id] * (SCAN_SHORT + SCAN_LONG * arg(m, id)) * reachGain(m);
            // thin fins vent best
            case VENT -> m[id] * (1f - 0.5f * arg(m, id));
            // a long core spreads the flux
            case EMP -> m[id] * (1f - 0.5f * arg(m, id));
            default -> m[id];
        };
    }

    /** {@code m} with every module that needs the suit's power zeroed: the springs and plates that are mechanical work unpowered. Null when none is left. */
    static float[] passive(float[] m) {
        if (m == null)
            return null;
        float[] live = null;
        for (int id : new int[] {SPRING, TREAD, FILTER, THERMAL, PLATE}) {
            if (m[id] <= 0f)
                continue;
            if (live == null)
                live = new float[SLOTS];
            live[id] = m[id];
            live[COUNT + id] = m[COUNT + id];
        }
        return live;
    }

    // --- placeholder figures, tuned in play (the suit's own file is SuitTuning; these are per-module and live beside their code) ---
    private static final float OXYGEN = 3f, KB_PLATING = 0.6f,
            STRIDE = 0.12f, STEP = 1f, WATER = 0.6f, FALL_CUT = 0.5f, EFFICIENCY = 0.7f, JUMP = 0.12f,
            SAFE_FALL = 5f, LOAD = 0.5f;
    private static final float BLAST_CUT = 0.4f, FIRE_CUT = 0.5f, LINING_MIN = 0.3f, SHOT_CUT = 0.3f, SINK_GAIN = 3f, LENS_GAIN = 0.5f;
    /** Throat width against its widest, at the narrowest bore and at the middle one. */
    private static final float THROAT_MIN = 0.4f, THROAT_MID = 0.7f;
    private static final float STRIDE_AMPS = 0.3f, MAGNET_AMPS = 0.2f, FILTER_COST = 15f, LINING_COST = 5f, STOMP_COST = 50f, STOMP_PER_BLOCK = 0.5f, STOMP_MAX = 12f, MAGNET_BASE = 3f, MAGNET_PER = 3f, MAGNET_SPEED = 0.25f;
    /** A lens: the reach (blocks) a short-focus lens adds; the share of thermal range a long-focus lens adds; the share of its light a glass must turn back to keep glare out; a scan flash's reach at the short and long end of the focal length. */
    private static final float LENS_REACH_BLOCK = 1.5f, LENS_REACH_ENTITY = 1f, GLARE_MIN = 0.35f, FILTER_MIN = 0.3f, SCAN_SHORT = 0.6f, SCAN_LONG = 0.8f;

    private static ResourceLocation id(String name) {
        return Feedback.id("suit_mod_" + name);
    }

    private static final ResourceLocation OXY = id("oxygen"), BR = id("block_reach"), ER = id("entity_reach"), KB = id("knockback"),
            MS = id("speed"), SH = id("step"), WE = id("water"), FM = id("fall_mult"), ME = id("efficiency"), JS = id("jump"), SF = id("safe_fall");

    private static float p(float[] m, int id) {
        return m == null ? 0f : m[id];
    }

    // --- what a class buys, as numbers: p is the part's power, a the tuned axis's share of its range. The module's own code and the Blueprint Table's readout both read these. ---

    /** Blocks of reach a short-focus lens adds. */
    public static float reach(float p, float a) {
        return LENS_REACH_BLOCK * p * (1f - a);
    }

    /** Multiplier on thermal sight's range from a long-focus lens. */
    public static float sight(float p, float a) {
        return 1f + LENS_GAIN * p * a;
    }

    /** Share of fall damage a long-travel spring takes off. */
    public static float fallCut(float p, float a) {
        return FALL_CUT * p * a;
    }

    /** Jump strength a stiff short spring adds. */
    public static float jump(float p, float a) {
        return JUMP * p * (1f - a);
    }

    /** Stomp damage a spring's landing gives back, per block fallen (before the cap). */
    public static float stomp(float p, float a) {
        return STOMP_PER_BLOCK * p * a;
    }

    /** Share of walking speed a short-stroke actuator adds. */
    public static float stride(float p, float a) {
        return STRIDE * p * (1f - a);
    }

    /** Blocks of step height a long-stroke actuator adds. */
    public static float step(float p, float a) {
        return STEP * p * a;
    }

    /** Water movement a wide tread adds. */
    public static float waterSpeed(float p, float a) {
        return WATER * p * a;
    }

    /** Ground movement efficiency a narrow tread adds. */
    public static float groundGrip(float p, float a) {
        return EFFICIENCY * p * (1f - a);
    }

    /** Breath a thin filter bed adds (the oxygen bonus). */
    public static float breath(float p, float a) {
        return OXYGEN * p * (1f - a);
    }

    /** Whether a bed deep enough scrubs poison, nausea and hunger. */
    public static boolean scrubs(float p, float a) {
        return p * a >= FILTER_MIN;
    }

    /** Whether a lens that turns this much light back keeps blindness and darkness off. */
    public static boolean keepsGlare(float p) {
        return 1f - p >= GLARE_MIN;
    }

    /** Share of fire damage a thick heat plate takes off. */
    public static float fireCut(float p, float a) {
        return FIRE_CUT * p * a;
    }

    /** Multiplier on the rate the suit sheds heat from a thin plate. */
    public static float cooling(float p, float a) {
        return 1f + SINK_GAIN * p * (1f - a);
    }

    /** Whether a thick enough plate keeps frost off the skin. */
    public static boolean lines(float p, float a) {
        return p * a >= LINING_MIN;
    }

    /** Share of blast damage a thick plate takes off. */
    public static float blastCut(float p, float a) {
        return BLAST_CUT * p * a;
    }

    /** Share of a projectile's damage a thin plate turns. */
    public static float shotCut(float p, float a) {
        return SHOT_CUT * p * (1f - a);
    }

    /** Knockback resistance a thick plate gives. */
    public static float knockResist(float p, float a) {
        return Math.min(1f, KB_PLATING * p * a);
    }

    /** Blocks an electromagnet pulls from. */
    public static float magnetRadius(float p, float a) {
        return MAGNET_BASE + MAGNET_PER * p * a;
    }

    /** A nozzle's push and fuel burn against the mid-range throat, for a tuned share {@code a}. */
    public static float throatOf(float a) {
        float s = THROAT_MIN + (1f - THROAT_MIN) * a;
        return s * s / (THROAT_MID * THROAT_MID);
    }

    /** Eu a tick an attachment of class {@code id} draws while it works (an actuator striding, a coil pulling); 0 for a passive one. */
    public static float draw(int id, float p) {
        return id == ACTUATOR ? STRIDE_AMPS * p : id == COIL ? MAGNET_AMPS * 2f : 0f;
    }

    /** Eu one use of a class costs: a clearing or lining the suit pays for each time it works; 0 for a class with none. */
    public static float perUse(int id) {
        return id == OPTIC || id == FILTER ? FILTER_COST : id == THERMAL ? LINING_COST : 0f;
    }

    // --- physics of the plate and the bus (read whether or not the suit is powered) -----------------------

    /** Multiplier on the rate the suit sheds heat: a thin conductor spreads its heat to the air. */
    static float cooling(float[] m) {
        return cooling(p(m, THERMAL), arg(m, THERMAL));
    }

    /** Multiplier on thermal sight's reach. */
    static float lens(float[] m) {
        return sight(p(m, OPTIC), arg(m, OPTIC)) * (p(m, OPTIC) > 0f ? reachGain(m) : 1f);
    }

    /** Added to the pack's carry factor. */
    static float carry(float[] m) {
        // a thick plate down the leg is a frame: it takes the weight of a pack to the ground
        return LOAD * p(m, PLATE) * arg(m, PLATE);
    }

    /** A nozzle's push and the fuel it burns both go with its throat area, against the mid-range throat (as the thruster block's nozzle does). */
    static float throat(float[] m) {
        return throatOf(arg(m, THRUSTER));
    }

    // --- attributes ------------------------------------------------------------------------------------------

    /** Sets (or, for null, takes off) every attribute modifier the modules give. Called when the fit or the power changes. */
    static void attributes(ServerPlayer pl, float[] m) {
        // A filter bed trades breath against scrubbing: a thin bed passes the air easily and the breath lasts, a deep one cleans it.
        add(pl, Attributes.OXYGEN_BONUS, OXY, breath(p(m, FILTER), arg(m, FILTER)), AttributeModifier.Operation.ADD_VALUE);
        float near = p(m, OPTIC) * (1f - arg(m, OPTIC));
        add(pl, Attributes.BLOCK_INTERACTION_RANGE, BR, LENS_REACH_BLOCK * near, AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.ENTITY_INTERACTION_RANGE, ER, LENS_REACH_ENTITY * near, AttributeModifier.Operation.ADD_VALUE);
        // a thick plate has the mass to take a blow without moving
        add(pl, Attributes.KNOCKBACK_RESISTANCE, KB, knockResist(p(m, PLATE), arg(m, PLATE)), AttributeModifier.Operation.ADD_VALUE);
        // An actuator's stroke trades cadence against reach: a short one strides fast, a long one lifts the foot over a step.
        float act = p(m, ACTUATOR), reach = arg(m, ACTUATOR);
        add(pl, Attributes.MOVEMENT_SPEED, MS, stride(act, reach), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(pl, Attributes.STEP_HEIGHT, SH, step(act, reach), AttributeModifier.Operation.ADD_VALUE);
        // A tread's span trades grip against paddle: a narrow pad bites the ground, a wide one pushes water.
        float tread = p(m, TREAD), span = arg(m, TREAD);
        add(pl, Attributes.WATER_MOVEMENT_EFFICIENCY, WE, waterSpeed(tread, span), AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.MOVEMENT_EFFICIENCY, ME, groundGrip(tread, span), AttributeModifier.Operation.ADD_VALUE);
        // A spring's stroke trades kick against cushion: a stiff short one throws the jump, a soft long one takes the landing.
        float spring = p(m, SPRING), travel = arg(m, SPRING);
        add(pl, Attributes.FALL_DAMAGE_MULTIPLIER, FM, -fallCut(spring, travel), AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.JUMP_STRENGTH, JS, jump(spring, travel), AttributeModifier.Operation.ADD_VALUE);
        add(pl, Attributes.SAFE_FALL_DISTANCE, SF, SAFE_FALL * spring * travel, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void add(ServerPlayer pl, Holder<Attribute> attribute, ResourceLocation id, float value, AttributeModifier.Operation op) {
        SuitServer.mod(pl, attribute, id, value, op);
    }

    // --- per tick -----------------------------------------------------------------------------------------------

    /** Runs the modules that act each tick. Returns Eu to draw this tick. */
    static float tick(ServerPlayer pl, float[] m) {
        if (m == null)
            return 0f;
        float draw = 0f;
        int t = pl.tickCount;
        float stride = p(m, ACTUATOR);
        if (stride > 0f && pl.onGround() && !pl.isShiftKeyDown()) {
            Vec3 v = pl.getDeltaMovement();
            if (v.x * v.x + v.z * v.z > 0.0025)
                draw += STRIDE_AMPS * stride;
        }
        if (t % 10 == 0) {
            if (scrubs(p(m, FILTER), arg(m, FILTER)))
                draw += clear(pl, MobEffects.POISON, MobEffects.CONFUSION, MobEffects.HUNGER);
            // a glass that turns light back keeps glare out: the less it passes, the more it rejects
            if (p(m, OPTIC) > 0f && keepsGlare(p(m, OPTIC)))
                draw += clear(pl, MobEffects.BLINDNESS, MobEffects.DARKNESS);
            // a thick plate keeps the cold off the skin, while the suit warms it
            if (lines(p(m, THERMAL), arg(m, THERMAL)) && pl.getTicksFrozen() > 0) {
                pl.setTicksFrozen(0);
                draw += LINING_COST;
            }
        }
        float coil = p(m, COIL);
        if (coil > 0f && t % 2 == 0) {
            // an electromagnet: a long core reaches, a short one pulls hard; only what is magnetic is drawn
            double r = magnetRadius(coil, arg(m, COIL));
            double speed = MAGNET_SPEED * coil * (1.2f - arg(m, COIL));
            Vec3 to = pl.position().add(0, 0.5, 0);
            boolean any = false;
            for (ItemEntity e : pl.level().getEntitiesOfClass(ItemEntity.class, pl.getBoundingBox().inflate(r),
                    i -> !i.hasPickUpDelay() && io.github.soundgoodizerfan.feedback.machine.electromagnet.ElectromagnetBlockEntity.isMagnetic(i.getItem()))) {
                Vec3 d = to.subtract(e.position());
                if (d.lengthSqr() > 0.25) {
                    e.setDeltaMovement(d.normalize().scale(Math.min(0.6, speed)));
                    any = true;
                }
            }
            if (any)
                draw += MAGNET_AMPS * 2f;
        }
        return draw;
    }

    @SafeVarargs
    private static float clear(ServerPlayer pl, Holder<net.minecraft.world.effect.MobEffect>... effects) {
        float cost = 0f;
        for (Holder<net.minecraft.world.effect.MobEffect> e : effects)
            if (pl.hasEffect(e)) {
                pl.removeEffect(e);
                cost += FILTER_COST;
            }
        return cost;
    }

    // --- events ------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer pl))
            return;
        float[] m = SuitServer.mods(pl);
        if (m == null)
            return;
        var src = event.getSource();
        float k = 1f;
        // plate reads its material's hardness and toughness: a thin one turns a shot, a thick one soaks a blast
        if (src.is(DamageTypeTags.IS_EXPLOSION))
            k -= blastCut(m[PLATE], arg(m, PLATE));
        if (src.is(DamageTypeTags.IS_FIRE))
            k -= fireCut(m[THERMAL], arg(m, THERMAL));
        if (src.is(DamageTypeTags.IS_PROJECTILE))
            k -= shotCut(m[PLATE], arg(m, PLATE));
        if (k < 1f)
            event.setAmount(event.getAmount() * Math.max(0.1f, k));
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer pl))
            return;
        float[] m = SuitServer.mods(pl);
        // the energy a spring takes up on a hard landing is the energy it gives back to the ground around it
        float rebound = m == null ? 0f : m[SPRING] * arg(m, SPRING);
        if (rebound <= 0f || event.getDistance() < 3f)
            return;
        float dmg = Math.min(STOMP_MAX, STOMP_PER_BLOCK * rebound * event.getDistance());
        AABB box = pl.getBoundingBox().inflate(3, 1, 3);
        for (LivingEntity e : pl.level().getEntitiesOfClass(LivingEntity.class, box, x -> x != pl && x.isAlive()))
            e.hurt(pl.damageSources().playerAttack(pl), dmg);
    }
}
