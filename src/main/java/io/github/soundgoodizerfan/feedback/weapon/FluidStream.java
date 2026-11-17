// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.FumeCloud;
import io.github.soundgoodizerfan.feedback.core.hazard.HotIgnition;
import io.github.soundgoodizerfan.feedback.core.hazard.SpatialHop;
import io.github.soundgoodizerfan.feedback.core.hazard.Vapour;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.net.CasterStreamPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.potion.PotionFluids;
import io.github.soundgoodizerfan.feedback.potion.PotionSpray;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

/**
 * One tick of a fluid caster's stream: a swept ray from the muzzle, one block clip and one entity query, no entity and no fluid block
 * placed. What a hit does is read from the fluid's own figures and never from which fluid it is: its temperature scalds or chills, a
 * fluid that burns (a combustion with a flash point it is over) lights from an open flame in the stream's path and sets what it hits
 * alight, water (by tag) douses, a corrosive one bites armour, and mass flow times exit speed shoves a body. A container the stream
 * meets (a fluid handler, a cauldron, farmland) takes the fluid in; nothing else does. A fine mist (an atomiser) leaves a
 * {@link Vapour} source where it lands.
 *
 * <p>Stream-hit entity damage reads {@link CasterTuning}; every figure there is a placeholder.
 */
public final class FluidStream {

    private FluidStream() {
    }

    /**
     * What the stream did that the caster must answer for.
     *
     * @param ignited     the stream burned (a flame was in its path).
     * @param pilotLit    the nozzle's pilot burned this tick: it takes its bleed from the tank.
     * @param igniteBlocks distance from the muzzle of the first flame in the path, or a large number for none.
     */
    public record Result(boolean ignited, boolean pilotLit, float igniteBlocks, float length) {
    }

    /** The material of {@code fluid}, or null if it has none. */
    private static Material material(FluidStack fluid) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        return materials.get(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
    }

    /** Whether {@code fluid} burns as it flows: it states a combustion and is at or over its flash point. */
    public static boolean burnable(FluidStack fluid) {
        Material m = material(fluid);
        if (m == null || m.combustion().isEmpty())
            return false;
        return m.combustion().get().flashPoint().map(f -> FluidHeat.of(fluid).value() >= f.value()).orElse(true);
    }

    /**
     * Throws one tick of the stream.
     *
     * @param fluid    what flows, stamped with its temperature; the amount is ignored.
     * @param flowMb   mB leaving this tick.
     * @param speed    exit speed, blocks a tick.
     * @param reach    blocks it flies.
     * @param spreadDeg the cone's half-angle.
     * @param atomiser the nozzle makes a mist.
     * @param flameReach blocks from the muzzle the nozzle's pilot flame stands out, 0 for none: it burns on a fluid that burns, unless the muzzle is under water.
     */
    public static Result cast(ServerLevel level, LivingEntity shooter, float yaw, float pitch, FluidStack fluid, int flowMb, float speed,
                              float reach, float spreadDeg, boolean atomiser, float flameReach) {
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        Vec3 start = shooter.getEyePosition().add(dir.scale(0.4));
        Vec3 far = start.add(dir.scale(reach));
        BlockHitResult block = level.clip(new ClipContext(start, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        boolean blocked = block.getType() != HitResult.Type.MISS;
        Vec3 end = blocked ? block.getLocation() : far;
        float length = (float) start.distanceTo(end);

        Material m = material(fluid);
        float tu = FluidHeat.of(fluid).value();
        float density = m == null ? 1f : (float) m.density().muPerMb();
        boolean water = fluid.is(FluidTags.WATER);
        boolean burnable = burnable(fluid);
        // A potion is carried in water: it wets what it hits as water does (but is no plain water: no cauldron, no farmland).
        boolean potion = PotionFluids.is(fluid);
        boolean wet = water || potion && PotionSpray.wet(fluid);
        Figures f = Figures.of(m, tu, density, wet);

        // An open flame in the path (or the wielder alight) lights a fluid that burns; the nearest sets how far from the muzzle it is.
        float ignitionAt = Float.MAX_VALUE;
        // The pilot burns the tank's own fluid at the nozzle: out when the muzzle is under water, nothing to burn when the fluid does not.
        boolean pilot = burnable && flameReach > 0f && !level.getFluidState(BlockPos.containing(start)).is(FluidTags.WATER);
        if (pilot) {
            ignitionAt = Math.min(flameReach, length);
            if (level.getGameTime() % CasterTuning.PILOT_LIGHT_INTERVAL_TICKS == 0)
                lightTip(level, shooter, dir.scale(ignitionAt).add(start));
        }
        if (burnable) {
            if (shooter.isOnFire())
                ignitionAt = Math.min(ignitionAt, 0.4f);
            for (int d = 1; d <= (int) length + 1 && ignitionAt > d; d++) {
                BlockPos at = BlockPos.containing(start.add(dir.scale(Math.min(d, length))));
                if (HotIgnition.isSource(level.getBlockState(at)) || level.getFluidState(at).is(FluidTags.LAVA)) {
                    ignitionAt = d;
                    break;
                }
            }
        }

        // Bodies in the cone.
        double tan = Math.tan(Math.toRadians(spreadDeg));
        AABB box = new AABB(start, end).inflate(length * tan + 1.0);
        List<LivingEntity> inCone = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != shooter && e.isAlive() && !e.isSpectator()
                && inside(start, dir, length, tan, e));
        boolean lit = ignitionAt < Float.MAX_VALUE;
        if (burnable && !lit)
            for (LivingEntity e : inCone)
                if (e.isOnFire()) {
                    lit = true;
                    break;
                }
        float share = inCone.isEmpty() ? 0f : (float) flowMb / inCone.size();
        for (LivingEntity target : inCone) {
            hit(level, shooter, target, dir, share, speed, f, burnable && lit);
            if (potion)
                PotionSpray.body(level, shooter, target, fluid, Math.max(1, Math.round(share)));
        }
        if (f.spatial() && !inCone.isEmpty() && level.getGameTime() % CasterTuning.SPATIAL_INTERVAL_TICKS == 0)
            SpatialHop.hop(level, inCone.get(0).position(), flowMb);

        // Where it lands.
        if (blocked) {
            BlockPos at = block.getBlockPos();
            Direction face = block.getDirection();
            landed(level, shooter, fluid, flowMb, tu, water, wet, (burnable && lit) || tu >= CasterTuning.HOT_IGNITE_TU, at, face);
            if (f.gas())
                cloud(level, fluid, f, at.relative(face));
            if (potion) {
                Vec3 spot = Vec3.atCenterOf(at.relative(face));
                PotionSpray.landing(level, shooter, spot, fluid, flowMb);
                if (atomiser)
                    PotionSpray.mist(level, spot, fluid, flowMb);
            } else if (atomiser) {
                BlockPos mist = at.relative(face);
                Vapour.source(level, mist, Vapour.potency(fluid), CasterTuning.MIST_LINGER_TICKS, Vapour.burnTicks(fluid), false);
            }
        } else if (f.gas()) {
            cloud(level, fluid, f, BlockPos.containing(end));
        } else if (atomiser) {
            // A mist that hits nothing hangs where it runs out.
            if (potion)
                PotionSpray.mist(level, end, fluid, flowMb);
            else
                Vapour.source(level, BlockPos.containing(end), Vapour.potency(fluid), CasterTuning.MIST_LINGER_TICKS, Vapour.burnTicks(fluid), false);
        }

        int rgb = potion ? PotionSpray.colour(fluid) : m != null && m.color().isPresent() ? m.color().get() : water ? 0x3F76E4 : 0xC8D8FF;
        CasterStreamPayload.send(level, start, dir, length, spreadDeg, rgb, burnable && lit ? CasterStreamPayload.BURNING
                : atomiser || f.gas() ? CasterStreamPayload.MIST : CasterStreamPayload.LIQUID, flowMb, pilot ? flameReach : 0f);
        return new Result(burnable && lit, pilot, ignitionAt, length);
    }

    /**
     * The pilot's flame lights what it touches: a campfire or candle (an unlit block with the lit property, not waterlogged), a fire where
     * a flammable block is beside the tip, and a flammable block the tip is in (it catches, as tnt does). By tag and property, never by block.
     */
    private static void lightTip(ServerLevel level, LivingEntity shooter, Vec3 tip) {
        BlockPos at = BlockPos.containing(tip);
        BlockState state = level.getBlockState(at);
        if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)
                && (state.is(BlockTags.CAMPFIRES) || state.is(BlockTags.CANDLES) || state.is(BlockTags.CANDLE_CAKES))
                && !(state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))) {
            level.setBlockAndUpdate(at, state.setValue(BlockStateProperties.LIT, true));
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK))
            return;
        if (state.isAir()) {
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (level.getBlockState(next).isFlammable(level, next, d.getOpposite()) && BaseFireBlock.canBePlacedAt(level, at, Direction.UP)) {
                    level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                    return;
                }
            }
        } else if (state.isFlammable(level, at, Direction.UP)) {
            state.onCaughtFire(level, at, Direction.UP, shooter);
        }
    }

    /** Whether {@code e} is inside the stream's cone: along the ray, and no farther from its line than the cone and its own width allow. */
    private static boolean inside(Vec3 start, Vec3 dir, float length, double tan, LivingEntity e) {
        Vec3 to = e.getBoundingBox().getCenter().subtract(start);
        double t = to.dot(dir);
        if (t <= 0 || t > length + e.getBbWidth())
            return false;
        double off = to.subtract(dir.scale(t)).length();
        return off <= t * tan + e.getBbWidth() / 2 + 0.2;
    }

    /**
     * What one fluid is, as the stream reads it: every field is a figure of its material (or of its temperature stamp), never its id.
     *
     * @param gas        hotter than its boiling point: it does not stream, it disperses and hangs.
     * @param burnSeconds how long a burning fuel keeps a body alight: its burn time, held to a range.
     * @param flameTu    the temperature of its flame, 0 if it has none.
     * @param oxygen     its combustion's oxygen fraction: above 0 it feeds a fire.
     * @param viscosity  how thick it is; a thick one clings.
     * @param toxicity   harm index (log of the lethal dose), the poison a splash gives.
     * @param corrosivity pH distance from neutral: how hard it burns flesh and eats armour.
     * @param radiation  radioactivity (Ru).
     * @param fume       strength of the poison cloud a gas of it leaves, 0 for none.
     */
    private record Figures(float tu, float density, boolean water, boolean gas, boolean fuel, float burnSeconds, float flameTu, float oxygen,
                           float viscosity, float toxicity, float corrosivity, float radiation, boolean spatial, int fume) {

        static Figures of(Material m, float tu, float density, boolean water) {
            if (m == null)
                return new Figures(tu, density, water, false, false, CasterTuning.IGNITE_SECONDS, 0f, 0f, 0f, 0f, 0f, 0f, false, 0);
            Optional<Material.Combustion> c = m.combustion();
            boolean gas = m.thermal().boilingPoint().map(b -> tu > b.value()).orElse(false);
            float burn = c.flatMap(Material.Combustion::burnTicks)
                    .map(t -> Math.max(CasterTuning.BURN_SECONDS_MIN, Math.min(CasterTuning.BURN_SECONDS_MAX, t / 20f))).orElse(CasterTuning.IGNITE_SECONDS);
            float flame = c.flatMap(Material.Combustion::flameTemperature).map(t -> t.value()).orElse(0f);
            float oxygen = c.map(Material.Combustion::oxygenFraction).orElse(0f);
            float tox = m.toxicity().orElse(0f);
            int fume = m.perks().getOrDefault(Material.Perk.TOXIC, 0);
            if (fume == 0 && tox >= CasterTuning.GAS_CLOUD_MIN_TOXICITY)
                fume = Math.min(CasterTuning.GAS_CLOUD_MAX, Math.round(tox));
            return new Figures(tu, density, water, gas, c.isPresent(), burn, flame, oxygen, m.molten().viscosity().orElse(0f), tox,
                    m.corrosivity().orElse(0f), m.radioactivity().map(r -> r.value()).orElse(0f), m.traits().contains(Material.Trait.SPATIAL), fume);
        }
    }

    /** A gas that runs out leaves what it is where it hangs: a poison cloud if it is toxic, a vapour that a flame sets off if it burns. */
    private static void cloud(ServerLevel level, FluidStack fluid, Figures f, BlockPos at) {
        if (f.fume() > 0)
            FumeCloud.release(level, Vec3.atCenterOf(at), f.fume());
        if (f.fuel())
            Vapour.source(level, at, 1f, CasterTuning.MIST_LINGER_TICKS, Vapour.burnTicks(fluid), false);
    }

    /** What a stream does to one body: each effect is a figure of the fluid. */
    private static void hit(ServerLevel level, LivingEntity shooter, LivingEntity target, Vec3 dir, float share, float speed, Figures f,
                            boolean burning) {
        float tu = f.tu();
        if (f.water()) {
            target.clearFire();
            if (target.isSensitiveToWater())
                target.hurt(level.damageSources().drown(), Math.max(CasterTuning.WATER_HURT_MIN, Math.min(CasterTuning.WATER_HURT_MAX, share * CasterTuning.WATER_HURT_PER_MB)));
        }
        // Heat and cold: scald, light cloth and hair, freeze flesh; the colder, the faster frost builds.
        if (tu > CasterTuning.SCALD_TU)
            target.hurt(Hazards.source(level, Hazards.SCALD, shooter), Math.min(CasterTuning.SCALD_MAX, (tu - CasterTuning.SCALD_TU) * share * CasterTuning.SCALD_PER_TU_MB));
        else if (tu <= CasterTuning.FREEZE_TU) {
            float depth = Math.min(CasterTuning.FREEZE_DEPTH_MAX, 1f + -tu / CasterTuning.FREEZE_DEPTH_TU);
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + Math.round(share * CasterTuning.FREEZE_TICKS_PER_MB * depth)));
            if (tu < CasterTuning.FROSTBITE_TU)
                target.hurt(level.damageSources().freeze(), Math.min(CasterTuning.FROSTBITE_MAX, (CasterTuning.FROSTBITE_TU - tu) * share * CasterTuning.FROSTBITE_PER_TU_MB));
        }
        if (tu >= CasterTuning.HOT_IGNITE_TU)
            target.igniteForSeconds(CasterTuning.HOT_IGNITE_SECONDS);
        // A burning fuel: as long as it burns, and worse the hotter its flame.
        if (burning) {
            target.igniteForSeconds(f.burnSeconds());
            if (f.flameTu() > CasterTuning.FLAME_BASE_TU)
                target.hurt(Hazards.source(level, Hazards.HOT_CONTACT, shooter),
                        Math.min(CasterTuning.FLAME_MAX, (f.flameTu() - CasterTuning.FLAME_BASE_TU) * share * CasterTuning.FLAME_PER_TU_MB));
        }
        // An oxidiser on a body already alight feeds the fire.
        if (f.oxygen() > 0f && target.isOnFire()) {
            target.hurt(Hazards.source(level, Hazards.HOT_CONTACT, shooter), Math.min(CasterTuning.FLARE_MAX, f.oxygen() * share * CasterTuning.FLARE_PER_MB));
            target.setRemainingFireTicks(Math.min(CasterTuning.FLARE_FIRE_MAX_TICKS, target.getRemainingFireTicks() + CasterTuning.FLARE_FIRE_TICKS));
        }
        // A thick fluid coats and drags.
        if (f.viscosity() >= CasterTuning.CLING_VISCOSITY) {
            int level10 = Math.max(0, Math.min(3, (int) Math.floor(Math.log10(f.viscosity() / CasterTuning.CLING_VISCOSITY))));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CasterTuning.CLING_TICKS, level10));
        }
        // An acid or a base burns flesh by how far its pH is from neutral, and eats armour the same way.
        if (f.corrosivity() > 0f) {
            target.hurt(Hazards.source(level, Hazards.CHEMICAL_BURN, shooter), Math.min(CasterTuning.CHEM_MAX, f.corrosivity() * share * CasterTuning.CHEM_PER_PH_MB));
            if (level.random.nextFloat() < share * CasterTuning.ARMOUR_BITE_PER_MB * f.corrosivity() / CasterTuning.CORROSIVITY_REF)
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                    if (!target.getItemBySlot(slot).isEmpty() && target.getItemBySlot(slot).isDamageableItem())
                        target.getItemBySlot(slot).hurtAndBreak(1, level, target, item -> {
                        });
        }
        // A toxic splash poisons (never kills: poison stops at one heart).
        if (f.toxicity() >= CasterTuning.POISON_MIN_TOXICITY)
            target.addEffect(new MobEffectInstance(MobEffects.POISON,
                    Math.min(CasterTuning.POISON_MAX_TICKS, Math.round(f.toxicity() * share * CasterTuning.POISON_TICKS_PER_TOX_MB)),
                    f.toxicity() >= CasterTuning.POISON_STRONG_TOXICITY ? 1 : 0));
        // A radioactive one withers and outlines, as the radiant perk does.
        if (f.radiation() > 0f) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    Math.min(CasterTuning.RADIATION_MAX_TICKS, Math.round(f.radiation() * share * CasterTuning.RADIATION_TICKS_PER_RU_MB))));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, CasterTuning.RADIATION_GLOW_TICKS));
        }
        if (f.gas())
            return;
        // Mass flow times exit speed, a fire hose's shove; past a dynamic pressure a jet cuts, as a water cutter does.
        float push = Math.min(CasterTuning.SHOVE_MAX, share * f.density() * speed * CasterTuning.SHOVE_PER_MOMENTUM);
        if (push > 0.001f) {
            target.push(dir.x * push, dir.y * push * 0.5, dir.z * push);
            target.hurtMarked = true;
        }
        float q = f.density() * speed * speed;
        if (q > CasterTuning.CUT_MIN_Q)
            target.hurt(shooter instanceof net.minecraft.world.entity.player.Player p ? level.damageSources().playerAttack(p) : level.damageSources().mobAttack(shooter),
                    Math.min(CasterTuning.CUT_MAX, (q - CasterTuning.CUT_MIN_Q) * CasterTuning.CUT_PER_Q));
    }

    /** What the stream does where it stops on a block: a container takes it in, water douses and moistens, a flame lights a fire. */
    private static void landed(ServerLevel level, LivingEntity shooter, FluidStack fluid, int flowMb, float tu, boolean water, boolean wet, boolean burning,
                               BlockPos at, Direction face) {
        BlockState state = level.getBlockState(at);
        // A container takes the fluid in: the only thing that does.
        var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, at, face);
        if (handler != null) {
            FluidStack in = FluidHeat.stamp(fluid.copyWithAmount(flowMb), tu);
            handler.fill(in, IFluidHandler.FluidAction.EXECUTE);
        } else if (water && state.is(BlockTags.CAULDRONS)) {
            // A cauldron fills a level to each cauldron's worth of water.
            if (level.random.nextFloat() < flowMb / CasterTuning.CAULDRON_MB_PER_LEVEL) {
                if (state.getBlock() instanceof LayeredCauldronBlock && state.hasProperty(LayeredCauldronBlock.LEVEL)) {
                    int lvl = state.getValue(LayeredCauldronBlock.LEVEL);
                    if (lvl < 3 && state.is(Blocks.WATER_CAULDRON))
                        level.setBlockAndUpdate(at, state.setValue(LayeredCauldronBlock.LEVEL, lvl + 1));
                } else if (state.is(Blocks.CAULDRON))
                    level.setBlockAndUpdate(at, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1));
            }
        }
        if (water) {
            // Wet ground: farmland (or the farmland a crop stands on) is watered.
            BlockPos farm = state.getBlock() instanceof FarmBlock ? at : level.getBlockState(at.below()).getBlock() instanceof FarmBlock ? at.below() : null;
            if (farm != null && level.getBlockState(farm).hasProperty(FarmBlock.MOISTURE))
                level.setBlockAndUpdate(farm, level.getBlockState(farm).setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
        }
        if (wet) {
            // Anything wet puts out a lit fire box or a fire.
            if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT))
                level.setBlockAndUpdate(at, state.setValue(BlockStateProperties.LIT, false));
            BlockPos beside = at.relative(face);
            if (level.getBlockState(beside).is(BlockTags.FIRE))
                level.removeBlock(beside, false);
        }
        if (burning && level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            BlockPos fire = at.relative(face);
            if (BaseFireBlock.canBePlacedAt(level, fire, face))
                level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
        }
    }

    /**
     * The whole tank let out at the carrier's feet, as a stream that landed there: a container below takes it, a cauldron fills, farmland is
     * watered. Nothing places a fluid block.
     */
    public static void pour(ServerLevel level, LivingEntity holder, FluidStack fluid) {
        if (fluid.isEmpty())
            return;
        landed(level, holder, fluid, fluid.getAmount(), FluidHeat.of(fluid).value(), fluid.is(FluidTags.WATER), fluid.is(FluidTags.WATER), false,
                holder.blockPosition().below(), Direction.UP);
    }

    /** What a fluid is, for a tooltip or a check: its material, or empty. */
    public static Optional<Material> materialOf(FluidStack fluid) {
        return Optional.ofNullable(material(fluid));
    }
}
