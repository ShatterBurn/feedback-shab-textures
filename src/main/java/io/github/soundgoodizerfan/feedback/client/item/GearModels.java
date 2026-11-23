// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.client.gun.PlateDraw;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Worn modular armour on vanilla's own armour cubes (the same humanoid mesh, so it moves, sneaks and swings as vanilla's
 * does, in the dyed layer sprite) with a few extra cubes per variant of the plate. The extras are Blockbench geo files and the
 * variant table a JSON beside them ({@link GearGeo}); every extra rides a vanilla part, so nothing here animates by itself. The
 * variant's extras are shown and the rest hidden each time an armour piece asks for the model; studs are drawn in their own
 * material's colour after the rest.
 */
public final class GearModels {

    private GearModels() {
    }

    /** The armour model: vanilla's mesh plus the extras, set up per piece by the extension. */
    private static final class Gear extends HumanoidModel<LivingEntity> {
        private final Map<String, ModelPart> extra = new HashMap<>();
        private final Map<String, ModelPart> host = new HashMap<>();
        private final Map<String, List<float[]>> boxes = new HashMap<>();
        private List<String> on = List.of();
        private int studs = -1, plateColour = 0xFFFFFFFF, calls;
        private Material plateMaterial, studMaterial;

        final int generation = GearGeo.generation();

        Gear(ModelPart root, boolean inner) {
            super(root);
            for (GearGeo.Extra e : GearGeo.extras().values()) {
                ModelPart parent = switch (e.part()) {
                    case "head" -> head;
                    case "body" -> body;
                    case "right_arm" -> rightArm;
                    case "left_arm" -> leftArm;
                    case "right_leg" -> rightLeg;
                    default -> leftLeg;
                };
                extra.put(e.name(), parent.getChild(e.name()));
                host.put(e.name(), parent);
                boxes.put(e.name(), e.cubes().stream().map(c -> c.box(inner)).toList());
            }
        }

        void set(ItemStack stack, EquipmentSlot slot) {
            ModularDesign design = ModularStacks.design(stack);
            ModularDesign.Part plate = design == null ? null : design.part("P"), studPart = design == null ? null : design.part("T");
            on = GearGeo.wears(slotName(slot), plate == null ? "" : plate.variant());
            fit(design, plate, slot);
            var materials = ClientMaterials.get();
            plateMaterial = plate == null ? null : materials.get(plate.material());
            plateColour = plate == null ? 0xFFB4B4B8 : 0xFF000000 | MaterialColors.of(plate.material());
            studMaterial = studPart == null ? null : materials.get(studPart.material());
            studs = studPart != null && !GearGeo.studs(slotName(slot)).isEmpty()
                    ? 0xFF000000 | MaterialColors.of(studPart.material()) : -1;
            calls = 0;
        }

        /**
         * The plate's drawn axes shape the worn piece: a taller crown, a wider chest, a longer leg stretch the host part
         * (its extras ride along), and a closed face opening or a high boot shaft adds the visor or the cuff.
         */
        private void fit(ModularDesign design, ModularDesign.Part plate, EquipmentSlot slot) {
            for (ModelPart p : new ModelPart[]{head, body, rightLeg, leftLeg})
                p.xScale = p.yScale = p.zScale = 1f;
            var type = design == null ? null : ModularTypes.known(design.type());
            var role = type == null ? null : type.roles().get("P");
            if (plate == null || role == null)
                return;
            switch (slot) {
                case HEAD -> head.yScale = scale(role, plate, "crown");
                case CHEST -> {
                    body.xScale = scale(role, plate, "width");
                    body.yScale = scale(role, plate, "torso_length");
                }
                case LEGS -> {
                    float w = scale(role, plate, "width");
                    for (ModelPart leg : new ModelPart[]{rightLeg, leftLeg}) {
                        leg.xScale = leg.zScale = w;
                        leg.yScale = scale(role, plate, "length");
                    }
                }
                default -> {
                }
            }
            for (GearGeo.Rule rule : GearGeo.rules()) {
                if (!rule.slot().equals(slotName(slot)))
                    continue;
                boolean in = band(role, plate, rule.axis()).equals(rule.band());
                if (in == rule.not())
                    continue;
                List<String> shown = new ArrayList<>(on);
                shown.removeAll(rule.remove());
                for (String n : rule.add())
                    if (!shown.contains(n))
                        shown.add(n);
                on = shown;
            }
        }

        private static String slotName(EquipmentSlot slot) {
            return switch (slot) {
                case HEAD -> "head";
                case CHEST -> "chest";
                case LEGS -> "legs";
                case FEET -> "feet";
                default -> "";
            };
        }

        private static float axisValue(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            return a == null ? 0f : plate.measures().axes().getOrDefault(name, a.def());
        }

        /** 1 at the axis's default, a little over or under at its ends. */
        private static float scale(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            return a == null || a.def() <= 0f ? 1f : Mth.clamp(1f + 0.5f * (axisValue(role, plate, name) - a.def()) / a.def(), 0.85f, 1.25f);
        }

        private static String band(ModularType.Role role, ModularDesign.Part plate, String name) {
            ModularType.Axis a = role.axes().get(name);
            ModularType.Band b = a == null ? null : a.band(axisValue(role, plate, name));
            return b == null ? "" : b.name();
        }

        /**
         * The first pass draws vanilla's cubes, then the extras with the gun sheet. The trim and glint passes that follow
         * draw the extras as vanilla cubes in the armour layer's layout, which the trim sprite and the glint cover too.
         */
        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
            boolean first = calls++ == 0;
            Map<String, Boolean> saved = new HashMap<>();
            for (var e : extra.entrySet()) {
                saved.put(e.getKey(), e.getValue().visible);
                e.getValue().visible = !first && !isStud(e.getKey()) && on.contains(e.getKey());
            }
            super.renderToBuffer(pose, buffer, light, overlay, color);
            for (var e : saved.entrySet())
                extra.get(e.getKey()).visible = e.getValue();
            if (!first)
                return;
            var source = Minecraft.getInstance().renderBuffers().bufferSource();
            for (var e : extra.entrySet()) {
                String name = e.getKey();
                boolean stud = isStud(name);
                if (stud ? studs == -1 || !GearGeo.studs(slotName(currentSlot)).contains(name) : !on.contains(name))
                    continue;
                ModelPart h = host.get(name);
                if (!h.visible)
                    continue;
                pose.pushPose();
                h.translateAndRotate(pose);
                PlateDraw draw = new PlateDraw(pose, source, light, overlay).material(stud ? studMaterial : plateMaterial, stud ? studs : plateColour);
                for (float[] b : boxes.get(name))
                    draw.box(b[0], b[1], b[2], b[3], b[4], b[5]);
                draw.end();
                pose.popPose();
            }
            PlateDraw.flush(source);
        }

        private static boolean isStud(String name) {
            for (String slot : new String[]{"head", "chest"})
                if (GearGeo.studs(slot).contains(name))
                    return true;
            return false;
        }

        EquipmentSlot currentSlot = EquipmentSlot.CHEST;
    }

    private static Gear outer, inner;

    /** Lets a modular armour piece draw as vanilla armour plus its variant's extras. */
    public static final class Extension implements IClientItemExtensions {
        /** The plate's colour: vanilla only reads the dye component off items tagged dyeable, which a modular piece is not. */
        @Override
        public int getDefaultDyeColor(ItemStack stack) {
            var dyed = stack.get(DataComponents.DYED_COLOR);
            return dyed == null ? 0xFFFFFFFF : 0xFF000000 | dyed.rgb();
        }

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
            boolean legs = slot == EquipmentSlot.LEGS;
            if (GearGeo.root(legs) == null)
                return original;
            if (legs ? inner == null || inner.generation != GearGeo.generation() : outer == null || outer.generation != GearGeo.generation()) {
                Gear g = new Gear(GearGeo.root(legs), legs);
                if (legs)
                    inner = g;
                else
                    outer = g;
            }
            Gear gear = legs ? inner : outer;
            gear.currentSlot = slot;
            gear.set(stack, slot);
            return gear;
        }
    }
}
