// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.CasterStreamPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a fluid caster's stream. Draws only; decides nothing. A liquid or burning stream is a ribbon (as a bullet's streak is, see
 * {@link GunFx}): one per caster, kept between the server's one-a-tick messages, stepped each client tick and drawn after the
 * particles from the last two states by the partial tick, so it is smooth at any frame rate. It is a camera-facing strip from the nozzle
 * to where the stream lands, a coloured halo round a bright core, widening with the cone and thickening with the mB a tick. The flow is
 * bands of brightness that run down the strip (two sine waves at different pitches, scrolled at about the stream's own speed), so it
 * needs no texture. Particles do the rest: drops that break away from its edge, a burst at the nozzle when the valve opens, spray off
 * the wall it lands on, flames riding a burning stream and the whole of a mist (slowing soft puffs, no ribbon).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class StreamFx {

    /** Vanilla's water drop: friction per tick. */
    private static final float DROP_FRICTION = 0.98f;
    /** Dust and flame particles slow by this much a tick. */
    private static final float PUFF_FRICTION = 0.96f;
    /** Ticks a ribbon fades over once its messages stop. */
    private static final int FADE_TICKS = 3;

    /** One caster's stream: the last two states, so it is drawn between them. */
    private static final class Ribbon {
        Vec3 prevStart, start, prevDir, dir;
        float prevLength, length, spread;
        int rgb, flow;
        byte kind;
        /** The wielder's entity id (-1 when none is near the nozzle). */
        int owner = -1;
        /** Ticks since the last message; 0 on the tick one arrived. */
        int age;
        boolean updated;
    }

    private static final Map<Integer, Ribbon> RIBBONS = new HashMap<>();

    private StreamFx() {
    }

    // ---------------------------------------------------------------- state

    /** A world joined is after any resource reload: the sprite averages are read again. */
    @SubscribeEvent
    public static void onJoin(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        FluidTint.clear();
    }

    private static int rgb(CasterStreamPayload p) {
        return p.rgb() >= 0 ? p.rgb() : FluidTint.of(p.fluid(), 0xC8D8FF);
    }

    public static void draw(CasterStreamPayload p) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null)
            return;
        Vec3 start = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());

        // One ribbon per wielder: the nearest player to the nozzle.
        int key = -1;
        double best = 2.5 * 2.5;
        for (Player pl : level.players()) {
            double d = pl.getEyePosition().distanceToSqr(start);
            if (d < best) {
                best = d;
                key = pl.getId();
            }
        }
        Ribbon ribbon = RIBBONS.get(key);
        boolean opening = ribbon == null || ribbon.age > 0 && ribbon.age >= FADE_TICKS;
        if (ribbon == null) {
            ribbon = new Ribbon();
            RIBBONS.put(key, ribbon);
            ribbon.prevStart = start;
            ribbon.prevDir = dir;
            ribbon.prevLength = p.length();
        } else {
            ribbon.prevStart = ribbon.start;
            ribbon.prevDir = ribbon.dir;
            ribbon.prevLength = ribbon.length;
            if (opening) {
                ribbon.prevStart = start;
                ribbon.prevDir = dir;
                ribbon.prevLength = p.length();
            }
        }
        ribbon.start = start;
        ribbon.dir = dir;
        ribbon.length = p.length();
        ribbon.spread = p.spread();
        ribbon.rgb = rgb(p);
        ribbon.flow = p.flowMb();
        ribbon.kind = p.kind();
        ribbon.owner = key;
        ribbon.age = 0;
        ribbon.updated = true;

        particles(mc, level, p, opening);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            RIBBONS.clear();
            return;
        }
        if (mc.isPaused())
            return;
        for (Iterator<Ribbon> it = RIBBONS.values().iterator(); it.hasNext(); ) {
            Ribbon r = it.next();
            if (r.updated) {
                r.updated = false;
                continue;
            }
            r.prevStart = r.start;
            r.prevDir = r.dir;
            r.prevLength = r.length;
            if (++r.age > FADE_TICKS)
                it.remove();
        }
    }

    // ---------------------------------------------------------------- the ribbon

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RIBBONS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = mc.level.getGameTime() + partial;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson() && mc.player != null;
        for (Ribbon r : RIBBONS.values()) {
            if (r.kind == CasterStreamPayload.MIST)
                continue;
            Vec3 start = r.prevStart.lerp(r.start, partial);
            Vec3 dir = r.prevDir.lerp(r.dir, partial);
            if (dir.lengthSqr() < 1e-8)
                continue;
            dir = dir.normalize();
            float length = Mth.lerp(partial, r.prevLength, r.length);
            // Your own stream follows your live eye and look every frame, not the server's last tick, or it trails behind a turning camera.
            boolean own = mc.player != null && r.owner == mc.player.getId();
            if (own) {
                dir = mc.player.getViewVector(partial);
                start = mc.player.getEyePosition(partial).add(dir.scale(0.4));
            }
            // Fading out once the messages stop.
            float life = r.age == 0 ? 1f : 1f - (r.age - 1 + partial) / (FADE_TICKS + 1f);
            if (life > 0f && length > 0.05f)
                ribbon(m, vc, r, start, dir, length, life, cam, time, firstPerson && own);
        }
        buffers.endBatch(RenderType.debugQuads());
        pose.popPose();
    }

    /** One strip: a halo and a core, each a run of quads whose brightness bands scroll down the stream. */
    private static void ribbon(Matrix4f m, VertexConsumer vc, Ribbon r, Vec3 start, Vec3 dir, float length, float life, Vec3 cam, double time, boolean own) {
        boolean burning = r.kind == CasterStreamPayload.BURNING;
        float cr = (r.rgb >> 16 & 255) / 255f, cg = (r.rgb >> 8 & 255) / 255f, cb = (r.rgb & 255) / 255f;
        // The core: the fluid's colour pushed toward white, or toward flame when it burns.
        float kr = burning ? 1f : Mth.lerp(0.55f, cr, 1f), kg = burning ? 0.78f : Mth.lerp(0.55f, cg, 1f), kb = burning ? 0.35f : Mth.lerp(0.55f, cb, 1f);
        float body = (float) Math.sqrt(Math.max(1, r.flow));
        double tan = Math.tan(Math.toRadians(r.spread));
        float w0 = 0.03f + 0.012f * Math.min(body, 8f);
        // Your own stream, seen down its length, would be edge-on: it leaves from the barrel (low and to the right of the eye) and
        // closes on the real ray over a few blocks, so it is seen from the side as it sweeps.
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 muzzle = own ? right.scale(0.17).add(0, -0.13, 0) : Vec3.ZERO;
        int n = Mth.clamp(Math.round(length * 3), 6, 48);
        Vec3 prevTop = null, prevBot = null, prevCoreTop = null, prevCoreBot = null;
        float prevA = 0, prevCoreA = 0;
        for (int i = 0; i <= n; i++) {
            float s = i / (float) n, t = s * length;
            Vec3 at = start.add(dir.scale(t)).add(muzzle.scale(Math.max(0, 1 - t / 9f)));
            Vec3 toCam = cam.subtract(at);
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() < 1e-8)
                continue;
            side = side.normalize();
            // The cone widens it; the near-eye fade stops it smearing across the view.
            float w = w0 + (float) (t * tan * 0.35);
            double near = Mth.clamp((toCam.length() - 0.1) / 0.6, 0, 1);
            // Bands of brightness running down the stream, a broad slow one and a fine quick one.
            double band = 0.5 + 0.5 * Math.sin(t * 4.5 - time * 5.0 + Math.sin(t * 1.7) * 2.0);
            double fine = 0.5 + 0.5 * Math.sin(t * 11.0 - time * 12.0);
            float flow = (float) (0.6 + 0.3 * band + 0.1 * fine);
            float taper = 1f - 0.45f * s * s;
            float a = (float) (0.42 * flow * taper * life * near);
            float coreA = (float) (0.85 * (0.7 + 0.3 * fine) * taper * life * near);
            Vec3 top = at.add(side.scale(w)), bot = at.subtract(side.scale(w));
            Vec3 coreTop = at.add(side.scale(w * 0.4)), coreBot = at.subtract(side.scale(w * 0.4));
            if (prevTop != null) {
                quad(m, vc, prevTop, prevBot, bot, top, cr, cg, cb, prevA, a);
                quad(m, vc, prevCoreTop, prevCoreBot, coreBot, coreTop, kr, kg, kb, prevCoreA, coreA);
            }
            prevTop = top;
            prevBot = bot;
            prevCoreTop = coreTop;
            prevCoreBot = coreBot;
            prevA = a;
            prevCoreA = coreA;
        }
    }

    private static void quad(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float aNear, float aFar) {
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r, g, bl, aNear);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r, g, bl, aNear);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r, g, bl, aFar);
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r, g, bl, aFar);
    }

    // ---------------------------------------------------------------- particles

    /** What the ribbon cannot be: drops breaking from its edge, the nozzle burst, flames, mist and the spray where it lands. */
    private static void particles(Minecraft mc, ClientLevel level, CasterStreamPayload p, boolean opening) {
        RandomSource random = level.random;
        Vec3 start = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
        float length = p.length();
        int c = rgb(p);
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        double tan = Math.tan(Math.toRadians(p.spread()));
        boolean mist = p.kind() == CasterStreamPayload.MIST;
        // Fewer particles when the player has asked for fewer.
        ParticleStatus status = mc.options.particles().get();
        float thin = status == ParticleStatus.ALL ? 1f : status == ParticleStatus.DECREASED ? 0.5f : 0.2f;
        float body = (float) Math.sqrt(Math.max(1, p.flowMb()));
        float fat = 1f + 0.12f * Math.min(body, 8f);

        // Two unit vectors across the ray, for sideways spread.
        Vec3 across1 = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 across2 = dir.cross(across1).normalize();

        if (mist) {
            int life = Mth.clamp(Math.round(5 + length * 0.5f), 6, 14);
            float v = travelSpeed(length, life, PUFF_FRICTION);
            int count = Math.round((3 + body) * thin);
            for (int i = 0; i < count; i++) {
                double u = random.nextDouble();
                Vec3 vel = dir.scale(v * (0.85 + random.nextDouble() * 0.3)).add(across(across1, across2, random, v * tan));
                launch(new DustParticleOptions(new Vector3f(r, g, b), (0.9f + random.nextFloat() * 0.8f) * fat), start.add(vel.scale(u)), vel, life, 1f,
                        -1, 0, 0);
            }
        } else {
            int life = Mth.clamp(Math.round(3 + length * 0.8f), 4, 20);
            float v = travelSpeed(length, life, DROP_FRICTION);
            // Drops that break off the ribbon and stray across the cone, a little behind its head.
            int drops = Math.round((1 + body * 0.6f) * thin);
            for (int i = 0; i < drops; i++) {
                double u = random.nextDouble();
                Vec3 vel = dir.scale(v * (0.9 + random.nextDouble() * 0.12)).add(across(across1, across2, random, v * (tan + 0.02)));
                launch(ParticleTypes.SPLASH, start.add(vel.scale(u)), vel, life, (0.4f + random.nextFloat() * 0.4f) * fat, r, g, b);
            }
            if (p.kind() == CasterStreamPayload.BURNING) {
                float fv = travelSpeed(length, life, PUFF_FRICTION);
                int flames = Math.round((2 + body * 0.5f) * thin);
                for (int i = 0; i < flames; i++) {
                    double u = random.nextDouble();
                    Vec3 vel = dir.scale(fv * (0.7 + random.nextDouble() * 0.3)).add(across(across1, across2, random, fv * tan * 0.5));
                    launch(ParticleTypes.FLAME, start.add(vel.scale(u)), vel, Math.max(3, Math.round(life * (0.4f + random.nextFloat() * 0.6f))), 1f, -1, 0,
                            0);
                }
                if (random.nextInt(3) == 0)
                    launch(ParticleTypes.SMOKE, start.add(dir.scale(length * random.nextDouble())), new Vec3(0, 0.03, 0), 12, 1f, -1, 0, 0);
            }
        }

        // The valve has just opened: a puff of drops thrown wide at the nozzle.
        if (opening && !mist) {
            int burst = Math.round(10 * thin);
            for (int i = 0; i < burst; i++) {
                Vec3 vel = dir.scale(0.25 + random.nextDouble() * 0.3).add(across(across1, across2, random, 0.25));
                launch(ParticleTypes.SPLASH, start, vel, 6 + random.nextInt(5), 0.6f + random.nextFloat() * 0.4f, r, g, b);
            }
        }

        // The pilot's flame at the nozzle's end.
        if (p.flame() > 0f)
            for (int i = 0; i < 2; i++) {
                Vec3 tip = start.add(dir.scale(p.flame() * random.nextDouble()));
                level.addParticle(ParticleTypes.FLAME, tip.x, tip.y, tip.z, dir.x * 0.02, dir.y * 0.02 + 0.01, dir.z * 0.02);
            }

        // Where it lands: on a wall the stream sprays back off the face; in the air a mist just hangs where it runs out.
        Vec3 end = start.add(dir.scale(length));
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(dir.scale(length + 0.1)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 normal = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
            Vec3 at = hit.getLocation().add(normal.scale(0.05));
            int spray = Math.round((2 + body * 0.7f) * thin);
            for (int i = 0; i < spray; i++) {
                Vec3 vel = normal.scale(0.08 + random.nextDouble() * 0.12).add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2 + 0.05,
                        (random.nextDouble() - 0.5) * 0.2);
                if (mist)
                    launch(new DustParticleOptions(new Vector3f(r, g, b), 0.8f + random.nextFloat() * 0.6f), at, vel.scale(0.5), 8 + random.nextInt(6), 1f,
                            -1, 0, 0);
                else
                    launch(ParticleTypes.SPLASH, at, vel, 6 + random.nextInt(6), (0.5f + random.nextFloat() * 0.5f) * fat, r, g, b);
            }
        } else if (mist)
            level.addParticle(ParticleTypes.CLOUD, end.x, end.y, end.z, 0, 0.02, 0);
    }

    /** Launch speed that carries a particle losing {@code friction} of its speed a tick, for {@code life} ticks, a total of {@code distance}. */
    private static float travelSpeed(float distance, int life, float friction) {
        return distance * (1f - friction) / (1f - (float) Math.pow(friction, life));
    }

    /** A random sideways vector across the ray, up to {@code scale} long and denser toward the middle of the cone. */
    private static Vec3 across(Vec3 a, Vec3 b, RandomSource random, double scale) {
        double radius = Math.sqrt(random.nextDouble()) * scale, angle = random.nextDouble() * Math.PI * 2;
        return a.scale(Math.cos(angle) * radius).add(b.scale(Math.sin(angle) * radius));
    }

    /**
     * Makes one vanilla particle and overrides what its constructor made of the velocity, the life and the size, then the tint when
     * {@code r} is not negative. A particle the engine refuses (a full pool, particles off) is dropped.
     */
    private static void launch(ParticleOptions options, Vec3 at, Vec3 vel, int life, float scale, float r, float g, float b) {
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(options, at.x, at.y, at.z, 0, 0, 0);
        if (particle == null)
            return;
        particle.setParticleSpeed(vel.x, vel.y, vel.z);
        particle.setLifetime(life);
        if (scale != 1f)
            particle.scale(scale);
        if (r >= 0)
            particle.setColor(r, g, b);
    }
}
