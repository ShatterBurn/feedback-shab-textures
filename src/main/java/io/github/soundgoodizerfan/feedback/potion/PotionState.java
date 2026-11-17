// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * What a bottle of potion carries (potions spec 4): the effect the brew landed on, its level and length, and what vanilla's potion contents
 * cannot hold, a potency that fades on the food lazy clock ({@code Cooking.current}'s pattern: stamped, settled on read, nothing ticked).
 * A fade weakens the potion and never shortens it. No effect is a dud draught.
 *
 * @param effect    the mob effect, or empty for a dud draught.
 * @param amplifier the level when fresh (0 is I).
 * @param duration  ticks; fading does not touch it.
 * @param potency   0..1 of its strength left.
 * @param solvent   the base liquid it is carried in.
 * @param taste     three figures (sweet, bitter, pungent), the strength-weighted mean of what went in (spec 7).
 * @param stamp     the game tick {@code potency} was true on.
 * @param envTu     the temperature around it at the stamp.
 * @param tint      a dud draught's colour (24-bit RGB): the water's own, or that of the compound class the nearest unmade node leans on; -1 for a real potion, which keeps its look.
 */
public record PotionState(Optional<ResourceLocation> effect, int amplifier, int duration, float potency, String solvent, List<Float> taste, long stamp, float envTu, int tint) {

    public static final Codec<PotionState> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("effect").forGetter(PotionState::effect),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(PotionState::amplifier),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(PotionState::duration),
            Codec.FLOAT.optionalFieldOf("potency", 1f).forGetter(PotionState::potency),
            Codec.STRING.optionalFieldOf("solvent", "water").forGetter(PotionState::solvent),
            Codec.FLOAT.listOf().optionalFieldOf("taste", List.of(0f, 0f, 0f)).forGetter(PotionState::taste),
            Codec.LONG.optionalFieldOf("stamp", 0L).forGetter(PotionState::stamp),
            Codec.FLOAT.optionalFieldOf("env", 20f).forGetter(PotionState::envTu),
            Codec.INT.optionalFieldOf("tint", -1).forGetter(PotionState::tint)
    ).apply(i, PotionState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotionState> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public boolean dud() {
        return effect.isEmpty();
    }

    /**
     * The potency now: halved every {@link PotionTuning#HALF_LIFE_TICKS} at room temperature, faster warmer and slower colder, by the mean of
     * the surroundings then and now. Never mutates; a stamp of 0 has had no time pass.
     */
    public float potencyNow(Level level) {
        return potencyAt(level.getGameTime(), AmbientTable.at(level).value());
    }

    /** The potency on game tick {@code now} with {@code ambientTu} around it: the same fade as {@link #potencyNow}, without a level. */
    public float potencyAt(long now, float ambientTu) {
        if (stamp <= 0L || now <= stamp)
            return potency;
        float ambient = ThermalTuning.AMBIENT_TU.value();
        float heat = Mth.clamp((envTu + ambientTu) * 0.5f / ambient, 0f, 3f);
        return potency * (float) Math.pow(0.5, (now - stamp) * heat / PotionTuning.HALF_LIFE_TICKS);
    }

    /** Whether two states are the same brew (effect, level, length, solvent and a dud's tint), so they can share a tank whatever their potency. */
    public boolean sameBrew(PotionState o) {
        return effect.equals(o.effect) && amplifier == o.amplifier && duration == o.duration && solvent.equals(o.solvent) && tint == o.tint;
    }

    /**
     * {@code amountA} mB of this and {@code amountB} mB of {@code o} run together: both settled to the later stamp (each fading by its own
     * surroundings; the later one's stands for the air round both), then the amount-weighted mean of potency, taste and temperature. Only
     * the same brew merges ({@link #sameBrew}); callers check first. No level needed, so a tank's fill can do it.
     */
    public PotionState merge(int amountA, PotionState o, int amountB) {
        long now = Math.max(stamp, o.stamp);
        float ambient = o == this || o.stamp >= stamp ? o.envTu : envTu;
        float pa = potencyAt(now, ambient), pb = o.potencyAt(now, ambient);
        float total = Math.max(1, amountA + amountB), wa = amountA / total, wb = amountB / total;
        List<Float> mixed = List.of(taste.get(0) * wa + o.taste.get(0) * wb, taste.get(1) * wa + o.taste.get(1) * wb, taste.get(2) * wa + o.taste.get(2) * wb);
        return new PotionState(effect, amplifier, duration, pa * wa + pb * wb, solvent, mixed, now, envTu * wa + o.envTu * wb, tint);
    }

    /** The level it gives now: potency scales the fresh level down, and under {@link PotionTuning#MIN_POTENCY} it gives nothing (-1). */
    public int amplifierNow(Level level) {
        float p = potencyNow(level);
        return p < PotionTuning.MIN_POTENCY ? -1 : Math.round(amplifier * p);
    }
}
