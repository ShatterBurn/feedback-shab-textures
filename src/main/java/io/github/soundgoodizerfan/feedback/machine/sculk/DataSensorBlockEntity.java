// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;

/**
 * Listens for vibrations the way a vanilla sculk sensor does (same radius, same frequencies) and holds the last one
 * for a vanilla cooldown. A controller reads channel 0 as the frequency over 15, and channel 1 to 15 as whether that
 * exact frequency was just heard. Event-driven: it ticks nothing and costs one call per vibration in range.
 * <p>
 * A careful step is not heard and a wool block on the line from the vibration to the sensor muffles it, as at a sculk sensor.
 */
public class DataSensorBlockEntity extends BlockEntity
        implements DataNode, ChannelSource, GameEventListener.Provider<DataSensorBlockEntity.Ear> {


    private final Ear ear = new Ear();
    private int frequency;
    private long heardAt = Long.MIN_VALUE / 2;

    public DataSensorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DATA_SENSOR.get(), pos, state);
    }

    @Override
    public Ear getListener() {
        return ear;
    }

    /** The listening half, kept apart so the block entity itself is not a game event listener on every call path. */
    public final class Ear implements GameEventListener {
        private final PositionSource source = new BlockPositionSource(worldPosition);

        @Override
        public PositionSource getListenerSource() {
            return source;
        }

        @Override
        public int getListenerRadius() {
            return ElectricMachineTuning.DATA_SENSOR_RADIUS;
        }

        @Override
        public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 pos) {
            if (isRemoved() || !event.is(GameEventTags.VIBRATIONS))
                return false;
            // As a sculk sensor does: a careful step is not heard, and wool in the way muffles the sound.
            if (context.sourceEntity() != null && context.sourceEntity().isSteppingCarefully() && event.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING))
                return false;
            if (muffled(level, pos))
                return false;
            frequency = VibrationSystem.getGameEventFrequency(event);
            heardAt = level.getGameTime();
            io.github.soundgoodizerfan.feedback.core.Awards.near(level, getBlockPos(), "progress/deepdark/listen");
            return true;
        }
    }

    /** Whether a vibration-dampening block (wool, by the vanilla tag) stands on the straight line from {@code from} to this sensor. */
    private boolean muffled(Level level, Vec3 from) {
        Vec3 to = Vec3.atCenterOf(worldPosition);
        Vec3 d = to.subtract(from);
        int steps = (int) Math.ceil(d.length() * 2.0);
        for (int i = 1; i < steps; i++) {
            BlockPos p = BlockPos.containing(from.add(d.scale((double) i / steps)));
            if (!p.equals(worldPosition) && level.getBlockState(p).is(net.minecraft.tags.BlockTags.DAMPENS_VIBRATIONS))
                return true;
        }
        return false;
    }

    private boolean fresh() {
        return level != null && level.getGameTime() - heardAt <= ElectricMachineTuning.DATA_SENSOR_HOLD_TICKS;
    }

    @Override
    public float read(int channel) {
        if (!fresh())
            return 0f;
        return channel == 0 ? frequency / 15f : (frequency == channel ? 1f : 0f);
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

}
