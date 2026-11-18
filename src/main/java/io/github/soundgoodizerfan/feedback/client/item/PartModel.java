// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.type.HaftedLayers;
import io.github.soundgoodizerfan.feedback.modular.type.ModularHeads;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * A shaped part drawn as the layer it is on a tool: a head part is its head's cuts at the length its edge axis was made to, a guard or
 * binding its layer file in its variant, at the place they sit on a tool with no haft under them. So a part looks like what it will be
 * before it is put on anything, and a long or short head shows its measurements. Tinted by the part's own material (tint 0, as
 * {@link ModularClient} does for a part). A part with no layer files keeps its plain sprite.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PartModel {

    private PartModel() {
    }

    /** The addon parts that are layers of a tool: item path to the role letter and the layer's file name. */
    private static final Map<String, String[]> ADDONS = Map.of("sword_guard", new String[] {"G", "guard"}, "tool_binding", new String[] {"B", "binding"});

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        for (var part : io.github.soundgoodizerfan.feedback.registry.FModular.PARTS) {
            String path = BuiltInRegistries.ITEM.getKey(part.get()).getPath();
            if (HaftedLayers.kindFile(path).isEmpty() && !ADDONS.containsKey(path))
                continue;
            ModelResourceLocation key = ModelResourceLocation.inventory(Feedback.id(path));
            BakedModel base = event.getModels().get(key);
            if (base != null)
                event.getModels().put(key, new HaftedModel.Layered(base, new Resolver(base, path)));
        }
    }

    private static final class Resolver extends ItemOverrides {
        private final HaftedModel.Resolver baker;
        private final BakedModel base;
        private final String path;
        /** One model per look: variant and edge slices. Empty means no layer files, the plain sprite stands. */
        private final Map<String, Optional<BakedModel>> looks = new HashMap<>();

        Resolver(BakedModel base, String path) {
            this.baker = new HaftedModel.Resolver(base);
            this.base = base;
            this.path = path;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            MadeOf made = stack.get(FDataComponents.MADE_OF.get());
            if (made != null) {
                var look = looks.computeIfAbsent(made.variant() + "|" + made.measures().axes(), k -> build(made));
                if (look.isPresent())
                    return look.get();
            }
            return base.getOverrides().resolve(base, stack, level, entity, seed);
        }

        private Optional<BakedModel> build(MadeOf made) {
            List<HaftedLayers.Placement> slot, held;
            var kind = HaftedLayers.kindFile(path);
            if (kind.isPresent()) {
                int edge = HaftedLayers.edgeSteps(made.measures(), ModularHeads.axesOf(path));
                slot = HaftedLayers.headAlone(kind.get(), HaftedLayers.faceVariant(kind.get(), made.variant(), made.measures()), edge, baker.known, false);
                held = HaftedLayers.headAlone(kind.get(), HaftedLayers.faceVariant(kind.get(), made.variant(), made.measures()), edge, baker.known, true);
            } else {
                String[] addon = ADDONS.get(path);
                slot = held = HaftedLayers.addonAlone(addon[0], addon[1], HaftedLayers.artVariant(addon[0], made.variant(), made.measures()), baker.known);
            }
            if (slot.isEmpty() || held.isEmpty())
                return Optional.empty();
            BakedModel in = baker.built.computeIfAbsent(slot, l -> baker.bake(l, role -> 0));
            BakedModel out = baker.built.computeIfAbsent(held, l -> baker.bake(l, role -> 0));
            return Optional.of(in == out ? in : baker.bySize.computeIfAbsent(List.of(in, out), k -> new HaftedModel.BySize(in, out)));
        }
    }
}
