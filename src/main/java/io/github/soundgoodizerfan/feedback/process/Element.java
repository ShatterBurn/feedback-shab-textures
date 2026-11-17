// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * What a finished amethyst resonator or sculk lattice is to a board: its kind, the character its build gave it and its fixed figures
 * ({@link Circuit.Line}), baked once from the parts and their materials ({@code modular/bake/ElementDesign}) and kept on the stack as the
 * {@code ELEMENT} component, so mounting needs no material lookup. A living part of fixed figures: nothing drifts or grows.
 *
 * @param kind      {@link Board.Kind#RESONATOR} or {@link Board.Kind#LATTICE}
 * @param character computed from the build, never chosen
 */
public record Element(Board.Kind kind, Character character, Circuit.Line line) {

    /** What the build makes of it, in a word. */
    public enum Character implements StringRepresentable {
        /** A resonator cut for steadiness: few channels. */
        CLOCK("clock"),
        /** A resonator between. */
        BAND("band"),
        /** A resonator cut narrow: many channels, less bandwidth. */
        FILTER("filter"),
        /** A lattice that mostly holds. */
        STORE("store"),
        /** A lattice between. */
        MIXED("mixed"),
        /** A lattice that mostly learns. */
        LEARNER("learner");

        public static final Codec<Character> CODEC = StringRepresentable.fromEnum(Character::values);
        private final String key;

        Character(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "feedback.element.character." + key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final Codec<Element> CODEC = RecordCodecBuilder.create(i -> i.group(
            Board.Kind.CODEC.fieldOf("kind").forGetter(Element::kind),
            Character.CODEC.fieldOf("character").forGetter(Element::character),
            Circuit.Line.CODEC.fieldOf("line").forGetter(Element::line)).apply(i, Element::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Element> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
