// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/**
 * The fluid caster's flame as a shader: a {@link RenderType} whose fragment shader ({@code shaders/core/flame.fsh}) scrolls two octaves of
 * noise along the stream and maps it through the temperature ramp of {@link StreamFx}. The plain flame is the baseline: this is used only
 * while the {@code shaderFlame} setting is on and the shader loaded, so a pack or driver that breaks it costs nothing but the choice.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FlameShader {

    private static ShaderInstance shader;

    private FlameShader() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Feedback.id("flame"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    s -> shader = s);
        } catch (Exception e) {
            Feedback.LOGGER.warn("Flame shader did not load; the plain flame is drawn", e);
            shader = null;
        }
    }

    /** Whether the shader flame should be drawn now. */
    public static boolean ready() {
        return shader != null && ClientSettings.shaderFlame();
    }

    /** Additive, no depth write, drawn with the strips' own quads (position, uv, colour). */
    private static final RenderType TYPE = RenderType.create("feedback_flame", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("feedback_flame_blend", () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
                    }, () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .createCompositeState(false));

    public static RenderType type() {
        return TYPE;
    }
}
