// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * The robots that carry an endernet upgrade, by the pair id it is bonded under: the other end of that pair, a Data Port, is the robot's base
 * station ({@code machine/ender/DataPortBlockEntity}), reading its senses and holding its actions over the pair with the same costs as any
 * endernet crossing. A robot registers while it is loaded and its leash can carry (a live brain, power in the pack), so a robot out of its
 * loaded chunk, unpowered or with no brain is simply not there, as an unloaded end of any pair is not.
 */
public final class RobotLeash {

    private static final Map<UUID, RobotEntity> LIVE = new HashMap<>();

    private RobotLeash() {
    }

    public static void join(UUID pair, RobotEntity robot) {
        LIVE.put(pair, robot);
    }

    public static void leave(UUID pair, RobotEntity robot) {
        LIVE.remove(pair, robot);
    }

    @Nullable
    public static RobotEntity get(UUID pair) {
        RobotEntity r = LIVE.get(pair);
        return r == null || r.isRemoved() ? null : r;
    }
}
