// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import org.joml.Vector3d;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;

import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.space.EndRoute;
import io.github.soundgoodizerfan.feedback.space.SpaceTravel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Reads how far off the End (the gas giant in the Gap's sky) the vehicle's front points, and how far away it is, for a Controller's Input card. It is an
 * unsided {@link SensorFitting} like the flight sensor, so the card machinery serves it unchanged. One reading at a time, stepped by use:
 * the turn needed to the left or right (yaw), up or down (pitch), or the distance. It reads only in the Gap; anywhere else it reads zero.
 * It senses and controls nothing, and quantised to its own resolution a sloppy reading costs a longer trip, never a refusal.
 */
public class BearingSensorBlockEntity extends BlockEntity implements SensorFitting, VehiclePart {

    /** What it reads, with the quantity that reading is. */
    public enum Mode implements StringRepresentable {
        /** Degrees to turn the front to face the End, positive to the right (clockwise seen from above). */
        YAW("yaw", Quantity.ATTITUDE),
        /** Degrees to tip the front up (positive) to face it. */
        PITCH("pitch", Quantity.ATTITUDE),
        /** Blocks, the Gap's own size, to the End. */
        DISTANCE("distance", Quantity.POSITION);

        private final String key;
        private final Quantity quantity;

        Mode(String key, Quantity quantity) {
            this.key = key;
            this.quantity = quantity;
        }

        @Override
        public String getSerializedName() {
            return key;
        }

        public Quantity quantity() {
            return quantity;
        }
    }

    private Mode mode = Mode.YAW;

    public BearingSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.BEARING_SENSOR_ENTITY.get(), pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public void nextMode() {
        mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length];
        setChanged();
    }

    /** Degrees round from south (clockwise from above) a horizontal direction points. */
    private static double heading(Vector3d v) {
        return Math.toDegrees(Math.atan2(-v.x, v.z));
    }

    private float truth(Mode m) {
        if (level == null || !level.dimension().equals(SpaceTravel.GAP))
            return 0f;
        Direction facing = getBlockState().getValue(BearingSensorBlock.FACING);
        Vector3d front = new Vector3d(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 at = Vec3.atCenterOf(worldPosition);
        if (SableCompanion.INSTANCE.getContaining(this) instanceof ServerSubLevel body) {
            at = body.logicalPose().transformPosition(at);
            body.logicalPose().transformNormal(front);
        }
        Vector3d want = EndRoute.planet().sub(at.x, at.y, at.z);
        double distance = want.length();
        if (distance < 1e-6)
            return 0f;
        return switch (m) {
            case DISTANCE -> (float) distance;
            case PITCH -> (float) Math.toDegrees(Math.asin(Math.clamp(want.y / distance, -1, 1))
                    - Math.asin(Math.clamp(front.y, -1, 1)));
            // The turn to the right is the heading dropping: heading is measured clockwise from above, so a larger heading is to the right.
            case YAW -> {
                if (front.x * front.x + front.z * front.z < 1e-6)
                    yield 0f;
                double d = heading(want) - heading(front);
                yield (float) (d - 360 * Math.floor((d + 180) / 360));
            }
        };
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == mode.quantity();
    }

    @Override
    public float resolution(Quantity quantity) {
        return quantity == Quantity.POSITION ? VehicleTuning.FLIGHT_POSITION_RESOLUTION : VehicleTuning.FLIGHT_ATTITUDE_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.bearing_sensor");
    }

    @Override
    public Component readValue() {
        return Instruments.signed(this, Component.translatable("feedback.bearing_sensor.reading." + mode.getSerializedName(),
                Instruments.format(truth(mode), resolution(mode.quantity()))));
    }

    @Override
    public float readRaw() {
        return Instruments.quantise(truth(mode), resolution(mode.quantity()));
    }

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FVehicles.BEARING_SENSOR_ITEM.get());
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @org.jetbrains.annotations.Nullable
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Mode", mode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = Mode.values()[Math.floorMod(tag.getInt("Mode"), Mode.values().length)];
    }
}
