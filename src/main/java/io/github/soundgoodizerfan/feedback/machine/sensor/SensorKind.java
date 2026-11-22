// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * What a sensor block looks at: the face it points at. Sensing only, nothing here controls anything; a controller card links the block
 * and reads {@link #read}. Every reading is a fraction from 0 to 1 and is quantised to a few bands, the cheap sensor's coarseness.
 */
public enum SensorKind {

    /** A crop's growth: the block's {@code age} state over its largest age. Reads by property, so any crop with an age works. */
    RIPENESS("ripeness_probe") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockState state = level.getBlockState(pos.relative(face));
            for (var property : state.getProperties())
                if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                    int max = age.getPossibleValues().stream().max(Integer::compare).orElse(1);
                    return band(state.getValue(age) / (float) max, finer(4, self));
                }
            return 0f;
        }
    },
    /** Light at the face: the brighter of block light and daylight. Reads its own lamp's light too, which is the trap. */
    PHOTOCELL("photocell") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockPos at = pos.relative(face);
            int sky = Math.max(0, level.getBrightness(LightLayer.SKY, at) - level.getSkyDarken());
            int block = level.getBrightness(LightLayer.BLOCK, at);
            return band(Math.max(sky, block) / 15f, finer(5, self));
        }
    },
    /** A creature or a dropped item standing in the face's block. */
    PRESENCE("presence_plate") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            AABB box = new AABB(pos.relative(face));
            return level.getEntities((net.minecraft.world.entity.Entity) null, box,
                    e -> e instanceof net.minecraft.world.entity.LivingEntity || e instanceof net.minecraft.world.entity.item.ItemEntity).isEmpty() ? 0f : 1f;
        }
    },
    /** How full the inventory or tank at the face is. */
    FILL("fill_gauge") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            BlockPos at = pos.relative(face);
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, at, face.getOpposite());
            if (items != null && items.getSlots() > 0) {
                float filled = 0f;
                for (int i = 0; i < items.getSlots(); i++) {
                    var stack = items.getStackInSlot(i);
                    if (!stack.isEmpty())
                        filled += stack.getCount() / (float) Math.min(items.getSlotLimit(i), stack.getMaxStackSize());
                }
                return band(filled / items.getSlots(), finer(8, self));
            }
            var fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, at, face.getOpposite());
            if (fluids != null && fluids.getTanks() > 0) {
                float have = 0f, cap = 0f;
                for (int i = 0; i < fluids.getTanks(); i++) {
                    have += fluids.getFluidInTank(i).getAmount();
                    cap += fluids.getTankCapacity(i);
                }
                return cap <= 0f ? 0f : band(have / cap, finer(8, self));
            }
            return 0f;
        }
    },
    /**
     * How taut the fixed line is that runs through the sensor's own block: the load hanging on it over what its material is rated for, in
     * bands, 0 for a slack line or none. A line that is over its rating reads full, and is already straining ({@code FixedLines}).
     */
    TENSION("tension_gauge") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            return band(io.github.soundgoodizerfan.feedback.combat.tether.FixedLines.tensionAt(level, pos), finer(6, self));
        }
    },
    /** A change in the face block's state: high for a short time after it, and a tick late. The observer's role. */
    OBSERVER("observer") {
        @Override
        public float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self) {
            return self != null && self.pulse() > 0 ? 1f : 0f;
        }
    };

    private final String id;

    SensorKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public abstract float read(Level level, BlockPos pos, Direction face, SensorBlockEntity self);

    /** The bands a reading is cut into: {@code base}, or more when the sensor carries a quiet circuit ({@link SensorBlockEntity#stepFactor}). */
    static int finer(int base, SensorBlockEntity self) {
        return self == null ? base : Math.max(base, Math.round(base / self.stepFactor()));
    }

    /** Rounds a 0-1 fraction to {@code steps} equal bands. */
    static float band(float fraction, int steps) {
        return Math.round(Math.max(0f, Math.min(1f, fraction)) * steps) / (float) steps;
    }
}
