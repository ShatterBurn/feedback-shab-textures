// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.lso;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.hazard.HazardTuning;
import io.github.soundgoodizerfan.feedback.modular.item.ModularArmor;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import sfiomn.legendarysurvivaloverhaul.registry.AttributeRegistry;

/**
 * Worn modular armour insulates by what it is made of. Each part's material scores 1 at no conductivity down to 0 at the
 * grip ceiling ({@link HazardTuning#GRIP_MAX_CONDUCTIVITY}, the same line that decides whether a glove keeps a hand from a hot
 * piece); an organic with no stated conductivity scores half. The piece is the count-weighted mean of its parts, scaled by the
 * share of the body its slot covers, and given to LSO as thermal resistance (it narrows both heat and cold). Plate of metal
 * therefore insulates nothing, and a thick leather or wool layer does: a property of the material, never of an item id.
 */
final class LsoInsulation {

    private LsoInsulation() {
    }

    static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModularArmor armor))
            return;
        EquipmentSlot slot = armor.getEquipmentSlot();
        float share = switch (slot) {
            case HEAD -> LsoTuning.SHARE_HEAD;
            case CHEST -> LsoTuning.SHARE_CHEST;
            case LEGS -> LsoTuning.SHARE_LEGS;
            case FEET -> LsoTuning.SHARE_FEET;
            default -> 0f;
        };
        if (share <= 0f)
            return;
        float score = score(ModularStacks.design(stack));
        if (score <= 0f)
            return;
        event.addModifier(AttributeRegistry.THERMAL_RESISTANCE,
                new AttributeModifier(Feedback.id("lso_insulation_" + slot.getName()), LsoTuning.INSULATION_SET * share * score,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.bySlot(slot));
    }

    private static float score(ModularDesign design) {
        if (design == null)
            return 0f;
        Map<ResourceLocation, Material> table = MaterialTable.get().materials();
        if (table.isEmpty())
            table = ClientMaterials.get();
        float sum = 0f;
        int count = 0;
        for (ModularDesign.Part part : design.parts()) {
            int n = Math.max(1, part.count());
            Material material = table.get(part.material());
            sum += n * (material == null ? 0f : insulation(material));
            count += n;
        }
        return count == 0 ? 0f : Mth.clamp(sum / count, 0f, 1f);
    }

    private static float insulation(Material material) {
        return material.thermal().conductivity()
                .map(c -> Mth.clamp(1f - c.workPerTickPerTu() / HazardTuning.GRIP_MAX_CONDUCTIVITY, 0f, 1f))
                .orElseGet(() -> material.kind().map(k -> k == Material.Kind.ORGANIC ? LsoTuning.ORGANIC_INSULATION : 0f).orElse(0f));
    }
}
