// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.AmbientRadiation;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FCombat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Where there is no air there is the sky's radiation: a player takes the dimension's {@link AmbientRadiation} dose, through
 * what stands between them and it. Six rays along the axes count the walls ({@link Radiation#openShare}), cast in a body's
 * own plot for a person aboard one (the hull is the shield), then armour and wards cut it as for any dose. It adds to the
 * running dose {@code RadiationEvents} clears and acts on. Once a second, and only where a dimension lists a figure.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class AmbientDose {
    private AmbientDose() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % HazardTuning.RAD_INTERVAL_TICKS != 0
                || p.isSpectator() || p.isCreative())
            return;
        float sky = AmbientRadiation.at(p.level());
        if (sky <= 0f)
            return;
        var materials = MaterialTable.get().materials();
        BlockPos centre = p.blockPosition().above();
        if (SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(p) instanceof ServerSubLevel body)
            centre = BlockPos.containing(body.logicalPose().transformPositionInverse(p.getEyePosition()));
        float open = Radiation.openShare(p.level(), centre, materials);
        float passes = Radiation.shield(p, materials)
                * io.github.soundgoodizerfan.feedback.potion.Ward.passes(p, io.github.soundgoodizerfan.feedback.potion.Ward.Hazard.RADIATION);
        float seconds = HazardTuning.RAD_INTERVAL_TICKS / 20f;
        p.setData(FCombat.RADIATION_DOSE, p.getData(FCombat.RADIATION_DOSE) + sky * open * passes * seconds);
    }
}
