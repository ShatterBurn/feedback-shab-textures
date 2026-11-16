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

import io.github.soundgoodizerfan.feedback.core.hazard.Corrosion;
import io.github.soundgoodizerfan.feedback.core.hazard.Hazards;
import io.github.soundgoodizerfan.feedback.core.hazard.HotIgnition;
import io.github.soundgoodizerfan.feedback.core.hazard.Vapour;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.net.CasterStreamPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
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
        boolean corrosive = Corrosion.corrosive(fluid);

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
        for (LivingEntity target : inCone)
            hit(level, shooter, target, dir, fluid, share, speed, density, tu, water, burnable && lit, corrosive);

        // Where it lands.
        if (blocked) {
            BlockPos at = block.getBlockPos();
            Direction face = block.getDirection();
            landed(level, shooter, fluid, flowMb, tu, water, burnable && lit, at, face);
            if (atomiser) {
                BlockPos mist = at.relative(face);
                Vapour.source(level, mist, Vapour.potency(fluid), CasterTuning.MIST_LINGER_TICKS, Vapour.burnTicks(fluid), false);
            }
        } else if (atomiser) {
            // A mist that hits nothing hangs where it runs out.
            Vapour.source(level, BlockPos.containing(end), Vapour.potency(fluid), CasterTuning.MIST_LINGER_TICKS, Vapour.burnTicks(fluid), false);
        }

        int rgb = m != null && m.color().isPresent() ? m.color().get() : water ? 0x3F76E4 : 0xC8D8FF;
        CasterStreamPayload.send(level, start, dir, length, spreadDeg, rgb, burnable && lit ? CasterStreamPayload.BURNING
                : atomiser ? CasterStreamPayload.MIST : CasterStreamPayload.LIQUID, flowMb, pilot ? flameReach : 0f);
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

    /** What a stream does to one body: each effect is a figure of the fluid. */
    private static void hit(ServerLevel level, LivingEntity shooter, LivingEntity target, Vec3 dir, FluidStack fluid, float share, float speed,
                            float density, float tu, boolean water, boolean burning, boolean corrosive) {
        if (water) {
            target.clearFire();
            if (target.isSensitiveToWater())
                target.hurt(level.damageSources().drown(), Math.max(CasterTuning.WATER_HURT_MIN, Math.min(CasterTuning.WATER_HURT_MAX, share * CasterTuning.WATER_HURT_PER_MB)));
        }
        if (tu > CasterTuning.SCALD_TU)
            target.hurt(Hazards.source(level, Hazards.SCALD, shooter), Math.min(CasterTuning.SCALD_MAX, (tu - CasterTuning.SCALD_TU) * share * CasterTuning.SCALD_PER_TU_MB));
        else if (tu <= CasterTuning.FREEZE_TU)
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + Math.round(share * CasterTuning.FREEZE_TICKS_PER_MB)));
        if (burning)
            target.igniteForSeconds(CasterTuning.IGNITE_SECONDS);
        if (corrosive && level.random.nextFloat() < share * CasterTuning.ARMOUR_BITE_PER_MB)
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
                if (!target.getItemBySlot(slot).isEmpty() && target.getItemBySlot(slot).isDamageableItem())
                    target.getItemBySlot(slot).hurtAndBreak(1, level, target, item -> {
                    });
        // Mass flow times exit speed, a fire hose's shove.
        float push = Math.min(CasterTuning.SHOVE_MAX, share * density * speed * CasterTuning.SHOVE_PER_MOMENTUM);
        if (push > 0.001f) {
            target.push(dir.x * push, dir.y * push * 0.5, dir.z * push);
            target.hurtMarked = true;
        }
    }

    /** What the stream does where it stops on a block: a container takes it in, water douses and moistens, a flame lights a fire. */
    private static void landed(ServerLevel level, LivingEntity shooter, FluidStack fluid, int flowMb, float tu, boolean water, boolean burning,
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
            // Wet ground: farmland (or the farmland a crop stands on) is watered; a lit fire box or a fire is put out.
            BlockPos farm = state.getBlock() instanceof FarmBlock ? at : level.getBlockState(at.below()).getBlock() instanceof FarmBlock ? at.below() : null;
            if (farm != null && level.getBlockState(farm).hasProperty(FarmBlock.MOISTURE))
                level.setBlockAndUpdate(farm, level.getBlockState(farm).setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
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

    /** What a fluid is, for a tooltip or a check: its material, or empty. */
    public static Optional<Material> materialOf(FluidStack fluid) {
        return Optional.ofNullable(material(fluid));
    }
}
