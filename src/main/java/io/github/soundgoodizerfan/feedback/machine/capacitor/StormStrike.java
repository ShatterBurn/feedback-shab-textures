// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.capacitor;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.LightningRodBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * A strike that lands on a lightning rod standing on a Leyden jar charges the jar.
 *
 * <p>Lightning is stored charge falling from a storm cloud (the same charge a creeper's reserve takes up); a rod is a
 * conductor that gathers it, and a jar under the rod is where it goes. The strike arrives at
 * {@link ElectricMachineTuning#STORM_STRIKE_BAND} carrying {@link ElectricMachineTuning#STORM_STRIKE_PACKETS} packets of it:
 * a jar whose glass is rated that band takes it up to its capacity, and a jar rated lower punctures (the glass goes, and
 * whoever stands near takes what the jar held). A rod with no jar under it does what a vanilla rod does and nothing more.
 * The rod is found by its block class, as the vanilla strike itself finds it, never by id.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class StormStrike {

    private StormStrike() {
    }

    @SubscribeEvent
    public static void onBolt(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level))
            return;
        BlockPos at = bolt.blockPosition();
        // The vanilla strike is placed on the block above the rod it was drawn to; a bolt that lands on the rod itself counts too.
        for (BlockPos rod : new BlockPos[] {at.below(), at}) {
            if (!(level.getBlockState(rod).getBlock() instanceof LightningRodBlock))
                continue;
            if (level.getBlockEntity(rod.below()) instanceof LeydenJarBlockEntity jar) {
                if (jar.band().compareTo(ElectricMachineTuning.STORM_STRIKE_BAND) < 0)
                    jar.puncture();
                else
                    jar.inject(ElectricMachineTuning.STORM_STRIKE_PACKETS * ElectricRules.packetEu(ElectricMachineTuning.STORM_STRIKE_BAND), 1f);
            }
            return;
        }
    }
}
