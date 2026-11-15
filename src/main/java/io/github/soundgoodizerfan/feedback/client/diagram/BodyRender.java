// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.diagram;

import java.util.Collection;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.VanillaSubLevelBlockEntityRenderer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import foundry.veil.api.client.render.CameraMatrices;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.impl.client.render.perspective.LevelPerspectiveCamera;
import io.github.soundgoodizerfan.feedback.Feedback;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Draws a Sable body, and any bodies joined to it, as its real blocks into an offscreen target, from a
 * fixed angle about the body's own axes, then lets the diagram's ink pass (the {@code feedback:diagram}
 * Veil pipeline) turn it into line work. The Contraption Diagram's page shows the target.
 * <p>
 * Read from Create Simulated's contraption diagram (MIT; design only): an orthographic camera sized from
 * the plot box, drawn through Sable's own section-layer dispatcher into a Veil framebuffer, redrawn about
 * twelve times a second, then a post pass. Taken: that structure. Rejected: its custom light texture and
 * dispatcher mixins, its Flywheel embedding and entity pass, its shader, palette and dither assets.
 */
public final class BodyRender implements AutoCloseable {

    public static final int SIZE = 256;
    private static final float FPS = 12f;
    private static final Logger LOG = LoggerFactory.getLogger(BodyRender.class);
    private static final LevelPerspectiveCamera CAMERA = new LevelPerspectiveCamera();
    private static final Matrix4f TRANSFORM = new Matrix4f();
    private static final Matrix4f BACKUP_PROJECTION = new Matrix4f();
    private static final CameraMatrices BACKUP_MATRICES = new CameraMatrices();
    private static boolean entitiesFailed;

    private AdvancedFbo fbo = AdvancedFbo.withSize(SIZE, SIZE).addColorTextureBuffer().setDepthTextureBuffer().build(true);
    private AdvancedFbo inked = AdvancedFbo.withSize(SIZE, SIZE).addColorTextureBuffer().build(true);
    private float sinceDraw = 1000f;
    private boolean drawn, usedInk, failed;

    /** The camera's orientation in the body's plot frame; the arrows are projected with the same one. */
    public final Quaternionf view = new Quaternionf();
    /** The point the camera looks at, in the body's plot frame. */
    public final Vector3d centre = new Vector3d();
    /** Half the width of the page in blocks. */
    public float radius = 2f;

    public boolean ready() {
        return drawn;
    }

    /**
     * Redraws at twelve frames a second.
     *
     * @param root the body the sensor sits on; the view is fixed to its frame
     * @param group root and every body drawn with it
     * @param extent blocks the drawing must fit, the root's plot box plus any joined bodies
     */
    public void render(ClientSubLevel root, Collection<ClientSubLevel> group, float yaw, float pitch, float extent, float partialTicks) {
        if (failed || fbo == null || VeilLevelPerspectiveRenderer.isRenderingPerspective())
            return;
        try {
            draw(root, group, yaw, pitch, extent, partialTicks);
        } catch (RuntimeException e) {
            failed = true;
            LOG.warn("The diagram cannot draw the body; arrows only from now on", e);
        } finally {
            AdvancedFbo.unbind();
        }
    }

    private void draw(ClientSubLevel root, Collection<ClientSubLevel> group, float yaw, float pitch, float extent, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        sinceDraw += mc.getTimer().getRealtimeDeltaTicks();
        if (sinceDraw < 20f / FPS && drawn)
            return;
        sinceDraw = 0;

        BoundingBox3ic box = root.getPlot().getBoundingBox();
        centre.set((box.minX() + box.maxX() + 1) / 2.0, (box.minY() + box.maxY() + 1) / 2.0, (box.minZ() + box.maxZ() + 1) / 2.0);
        radius = Math.max(2f, (extent + 1) * 0.55f);
        view.identity().rotateY(yaw).rotateX(pitch);

        Matrix4f projection = new Matrix4f().ortho(-radius, radius, -radius, radius, 0.1f, radius * 2f);
        Vector3d local = view.transform(new Vector3d(0, 0, radius)).add(centre);
        Vector3d camera = new Vector3d();
        Pose3dc pose = root.renderPose(partialTicks);
        pose.transformPosition(local, camera);

        fbo.bind(true);
        fbo.clear();
        Quaternionf orientation = new Quaternionf(pose.orientation()).conjugate();
        orientation.premul(view.conjugate(new Quaternionf()));
        group(fbo, root.getLevel(), group, projection, camera, orientation, partialTicks);

        PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        PostPipeline pipeline = manager.getPipeline(Feedback.id("diagram"));
        usedInk = pipeline != null;
        if (pipeline != null) {
            pipeline.getUniformSafe("LineColor").setVector(0.91f, 0.945f, 1f, 1f);
            pipeline.getUniformSafe("WashColor").setVector(0.16f, 0.37f, 0.62f, 0.55f);
            pipeline.getUniformSafe("InSize").setVector(SIZE, SIZE);
            PostPipeline.Context context = manager.getPostPipelineContext();
            context.setFramebuffer(Feedback.id("diagram"), fbo);
            context.setFramebuffer(Feedback.id("diagram_final"), inked);
            manager.runPipeline(pipeline, false);
        }
        AdvancedFbo.unbind();
        drawn = true;
    }

    private static void group(AdvancedFbo target, ClientLevel level, Collection<ClientSubLevel> subLevels, Matrix4f projection, Vector3d cameraPosition,
                              Quaternionf orientation, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        buffers.endBatch();
        if (subLevels.isEmpty())
            return;
        GameRenderer gameRenderer = mc.gameRenderer;
        LightTexture lightTexture = gameRenderer.lightTexture();
        VanillaSubLevelBlockEntityRenderer beRenderer = new VanillaSubLevelBlockEntityRenderer(mc.getBlockEntityRenderDispatcher(),
                mc.renderBuffers(), new Long2ObjectOpenHashMap<>());

        CAMERA.setup(cameraPosition, null, level, orientation, 0f);
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(CAMERA.rotation());

        BACKUP_PROJECTION.set(RenderSystem.getProjectionMatrix());
        gameRenderer.resetProjectionMatrix(TRANSFORM.set(projection));
        CameraMatrices matrices = VeilRenderSystem.renderer().getCameraMatrices();
        matrices.backup(BACKUP_MATRICES);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.mul(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        VeilRenderSystem.renderer().getDynamicBufferManger().getDynamicFbo(target).bind(true);
        try {
            Lighting.setupNetherLevel();
            for (RenderType layer : RenderType.chunkBufferLayers()) {
                layer.setupRenderState();
                ShaderInstance shader = RenderSystem.getShader();
                shader.setDefaultUniforms(VertexFormat.Mode.QUADS, RenderSystem.getModelViewMatrix(), projection, mc.getWindow());
                shader.apply();
                SubLevelRenderDispatcher.get().renderSectionLayer(subLevels, layer, shader, cameraPosition.x, cameraPosition.y,
                        cameraPosition.z, RenderSystem.getModelViewMatrix(), projection, partialTicks);
                VertexConsumer consumer = buffers.getBuffer(layer);
                for (ClientSubLevel sub : subLevels) {
                    SubLevelRenderData data = sub.getRenderData();
                    if (data instanceof VanillaSingleSubLevelRenderData single)
                        single.renderSingleBlock(layer, consumer, new Matrix4f(), cameraPosition.x, cameraPosition.y, cameraPosition.z);
                }
                buffers.endBatch(layer);
                shader.clear();
                layer.clearRenderState();
            }
            if (!entitiesFailed) {
                try {
                    SubLevelRenderDispatcher.get().renderBlockEntities(subLevels, beRenderer, cameraPosition.x, cameraPosition.y,
                            cameraPosition.z, partialTicks);
                } catch (RuntimeException e) {
                    entitiesFailed = true;
                    LOG.warn("The diagram cannot draw block entities; blocks only from now on", e);
                }
            }
            buffers.endBatch();
        } finally {
            matrices.restore(BACKUP_MATRICES);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(BACKUP_PROJECTION, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
            AdvancedFbo.unbind();
            Lighting.setupFor3DItems();
            lightTexture.updateLightTexture(partialTicks);
        }
    }

    /** Puts the last drawing on the GUI at {@code x, y}, {@code size} pixels square. */
    public void blit(GuiGraphics g, int x, int y, int size) {
        if (!drawn)
            return;
        int id = (usedInk ? inked : fbo).getColorTextureAttachment(0).getId();
        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.addVertex(m, x, y, 0f).setUv(0f, 1f).setColor(0xFFFFFFFF);
        b.addVertex(m, x, y + size, 0f).setUv(0f, 0f).setColor(0xFFFFFFFF);
        b.addVertex(m, x + size, y + size, 0f).setUv(1f, 0f).setColor(0xFFFFFFFF);
        b.addVertex(m, x + size, y, 0f).setUv(1f, 1f).setColor(0xFFFFFFFF);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @Override
    public void close() {
        if (fbo != null) {
            fbo.free();
            fbo = null;
        }
        if (inked != null) {
            inked.free();
            inked = null;
        }
    }
}
