// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

// Design from GregTech CEu Modern's GTOreDefinition, trimmed; see THIRD-PARTY-LICENSES.md.
/**
 * One deposit: where an ore occurs and what a vein of it looks like (mechanics §9.2). Loaded from
 * {@code data/<ns>/ore_vein/*.json} by {@link OreVeinTable}; placed by {@link OreVeinFeature}.
 *
 * <p>Every number in a deposit file is Invented (placeholder), though the vanilla
 * deposits' files start from vanilla's own height window and a weight that keeps vanilla's relative abundance.
 *
 * <p>This table is the single source of truth for where and how often: the one placed feature
 * ({@code worldgen/placed_feature/ore_vein.json}) has no placement modifiers, and
 * {@link OreVeinFeature} fills every region with veins drawn by {@code weight}, each at its own
 * height. Several files may share an ore (a coal seam near the surface and another deep down).
 *
 * <p><b>Adding an ore</b> (mechanics §9.6):
 * <ol>
 * <li>Register {@code <name>_ore} and {@code deepslate_<name>_ore} blocks in {@code FBlocks}
 * (the {@code ore(...)} helper), their block items in {@code FItems}, and the creative tab entry.</li>
 * <li>Add blockstate, block model and item model, lang names, and a loot table for each block
 * under {@code data/feedback/loot_table/blocks/}.</li>
 * <li>Add both blocks to {@code FormData.BlockTagsGen} (pickaxe, and a {@code needs_*_tool} tag
 * matching the tier), then run {@code runData}; add the vanilla ore tags
 * ({@code minecraft:<name>_ores}, block and item) so other mods and recipes see them.</li>
 * <li>Add {@code data/feedback/ore_vein/<name>.json} (one per height band) and point the
 * material's {@code forms.ore} / {@code forms.deepslate_ore} at the new blocks.</li>
 * <li>To retire a vanilla ore, list its placed features in
 * {@code neoforge/biome_modifier/remove_vanilla_ores.json}.</li>
 * </ol>
 *
 * <p><b>Tag fields are plain ids</b> ({@code "minecraft:is_badlands"}, never {@code "#..."}): the
 * codec is {@code TagKey.codec}, which rejects the hash. <b>One-block ores</b> (the Nether's) name
 * the same block as {@code ore} and {@code deepslate_ore} and set {@code host} to a tag of their
 * stone; the deepslate host never matches there, so the second block is never placed.
 *
 * @param ore          block placed where the host is a stone.
 * @param deepslateOre block placed where the host is a deepslate.
 * @param host         what {@code ore} may replace.
 * @param deepslateHost what {@code deepslateOre} may replace.
 * @param radius       horizontal reach of the vein (blocks), varied a fifth either way per vein.
 * @param thickness    vertical reach of the vein (blocks): veins are flat.
 * @param weight       share of the dimension's vein slots this deposit takes; its rarity. In a file it is an optional
 *                     override: absent, {@link OreVeinTable} derives it from the ore's material ({@code abundance}) times
 *                     {@code share}; a loaded or synced vein always carries the figure it is drawn by.
 * @param density     chance a block in the vein becomes ore at its heart, 0 to 1 (half that at the rim): a lean or rich vein.
 * @param minY         lowest Y a vein may be centred at.
 * @param maxY         highest Y a vein may be centred at.
 * @param triangle     whether Y is drawn triangular (most likely mid-window) rather than uniform.
 * @param airDiscard   chance a block touching air is left unmined stone, 0 to 1: a buried deposit
 *                     shows less in cave walls.
 * @param dimensions   dimensions it occurs in.
 * @param indicators   the signs a vein that comes near enough to the surface shows (mechanics §9.5), so it
 *                     can be found by walking: one id or a list in the file's {@code indicator} key, empty
 *                     means none. Each of the three spots draws one of them. How a block is laid follows from
 *                     what it is: a block in {@code feedback:ore_stains} replaces the dirt in a patch, a small
 *                     flower is planted in a patch where it can stand, anything else (a loose rock) is set
 *                     down as a single heap. Any block id, so a pack may name its own.
 * @param layers       see {@link Layers}: a cap, a rim and scattered ores beside the core.
 * @param indicatorDepth how far below the surface (blocks) a vein's centre may be and still show an outcrop.
 * @param id           the file's id, set by {@link OreVeinTable}; what a {@link Companion} names.
 * @param material     the material this deposit yields, for its abundance; absent, the material whose {@code forms.ore} is
 *                     this deposit's {@code ore} (a mineral whose ore block is no material's form names it here).
 * @param share        the part of its material's abundance this deposit takes (a metal with two deposits splits it);
 *                     scales the derived weight, unused when a file sets {@code weight}.
 * @param province     the name of the province this deposit belongs to, if any: deposits of one province are
 *                     rich or barren together across the world ({@link Provinces}).
 * @param biomeWeights what each biome does to the deposit's weight: the factors of every tag the biome at the vein's
 *                     centre is in multiply; a factor of 0 keeps the deposit out.
 * @param biomeDefault the factor where the centre's biome is in none of the listed tags (1 when none are listed, which
 *                     leaves the deposit everywhere; 0 makes the listed tags the only places it occurs).
 * @param companions   deposits that tend to lie beside this one, drawn from the same slot.
 */
public record OreVein(ResourceLocation ore, ResourceLocation deepslateOre, TagKey<Block> host,
                      TagKey<Block> deepslateHost, int radius, int thickness, int weight, float density,
                      int minY, int maxY, boolean triangle, float airDiscard, List<ResourceLocation> dimensions,
                      List<ResourceLocation> indicators, int indicatorDepth, Layers layers,
                      Optional<ResourceLocation> id, Optional<ResourceLocation> material, float share,
                      Optional<String> province, List<BiomeWeight> biomeWeights, float biomeDefault,
                      List<Companion> companions) {

    /** Widest a vein may be; a region is wider, which is what lets a chunk look only at its neighbouring regions. */
    public static final int MAX_RADIUS = 20;

    private static final TagKey<Block> STONE_HOST = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("stone_ore_replaceables"));
    private static final TagKey<Block> DEEPSLATE_HOST = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("deepslate_ore_replaceables"));

    public static final Codec<OreVein> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("ore").forGetter(OreVein::ore),
            ResourceLocation.CODEC.fieldOf("deepslate_ore").forGetter(OreVein::deepslateOre),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("host", STONE_HOST).forGetter(OreVein::host),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("deepslate_host", DEEPSLATE_HOST)
                    .forGetter(OreVein::deepslateHost),
            Codec.intRange(1, MAX_RADIUS).fieldOf("radius").forGetter(OreVein::radius),
            Codec.intRange(1, 16).fieldOf("thickness").forGetter(OreVein::thickness),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("weight", 0).forGetter(OreVein::weight),
            Codec.floatRange(0f, 1f).fieldOf("density").forGetter(OreVein::density),
            Codec.INT.fieldOf("min_y").forGetter(OreVein::minY),
            Codec.INT.fieldOf("max_y").forGetter(OreVein::maxY),
            Codec.BOOL.optionalFieldOf("triangle", false).forGetter(OreVein::triangle),
            Codec.floatRange(0f, 1f).optionalFieldOf("air_discard", 0.8f).forGetter(OreVein::airDiscard),
            ResourceLocation.CODEC.listOf().optionalFieldOf("dimension_filter",
                    List.of(ResourceLocation.withDefaultNamespace("overworld"))).forGetter(OreVein::dimensions),
            Tail.CODEC.forGetter(v -> new Tail(v.indicators(), v.indicatorDepth(), v.layers())),
            Placement.CODEC.forGetter(v -> new Placement(v.id(), v.material(), v.share(), v.province(), v.biomeWeights(),
                    v.biomeDefault(), v.companions()))
    ).apply(instance, (ore, deepslateOre, host, deepslateHost, radius, thickness, weight, density, minY, maxY, triangle,
                       airDiscard, dimensions, tail, placement) -> new OreVein(ore, deepslateOre, host, deepslateHost, radius,
            thickness, weight, density, minY, maxY, triangle, airDiscard, dimensions, tail.indicators(),
            tail.indicatorDepth(), tail.layers(), placement.id(), placement.material(), placement.share(),
            placement.province(), placement.biomeWeights(), placement.biomeDefault(), placement.companions())));

    /** What one biome tag does to a deposit's weight. */
    public record BiomeWeight(TagKey<Biome> biome, float factor) {
        public static final Codec<BiomeWeight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TagKey.codec(Registries.BIOME).fieldOf("biome").forGetter(BiomeWeight::biome),
                Codec.floatRange(0f, 100f).fieldOf("factor").forGetter(BiomeWeight::factor)
        ).apply(instance, BiomeWeight::new));
    }

    /**
     * A deposit that tends to lie beside another (galena with sphalerite and pyrite). Rolled from the parent's slot
     * hash, so every chunk agrees; it does not take a slot of the dimension's budget. The offset and size are capped so a
     * companion stays within the reach {@link #MAX_RADIUS} lets a chunk look.
     *
     * @param vein      the companion deposit's file id.
     * @param chance    chance the parent has this companion, 0 to 1.
     * @param minOffset nearest its centre lies to the parent's, in blocks.
     * @param maxOffset farthest, at most {@link #MAX_OFFSET}.
     * @param sizeScale how large it is next to its own file's radius, 0.2 to 1.
     */
    public record Companion(ResourceLocation vein, float chance, int minOffset, int maxOffset, float sizeScale) {
        public static final Codec<Companion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("vein").forGetter(Companion::vein),
                Codec.floatRange(0f, 1f).fieldOf("chance").forGetter(Companion::chance),
                Codec.intRange(0, MAX_OFFSET).optionalFieldOf("min_offset", 4).forGetter(Companion::minOffset),
                Codec.intRange(0, MAX_OFFSET).optionalFieldOf("max_offset", 14).forGetter(Companion::maxOffset),
                Codec.floatRange(0.2f, 1f).optionalFieldOf("size_scale", 0.7f).forGetter(Companion::sizeScale)
        ).apply(instance, Companion::new));
    }

    /** Farthest a companion's centre may lie from its parent's; with {@link #MAX_RADIUS} it stays under a region's width. */
    public static final int MAX_OFFSET = 20;

    /** Keys that decide how often and where a deposit is drawn; see the record's parameters. */
    private record Placement(Optional<ResourceLocation> id, Optional<ResourceLocation> material, float share,
                             Optional<String> province, List<BiomeWeight> biomeWeights, float biomeDefault,
                             List<Companion> companions) {
        static final com.mojang.serialization.MapCodec<Placement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("id").forGetter(Placement::id),
                ResourceLocation.CODEC.optionalFieldOf("material").forGetter(Placement::material),
                Codec.floatRange(0f, 100f).optionalFieldOf("share", 1f).forGetter(Placement::share),
                Codec.STRING.optionalFieldOf("province").forGetter(Placement::province),
                BiomeWeight.CODEC.listOf().optionalFieldOf("biome_weights", List.of()).forGetter(Placement::biomeWeights),
                Codec.floatRange(0f, 100f).optionalFieldOf("biome_default", 1f).forGetter(Placement::biomeDefault),
                Companion.CODEC.listOf().optionalFieldOf("companions", List.of()).forGetter(Placement::companions)
        ).apply(instance, Placement::new));
    }

    /** This deposit with the id its file gave it and the weight it is drawn by. */
    public OreVein resolved(ResourceLocation fileId, int drawWeight) {
        return new OreVein(ore, deepslateOre, host, deepslateHost, radius, thickness, drawWeight, density, minY, maxY,
                triangle, airDiscard, dimensions, indicators, indicatorDepth, layers, Optional.of(fileId), material,
                share, province, biomeWeights, biomeDefault, companions);
    }

    /** One more ore in a vein: {@code ore} where the host is a stone, {@code deepslate_ore} where it is a deepslate. */
    public record Zone(ResourceLocation ore, ResourceLocation deepslateOre, float density) {
        public static final Codec<Zone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("ore").forGetter(Zone::ore),
                ResourceLocation.CODEC.optionalFieldOf("deepslate_ore").forGetter(z -> Optional.of(z.deepslateOre())),
                Codec.floatRange(0f, 1f).fieldOf("density").forGetter(Zone::density)
        ).apply(instance, (ore, deepslate, density) -> new Zone(ore, deepslate.orElse(ore), density)));
    }

    /**
     * How a vein is laid out. The plain {@code ore} is its <em>core</em>. A {@code cap} replaces it in
     * the top of the vein (an oxidised cap over a deeper body: the weathered ores of a sulphide vein),
     * a {@code rim} in its outer shell (the halo), and each {@code scatter} ore sprinkles through the
     * whole of it at its own density. All optional; a vein with none is one ore throughout.
     */
    public record Layers(Optional<Zone> cap, Optional<Zone> rim, List<Zone> scatter) {
        public static final Layers NONE = new Layers(Optional.empty(), Optional.empty(), List.of());
    }

    /** The keys that did not fit the codec's sixteen. */
    private record Tail(List<ResourceLocation> indicators, int indicatorDepth, Layers layers) {
        static final com.mojang.serialization.MapCodec<Tail> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.withAlternative(ResourceLocation.CODEC.listOf(), ResourceLocation.CODEC, List::of).optionalFieldOf("indicator", List.of())
                        .forGetter(Tail::indicators),
                Codec.INT.optionalFieldOf("indicator_depth", 24).forGetter(Tail::indicatorDepth),
                Zone.CODEC.optionalFieldOf("cap").forGetter(t -> t.layers().cap()),
                Zone.CODEC.optionalFieldOf("rim").forGetter(t -> t.layers().rim()),
                Zone.CODEC.listOf().optionalFieldOf("scatter", List.of()).forGetter(t -> t.layers().scatter())
        ).apply(instance, (indicator, depth, cap, rim, scatter) -> new Tail(indicator, depth, new Layers(cap, rim, scatter))));
    }
}
