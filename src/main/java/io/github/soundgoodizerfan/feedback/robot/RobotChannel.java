// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;

import org.jetbrains.annotations.Nullable;

/**
 * A robot's own senses and actions, named by a program card through {@link DataNodeRef#local}. An action is a switch the program holds
 * on or off (the Controller is a switch, never a dial); a sense reads back as a boolean, or for {@link #CHARGE} as a share. The ordinal is
 * what a printed card stores, so a program moves between robots of any form that carry the channel.
 */
public enum RobotChannel {
    /** Roll forward while on. */
    WALK(true, true),
    TURN_LEFT(true, true),
    TURN_RIGHT(true, true),
    /** Break the block ahead; one block per step it can afford. */
    MINE(true, true),
    /** Set the first block it carries into the space ahead. */
    PLACE(true, true),
    /** Take the items lying ahead into its cargo. */
    PICK_UP(true, true),
    /** Drop the first stack it carries ahead. */
    DROP(true, true),
    /** The space ahead is blocked. */
    SOLID_AHEAD(false, true),
    /** The block ahead is something the tool head can break. */
    BREAKABLE_AHEAD(false, true),
    ITEM_AHEAD(false, true),
    CARGO_FULL(false, true),
    /** Share of the battery's charge left; a number, not a boolean. */
    CHARGE(false, false);

    private final boolean actuator;
    private final boolean bool;

    RobotChannel(boolean actuator, boolean bool) {
        this.actuator = actuator;
        this.bool = bool;
    }

    public boolean actuator() {
        return actuator;
    }

    /** Whether it reads as a boolean (a state) rather than a number. */
    public boolean bool() {
        return bool;
    }

    public DataNodeRef ref() {
        return DataNodeRef.local(ordinal());
    }

    @Nullable
    public static RobotChannel of(int ordinal) {
        RobotChannel[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    public String langKey() {
        return "robot.feedback.channel." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
