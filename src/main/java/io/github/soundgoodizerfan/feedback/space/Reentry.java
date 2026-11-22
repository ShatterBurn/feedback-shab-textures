// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

/**
 * Coming back (docs/specs/feedback_space_spec.md 6): a body moving fast through air heats on its leading face, in proportion to the
 * air's density and the cube of its speed. The heat is not stored. Once a second the wall temperature that flux would hold is worked
 * out, and each voxel on the windward face made of a material whose failure temperature ({@code Material.failureTemperature}: a
 * burnable one burns up at its oxidation temperature, a metal holds until it melts) is below it is lost with a chance that grows with how
 * far over it is. What is lost is a voxel that carried the heat away by going; the voxel behind it is the leading face next. So a thick
 * layer of something that burns gets a craft down slowly and is spent doing it, a refractory plate holds, and a bare hull of metal that
 * cannot take the temperature goes a voxel at a time. Steep and fast (a short, hot pass) and shallow and long (a cooler, long one) both
 * work, by what the hull is made of. Speeds are full-size equivalents (a body in the Gap does not meet air).
 * <p>
 * A voxel's material is its form block's, or the material its block's item is a form of ({@link #hullMaterial}), so vanilla blocks that are a
 * material's form (iron, wool, wood) burn or hold by that material; a block that is no material's form is inert.
 * shortcut: only micro-cell voxels are scanned, and the windward face is found along the one axis the velocity points most along.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Reentry {
    private Reentry() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % SpaceTuning.REENTRY_INTERVAL_TICKS != 0
                || level.dimension().equals(SpaceTravel.GAP))
            return;
        var container = ServerSubLevelContainer.getContainer(level);
        if (container == null)
            return;
        for (ServerSubLevel body : container.getAllSubLevels()) {
            if (body.isRemoved())
                continue;
            Vector3d v = body.latestLinearVelocity;
            double speed = v.length();
            double density = AirDensityTable.at(level, body.logicalPose().position().y());
            double flux = density * speed * speed * speed;
            if (flux <= SpaceTuning.REENTRY_FLUX_FLOOR)
                continue;
            scorch(level, body, (flux - SpaceTuning.REENTRY_FLUX_FLOOR) * SpaceTuning.REENTRY_TU_PER_FLUX,
                    body.logicalPose().transformNormalInverse(new Vector3d(v).div(speed), new Vector3d()));
        }
    }

    /** Loses the windward voxels that cannot take a wall at {@code tu}; {@code ahead} is the way the body moves, in its own frame. */
    private static void scorch(ServerLevel level, ServerSubLevel body, double tu, Vector3d ahead) {
        Direction lead = Direction.getNearest(ahead.x, ahead.y, ahead.z);
        var materials = MaterialTable.get().materials();
        for (BlockEntitySubLevelActor a : body.getPlot().getBlockEntityActors()) {
            if (!(a instanceof MicroCellBlockEntity cell))
                continue;
            boolean changed = false;
            for (int z = 0; z < MicroCellBlockEntity.N; z++)
                for (int y = 0; y < MicroCellBlockEntity.N; y++)
                    for (int x = 0; x < MicroCellBlockEntity.N; x++) {
                        BlockState state = cell.get(x, y, z);
                        if (state == null || !exposed(level, cell, x, y, z, lead))
                            continue;
                        var hull = hullMaterial(state);
                        var material = hull == null ? null : materials.get(hull);
                        if (material == null || material.failureTemperature().isEmpty())
                            continue;
                        double fails = material.failureTemperature().get().value();
                        if (tu <= fails || level.random.nextDouble() > Math.min(1.0, (tu - fails) / fails))
                            continue;
                        cell.set(MicroCellBlockEntity.index(x, y, z), null);
                        changed = true;
                        var at = body.logicalPose().transformPosition(new net.minecraft.world.phys.Vec3(
                                cell.getBlockPos().getX() + (x + 0.5) / MicroCellBlockEntity.N,
                                cell.getBlockPos().getY() + (y + 0.5) / MicroCellBlockEntity.N,
                                cell.getBlockPos().getZ() + (z + 0.5) / MicroCellBlockEntity.N));
                        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.02);
                    }
            if (changed)
                cell.commit();
        }
    }

    /**
     * What a hull voxel is made of: a form block's own material, else the material the block's item is a form of (an iron block is iron, a
     * wool block is wool: a block-to-material table is the material table's own {@code forms}), or null for a block of none (inert).
     */
    private static net.minecraft.resources.ResourceLocation hullMaterial(BlockState state) {
        if (state.getBlock() instanceof FormBlock form)
            return form.material();
        return io.github.soundgoodizerfan.feedback.process.MaterialIngredient.locate(state.getBlock().asItem())
                .map(io.github.soundgoodizerfan.feedback.process.Material.Located::id).orElse(null);
    }

    /** Whether the voxel's face toward {@code lead} meets air: the next voxel that way, in this cell or the next, is empty. */
    private static boolean exposed(ServerLevel level, MicroCellBlockEntity cell, int x, int y, int z, Direction lead) {
        int nx = x + lead.getStepX(), ny = y + lead.getStepY(), nz = z + lead.getStepZ();
        int n = MicroCellBlockEntity.N;
        if (nx >= 0 && nx < n && ny >= 0 && ny < n && nz >= 0 && nz < n)
            return cell.get(nx, ny, nz) == null;
        BlockPos next = cell.getBlockPos().relative(lead);
        if (level.getBlockEntity(next) instanceof MicroCellBlockEntity other)
            return other.get(Math.floorMod(nx, n), Math.floorMod(ny, n), Math.floorMod(nz, n)) == null;
        return level.getBlockState(next).isAir();
    }
}
