// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;

/**
 * The physics of a spring-piston air gun, read from its design. A spring stores energy (its volume times its stock's yield
 * strength); the piston and barrel hand it to a pellet: the push reaching the pellet is what a barrel of that length and bore
 * keeps of it, and the pellet shares it with the piston's moving mass. The pellet is a plug of the bore, cut from a nugget of any
 * metal: its mass is its volume times that metal's density, never a named item. The receiver is the mount the spring is rated
 * against ({@link SpringSet}).
 *
 * @param energy     work the spring stores, before any set.
 * @param pistonMass kilograms the piston moves.
 * @param boreMm     the bore, millimetres.
 * @param lengthM    the barrel's length, metres.
 * @param rating     work the receiver holds before the spring takes a set.
 * @param cock       multiple of the cocking time (a mainspring attuned to release lets go sooner).
 */
public record LauncherSpec(float energy, float pistonMass, float boreMm, float lengthM, float rating, float cock, Seat seat) {

    /** What stores the work: a spring cocked by hand, a gas reservoir pumped up, a solenoid fed by a cell, a boiler heated. */
    public enum Seat {
        SPRING, RESERVOIR, SOLENOID, STEAM;

        public static Seat of(String variant) {
            return switch (variant) {
                case "reservoir_seat" -> RESERVOIR;
                case "solenoid_seat" -> SOLENOID;
                case "steam_seat" -> STEAM;
                default -> SPRING;
            };
        }
    }

    /** Share of the stored energy a level of store adds to the mainspring; share of the cocking time a level of release cuts, and the least left. [OPEN] until played */
    private static final float STORE_PER_LEVEL = 0.12f, RELEASE_PER_LEVEL = 0.12f, COCK_FLOOR = 0.4f;

    public static final String SPRING = "launcher_seat", PISTON = "piston", BARREL = "barrel", RECEIVER = "receiver";

    /**
     * What one shot is.
     *
     * @param speed  blocks per tick at the muzzle.
     * @param work the pellet's kinetic energy.
     * @param damage the hit it deals at the muzzle.
     * @param mass   the pellet's kilograms.
     */
    public record Shot(float speed, float work, float damage, float mass) {
    }

    /** The spec of {@code design}, or empty if it lacks a spring, a piston, a barrel or a receiver, or a material is unknown. */
    public static Optional<LauncherSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        float energy = 0, rating = 0, piston = 0, bore = 0, length = 0;
        boolean spring = false, pist = false, barrel = false, receiver = false;
        Seat seat = Seat.SPRING;
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                return Optional.empty();
            switch (role.name()) {
                case SPRING -> {
                    seat = Seat.of(role.variant(p).map(ModularType.Variant::name).orElse("spring_seat"));
                    float perLs = switch (seat) {
                        case RESERVOIR -> ModularTuning.SEAT_RES_J_PER_LS;
                        case SOLENOID -> ModularTuning.SEAT_SOL_J_PER_LS;
                        case STEAM -> ModularTuning.SEAT_STEAM_J_PER_LS;
                        default -> ModularTuning.AIR_JOULES_PER_LS;
                    };
                    energy += perLs * role.volume(p) * ModularBake.strength(m);
                    spring = true;
                }
                case PISTON -> {
                    piston += ModularBake.mass(role, p, m);
                    pist = true;
                }
                case BARREL -> {
                    bore = role.axis(p, "bore");
                    length = role.axis(p, "barrel_length");
                    barrel = true;
                }
                case RECEIVER -> {
                    rating += ModularTuning.AIR_RATING_PER_LS * role.volume(p) * ModularBake.strength(m);
                    receiver = true;
                }
                default -> { }
            }
        }
        // The mainspring's attunements: store packs more energy into the same spring, release lets go sooner.
        int store = 0, release = 0;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(design))
            if (a.kind().equals("store"))
                store += a.level();
            else if (a.kind().equals("release"))
                release += a.level();
        energy *= 1f + STORE_PER_LEVEL * store;
        float cock = Math.max(COCK_FLOOR, 1f - RELEASE_PER_LEVEL * release);
        return spring && pist && barrel && receiver && bore > 0 && length > 0
                ? Optional.of(new LauncherSpec(energy, piston, bore, length, rating, cock, seat)) : Optional.empty();
    }

    /** Share of the spring's energy that reaches the pellet and piston: a longer barrel keeps the push, a wider bore wastes it. */
    public float efficiency() {
        float len = lengthM / (lengthM + ModularTuning.AIR_LENGTH_HALF);
        float wide = boreMm / ModularTuning.AIR_BORE_HALF;
        return ModularTuning.AIR_ENVELOPE * len / (1f + wide * wide);
    }

    /** Litres of metal in one pellet of this bore. */
    public float pelletLitres() {
        float r = boreMm / 2f;
        return ModularTuning.AIR_PELLET_LENGTHS * (float) Math.PI * r * r * boreMm * 1e-6f;
    }

    /** How many pellets of this bore one nugget is worth. */
    public int perNugget() {
        return Math.max(1, (int) (ModularTuning.AIR_NUGGET_LITRES / pelletLitres()));
    }

    /** The shot of a pellet of {@code density} (kilograms per litre) from the spring with {@code set} of its energy lost. */
    public Shot shot(float density, float set) {
        float mass = pelletLitres() * density;
        float moving = mass + ModularTuning.AIR_PISTON_SHARE * pistonMass;
        float speed = (float) Math.sqrt(2f * energy * (1f - set) * efficiency() / Math.max(1e-6f, moving)) / FTuning.TICKS_PER_SECOND;
        speed = Math.min(speed, ModularTuning.AIR_SPEED_MAX);
        float work = 0.5f * mass * (speed * FTuning.TICKS_PER_SECOND) * (speed * FTuning.TICKS_PER_SECOND);
        return new Shot(speed, work, work * ModularTuning.AIR_DAMAGE_EFFICIENCY / io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning.WORK_PER_DAMAGE, mass);
    }

    /** Ticks a wielder of strength 1 takes to cock the spring. */
    public float cockTicks() {
        return cock * Math.min(ModularTuning.AIR_COCK_MAX_TICKS, Math.max(ModularTuning.AIR_COCK_MIN_TICKS,
                energy / ModularTuning.AIR_COCK_J_PER_TICK));
    }

    /** The work that pushes a rocket of {@code shellKg} out of the tube at {@code blocksPerTick}: the tube only ejects it, the motor does the rest. */
    public float ejectWork(float shellKg, float blocksPerTick) {
        float moving = shellKg + ModularTuning.AIR_PISTON_SHARE * pistonMass;
        float mps = blocksPerTick * FTuning.TICKS_PER_SECOND;
        return 0.5f * moving * mps * mps / Math.max(1e-3f, efficiency());
    }

    /** The bore as the whole millimetre a round's die is set to. */
    public int boreInt() {
        return Math.round(boreMm);
    }

    /**
     * The gun figures of a shot of a shell weighing {@code shellKg} (the round's own projectile mass), with {@code available} work in the seat, a wear {@code condition} and the gun's own mass {@code gunKg}. The shell's speed is the seat's work shared with the
     * piston ({@link #shot}); the figures are what the shared gun code reads for kick, spread, sound and the swept flight.
     */
    public io.github.soundgoodizerfan.feedback.weapon.GunFigures figures(float shellKg, float available, float gunKg, float condition) {
        float moving = shellKg + ModularTuning.AIR_PISTON_SHARE * pistonMass;
        float wear = 1f - io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_WEAR_ENERGY_LOSS * (1f - condition)
                / (1f - io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_CONDITION_FLOOR);
        float speed = (float) Math.sqrt(2f * available * efficiency() * wear / Math.max(1e-6f, moving)) / FTuning.TICKS_PER_SECOND;
        speed = Math.min(speed, ModularTuning.AIR_SPEED_MAX);
        float mps = speed * FTuning.TICKS_PER_SECOND;
        float work = 0.5f * shellKg * mps * mps;
        float impulse = shellKg * mps;
        float spread = ModularTuning.AIR_SPREAD * ModularTuning.AIR_LENGTH_HALF / Math.max(0.05f, lengthM) / Math.max(0.1f, condition);
        float kick = io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_KICK_DEG_PER_IMPULSE * impulse
                * (io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_KICK_REFERENCE_MU / Math.max(0.05f, gunKg));
        return new io.github.soundgoodizerfan.feedback.weapon.GunFigures(new io.github.soundgoodizerfan.feedback.core.unit.Pu(0f),
                new io.github.soundgoodizerfan.feedback.core.unit.Pu(0f), speed, work,
                new io.github.soundgoodizerfan.feedback.core.unit.Mu(shellKg), new io.github.soundgoodizerfan.feedback.core.unit.Mu(gunKg),
                impulse, ModularTuning.AIR_SHOT_COOLDOWN * 50, Math.max(io.github.soundgoodizerfan.feedback.weapon.GunTuning.GUN_SPREAD_FLOOR_DEG, spread),
                kick, ModularTuning.AIR_VOLUME, 0f, 1f);
    }

    /** The spring's load over what the receiver holds: above 1 each cocking sets it, far above it snaps ({@link SpringSet}). */
    public float load(float set) {
        return rating > 0 ? energy * (1f - set) / rating : 0f;
    }
}
