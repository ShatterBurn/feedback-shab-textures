// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

/**
 * A node whose readings are named channels that another controller's Sensor card picks one of: the readable variables of a Controller (a
 * fleet shares state through them), the senses of a robot behind a radio mast. The Programmer cycles a Sensor card's channel through
 * these names instead of waiting for a key, as it does for a vehicle seat. A lang key or a plain name each shows as written.
 */
public interface RemoteChannels {

    /** The names of the sense channels, by channel index; empty if there is nothing to pick yet. */
    String[] senseKeys();
}
