// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.worldgen;

/**
 * Ore provinces: a large region of the world in which one group of deposits is rich or barren together, as a real
 * metallogenic belt is. A deposit names its province in its file; the province's richness at a point is a smooth value
 * noise drawn from the world seed and the province's name alone, so every chunk agrees without storing anything.
 *
 * <p>Invented; no prior art (TerraFirmaCraft has no provinces and was read only for what it does not do, see
 * {@code THIRD-PARTY-LICENSES.md}).
 */
public final class Provinces {

    /** Blocks between lattice points of the coarse octave: the size of a rich or barren area. */
    static final int WAVELENGTH = 512;
    /** Richest a province makes a deposit (a multiple of its weight) and the barest, at full scale. */
    static final float RICH = 2.5f, BARREN = 0.25f;

    private Provinces() {}

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** A lattice value in [0, 1) for a province at a lattice cell. */
    private static double lattice(long key, int x, int z) {
        return (mix(key ^ mix(x * 341873128712L + z * 132897987541L)) >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** One octave of value noise, [0, 1], with lattice spacing {@code wavelength}. */
    private static double octave(long key, double x, double z, int wavelength) {
        double fx = x / wavelength, fz = z / wavelength;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = smooth(fx - x0), tz = smooth(fz - z0);
        double a = lattice(key, x0, z0), b = lattice(key, x0 + 1, z0), c = lattice(key, x0, z0 + 1),
                d = lattice(key, x0 + 1, z0 + 1);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    /**
     * The factor a province puts on its deposits' weight at a column: one below 1 where it is barren, above 1 where
     * rich, between {@link #BARREN} and {@link #RICH} before {@code scale}. A scale of 0 is no province at all, 1 the full
     * range; between, the factor is raised to it, so the ends move toward 1 together.
     */
    public static double factor(long worldSeed, String province, int x, int z, float scale) {
        if (scale <= 0f)
            return 1.0;
        long key = mix(worldSeed ^ mix(province.hashCode() * 0x9E3779B97F4A7C15L));
        // Two octaves: the broad belt and a finer patchiness inside it, weighted 3 to 1.
        double n = 0.75 * octave(key, x, z, WAVELENGTH) + 0.25 * octave(key ^ 0x5DEECE66DL, x, z, WAVELENGTH / 3);
        // Value noise bunches around the middle; stretch it so belts reach both ends.
        double stretched = Math.max(0.0, Math.min(1.0, (n - 0.5) * 1.8 + 0.5));
        double full = BARREN * Math.pow(RICH / BARREN, stretched);
        return Math.pow(full, scale);
    }
}
