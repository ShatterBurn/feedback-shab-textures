// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.space.BreathingGear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/**
 * A respirator worn in the Curios {@code face} slot: a face piece and strap, and the supply they carry. With a
 * <b>filter cartridge</b> (porous media, a bed of a chosen depth) it concentrates what oxygen there is, down to a floor the bed
 * sets, wears one point a second of thin air it works through, and slows the breath (a share off sprint speed); it
 * works nowhere that has no oxygen to find. With an <b>air cylinder</b> it breathes from its own store of {@code air}
 * (any place, the Compressor fills it), drawing only while the air outside is too thin. The kind is what was
 * built: nothing picks it. A filter mask also cuts fume dose ({@code Skills.fumeShare}).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class ModularRespirator extends ModularItem implements BreathingGear {

    public static final String FACE = "B";
    public static final String FILTER = "F";
    public static final String CYLINDER = "T";

    public ModularRespirator(Properties properties) {
        super(properties);
    }

    @Override
    public boolean supply(ItemStack worn, ServerPlayer wearer, float breathable) {
        if (ModularStacks.broken(worn))
            return false;
        ModularFigures f = ModularStacks.figures(worn);
        ModularDesign d = ModularStacks.design(worn);
        if (f == null || d == null)
            return false;
        if (d.part(FILTER) != null) {
            if (breathable < f.get(Fig.BREATH_FLOOR))
                return false;
            if (wearer.tickCount % 20 == 0)
                worn.hurtAndBreak(1, wearer.serverLevel(), wearer, item -> { });
            return true;
        }
        if (d.part(CYLINDER) == null)
            return false;
        IFluidHandler tank = worn.getCapability(Capabilities.FluidHandler.ITEM);
        return tank != null && tank.drain(ModularTuning.RESPIRATOR_DRAW_MB_PER_TICK, IFluidHandler.FluidAction.EXECUTE).getAmount() > 0;
    }

    /** Whether {@code who} wears a whole filter mask: it keeps fumes' poison down. */
    public static boolean filtering(LivingEntity who) {
        return CuriosApi.getCuriosInventory(who).flatMap(inv -> inv.findFirstCurio(s -> {
            if (!(s.getItem() instanceof ModularRespirator) || ModularStacks.broken(s))
                return false;
            ModularDesign d = ModularStacks.design(s);
            return d != null && d.part(FILTER) != null;
        })).isPresent();
    }

    /** The breathing resistance of a filter bed, while the mask is worn. */
    @SubscribeEvent
    public static void onCurioAttributes(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof ModularRespirator) || ModularStacks.broken(stack))
            return;
        ModularFigures f = ModularStacks.figures(stack);
        if (f == null || f.get(Fig.BREATH_DRAG) <= 0f)
            return;
        ResourceLocation id = Feedback.id("respirator_" + event.getSlotContext().identifier() + "_" + event.getSlotContext().index());
        event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(id, -f.get(Fig.BREATH_DRAG), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }
}
