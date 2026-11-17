// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

/**
 * One thing a held weapon lets its wielder switch with the single "Gun option" key (a flamethrower's pilot flame is the first). The item
 * declares them ({@link GunMechanism#switches}) and carries the state itself; the key only asks the server to flip one. Not a
 * {@link GunOption}, which is a part fitted to the gun.
 *
 * @param id name, also the tail of the lang key {@code message.feedback.option.<id>} (the on/off line).
 * @param on the switch's state now.
 */
public record GunSwitch(String id, boolean on) {
}
