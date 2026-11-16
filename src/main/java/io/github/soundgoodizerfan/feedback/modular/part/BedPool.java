// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.Edge;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchShaping;
import io.github.soundgoodizerfan.feedback.modular.blueprint.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.Measures;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/**
 * The beds the world makes for itself: a vanilla bed that turns up (a village, a structure, an old world, a command) becomes a framed
 * {@link ModularBed} whose frame and mattress are drawn from the small pools below, so a village's beds come out varied: oak or spruce
 * bedsteads, now and then a bronze or iron one; a wool or coated cloth cover over wool, feather down or straw padding. The draw is a
 * function of the bed's position, so both halves of one bed agree and a reload draws it the same. Players never meet this: theirs are built
 * at the benches from parts. The pools are a worldgen table (identity by necessity, like the vanilla swap table), and an entry the
 * material table lacks is skipped.
 */
public final class BedPool {

    private BedPool() {
    }

    /** Frame materials: mostly woods, a few basic metals. A repeated entry is a likelier draw. */
    private static final List<String> FRAMES = List.of("oak", "oak", "oak", "spruce", "spruce", "birch", "birch", "dark_oak", "jungle", "acacia",
            "bronze", "wrought_iron");

    /** Cover materials: cloth first, a hide now and then. */
    private static final List<String> COVERS = List.of("wool", "wool", "wool", "wool", "coated_cloth", "leather");

    /** Padding materials: the fibrous organic stuff a mattress is stuffed with. */
    private static final List<String> PADDINGS = List.of("wool", "wool", "feather_down", "short_grass", "tall_grass", "rabbit_hide");

    /** The design a bed takes for the cover's colour: the vanilla colour a converted bed was, kept as the cover's dye. */
    public static int dye(DyeColor colour) {
        return colour.getTextureDiffuseColor() & 0xFFFFFF;
    }

    /** The stack of the bed drawn for the head at {@code headSeed} (a position hash) dyed {@code colour}, or empty when the tables cannot draw one yet. */
    public static ItemStack draw(long headSeed, DyeColor colour) {
        Map<ResourceLocation, Material> materials = io.github.soundgoodizerfan.feedback.process.MaterialIngredient.materialsNow();
        Map<ResourceLocation, ModularType> types = ModularTypes.known();
        ResourceLocation id = Feedback.id("bed");
        ModularType type = types.get(id);
        if (type == null || materials == null || materials.isEmpty())
            return ItemStack.EMPTY;
        RandomSource dice = RandomSource.create(headSeed);
        Optional<Part> frame = frame(type, types, materials, dice);
        Optional<Part> mattress = mattress(type, types, materials, dice);
        if (frame.isEmpty() || mattress.isEmpty())
            return ItemStack.EMPTY;
        ModularDesign design = new ModularDesign(id, List.of(mattress.get(), frame.get()), Edge.STANDARD, dye(colour));
        ItemStack out = new ItemStack(FModular.BED.get());
        return ModularStacks.assemble(out, design, materials, types) ? out : ItemStack.EMPTY;
    }

    private static Optional<Part> frame(ModularType type, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials,
                                        RandomSource dice) {
        ModularType.Role role = type.roles().get(ModularBed.FRAME);
        List<PartBenchShaping.Option> shapes = ToolDesigns.shapes(role, types);
        if (shapes.isEmpty())
            return Optional.empty();
        // A bedstead more often than a cot.
        PartBenchShaping.Option shape = shapes.stream().filter(o -> o.variant().name().equals("cot")).findFirst()
                .filter(o -> dice.nextInt(4) == 0).orElse(ToolDesigns.standard(role, types));
        for (ResourceLocation m : shuffled(FRAMES, dice)) {
            Optional<Material.Located> l = ToolDesigns.materials(type, ModularBed.FRAME, shape, materials).stream().filter(c -> c.id().equals(m)).findFirst();
            if (l.isPresent())
                return Optional.of(ToolDesigns.part(ModularBed.FRAME, l.get(), shape, shape.cells()));
        }
        return Optional.empty();
    }

    private static Optional<Part> mattress(ModularType type, Map<ResourceLocation, ModularType> types, Map<ResourceLocation, Material> materials,
                                           RandomSource dice) {
        ModularType.Role role = type.roles().get(ModularBed.MATTRESS);
        List<PartBenchShaping.Option> shapes = ToolDesigns.shapes(role, types);
        if (shapes.isEmpty())
            return Optional.empty();
        // A flat mattress, not a roll: this one is set on a frame.
        PartBenchShaping.Option shape = shapes.stream().filter(o -> o.variant().name().equals("mattress")).findFirst()
                .orElse(shapes.get(shapes.size() - 1));
        Optional<Material.Located> cover = Optional.empty();
        for (ResourceLocation m : shuffled(COVERS, dice)) {
            cover = ToolDesigns.materials(type, ModularBed.MATTRESS, shape, materials).stream().filter(c -> c.id().equals(m)).findFirst();
            if (cover.isPresent())
                break;
        }
        if (cover.isEmpty())
            return Optional.empty();
        Part part = ToolDesigns.part(ModularBed.MATTRESS, cover.get(), shape, shape.cells());
        ModularType.PartSlot padding = shape.shaping().slots().stream().filter(s -> s.name().equals("padding")).findFirst().orElse(null);
        if (padding != null) {
            List<Material.Located> fits = ToolDesigns.slotMaterials(padding, shape.shaping(), materials);
            for (ResourceLocation m : shuffled(PADDINGS, dice)) {
                Optional<Material.Located> l = fits.stream().filter(c -> c.id().equals(m)).findFirst();
                if (l.isPresent()) {
                    part = part.withMeasures(part.measures().withSlots(Map.of("padding", new Measures.Slot(l.get().id(), l.get().form()))));
                    break;
                }
            }
        }
        return Optional.of(part);
    }

    /** The pool's entries in a random order, each id once, first drawn first. */
    private static List<ResourceLocation> shuffled(List<String> pool, RandomSource dice) {
        List<String> left = new ArrayList<>(pool);
        List<ResourceLocation> out = new ArrayList<>();
        while (!left.isEmpty()) {
            String pick = left.remove(dice.nextInt(left.size()));
            left.removeIf(pick::equals);
            out.add(Feedback.id(pick));
        }
        return out;
    }
}
