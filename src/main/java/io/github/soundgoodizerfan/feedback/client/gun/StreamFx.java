// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.net.CasterStreamPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Draws a fluid caster's stream: droplets along the ray, a fan or a mist widening with distance, flame on a burning one. Draws only; decides nothing. */
public final class StreamFx {

    private StreamFx() {
    }

    public static void draw(CasterStreamPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        RandomSource random = mc.level.random;
        Vec3 start = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
        // A thicker stream is drawn with more drops, a mist with big soft ones.
        int drops = Math.min(48, 6 + Math.round(p.length() * 1.2f) + p.flowMb() / 4);
        float size = p.kind() == CasterStreamPayload.MIST ? 1.6f : 0.7f;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((p.rgb() >> 16 & 255) / 255f, (p.rgb() >> 8 & 255) / 255f, (p.rgb() & 255) / 255f), size);
        double tan = Math.tan(Math.toRadians(p.spread()));
        for (int i = 0; i < drops; i++) {
            double t = random.nextDouble() * p.length();
            double width = t * tan;
            Vec3 at = start.add(dir.scale(t)).add((random.nextDouble() - 0.5) * 2 * width, (random.nextDouble() - 0.5) * 2 * width,
                    (random.nextDouble() - 0.5) * 2 * width);
            mc.level.addParticle(dust, at.x, at.y, at.z, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1);
            if (p.kind() == CasterStreamPayload.BURNING && random.nextInt(3) == 0)
                mc.level.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, dir.x * 0.05, dir.y * 0.05 + 0.01, dir.z * 0.05);
        }
        // The pilot's flame at the nozzle's end.
        if (p.flame() > 0f)
            for (int i = 0; i < 2; i++) {
                Vec3 tip = start.add(dir.scale(p.flame() * random.nextDouble()));
                mc.level.addParticle(ParticleTypes.FLAME, tip.x, tip.y, tip.z, dir.x * 0.02, dir.y * 0.02 + 0.01, dir.z * 0.02);
            }
        // Where it lands.
        Vec3 end = start.add(dir.scale(p.length()));
        mc.level.addParticle(p.kind() == CasterStreamPayload.MIST ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, end.x, end.y, end.z, 0, 0.05, 0);
    }
}
