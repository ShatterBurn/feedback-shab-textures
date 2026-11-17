// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.CasterStreamPayload;
import io.github.soundgoodizerfan.feedback.weapon.CasterTuning;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 * Draws a fluid caster's stream as a spray. Draws only; decides nothing. Three things are drawn, all from the server's one-a-tick
 * {@link CasterStreamPayload}:
 * <ul>
 * <li>a camera-facing strip per caster ({@link Ribbon}): the bright core of a hard-driven liquid jet near the nozzle, the whole billowing
 * flame of a burning fuel, or a bare strip for a stream too far off to be worth droplets;</li>
 * <li>droplets ({@link Drop}): small client-side bodies with their own gravity and drag, kept in one list and drawn as camera-facing quads in
 * the same pass. Liquid drops fly in an arc from the exit speed, gas and mist are slowing puffs, flame, soot, fog and motes are the other
 * kinds. They never touch the particle engine, so the count is bounded here: per stream per tick by the particles setting, and for
 * other players' streams by {@link CasterTuning#SPRAY_MAX_LIVE} live drops;</li>
 * <li>the landing: a splash off the face the client's own clip finds, and a glow where a flame lands.</li>
 * </ul>
 * Shape comes from the stream's figures (spread, speed, flow, thickness, flags), never from a fluid id. Nothing here is a shader; the fire
 * ramp is vertex colour.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class StreamFx {

    /** Ticks a ribbon fades over once its messages stop. */
    private static final int FADE_TICKS = 3;
    /** Slowing per tick of puffs and of flames. */
    private static final float PUFF_DRAG = 0.9f, FLAME_DRAG = 0.93f;
    /** Most live droplets of all streams: a hard ceiling over the per-stream and crowd caps. */
    private static final int MAX_TOTAL = 1200;

    private static final byte DROP = 0, PUFF = 1, FLAME = 2, SOOT = 3, FOG = 4, MOTE = 5, FROST = 6, HALO = 7, EMBER = 8;

    /** One caster's stream: the last two states, so it is drawn between them. */
    private static final class Ribbon {
        Vec3 prevStart, start, prevDir, dir;
        float prevLength, length, spread, speed;
        int rgb, flow, flameTu;
        byte kind;
        /** The wielder's entity id (-1 when none is near the nozzle). */
        int owner = -1;
        /** Ticks since the last message; 0 on the tick one arrived. */
        int age;
        boolean updated;
    }

    /** One droplet (or puff, flame, soot, fog or mote): reused from a free list, so the droplets themselves are never reallocated once warm (the short-lived {@code Vec3}s of spawning and the strip are). */
    private static final class Drop {
        float x, y, z, ox, oy, oz, vx, vy, vz, size, r, g, b, hot;
        int age, life;
        byte type;
        /** Your own stream's: never held to the crowd cap. */
        boolean mine;
        /** A flared oxidiser's: it whitens as it flies. */
        boolean flare;
    }

    private static final Map<Integer, Ribbon> RIBBONS = new HashMap<>();
    private static final List<Drop> DROPS = new ArrayList<>();
    private static final ArrayDeque<Drop> FREE = new ArrayDeque<>();
    /** Live droplets that are not the player's own. */
    private static int others;

    /** A soft white disc (solid core, feathered rim), made once at runtime so no art file is needed: every droplet is this, tinted. */
    private static ResourceLocation disc;

    private static ResourceLocation disc(Minecraft mc) {
        if (disc == null) {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, 32, 32, false);
            for (int y = 0; y < 32; y++)
                for (int x = 0; x < 32; x++) {
                    float dx = (x + 0.5f) / 16f - 1f, dy = (y + 0.5f) / 16f - 1f;
                    float rad = (float) Math.sqrt(dx * dx + dy * dy);
                    float t = Mth.clamp((1f - rad) / 0.45f, 0f, 1f);
                    int a = Math.round(t * t * (3f - 2f * t) * 255f);
                    img.setPixelRGBA(x, y, a << 24 | 0xFFFFFF);
                }
            disc = Feedback.id("stream_disc");
            mc.getTextureManager().register(disc, new DynamicTexture(img));
        }
        return disc;
    }

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
        ribbon.speed = p.speed();
        ribbon.rgb = rgb(p);
        ribbon.flow = p.flowMb();
        ribbon.kind = p.kind();
        ribbon.flameTu = p.flameTu();
        ribbon.owner = key;
        ribbon.age = 0;
        ribbon.updated = true;

        boolean mine = mc.player != null && key == mc.player.getId();
        double far = mc.gameRenderer.getMainCamera().getPosition().distanceTo(start);
        // Past the far limit a stream is a bare strip; past half of it, half the droplets.
        float scale = mine || far <= CasterTuning.SPRAY_FAR * 0.5 ? 1f : far <= CasterTuning.SPRAY_FAR ? 0.5f : 0f;
        if (scale > 0f)
            spray(mc, level, p, opening, mine, scale);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            RIBBONS.clear();
            for (Drop d : DROPS)
                FREE.add(d);
            DROPS.clear();
            others = 0;
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
        step();
    }

    // ---------------------------------------------------------------- the droplets

    private static void spawn(byte type, double x, double y, double z, Vec3 vel, float size, int life, float r, float g, float b, boolean mine, float hot,
                              boolean flare) {
        if (DROPS.size() >= MAX_TOTAL || !mine && others >= CasterTuning.SPRAY_MAX_LIVE)
            return;
        Drop d = FREE.isEmpty() ? new Drop() : FREE.poll();
        d.x = d.ox = (float) x;
        d.y = d.oy = (float) y;
        d.z = d.oz = (float) z;
        d.vx = (float) vel.x;
        d.vy = (float) vel.y;
        d.vz = (float) vel.z;
        d.size = size;
        d.r = r;
        d.g = g;
        d.b = b;
        d.hot = hot;
        d.age = 0;
        d.life = Math.max(1, life);
        d.type = type;
        d.mine = mine;
        d.flare = flare;
        DROPS.add(d);
        if (!mine)
            others++;
    }

    /** One client tick of every droplet: its own gravity and drag by kind, growth, and removal at the end of its life. */
    private static void step() {
        for (int i = DROPS.size() - 1; i >= 0; i--) {
            Drop d = DROPS.get(i);
            if (++d.age >= d.life) {
                DROPS.set(i, DROPS.get(DROPS.size() - 1));
                DROPS.remove(DROPS.size() - 1);
                if (!d.mine)
                    others--;
                FREE.add(d);
                continue;
            }
            d.ox = d.x;
            d.oy = d.y;
            d.oz = d.z;
            float drag = 0.99f, lift = 0f, grow = 1f;
            switch (d.type) {
                case DROP -> lift = -CasterTuning.SPRAY_GRAVITY;
                case PUFF -> {
                    drag = PUFF_DRAG;
                    lift = 0.002f;
                    grow = 1.06f;
                }
                case FLAME -> {
                    drag = FLAME_DRAG;
                    lift = 0.012f;
                    grow = d.age < d.life / 3 ? 1.05f : 0.93f;
                }
                case SOOT -> {
                    drag = 0.92f;
                    lift = 0.01f;
                    grow = 1.04f;
                }
                case FOG -> {
                    drag = 0.92f;
                    lift = -0.001f;
                    grow = 1.05f;
                }
                case MOTE -> {
                    drag = 0.95f;
                    lift = 0.004f;
                }
                case EMBER -> {
                    drag = 0.98f;
                    lift = -0.02f;
                }
                case FROST -> {
                    drag = 0.95f;
                    lift = -0.004f;
                }
                default -> {
                    drag = 0f;
                }
            }
            d.vx *= drag;
            d.vy = d.vy * drag + lift;
            d.vz *= drag;
            d.x += d.vx;
            d.y += d.vy;
            d.z += d.vz;
            d.size *= grow;
        }
    }

    /** A random sideways vector across the ray, up to {@code scale} long and denser toward the middle of the cone. */
    private static Vec3 across(Vec3 a, Vec3 b, RandomSource random, double scale) {
        double radius = Math.sqrt(random.nextDouble()) * scale, angle = random.nextDouble() * Math.PI * 2;
        return a.scale(Math.cos(angle) * radius).add(b.scale(Math.sin(angle) * radius));
    }

    /** Launch speed that carries a body losing {@code friction} of its speed a tick, for {@code life} ticks, a total of {@code distance}. */
    private static float travelSpeed(float distance, int life, float friction) {
        return distance * (1f - friction) / (1f - (float) Math.pow(friction, life));
    }

    /**
     * One message's droplets. How many is capped by the particles setting; the shape is the nozzle's: the cone from {@code spread}, the
     * arc from the exit speed against gravity, the size from the cone and the fluid's thickness. {@code scale} thins far streams.
     */
    private static void spray(Minecraft mc, ClientLevel level, CasterStreamPayload p, boolean opening, boolean mine, float scale) {
        RandomSource random = level.random;
        Vec3 start = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
        float length = p.length();
        int c = rgb(p);
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        double tan = Math.tan(Math.toRadians(p.spread()));
        boolean mist = p.kind() == CasterStreamPayload.MIST, burning = p.kind() == CasterStreamPayload.BURNING;
        boolean frozen = (p.flags() & CasterStreamPayload.FROZEN) != 0, flared = (p.flags() & CasterStreamPayload.FLARED) != 0,
                potion = (p.flags() & CasterStreamPayload.POTION) != 0;
        ParticleStatus status = mc.options.particles().get();
        int cap = status == ParticleStatus.ALL ? CasterTuning.SPRAY_DROPS_ALL : status == ParticleStatus.DECREASED ? CasterTuning.SPRAY_DROPS_DECREASED
                : CasterTuning.SPRAY_DROPS_MINIMAL;
        float body = (float) Math.sqrt(Math.max(1, p.flowMb()));
        float thick = (p.thick() & 255) / 255f;
        float speed = Math.max(0.05f, p.speed());
        float hot = Mth.clamp((p.flameTu() - 600f) / 1600f, 0f, 1f);
        if (frozen) {
            r = Mth.lerp(0.5f, r, 0.9f);
            g = Mth.lerp(0.5f, g, 0.97f);
            b = Mth.lerp(0.5f, b, 1f);
        }

        Vec3 across1 = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 across2 = dir.cross(across1).normalize();

        if (mist) {
            int life = Mth.clamp(Math.round(5 + length * 0.5f), 6, 16);
            float v = travelSpeed(length, life, PUFF_DRAG);
            int count = Math.min(cap + 2, Math.round((3 + body) * scale));
            for (int i = 0; i < count; i++) {
                double u = random.nextDouble();
                Vec3 vel = dir.scale(v * (0.85 + random.nextDouble() * 0.3)).add(across(across1, across2, random, v * tan));
                spawn(PUFF, start.x + vel.x * u, start.y + vel.y * u, start.z + vel.z * u, vel, 0.12f + random.nextFloat() * 0.12f, life, r, g, b, mine, 0f,
                        false);
            }
        } else {
            // A drop flies at the exit speed and is gone when it has covered the stream's reach; gravity bends its path.
            int life = Mth.clamp(Math.round(length / (speed * 0.95f)), 3, 40);
            float size = Mth.lerp(Math.min(p.spread() / 20f, 1f), 0.07f, 0.03f) * (1f + thick * 1.6f) * (1f + 0.05f * Math.min(body, 8f));
            // A burning stream is fire, not liquid: its fuel is consumed, so no drops of it fly (embers do, below).
            int drops = burning ? 0 : Math.min(cap, Math.round((1 + body * 0.6f) * scale));
            for (int i = 0; i < drops; i++) {
                double u = random.nextDouble();
                Vec3 vel = dir.scale(speed * (0.9 + random.nextDouble() * 0.15)).add(across(across1, across2, random, speed * (tan + 0.02)));
                spawn(DROP, start.x + vel.x * u, start.y + vel.y * u, start.z + vel.z * u, vel, size * (0.7f + random.nextFloat() * 0.6f), life, r, g, b,
                        mine, 0f, flared);
            }
            if (burning) {
                int flife = Mth.clamp(Math.round(6 + length * 0.3f), 6, 14);
                float fv = travelSpeed(length, flife, FLAME_DRAG);
                int flames = Math.min(cap + 2, Math.round((2 + body * 0.5f) * scale));
                for (int i = 0; i < flames; i++) {
                    double u = random.nextDouble();
                    Vec3 vel = dir.scale(fv * (0.7 + random.nextDouble() * 0.3)).add(across(across1, across2, random, fv * tan * 0.5));
                    spawn(FLAME, start.x + vel.x * u, start.y + vel.y * u, start.z + vel.z * u, vel, 0.06f + random.nextFloat() * 0.07f, flife, 1f, 1f, 1f,
                            mine, hot, false);
                }
                // A few embers thrown on an arc.
                int embers = Math.min(2, cap);
                for (int i = 0; i < embers; i++) {
                    if (random.nextFloat() > scale)
                        continue;
                    Vec3 vel = dir.scale(speed * (0.5 + random.nextDouble() * 0.4)).add(across(across1, across2, random, speed * (tan + 0.05)));
                    spawn(EMBER, start.x, start.y, start.z, vel, 0.02f + random.nextFloat() * 0.015f, 10 + random.nextInt(8), 1f, 1f, 1f, mine, hot, false);
                }
                // Soot off the flame's edge: a cooler fuel leaves more.
                float soot = (1.2f - hot) * scale * (status == ParticleStatus.ALL ? 1f : 0.5f);
                int sootN = (int) soot + (random.nextFloat() < soot - (int) soot ? 1 : 0);
                for (int i = 0; i < sootN; i++) {
                    Vec3 at = start.add(dir.scale(length * (0.3 + 0.7 * random.nextDouble()))).add(across(across1, across2, random, 0.1 + 0.35 * tan * length));
                    spawn(SOOT, at.x, at.y, at.z, new Vec3(0, 0.03, 0), 0.08f + random.nextFloat() * 0.08f, 14 + random.nextInt(8), 0.1f, 0.1f, 0.1f, mine, 0f,
                            false);
                }
            }
            if (frozen && random.nextFloat() < scale) {
                Vec3 at = start.add(dir.scale(length * random.nextDouble())).add(across(across1, across2, random, 0.1 + tan * length * 0.4));
                spawn(FOG, at.x, at.y, at.z, new Vec3(0, 0, 0), 0.18f, 14, 0.85f, 0.93f, 1f, mine, 0f, false);
            }
            if (potion) {
                int motes = Math.min(cap, 1 + (random.nextFloat() < 0.5f ? 1 : 0));
                for (int i = 0; i < motes; i++) {
                    Vec3 at = start.add(dir.scale(length * random.nextDouble())).add(across(across1, across2, random, 0.1 + tan * length * 0.4));
                    spawn(MOTE, at.x, at.y, at.z, new Vec3(0, 0.01, 0), 0.03f, 12 + random.nextInt(8), Mth.lerp(0.4f, r, 1f), Mth.lerp(0.4f, g, 1f),
                            Mth.lerp(0.4f, b, 1f), mine, 0f, false);
                }
            }
        }

        // The valve has just opened: a puff of drops thrown wide at the nozzle.
        if (opening && !mist && !burning) {
            int burst = Math.round(Math.min(10, cap * 1.7f) * scale);
            for (int i = 0; i < burst; i++) {
                Vec3 vel = dir.scale(0.25 + random.nextDouble() * 0.3).add(across(across1, across2, random, 0.25));
                spawn(DROP, start.x, start.y, start.z, vel, 0.04f + random.nextFloat() * 0.03f, 6 + random.nextInt(5), r, g, b, mine, 0f, false);
            }
        }

        // The pilot's flame at the nozzle's end.
        if (p.flame() > 0f)
            for (int i = 0; i < 2; i++) {
                Vec3 tip = start.add(dir.scale(p.flame() * random.nextDouble()));
                spawn(FLAME, tip.x, tip.y, tip.z, dir.scale(0.02).add(0, 0.01, 0), 0.08f, 6 + random.nextInt(4), 1f, 1f, 1f, mine, 0.2f, false);
            }

        // Where it lands: on a wall the stream sprays back off the face; in the air a gas or mist hangs where it runs out.
        Vec3 end = start.add(dir.scale(length));
        BlockHitResult hit = level.clip(new ClipContext(start, start.add(dir.scale(length + 0.1)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 normal = new Vec3(hit.getDirection().getStepX(), hit.getDirection().getStepY(), hit.getDirection().getStepZ());
            Vec3 at = hit.getLocation().add(normal.scale(0.05));
            int splash = Math.min(cap, Math.round((2 + body * 0.7f) * scale));
            for (int i = 0; i < splash; i++) {
                Vec3 vel = normal.scale(0.08 + random.nextDouble() * 0.12).add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2 + 0.05,
                        (random.nextDouble() - 0.5) * 0.2);
                if (mist)
                    spawn(PUFF, at.x, at.y, at.z, vel.scale(0.5), 0.1f + random.nextFloat() * 0.1f, 8 + random.nextInt(6), r, g, b, mine, 0f, false);
                else if (!burning)
                    spawn(DROP, at.x, at.y, at.z, vel, 0.03f + random.nextFloat() * 0.03f, 6 + random.nextInt(6), r, g, b, mine, 0f, false);
            }
            if (burning)
                spawn(HALO, at.x, at.y, at.z, Vec3.ZERO, 0.5f + 0.05f * Math.min(body, 8f), 2, 1f, 1f, 1f, mine, hot, false);
            if (frozen)
                for (int i = 0; i < Math.min(cap, 2); i++)
                    spawn(FROST, at.x, at.y, at.z, new Vec3((random.nextDouble() - 0.5) * 0.06, 0.03, (random.nextDouble() - 0.5) * 0.06), 0.025f,
                            10 + random.nextInt(8), 0.85f, 0.95f, 1f, mine, 0f, false);
        } else if (mist) {
            spawn(PUFF, end.x, end.y, end.z, new Vec3(0, 0.02, 0), 0.25f, 14, r, g, b, mine, 0f, false);
        }
    }

    // ---------------------------------------------------------------- drawing

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RIBBONS.isEmpty() && DROPS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double time = mc.level.getGameTime() + partial;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        RenderType soft = RenderType.text(disc(mc));
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
            if (life <= 0f || length <= 0.05f)
                continue;
            boolean burning = r.kind == CasterStreamPayload.BURNING;
            boolean far = cam.distanceTo(start) > CasterTuning.SPRAY_FAR;
            // A liquid jet keeps a core only when it is hard driven, and only near the nozzle; a far stream is the bare strip.
            boolean jet = r.speed >= CasterTuning.SPRAY_CORE_SPEED_SHARE * CasterTuning.SPEED_MAX;
            if (!burning && !far && !jet)
                continue;
            float drawn = burning || far ? length : length / 3f;
            strip(m, vc, r, start, dir, length, drawn, life, cam, time, firstPerson && own, burning);
        }
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        buffers.endBatch(RenderType.debugQuads());
        VertexConsumer sc = buffers.getBuffer(soft);
        for (Drop d : DROPS)
            blot(m, sc, d, left, up, partial);
        buffers.endBatch(soft);
        pose.popPose();
    }

    /** The temperature ramp of a flame, 0 hot core to 1 soot rim: white-yellow, orange, deep red, soot. {@code hot} pushes the hot end toward white. */
    private static int ramp(float f, float hot) {
        float r0 = 1f, g0 = Mth.lerp(hot, 0.78f, 1f), b0 = Mth.lerp(hot, 0.25f, 0.88f);
        float r, g, b;
        if (f < 0.3f) {
            float t = f / 0.3f;
            r = Mth.lerp(t, r0, 1f);
            g = Mth.lerp(t, g0, 0.52f);
            b = Mth.lerp(t, b0, 0.1f);
        } else if (f < 0.65f) {
            float t = (f - 0.3f) / 0.35f;
            r = Mth.lerp(t, 1f, 0.75f);
            g = Mth.lerp(t, 0.52f, 0.12f);
            b = Mth.lerp(t, 0.1f, 0.04f);
        } else {
            float t = Math.min(1f, (f - 0.65f) / 0.35f);
            r = Mth.lerp(t, 0.75f, 0.12f);
            g = Mth.lerp(t, 0.12f, 0.1f);
            b = Mth.lerp(t, 0.04f, 0.1f);
        }
        return (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    /**
     * One strip: a halo and a core, each a run of quads. A liquid's brightness bands scroll down it; a flame takes its colour from
     * {@link #ramp} along its length, billows wider as it goes and has a dark rim. {@code drawn} is how far from the nozzle it runs.
     */
    private static void strip(Matrix4f m, VertexConsumer vc, Ribbon r, Vec3 start, Vec3 dir, float length, float drawn, float life, Vec3 cam, double time,
                              boolean own, boolean burning) {
        float cr = (r.rgb >> 16 & 255) / 255f, cg = (r.rgb >> 8 & 255) / 255f, cb = (r.rgb & 255) / 255f;
        // The core: the fluid's colour pushed toward white.
        float kr = Mth.lerp(0.55f, cr, 1f), kg = Mth.lerp(0.55f, cg, 1f), kb = Mth.lerp(0.55f, cb, 1f);
        float hot = Mth.clamp((r.flameTu - 600f) / 1600f, 0f, 1f);
        float body = (float) Math.sqrt(Math.max(1, r.flow));
        double tan = Math.tan(Math.toRadians(r.spread));
        // A harder drive is a tighter, brighter core; a feeble one a fat arc.
        float drive = Mth.clamp(r.speed / CasterTuning.SPEED_MAX, 0f, 1f);
        float w0 = (0.03f + 0.012f * Math.min(body, 8f)) * (burning ? 1.4f : 1.3f - 0.5f * drive);
        // Your own stream, seen down its length, would be edge-on: it leaves from the barrel (low and to the right of the eye) and
        // closes on the real ray over a few blocks, so it is seen from the side as it sweeps.
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 muzzle = own ? right.scale(0.17).add(0, -0.13, 0) : Vec3.ZERO;
        int n = Mth.clamp(Math.round(drawn * 3), 6, 48);
        Vec3 prevTop = null, prevBot = null, prevCoreTop = null, prevCoreBot = null, prevRimTop = null, prevRimBot = null;
        float prevA = 0, prevCoreA = 0, prevRimA = 0;
        int prevC = 0, prevCoreC = 0;
        for (int i = 0; i <= n; i++) {
            float s = i / (float) n, t = s * drawn;
            Vec3 at = start.add(dir.scale(t)).add(muzzle.scale(Math.max(0, 1 - t / 9f)));
            Vec3 toCam = cam.subtract(at);
            Vec3 side = dir.cross(toCam);
            if (side.lengthSqr() < 1e-8)
                continue;
            side = side.normalize();
            double near = Mth.clamp((toCam.length() - 0.1) / 0.6, 0, 1);
            float w, a, coreA, rimA = 0;
            int haloC, coreC;
            if (burning) {
                float fs = Mth.clamp(t / Math.max(length, 0.1f), 0f, 1f);
                // The flame billows: wider with distance and rippling as it rises.
                float billow = 1f + 0.25f * (float) Math.sin(t * 5 - time * 11 + s * 3);
                w = (w0 + (float) (t * tan * 0.6) + 0.05f * t) * billow;
                float taper = 1f - (float) Math.pow(fs, 1.5);
                a = (float) (0.55 * taper * life * near);
                coreA = (float) (0.9 * (1f - fs) * life * near);
                rimA = (float) (0.28 * taper * life * near);
                haloC = ramp(Math.min(1f, fs * 0.9f + 0.2f), hot);
                coreC = ramp(fs * 0.5f, hot);
            } else {
                // The cone widens it.
                w = w0 + (float) (t * tan * 0.35);
                // Bands of brightness running down the stream, a broad slow one and a fine quick one.
                double band = 0.5 + 0.5 * Math.sin(t * 4.5 - time * 5.0 + Math.sin(t * 1.7) * 2.0);
                double fine = 0.5 + 0.5 * Math.sin(t * 11.0 - time * 12.0);
                float flow = (float) (0.6 + 0.3 * band + 0.1 * fine);
                // The core thins to nothing at the end of its third: past it the stream is drops.
                float taper = 1f - 0.9f * s * s;
                a = (float) (0.42 * flow * taper * life * near);
                coreA = (float) (0.85 * (0.7 + 0.3 * fine) * taper * life * near);
                haloC = (int) (cr * 255) << 16 | (int) (cg * 255) << 8 | (int) (cb * 255);
                coreC = (int) (kr * 255) << 16 | (int) (kg * 255) << 8 | (int) (kb * 255);
            }
            Vec3 top = at.add(side.scale(w)), bot = at.subtract(side.scale(w));
            Vec3 coreTop = at.add(side.scale(w * 0.4)), coreBot = at.subtract(side.scale(w * 0.4));
            Vec3 rimTop = at.add(side.scale(w * 1.35)), rimBot = at.subtract(side.scale(w * 1.35));
            if (prevTop != null) {
                if (burning)
                    quad(m, vc, prevRimTop, prevRimBot, rimBot, rimTop, 0x1A1818, 0x1A1818, prevRimA, rimA);
                quad(m, vc, prevTop, prevBot, bot, top, prevC, haloC, prevA, a);
                quad(m, vc, prevCoreTop, prevCoreBot, coreBot, coreTop, prevCoreC, coreC, prevCoreA, coreA);
            }
            prevTop = top;
            prevBot = bot;
            prevCoreTop = coreTop;
            prevCoreBot = coreBot;
            prevRimTop = rimTop;
            prevRimBot = rimBot;
            prevA = a;
            prevCoreA = coreA;
            prevRimA = rimA;
            prevC = haloC;
            prevCoreC = coreC;
        }
    }

    private static void quad(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int nearRgb, int farRgb, float aNear, float aFar) {
        float r0 = (nearRgb >> 16 & 255) / 255f, g0 = (nearRgb >> 8 & 255) / 255f, b0 = (nearRgb & 255) / 255f;
        float r1 = (farRgb >> 16 & 255) / 255f, g1 = (farRgb >> 8 & 255) / 255f, b1 = (farRgb & 255) / 255f;
        vc.addVertex(m, (float) a.x, (float) a.y, (float) a.z).setColor(r0, g0, b0, aNear);
        vc.addVertex(m, (float) b.x, (float) b.y, (float) b.z).setColor(r0, g0, b0, aNear);
        vc.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(r1, g1, b1, aFar);
        vc.addVertex(m, (float) d.x, (float) d.y, (float) d.z).setColor(r1, g1, b1, aFar);
    }

    /** One droplet as a camera-facing soft disc, tinted and faded by its kind; a fast drop is stretched along its motion into a streak. */
    private static void blot(Matrix4f m, VertexConsumer vc, Drop d, Vector3f left, Vector3f up, float partial) {
        float f = (d.age + partial) / d.life;
        float r = d.r, g = d.g, b = d.b, a;
        switch (d.type) {
            case DROP -> {
                a = 0.85f * (f > 0.8f ? (1f - f) / 0.2f : 1f);
                if (d.flare) {
                    float t = Math.min(1f, f * 1.5f);
                    r = Mth.lerp(t, r, 0.85f);
                    g = Mth.lerp(t, g, 0.93f);
                    b = Mth.lerp(t, b, 1f);
                }
            }
            case PUFF -> a = 0.35f * (1f - f);
            case FLAME, HALO, EMBER -> {
                int c = ramp(d.type == HALO ? 0.2f : d.type == EMBER ? 0.15f + 0.4f * f : f, d.hot);
                r = (c >> 16 & 255) / 255f;
                g = (c >> 8 & 255) / 255f;
                b = (c & 255) / 255f;
                a = d.type == HALO ? 0.35f : d.type == EMBER ? 0.95f * (1f - f) : 0.6f * (1f - f * f);
            }
            case SOOT -> a = 0.4f * (1f - f);
            case FOG -> a = 0.3f * (1f - f) * Math.min(1f, f * 6f);
            case MOTE -> a = 0.9f * (0.5f + 0.5f * (float) Math.sin((d.age + partial) * 1.7f + d.x * 13f)) * (1f - f);
            default -> a = 0.8f * (1f - f);
        }
        float x = Mth.lerp(partial, d.ox, d.x), y = Mth.lerp(partial, d.oy, d.y), z = Mth.lerp(partial, d.oz, d.z);
        // Half-extents along two screen axes: round by default; a drop or ember with speed is long along its screen-space motion.
        float ax = left.x, ay = left.y, az = left.z, bx = up.x, by = up.y, bz = up.z, la = d.size, lb = d.size;
        if (d.type == DROP || d.type == EMBER) {
            float sx = d.vx * left.x + d.vy * left.y + d.vz * left.z, sy = d.vx * up.x + d.vy * up.y + d.vz * up.z;
            float len = (float) Math.sqrt(sx * sx + sy * sy);
            if (len > 1e-3f) {
                sx /= len;
                sy /= len;
                ax = left.x * sx + up.x * sy;
                ay = left.y * sx + up.y * sy;
                az = left.z * sx + up.z * sy;
                bx = -left.x * sy + up.x * sx;
                by = -left.y * sy + up.y * sx;
                bz = -left.z * sy + up.z * sx;
                la = d.size * (1f + Math.min(len * 5f, 3f));
                lb = d.size * 0.7f;
            }
        }
        float px = ax * la, py = ay * la, pz = az * la, qx = bx * lb, qy = by * lb, qz = bz * lb;
        vc.addVertex(m, x + px + qx, y + py + qy, z + pz + qz).setColor(r, g, b, a).setUv(1f, 0f).setLight(0xF000F0);
        vc.addVertex(m, x - px + qx, y - py + qy, z - pz + qz).setColor(r, g, b, a).setUv(0f, 0f).setLight(0xF000F0);
        vc.addVertex(m, x - px - qx, y - py - qy, z - pz - qz).setColor(r, g, b, a).setUv(0f, 1f).setLight(0xF000F0);
        vc.addVertex(m, x + px - qx, y + py - qy, z + pz - qz).setColor(r, g, b, a).setUv(1f, 1f).setLight(0xF000F0);
    }
}
