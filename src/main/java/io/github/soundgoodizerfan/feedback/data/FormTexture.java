// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.data;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.resources.ResourceLocation;

/**
 * Which sprite (and render type) a generated form block is drawn with, chosen by what the material is.
 * One file per rule under {@code data/feedback/form_texture/}: a {@code form}, an optional {@code kind}
 * and an optional {@code trait} that the material must have, the {@code sprite} to draw and an optional
 * {@code render_type}. Of the rules that match, the highest {@code priority} wins (ties by file name);
 * a form block no rule matches keeps the plain tinted cube. Read at datagen only: the chosen sprite is
 * baked into the generated model, so nothing here exists at runtime. A new look for a class of
 * material is one rule file and one sprite, never a branch in code.
 *
 * @param form       the form id the rule applies to ({@code block}).
 * @param kind       the material kind it needs, if any.
 * @param trait      the material trait it needs, if any.
 * @param sprite     the greyscale sprite, tinted per material by {@code client/FormTint}.
 * @param renderType the model's render type ({@code translucent}, {@code cutout}), if not solid.
 * @param priority   higher wins where several rules match.
 */
public record FormTexture(String form, Optional<Material.Kind> kind, Optional<Material.Trait> trait,
                          ResourceLocation sprite, Optional<String> renderType, int priority) {

    public static final Codec<FormTexture> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("form").forGetter(FormTexture::form),
            Material.Kind.CODEC.optionalFieldOf("kind").forGetter(FormTexture::kind),
            Material.Trait.CODEC.optionalFieldOf("trait").forGetter(FormTexture::trait),
            ResourceLocation.CODEC.fieldOf("sprite").forGetter(FormTexture::sprite),
            Codec.STRING.optionalFieldOf("render_type").forGetter(FormTexture::renderType),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(FormTexture::priority)).apply(i, FormTexture::new));

    private static Map<ResourceLocation, FormTexture> rules;

    private static Map<ResourceLocation, FormTexture> rules() {
        if (rules == null) {
            Map<ResourceLocation, FormTexture> out = new TreeMap<>();
            FormItems.jarJson("form_texture").forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> Feedback.LOGGER.error("Bad form_texture {}: {}", id, error))
                    .ifPresent(rule -> out.put(id, rule)));
            rules = out;
        }
        return rules;
    }

    /** The rule that decides how {@code form} of {@code material} is drawn, or empty for the plain cube. */
    public static Optional<FormTexture> find(Material material, String form) {
        return rules().values().stream()
                .filter(r -> r.form.equals(form)
                        && (r.kind.isEmpty() || material.kind().equals(r.kind))
                        && (r.trait.isEmpty() || material.traits().contains(r.trait.get())))
                .max(Comparator.comparingInt(FormTexture::priority));
    }
}
