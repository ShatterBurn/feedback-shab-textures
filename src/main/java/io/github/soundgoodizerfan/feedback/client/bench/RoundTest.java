// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import io.github.soundgoodizerfan.feedback.weapon.GunTooltip;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The Blueprint Table's round test: a drawn gun read against a cartridge on paper. Nothing is fired and nothing is measured: it is the
 * ballistics worked out for the parts exactly to drawing, so it says what would happen in perfect conditions, in exact figures (no
 * instrument is read, because no real thing is). The real gun's figures still come off the item with an instrument.
 */
final class RoundTest {

    private RoundTest() {}

    /** The lines of the test, or empty when the sheet is not a gun or no round is seated. */
    static List<Component> lines(Optional<ModularDesign> design, ItemStack round) {
        Cartridge cartridge = round.get(FWeapons.CARTRIDGE.get());
        if (design.isEmpty() || cartridge == null)
            return List.of();
        ModularType type = ModularTypes.known(design.get().type());
        if (type == null || type.family() != ModularType.Family.GUN)
            return List.of();
        var materials = ClientMaterials.get();
        Optional<GunSpec> spec = GunSpec.of(design.get(), type, materials);
        if (spec.isEmpty())
            return List.of();
        List<Component> out = new ArrayList<>();
        Optional<GunFigures> baked = spec.get().bake(materials, cartridge);
        if (baked.isEmpty()) {
            out.add(Component.translatable(fault(spec.get(), cartridge), String.format("%.1f", spec.get().boreMmReal()), cartridge.boreMm())
                    .withStyle(ChatFormatting.RED));
            return out;
        }
        GunFigures f = baked.get();
        out.addAll(GunTooltip.figures(f));
        out.add(Component.translatable("gui.feedback.round_test.kick", String.format("%.1f", f.kickDeg())).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("gui.feedback.round_test.heat",
                String.format("%.3g", f.muzzleEnergy() * GunTuning.GUN_HEAT_PER_WORK * f.heatFactor() / f.thermalMass().workPerTu()),
                String.format("%.3g", f.coolingRate().tuPerTick() * FTuning.TICKS_PER_SECOND)).withStyle(ChatFormatting.GRAY));
        // Every Work of the shot landing: the most one hit can be worth, before the round's shape and the target take their share.
        out.add(Component.translatable("gui.feedback.round_test.damage", String.format("%.1f", f.muzzleEnergy() / ParryTuning.WORK_PER_DAMAGE))
                .withStyle(ChatFormatting.GRAY));
        if (f.overpressure() > 1)
            out.add(Component.translatable("gui.feedback.round_test.overpressure", Math.round(f.overpressure() * 100)).withStyle(ChatFormatting.RED));
        return out;
    }

    /** Why the gun cannot fire the round: the key to say it by (a shot charge in a rifled bore, a bore that is not the round's, or a part that cannot hold it). */
    private static String fault(GunSpec spec, Cartridge round) {
        if (round.shot() && spec.has("gun_rifling"))
            return "gui.feedback.round_test.rifled";
        if (Math.abs(spec.boreMmReal() - round.boreMm()) > GunTuning.GUN_BORE_FIT_MM)
            return "gui.feedback.round_test.bore";
        return "gui.feedback.round_test.unfit";
    }
}
