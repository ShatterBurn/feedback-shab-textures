// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.vehicle.VehicleTuning;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.BodyPush;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Set;

/**
 * A wheel on a body (vehicles v2 spec, <em>wheels and tires</em>): a body turned by a hinge that carries a
 * tire hub ({@link TireHubBlock}) with a tire fitted. The hub says the wheel's radius and its tire the grip, softness and
 * rolling resistance; the tire is a raycast with a spring and a damper, not a collider, with a ring of rim rays that keeps it out of
 * walls, so the wheel is round where a ring of voxels is not. Tire stock voxels are ordinary blocks that collide. No suspension is in
 * the hub: the wheel is where its mount puts it, and a slider joint is the suspension.
 *
 * <p>Prior art: Create Simulated's wheel mount (MIT), design only: a tire is data with a radius and size is a field, not a class. Rejected:
 * its round-wheel method (three vertical rays offset by the radius; the rim ring here is the round part), built-in suspension, preset
 * radii, redstone brake and steering, a flat force per RPM, linear lateral grip, and friction read from the road alone.
 *
 * <p>Taken from the frame drivetrain's hub (three ground rays under the axle, spring sized so the car's
 * weight is shared over its wheels, damper no stronger than the closing speed, grip cap, rolling
 * resistance, and the rim rays that keep a wheel out of walls), with the wheel's own spin read off the
 * body rather than a network: the contact's slip is the velocity of the point of the body that touches.
 */
public final class MicroWheels {

    private static final double REACH = 0.25;
    private static final int GUARD_RAYS = 12;
    private static final double GUARD_SKIP_COS = 0.85;

    private MicroWheels() {
    }

    /** What a scan of a body finds: the tread radius (blocks), grip (mean friction of the tire voxels), rolling resistance, and softness (1 for the softest tread; a softer tire wears faster). */
    public record Shape(double radius, double friction, double rolling, double softness) {
    }

    /**
     * The wheel in {@code body}: the hubs with a tire fitted, a block's or a fine-space voxel's; the radius is the largest hub's, the
     * grip, rolling resistance and softness the mean of the fitted tires'. Null if it has none. The hub sits on the axle (the tread is
     * measured from the bearing's anchor).
     */
    @Nullable
    public static Shape scan(ServerLevel level, ServerSubLevel body, Vector3d anchor, Vector3d axis) {
        double[] sum = new double[4]; // radius (largest), friction, rolling, softness
        int[] n = {0};
        var box = body.getPlot().getBoundingBox();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState at = level.getBlockState(p);
            if (at.getBlock() instanceof TireHubBlock hub)
                tally(sum, n, hub.radius(false), TireHubBlock.tire(at));
            else if (level.getBlockEntity(p) instanceof MicroCellBlockEntity cell)
                for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++) {
                    BlockState v = cell.get(i);
                    if (v != null && v.getBlock() instanceof TireHubBlock hub)
                        tally(sum, n, hub.radius(true), TireHubBlock.tire(v));
                }
        }
        return n[0] == 0 ? null : new Shape(sum[0], sum[1] / n[0], sum[2] / n[0], sum[3] / n[0]);
    }

    private static void tally(double[] sum, int[] n, double radius, @Nullable io.github.soundgoodizerfan.feedback.process.Material tire) {
        if (tire == null)
            return;
        double hardness = tire.hardness().value();
        double friction = tire.mechanical().friction().orElse(1f);
        sum[0] = Math.max(sum[0], radius);
        sum[1] += friction;
        sum[2] += deformation(hardness, friction);
        sum[3] += 1 / (1 + Math.max(0, hardness) / VehicleTuning.ROLLING_HARDNESS);
        n[0]++;
    }

    /**
     * One side's rolling resistance: {@code ROLLING_SOFT / (1 + hardness / ROLLING_HARDNESS)}, and next to
     * nothing on a slicker surface than {@code ROLLING_SLICK} (ice wastes none).
     */
    private static double deformation(double hardness, double friction) {
        double r = VehicleTuning.ROLLING_SOFT / (1 + Math.max(0, hardness) / VehicleTuning.ROLLING_HARDNESS);
        return friction < VehicleTuning.ROLLING_SLICK ? r * friction / VehicleTuning.ROLLING_SLICK : r;
    }

    /** What the ground gives under the tread: its own deformation, more if it falls (sand) or slows a walker (soul sand). */
    private static double groundRolling(BlockState ground) {
        double r = deformation(ground.getBlock().defaultDestroyTime(), PhysicsBlockPropertyHelper.getFriction(ground));
        if (ground.getBlock() instanceof net.minecraft.world.level.block.Fallable)
            r += VehicleTuning.ROLLING_LOOSE;
        float speed = ground.getBlock().getSpeedFactor();
        if (speed > 0 && speed < 1)
            r += VehicleTuning.ROLLING_SINK * (1 / speed - 1);
        return r;
    }

    /** Offroad's floor under grip, so a slick block still holds a little. */
    private static double fudge(double friction) {
        return friction < 1 ? 0.1 + 0.9 * friction : friction;
    }

    /**
     * One physics step of one wheel: the ground under the tread pushes the wheel body along the
     * ground's normal and grips it sideways and along. {@code centre} and {@code axis} are the hub and
     * the axle in the world; {@code carried} the mass this wheel bears; {@code mine} the bodies the rays
     * pass through (the vehicle's own). Returns the road's torque on the wheel about {@code axis}, in
     * kpg x block^2 / s^2, for the rotation network to feel. {@code tread} (1 new to the floor) scales the tread's grip;
     * {@code out} is cleared and given the load on the ground (0 airborne) and the speed the tread slides at when it is held
     * past its grip, which the caller wears the tire by (no allocation per step).
     */
    public static double step(ServerLevel level, ServerSubLevel wheel, Set<SubLevel> mine, Vector3d centre, Vector3d axis,
                              Shape shape, double carried, double brake, double tread, double[] out, double dt) {
        out[0] = out[1] = 0;
        Vector3d down = new Vector3d(0, -1, 0);
        down.fma(-down.dot(axis), axis);
        if (down.lengthSquared() < 0.01)
            return 0; // an axle stood on end has no tread under it
        down.normalize();
        double omegaN = 2 * Math.PI * FTuning.SUSPENSION_HZ[1];
        Vector3d along = axis.cross(down, new Vector3d());
        guard(level, wheel, mine, centre, axis, down, shape, carried, omegaN, dt);

        double reach = shape.radius() + REACH, h = Double.MAX_VALUE;
        BlockState ground = null;
        Vector3d normal = null;
        for (int k = -1; k <= 1; k++) {
            Vector3d from = new Vector3d(along).mul(k * 0.5 * shape.radius()).add(centre);
            Vec3 a = new Vec3(from.x, from.y, from.z);
            Vec3 b = a.add(down.x * reach, down.y * reach, down.z * reach);
            ClipContext ray = new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) ray).sable$setSubLevelIgnoring(mine::contains);
            BlockHitResult hit = level.clip(ray);
            if (hit.getType() == HitResult.Type.MISS)
                continue;
            Vec3 at = hit.getLocation();
            var face = hit.getDirection().getNormal();
            Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
            var other = SableCompanion.INSTANCE.getContaining(level, at);
            if (other != null) {
                at = other.logicalPose().transformPosition(at);
                other.logicalPose().transformNormal(n);
            }
            double d = (at.x - a.x) * down.x + (at.y - a.y) * down.y + (at.z - a.z) * down.z;
            if (d < h) {
                h = d;
                ground = level.getBlockState(BlockPos.containing(hit.getLocation()));
                normal = n;
            }
        }
        if (ground == null || h >= shape.radius())
            return 0; // airborne: no force
        double compression = shape.radius() - h;
        Vector3d lever = new Vector3d(down).mul(h);
        Vector3d contact = new Vector3d(centre).add(lever);
        double bearing = -normal.dot(down);
        if (bearing <= 0.05)
            return 0;

        Vector3d v = BodyPush.pointVelocity(wheel, contact);
        double share = BodyPush.effectiveMass(wheel, contact, normal);
        share = Math.min(share, carried);
        double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(normal) / dt;
        double load = (carried * omegaN * omegaN * compression - damping) * bearing;
        if (load <= 0)
            return 0;
        out[0] = load;
        double grip = shape.friction() * fudge(PhysicsBlockPropertyHelper.getFriction(ground)) * Math.max(VehicleTuning.TIRE_CONDITION_FLOOR, tread);

        // The tread's own speed at the contact is the body's velocity there: spin and travel together.
        Vector3d slip = new Vector3d(v);
        slip.fma(-slip.dot(normal), normal);
        Vector3d friction = new Vector3d(slip).mul(-share * VehicleTuning.TIRE_SLIP_RESPONSE);
        Vector3d roll = axis.cross(lever, new Vector3d());
        double treadLength = roll.length();
        if (treadLength > 1e-9) {
            roll.div(treadLength);
            roll.fma(-roll.dot(normal), normal);
            // A brake holds the wheel against the road's turning of it, never more than stops it this step.
            double rolling = v.dot(roll);
            if (brake > 0) {
                double hold = Math.min(brake * VehicleTuning.BRAKE_CLAMP_SU.value() / io.github.soundgoodizerfan.feedback.core.unit.Units.SU_PER_KPG_TORQUE / h * dt, share * Math.abs(rolling));
                friction.fma(-Math.signum(rolling) * hold, roll);
            }
        }
        double cap = grip * load * dt;
        if (friction.length() > cap) {
            out[1] = slip.length();
            friction.normalize(cap);
        }
        // Rolling resistance: the tread flexing as it rolls, never more than stops it this step.
        if (treadLength > 1e-9) {
            double rolling = v.dot(roll);
            double resist = Math.min((shape.rolling() + groundRolling(ground)) * load * dt, share * Math.abs(rolling));
            friction.fma(-Math.signum(rolling) * resist, roll);
        }
        Vector3d impulse = new Vector3d(normal).mul(load * dt).add(friction);
        BodyPush.impulseAt(wheel, contact, impulse);
        return axis.dot(lever.cross(friction, new Vector3d())) / dt;
    }

    /**
     * The rest of the rim: rays out from the hub in the tire's plane, all round but the ground patch.
     * A hit inside the radius pushes the wheel out along the struck face, with the spring and damper and
     * no grip, so a wheel stops at a wall or a step instead of passing through it (tire voxels have no collider).
     */
    private static void guard(ServerLevel level, ServerSubLevel wheel, Set<SubLevel> mine, Vector3d centre, Vector3d axis,
                              Vector3d down, Shape shape, double carried, double omegaN, double dt) {
        Vector3d side = axis.cross(down, new Vector3d());
        for (int k = 0; k < GUARD_RAYS; k++) {
            double a = 2 * Math.PI * k / GUARD_RAYS;
            Vector3d dir = new Vector3d(down).mul(Math.cos(a)).fma(Math.sin(a), side);
            if (dir.dot(down) > GUARD_SKIP_COS)
                continue;
            Vec3 from = new Vec3(centre.x, centre.y, centre.z);
            Vec3 to = from.add(dir.x * shape.radius(), dir.y * shape.radius(), dir.z * shape.radius());
            ClipContext ray = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
            ((ClipContextExtension) ray).sable$setSubLevelIgnoring(mine::contains);
            BlockHitResult hit = level.clip(ray);
            if (hit.getType() == HitResult.Type.MISS)
                continue;
            Vec3 at = hit.getLocation();
            var face = hit.getDirection().getNormal();
            Vector3d n = new Vector3d(face.getX(), face.getY(), face.getZ());
            var other = SableCompanion.INSTANCE.getContaining(level, at);
            if (other != null) {
                at = other.logicalPose().transformPosition(at);
                other.logicalPose().transformNormal(n);
            }
            Vector3d contact = new Vector3d(at.x, at.y, at.z);
            double depth = shape.radius() - contact.distance(centre);
            double facing = -n.dot(dir);
            if (depth <= 0 || facing <= 0.05)
                continue;
            Vector3d v = BodyPush.pointVelocity(wheel, contact);
            if (other instanceof ServerSubLevel o)
                v.sub(BodyPush.pointVelocity(o, contact));
            double share = Math.min(BodyPush.effectiveMass(wheel, contact, n), carried);
            double damping = Math.min(2 * FTuning.TIRE_DAMPING_RATIO * omegaN * dt, 1) * share * v.dot(n) / dt;
            double push = (share * omegaN * omegaN * depth - damping) * facing;
            if (push <= 0)
                continue;
            Vector3d impulse = new Vector3d(n).mul(push * dt);
            BodyPush.impulseAt(wheel, contact, impulse);
            if (other instanceof ServerSubLevel o)
                BodyPush.impulseAt(o, contact, impulse.negate(new Vector3d()));
        }
    }
}
