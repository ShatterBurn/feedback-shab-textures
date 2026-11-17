// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Programmer inside a Computer: the same editing logic, held by the Computer block entity and never placed in the world on its own.
 * It is electric (takes every medium), is valid while its host is, and tells its host when it changes.
 */
public class EmbeddedProgrammer extends ProgrammerBlockEntity {

    private final BlockEntity host;
    public EmbeddedProgrammer(BlockEntity host) {
        super(host.getBlockPos(), host.getBlockState());
        this.host = host;
    }

    @Override
    protected boolean electric() {
        return true;
    }

    @Override
    protected java.util.Optional<io.github.soundgoodizerfan.feedback.control.data.DataNodeRef> attachedRef(int index) {
        if (!(host instanceof io.github.soundgoodizerfan.feedback.control.computer.ComputerBlockEntity c))
            return java.util.Optional.empty();
        var list = c.attached();
        return index >= 0 && index < list.size() ? java.util.Optional.of(list.get(index).ref()) : java.util.Optional.empty();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(host, player);
    }

    @Override
    public void sync() {
        host.setChanged();
        if (host.getLevel() != null && !host.getLevel().isClientSide)
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
    }
}
