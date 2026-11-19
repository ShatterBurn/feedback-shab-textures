// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.core.rotation.RotationMember;

import net.minecraft.world.level.block.state.BlockState;

/**
 * A rotation member that stands in a voxel of a finespace cell: a {@link MicroRotor} (a part with no block entity of its own) or a
 * {@link HostedRotor} (a hosted machine's rotation node). The propagator walks the voxel grid through this, with each voxel's block state.
 */
public interface FineMember extends RotationMember {

    MicroCellBlockEntity cell();

    int index();

    /** The state of the block in the voxel, which decides how it couples to its neighbours. */
    BlockState state();
}
