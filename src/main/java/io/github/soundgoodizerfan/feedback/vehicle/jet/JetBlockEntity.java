// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.jet;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import io.github.soundgoodizerfan.feedback.vehicle.ForceLedger;
import io.github.soundgoodizerfan.feedback.vehicle.VehiclePart;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import io.github.soundgoodizerfan.feedback.vehicle.engine.FuelBlend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A jet (flight A3): a compressor draws air through the intake, the combustor burns fuel in it, the turbine takes the compressor's share
 * out of the exhaust and the nozzle throws the rest backward. Thrust is the momentum the airflow gains: {@code mdot * (exhaust speed - airspeed)},
 * with {@code mdot = rho * intake area * (airspeed + the compressor's own draw)}; so thin air and a fast craft both cost thrust, and the jet
 * tops out where the airspeed meets the exhaust speed. The built figures ({@code BlockFigures}, case {@code jet}) are the intake, stage count,
 * nozzle kind and what heat the liner, blades and afterburner stand.
 * <p>
 * It lights only when something else turns the core: a starter shaft at {@code JET_STARTER_RPM}, or ram air past {@code JET_WINDMILL_SPEED}
 * (a windmill start); once spooled past {@code JET_SELF_SUSTAIN} it holds itself, and below that with neither it flames out. Misuse only wears:
 * a flame hotter than the blades or liner stand, a compressor tip faster than its metal, a throttle slammed up on a spooled core (a stall: no thrust
 * for a while, and a little wear). Burns {@link FuelBlend}; air only (no thrust where there is no oxygen or underwater).
 * <p>
 * The hand switch (right-click) holds full throttle and a card cannot lower it; the card alone sets the level once the switch is off.
 * Cost: while idle (no throttle, no spool) the server tick returns at once; running, it reads a density and the spool; the physics step is one impulse with nothing allocated, and nothing runs at zero spool.
 */
public class JetBlockEntity extends RotationNode implements BlockEntitySubLevelActor, Analog, DataNode, VehiclePart,
        io.github.soundgoodizerfan.feedback.machine.Mendable {

    private final FuelBlend fuel = new FuelBlend(this::setChanged);
    private final FigureStore figures = new FigureStore();
    /** The engine's weight on the vehicle: the bench's mass figure, carried. */
    private final io.github.soundgoodizerfan.feedback.vehicle.CarriedMass carried = new io.github.soundgoodizerfan.feedback.vehicle.CarriedMass();
    private final Set<DataNodeRef> links = new HashSet<>();

    private boolean hand;
    private float card, reheatCard;
    private float spool, condition = 1f, owed;
    private int stallTicks;
    /** 0 off, 1 spooling, 2 running, 3 stalled, 4 flamed out, 5 no air; set by the server tick. */
    private int status;

    /** Written by the server tick, read by the physics thread. */
    private volatile float power, rhoKgM3, reheat;
    /** Written by the physics step, read by the server tick (a windmill start). */
    private volatile float airspeed;

    private final ForceTotal force = new ForceTotal();
    private final Vector3d centre = new Vector3d(), dir = new Vector3d(), lin = new Vector3d(), worldDir = new Vector3d(), pushed = new Vector3d();

    public JetBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.JET_ENTITY.get(), pos, state);
    }

    public FuelBlend getFuel() {
        return fuel;
    }

    public boolean hand() {
        return hand;
    }

    public void setHand(boolean on) {
        hand = on;
        setChanged();
    }

    /** 0 off, 1 spooling, 2 running, 3 stalled, 4 flamed out, 5 no air. */
    public int status() {
        return status;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        carried.release(this);
    }

    public float spool() {
        return spool;
    }

    public float condition() {
        return condition;
    }

    /** Whether an afterburner is built in and lit. */
    public boolean reheating() {
        return reheat > 0f;
    }

    @Override
    public float mendableCondition() {
        return condition;
    }

    @Override
    public void mend(float by) {
        condition = Math.min(1f, condition + by);
        setChanged();
    }

    // --- the card ---------------------------------------------------------------------------------

    @Override
    public void setLevel(int channel, float level) {
        if (channel == 1)
            reheatCard = Math.clamp(level, 0f, 1f);
        else
            card = Math.clamp(level, 0f, 1f);
    }

    @Override
    public String[] channelKeys() {
        return new String[] {"feedback.analog.throttle", "feedback.analog.reheat"};
    }

    @Override
    public boolean acceptsLinks() {
        return false;
    }

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Nullable
    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- the server tick --------------------------------------------------------------------------

    private boolean hasAir(ServerLevel server, double y) {
        return AirDensityTable.oxygen(server) && AirDensityTable.at(server, y) >= VehicleTuning.THRUSTER_MIN_AIR_DENSITY
                && server.getFluidState(worldPosition).isEmpty();
    }

    public void tickServer() {
        if (!(level instanceof ServerLevel server))
            return;
        carried.tick(this, figures.mass());
        if (spool <= 0.001f && power <= 0f && !hand && card <= 0f) {
            status = 0;
            return;
        }
        Vec3 at = Vec3.atCenterOf(worldPosition);
        if (SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body)
            at = body.logicalPose().transformPosition(at);
        float flame = fuel.isEmpty() ? 0f : fuel.flame();
        boolean air = hasAir(server, at.y);
        rhoKgM3 = 2 * VehicleTuning.AIR_HALF_DENSITY_KPG_PER_BLOCK3 * 1000f * AirDensityTable.at(server, at.y);
        float throttle = Math.max(hand ? 1f : 0f, card);
        float target = flame > 0f && air ? throttle : 0f;
        boolean turned = Math.abs(getRpm().value()) >= VehicleTuning.JET_STARTER_RPM || airspeed >= VehicleTuning.JET_WINDMILL_SPEED;
        boolean lit = spool >= VehicleTuning.JET_SELF_SUSTAIN || turned;
        if (target > 0f && !lit)
            status = 4;
        else if (stallTicks > 0) {
            stallTicks--;
            target = 0f;
            status = 3;
        } else if (target - spool > VehicleTuning.JET_STALL_GAP && spool > 0.3f) {
            stallTicks = VehicleTuning.JET_STALL_TICKS;
            condition = Math.max(VehicleTuning.JET_CONDITION_FLOOR, condition - VehicleTuning.JET_STALL_WEAR);
            target = 0f;
            status = 3;
            setChanged();
        } else
            status = !air && throttle > 0f ? 5 : spool <= 0.001f ? 0 : target > spool ? 1 : 2;
        if (target > 0f && !lit)
            target = 0f;
        if (spool <= 0.001f && target <= 0f) {
            spool = 0f;
            power = 0f;
            reheat = 0f;
            return;
        }
        spool = Math.clamp(spool + Math.clamp(target - spool, -VehicleTuning.JET_SPOOL_DOWN, VehicleTuning.JET_SPOOL_UP), 0f, 1f);
        float hot = flame > 0f ? flame / FTuning.ENGINE_REFERENCE_FLAME_TU : 0f;
        float ab = figures.get("ReheatTu", 0f) > 0f && spool > 0.9f ? reheatCard : 0f;
        reheat = ab;
        power = flame > 0f && air ? spool * hot : 0f;
        if (power > 0f) {
            owed += VehicleTuning.JET_FUEL_MB_PER_TICK_M2 * figures.get("Intake", 0.28f) * spool * (1f + ab * VehicleTuning.JET_AB_FUEL) / hot;
            int drawn = (int) owed;
            owed -= drawn;
            if (drawn > 0)
                fuel.burn(drawn);
            wear(flame, ab);
            if (server.getGameTime() % 2 == 0) {
                Direction out = getBlockState().getValue(JetBlock.FACING).getOpposite();
                server.sendParticles(ab > 0f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, worldPosition.getX() + 0.5 + out.getStepX() * 0.8,
                        worldPosition.getY() + 0.5 + out.getStepY() * 0.8, worldPosition.getZ() + 0.5 + out.getStepZ() * 0.8,
                        2 + (int) (ab * 4), 0.1, 0.1, 0.1, 0.03);
            }
        }
    }

    /** Misuse only: a flame hotter than the blades or the liner stand, an afterburner past its own metal, a compressor tip over its metal's speed. */
    private void wear(float flame, float ab) {
        float over = 0f;
        float blades = figures.get("BladeTu", 0f), liner = figures.get("LinerTu", 0f), reheatTu = figures.get("ReheatTu", 0f);
        if (blades > 0f && flame > blades)
            over = Math.max(over, flame / blades - 1f);
        if (liner > 0f && flame > liner)
            over = Math.max(over, flame / liner - 1f);
        if (ab > 0f && reheatTu > 0f && flame * (1f + ab * VehicleTuning.JET_AB_BOOST) > reheatTu)
            over = Math.max(over, flame * (1f + ab * VehicleTuning.JET_AB_BOOST) / reheatTu - 1f);
        float limit = figures.get("TipLimit", 0f);
        if (limit > 0f) {
            float tip = VehicleTuning.JET_TIP_SPEED * spool * (1f + VehicleTuning.JET_STAGE_TIP * figures.get("Stages", 3f));
            if (tip > limit)
                over = Math.max(over, tip / limit - 1f);
        }
        if (over > 0f && condition > VehicleTuning.JET_CONDITION_FLOOR) {
            condition = Math.max(VehicleTuning.JET_CONDITION_FLOOR, condition - VehicleTuning.JET_WEAR_PER_TICK * over
                    * io.github.soundgoodizerfan.feedback.modular.attunement.MachineKinds.wearShare(this));
            setChanged();
        }
    }

    // --- the physics step -------------------------------------------------------------------------

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double dt) {
        Direction facing = getBlockState().getValue(JetBlock.FACING);
        dir.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        subLevel.logicalPose().transformNormal(dir, worldDir);
        handle.getLinearVelocity(lin);
        double v = Math.max(0, lin.dot(worldDir));
        airspeed = (float) v;
        float p = power, rho = rhoKgM3;
        if (p <= 0f || rho <= 0f)
            return;
        float s = spool, stages = figures.get("Stages", 3f), ab = reheat;
        double draw = VehicleTuning.JET_DRAW_SPEED * (1 + VehicleTuning.JET_STAGE_DRAW * stages) * s;
        double mdot = rho * figures.get("Intake", 0.28f) * (v + draw);
        // a divergent nozzle is poor at rest and better at speed; a converging one is the same throughout
        double nozzle = figures.get("Divergent", 0f) > 0f ? Math.min(1.3, VehicleTuning.JET_CD_REST + 0.45 * v / VehicleTuning.JET_CD_SPEED) : 1.0;
        double exhaust = VehicleTuning.JET_EXHAUST_SPEED * Math.sqrt(p / s) * (1 + VehicleTuning.JET_STAGE_EXHAUST * stages)
                * (1 + ab * VehicleTuning.JET_AB_BOOST) * nozzle * s;
        double thrustKpg = Math.max(0, mdot * (exhaust - v)) / 1000.0 * condition;
        if (thrustKpg <= 0)
            return;
        centre.set(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        pushed.set(dir).mul(thrustKpg * dt);
        force.applyImpulseAtPoint(subLevel.getMassTracker(), centre, pushed);
        ForceLedger.record(subLevel, ForceLedger.Cause.PROPULSION, centre.x, centre.y, centre.z, pushed.x, pushed.y, pushed.z);
        handle.applyForcesAndReset(force);
    }

    // --- save ---------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Hand", hand);
        tag.putFloat("Spool", spool);
        tag.putFloat("Condition", condition);
        CompoundTag fuelTag = new CompoundTag();
        fuel.save(fuelTag);
        tag.put("Fuel", fuelTag);
        figures.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hand = tag.getBoolean("Hand");
        spool = tag.getFloat("Spool");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
        if (tag.contains("Fuel"))
            fuel.load(tag.getCompound("Fuel"));
        figures.load(tag, registries);
    }
}
