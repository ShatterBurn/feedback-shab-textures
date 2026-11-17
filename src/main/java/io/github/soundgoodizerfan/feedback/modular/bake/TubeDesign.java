// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.bake;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import io.github.soundgoodizerfan.feedback.process.Tube;

import net.minecraft.resources.ResourceLocation;

/**
 * What a vacuum tube design is: envelope {@code E}, cathode {@code C}, plate {@code P} and optional grids {@code G}, read off the
 * built geometry and the materials, never an id. Everything the seal does not decide is here ({@link Build}); the seal ({@link #seal}) adds
 * the vacuum a vacuum dryer reached and gives the tube the figures a board carries ({@link Tube}).
 *
 * <p>Prior art: PowerGrid (Apache-2.0, design only). Taken: a tube's emission depends on how hot its heater runs, heater power derives from
 * the emitter's size, and the character (diode, triode, pentode, thyratron) is computed from the geometry and fill. Rejected: typed
 * continuous component knobs (an axis here is a measurement set when the part is cut), the MNA solver, overheat destroying the part,
 * id-keyed assembly, voltage and watt displays, assets.
 *
 * <h2>Where the figures come from</h2>
 * <ul>
 * <li><b>Cathode metal</b> sets emission against warm-up and power: its work function (from its electrode potential,
 * {@link ProcessTuning#TUBE_WORK_BASE}) says what temperature it needs, its melting point how hot it may be held. A metal that cannot be
 * held at the temperature it needs emits almost nothing (copper); a refractory metal emits fully, and a low work function needs a cooler
 * heater. The emitter's length (an axis) sets emission current and heater power together.</li>
 * <li><b>Grid count</b> (an axis of the grid part, none to three) and pitch set the stage gain; the grid metal's conductivity how well the
 * grid holds its potential.</li>
 * <li><b>Plate area</b> (an axis) trades power handling for speed; the plate metal's thermal conductivity says how well it sheds the heat
 * (a poor one slows the tube).</li>
 * <li><b>Envelope</b>: its tightness (its corrosion resistance) caps the vacuum a seal can reach, its service temperature caps the bake and
 * the warm bulb, its diameter (an axis) how fast the bulb warms.</li>
 * </ul>
 */
public final class TubeDesign {

    private TubeDesign() {
    }

    /** The role letters of a tube's parts. */
    public static final String ENVELOPE = "E", CATHODE = "C", GRID = "G", PLATE = "P";

    /**
     * A tube before it is sealed.
     *
     * @param grids       grids in the geometry, 0 to 3
     * @param emission    0-1, the cathode's emission at its working temperature
     * @param gain        stage gain at a perfect vacuum, before the character's share
     * @param speed       0-1, at a perfect vacuum, before the character's share
     * @param power       heater power, Eu a tick
     * @param warmTu      bulb temperature at which the cathode works
     * @param warmRate    Tu a tick the heater raises the bulb at full power
     * @param tightness   0-1, the envelope's cap on the vacuum a seal holds
     * @param serviceTu   the hottest the envelope runs before it slumps
     */
    public record Build(int grids, float emission, float gain, float speed, float power, float warmTu, float warmRate,
                        float tightness, float serviceTu) {
    }

    /** Whether {@code material} can do the job of the role (the bench offers only what bakes): an envelope insulates and states a service temperature; a cathode states a melting point and electrode potential. */
    public static boolean suits(String role, Material material) {
        return switch (role) {
            case ENVELOPE -> material.electrical().insulation().isPresent() && material.thermal().serviceTemperature().isPresent()
                    && material.corrosionResistance().isPresent();
            case CATHODE -> material.thermal().meltingPoint().isPresent() && material.electrical().electrodePotential().isPresent();
            case GRID -> material.electrical().conductivity().isPresent();
            case PLATE -> material.thermal().conductivity().isPresent();
            default -> true;
        };
    }

    /** The tube a design is before its seal, or empty if a part is missing or made of something its role cannot use. */
    public static Optional<Build> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part envelope = design.part(ENVELOPE), cathode = design.part(CATHODE), plate = design.part(PLATE), grid = design.part(GRID);
        if (envelope == null || cathode == null || plate == null)
            return Optional.empty();
        Material em = materials.get(envelope.material()), cm = materials.get(cathode.material()), pm = materials.get(plate.material());
        Material gm = grid == null ? null : materials.get(grid.material());
        if (em == null || cm == null || pm == null || (grid != null && gm == null))
            return Optional.empty();
        if (!suits(ENVELOPE, em) || !suits(CATHODE, cm) || !suits(PLATE, pm) || (gm != null && !suits(GRID, gm)))
            return Optional.empty();

        // --- the cathode: what it needs against what it can be held at ---
        float volts = cm.electrical().electrodePotential().get();
        float work = Math.max(ProcessTuning.TUBE_WORK_MIN, Math.min(ProcessTuning.TUBE_WORK_MAX,
                ProcessTuning.TUBE_WORK_BASE + ProcessTuning.TUBE_WORK_PER_VOLT * volts));
        float neededK = work * ProcessTuning.TUBE_K_PER_EV;
        float meltK = cm.thermal().meltingPoint().get().value() + ProcessTuning.TUBE_KELVIN;
        float heldK = Math.min(neededK, ProcessTuning.TUBE_MELT_SHARE * meltK);
        float emission = (float) Math.min(1.0, Math.exp(ProcessTuning.TUBE_EMISSION_SLOPE * (1f - neededK / heldK)));
        float emitterMm = type.roles().get(CATHODE).axis(cathode, "emitter");
        float emitter = emitterMm / ProcessTuning.TUBE_EMITTER_REF_MM;
        float power = ProcessTuning.TUBE_HEATER_EU * emitter * (heldK / ProcessTuning.TUBE_CATHODE_REF_K) * (heldK / ProcessTuning.TUBE_CATHODE_REF_K);

        // --- the envelope: tightness, heat, and the bulb's mass to warm ---
        float serviceTu = em.thermal().serviceTemperature().get().value();
        // gas-tightness: how inert the envelope is, times how still its melt was (a stiff glass leaks less than a runny ceramic)
        float visc = em.molten().viscosity().orElse(1f);
        float stiff = Math.max(0f, Math.min(1f, (float) Math.log10(Math.max(1f, visc)) / ProcessTuning.TUBE_TIGHT_VISC_LOG));
        float tightness = Math.max(0f, Math.min(1f, em.corrosionResistance().get()))
                * (ProcessTuning.TUBE_TIGHT_FLOOR + (1f - ProcessTuning.TUBE_TIGHT_FLOOR) * stiff);
        float bulbMm = type.roles().get(ENVELOPE).axis(envelope, "bulb");
        float bulb = bulbMm / ProcessTuning.TUBE_BULB_REF_MM;
        float ambient = ThermalTuning.AMBIENT_TU.value();
        float warmTu = Math.max(ProcessTuning.TUBE_MIN_WARM_TU,
                ambient + ((heldK - ProcessTuning.TUBE_KELVIN) - ambient) * ProcessTuning.TUBE_BULB_SHARE);
        // gross heating a tick per Eu a tick of heater; the circuit's stack sheds ThermalTuning.ITEM_COOLING_TU_PER_TICK against it, so a wide bulb on a weak heater never warms
        float warmRate = ProcessTuning.TUBE_WARM_TU_PER_EU / Math.max(0.1f, bulb);

        // --- grids and plate: gain and speed ---
        int grids = grid == null ? 0 : Math.max(0, Math.min(3, Math.round(type.roles().get(GRID).axis(grid, "grids"))));
        float gain = 0f;
        if (grids > 0) {
            float pitch = type.roles().get(GRID).axis(grid, "pitch") / 100f;
            // finest pitch (20 per cent) gives full gain, the coarsest (100 per cent) TUBE_COARSE_GAIN of it
            float fine = 1f - (1f - ProcessTuning.TUBE_COARSE_GAIN) * Math.max(0f, Math.min(1f, (pitch - 0.2f) / 0.8f));
            float hold = 1f - ProcessTuning.TUBE_GRID_COND_LOSS * (1f - Math.min(1f, gm.electrical().conductivity().get()));
            // gain follows the current the emitter can give: a longer emitter, a better cathode
            gain = ProcessTuning.TUBE_GRID_GAIN[grids] * fine * hold * emission * Math.min(1.5f, emitter);
        }
        float area = type.roles().get(PLATE).axis(plate, "area") / 100f;
        float sheds = Math.min(1f, pm.thermal().conductivity().get().raw() / ProcessTuning.TUBE_PLATE_COND_REF);
        float speed = Math.max(0f, 1f - ProcessTuning.TUBE_PLATE_SLOW * area - ProcessTuning.TUBE_GRID_SLOW * grids
                - ProcessTuning.TUBE_HOT_PLATE_SLOW * (1f - sheds) * area);
        return Optional.of(new Build(grids, emission, gain, speed, power, warmTu, warmRate, tightness, serviceTu));
    }

    /**
     * The vacuum quality a seal reaches: the dryer's depth (0 at its working gauge, 1 at its deepest) and how far its heat came between
     * the bake floor and the envelope's service temperature give it, capped by the envelope's tightness. An envelope heated past its
     * service temperature slumps and holds none.
     */
    public static float vacuum(Build build, float depth, float chamberTu) {
        if (chamberTu > build.serviceTu())
            return 0f;
        float top = build.serviceTu() * ProcessTuning.CIRCUIT_HEATER_LIMIT_SHARE;
        float bake = top <= ProcessTuning.TUBE_BAKE_MIN_TU ? 1f
                : Math.max(0f, Math.min(1f, (chamberTu - ProcessTuning.TUBE_BAKE_MIN_TU) / (top - ProcessTuning.TUBE_BAKE_MIN_TU)));
        float d = Math.max(0f, Math.min(1f, depth));
        return (ProcessTuning.TUBE_VACUUM_FLOOR + (1f - ProcessTuning.TUBE_VACUUM_FLOOR) * d)
                * (ProcessTuning.TUBE_BAKE_FLOOR + (1f - ProcessTuning.TUBE_BAKE_FLOOR) * bake) * build.tightness();
    }

    /** The figures a sealed tube carries: the character's share of speed, noise and gain on the geometry's, noise from the emitter and the gas left. */
    public static Tube seal(Build build, float vacuum) {
        Tube.Kind kind = Tube.kindOf(build.grids(), vacuum);
        int k = kind.ordinal();
        float noise = ProcessTuning.TUBE_NOISE_BASE + ProcessTuning.TUBE_NOISE_POOR_EMITTER * (1f - build.emission())
                + ProcessTuning.TUBE_NOISE_GAS * (1f - vacuum);
        float speed = build.speed() * (0.7f + 0.3f * vacuum) * ProcessTuning.TUBE_KIND_SPEED[k] * (0.2f + 0.8f * build.emission());
        return new Tube(kind, vacuum, build.gain() * ProcessTuning.TUBE_KIND_GAIN[k] * vacuumGain(kind, vacuum),
                Math.max(0f, Math.min(1f, speed)), Math.max(ProcessTuning.CIRCUIT_NOISE_FLOOR, Math.min(1f, noise * ProcessTuning.TUBE_KIND_NOISE[k])),
                build.power(), build.warmTu(), build.warmRate());
    }

    /** A gas-filled tube's gain is a trigger's, not an amplifier's: its stage gain does not fall with a soft seal. A vacuum tube's falls a little with one. */
    private static float vacuumGain(Tube.Kind kind, float vacuum) {
        return kind == Tube.Kind.THYRATRON ? 1f : 0.5f + 0.5f * vacuum;
    }
}
