// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.egg.dimensionalsable.SubLevelWarper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.sablescale.scale.SubLevelScale;
import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import javax.annotation.Nullable;

/**
 * The Shelf prototype (docs/specs/feedback_space_spec.md): an operator moves a Sable body and
 * everything joined to it between the Overworld and {@code feedback:shelf}, and scales one.
 * The move is Dimensional Sable's (MIT; serialises the body and loads it in the other level, keeping velocity),
 * the scale is Sable Scaling's (MIT; resamples the colliders and scales mass). Both are required mods and
 * called through their public API; nothing of theirs is copied.
 */
public final class SpaceTravel {
    public static final ResourceKey<Level> SHELF = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "shelf"));
    private static final double LOOK_REACH = 16;

    private SpaceTravel() {
    }

    /** The body the player stands on, else the one they look at; null if neither is a Sable body. */
    @Nullable
    public static ServerSubLevel bodyOf(ServerPlayer player) {
        if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player) instanceof ServerSubLevel s) return s;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(LOOK_REACH));
        var hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return SableCompanion.INSTANCE.getContaining(player.level(), hit.getBlockPos()) instanceof ServerSubLevel s ? s : null;
    }

    /** Bodies moved (the chain), or -1 when {@code body} is already in the Shelf. */
    public static int send(ServerLevel from, ServerSubLevel body) {
        if (from.dimension().equals(SHELF)) return -1;
        ServerLevel shelf = from.getServer().getLevel(SHELF);
        if (shelf == null) return -2;
        Vector3d at = new Vector3d(body.logicalPose().position());
        var aboard = aboard(from, body);
        int n = SubLevelWarper.warpConnectedSubLevels(body, shelf, at.set(at.x, SpaceTuning.ENTRY_Y, at.z));
        if (n > 0)
            fade(aboard, true);
        return n;
    }

    /** Bodies moved, or -1 when {@code body} is not in the Shelf. Arrives where the Overworld's air is thin but not zero. */
    public static int back(ServerLevel from, ServerSubLevel body) {
        if (!from.dimension().equals(SHELF)) return -1;
        ServerLevel overworld = from.getServer().overworld();
        Vector3d at = new Vector3d(body.logicalPose().position());
        var aboard = aboard(from, body);
        int n = SubLevelWarper.warpConnectedSubLevels(body, overworld, at.set(at.x, SpaceTuning.RETURN_Y, at.z));
        if (n > 0)
            fade(aboard, false);
        return n;
    }

    /** Players inside or beside the body: they see the crossing. */
    private static java.util.List<ServerPlayer> aboard(ServerLevel level, ServerSubLevel body) {
        var b = body.boundingBox();
        return level.getEntitiesOfClass(ServerPlayer.class,
                new net.minecraft.world.phys.AABB(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()).inflate(2));
    }

    private static void fade(java.util.List<ServerPlayer> players, boolean up) {
        for (ServerPlayer p : players)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new io.github.soundgoodizerfan.feedback.net.SpaceFadePayload(up));
    }

    /** Sets the scale of {@code body} and every body joined to it (Sable Scaling walks the chain). */
    public static void scale(ServerSubLevel body, double k) {
        SubLevelScale.apply(body, k);
    }

    public static double minScale() {
        return SubLevelScale.MIN_SCALE;
    }

    public static double maxScale() {
        return SubLevelScale.MAX_SCALE;
    }
}
