// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The weight of a built vehicle part (engine, gearbox, propeller, jet, thruster) on the vehicle it is part of.
 * <p>
 * Sable weighs a block by its state alone, so every block of one id would weigh the same. The part's block is
 * held light ({@link VehicleTuning#VEHICLE_PART_BLOCK_MU}, its {@code physics_block_properties} file) and the rest of what its design
 * weighs rides on a free box welded at the block's centre with contacts off, the weight and the inertia in the right place. The design
 * weighs one number, {@code Fig.MASS} (the Blueprint Table's mass figure), and this carries exactly it. Boxes are not saved, so the
 * owner's server tick re-makes it ({@link #tick}); {@link #release} takes it down with the block.
 * <p>
 * Not weightless: a vehicle of nothing but one part would weigh zero at the moment of lifting, before the box exists, and Sable would
 * discard the body. A part designed lighter than the block's own weight weighs that.
 */
public final class CarriedMass {

    /** Just inside the block, so the box never reaches past what is drawn. */
    private static final double BOX_HALF = 0.45;
    /** What Sable counts for the part's own block, kpg. */
    public static final double BLOCK_KPG = Units.kpg(new Mu(VehicleTuning.VEHICLE_PART_BLOCK_MU));

    private BoxPhysicsObject box;
    private PhysicsConstraintHandle weld;

    /** Puts {@code design} (the part's whole weight) on the vehicle {@code owner} is part of, once its box is in Sable. Server tick only. */
    public void tick(BlockEntity owner, Mu design) {
        if (design.value() <= 0f || !(owner.getLevel() instanceof ServerLevel server)
                || !(SableCompanion.INSTANCE.getContaining(owner) instanceof ServerSubLevel body))
            return;
        SubLevelPhysicsSystem system = SubLevelPhysicsSystem.get(server);
        if (box == null || box.isRemoved()) {
            var pos = owner.getBlockPos();
            Vector3d centre = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Pose3d pose = new Pose3d(body.logicalPose().transformPosition(centre, new Vector3d()),
                    new Quaterniond(body.logicalPose().orientation()), new Vector3d(), new Vector3d(1, 1, 1));
            box = new BoxPhysicsObject(pose, new Vector3d(BOX_HALF, BOX_HALF, BOX_HALF), Math.max(1e-3, Units.kpg(design) - BLOCK_KPG));
            system.addObject(box);
            weld = null;
        }
        if (box.getRuntimeId() == PhysicsPipelineBody.NULL_RUNTIME_ID || (weld != null && weld.isValid()))
            return;
        var pos = owner.getBlockPos();
        Vector3d centre = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        weld = system.getPipeline().addConstraint(body, box, new FixedConstraintConfiguration(centre, new Vector3d(), new Quaterniond()));
        weld.setContactsEnabled(false); // the box sits inside its own block
    }

    /** Takes the box down with its block. */
    public void release(BlockEntity owner) {
        if (weld != null && weld.isValid())
            weld.remove();
        if (box != null && owner.getLevel() instanceof ServerLevel server && !box.isRemoved())
            SubLevelPhysicsSystem.get(server).removeObject(box);
        box = null;
        weld = null;
    }

    /** What this part puts on its vehicle, block and box, in kpg, for tests; the block alone until the box exists. */
    public double kpg() {
        return BLOCK_KPG + (box == null || box.isRemoved() ? 0 : box.getMass());
    }
}
