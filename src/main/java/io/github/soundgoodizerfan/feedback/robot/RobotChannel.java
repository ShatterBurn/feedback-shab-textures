// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import org.jetbrains.annotations.Nullable;

/**
 * A robot's own senses and actions, named by a program card through {@link DataNodeRef#local}. An action is a switch the program holds
 * on or off (the Controller is a switch, never a dial); a sense reads back as a boolean, or as a number. The ordinal is what a printed
 * card stores, so a program moves between robots of any form that carry the channel. A sense that needs a fitted sensor part reads 0 on
 * a robot without it (see {@link RobotSpec}).
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
    CHARGE(false, false),
    /** Put the first stack it carries into the container ahead (any block that offers an item port). */
    GIVE(true, true),
    /** Take the first stack the container ahead lets out. */
    TAKE(true, true),
    /** Hold the switchable machine ahead engaged while on, released while off. */
    SWITCH(true, true),
    /** Blocks to the first solid thing ahead, up to the sensor's reach. Needs a rangefinder. */
    RANGE_AHEAD(false, false),
    /** How hard the block ahead is. Needs a probe. */
    HARDNESS_AHEAD(false, false),
    /** Which way it faces, in quarter turns from south. Needs an inertial unit. */
    HEADING(false, false),
    /** A beacon is within reach: standing by a landmark. */
    BEACON_NEAR(false, true),
    /** A charging dock or reel is the block ahead. */
    DOCK_AHEAD(false, true),
    /** Where dead reckoning puts it east of where it was set down, in blocks; drifts until a beacon corrects it. */
    POS_X(false, false),
    /** Where dead reckoning puts it south of where it was set down. */
    POS_Z(false, false),
    /** Rise while on; only a rotor robot light enough to fly answers. */
    ASCEND(true, true),
    /** Sink while on. */
    DESCEND(true, true),
    /** Blocks above the block it was set down on. */
    ALTITUDE(false, false),
    /** Use the first stack it carries on the block ahead as a hand would (nothing carried: an empty hand); how true the hand lands is its dexterity. */
    USE(true, true),
    /** Store where it is, as dead reckoning has it, in the chosen place; needs a lattice on its brain. Done on the switch coming on. */
    REMEMBER(true, true),
    /** Choose the next remembered place, once each time the switch comes on. */
    NEXT_PLACE(true, true),
    /** East and south of the chosen place from where it is now, in blocks, by its dead reckoning; 0 with no place stored. */
    PLACE_DX(false, false),
    PLACE_DZ(false, false),
    /** Blocks to the chosen place. */
    PLACE_DIST(false, false),
    /** The chosen place is within a block and a half. */
    AT_PLACE(false, true),
    /** How many places it holds. */
    PLACES(false, false);

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

    /** What a sensor card on this channel reads: a state, the battery's charge, or a figure no instrument quantity covers. */
    public Quantity quantity() {
        return bool ? Quantity.STATE : this == CHARGE ? Quantity.CHARGE : Quantity.FIGURE;
    }

    public DataNodeRef ref() {
        return DataNodeRef.local(ordinal());
    }

    private static final RobotChannel[] ALL = values();

    @Nullable
    public static RobotChannel of(int ordinal) {
        return ordinal >= 0 && ordinal < ALL.length ? ALL[ordinal] : null;
    }

    public String langKey() {
        return "robot.feedback.channel." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
