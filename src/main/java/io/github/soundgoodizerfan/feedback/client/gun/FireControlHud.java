// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.weapon.FireControl;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.SmartFuse;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The fire-control readout, a small card above the ammunition card while a gun with a live fire-control circuit is held: range to whatever the
 * crosshair is on, the lead to aim ahead of a moving target and the drop of the round over that range, each stepped as coarse as the circuit is
 * noisy ({@link FireControl}). A launcher with a live fuse-control circuit shows its fuse mode here instead. The reading is worked out client-side
 * from the same figures the shot flies by, once a tick; nothing is sent.
 */
public final class FireControlHud {

    private static final GuiSkin SKIN = GuiSkin.VANILLA;

    private static long at = -1;
    private static FireControl.Solution solution = FireControl.Solution.NONE;
    private static boolean fireControl, aimed;
    private static SmartFuse.Mode fuse = null;
    private static ItemStack lastGun = ItemStack.EMPTY;

    private FireControlHud() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("gun_fire_control"), FireControlHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !(mc.player.getMainHandItem().getItem() instanceof GunItem))
            return;
        ItemStack gun = mc.player.getMainHandItem();
        long now = mc.level.getGameTime();
        if (now != at || gun != lastGun) {
            at = now;
            lastGun = gun;
            read(mc, gun);
        }
        if (!fireControl && fuse == null)
            return;
        Font font = mc.font;
        int right = g.guiWidth() - 12, top = g.guiHeight() - 80 - 6 - 40;
        if (fuse != null) {
            Component line = Component.translatable("hud.feedback.fuse", Component.translatable(fuse.langKey()));
            int w = font.width(line);
            GuiStyle.hudCard(g, right - w - 10, top + 26, right + 6, top + 40);
            g.drawString(font, line, right - w, top + 30, SKIN.ink(), false);
            return;
        }
        Component[] lines = solution.valid()
                ? new Component[] {Component.translatable("hud.feedback.fire_control.range", fmt(solution.range())),
                        Component.translatable("hud.feedback.fire_control.lead", fmt(solution.lead())),
                        Component.translatable("hud.feedback.fire_control.drop", fmt(solution.drop()))}
                : new Component[] {Component.translatable("hud.feedback.fire_control.no_target")};
        int w = 0;
        for (Component c : lines)
            w = Math.max(w, font.width(c));
        int y0 = top + 40 - 4 - 10 * lines.length;
        GuiStyle.hudCard(g, right - w - 10, y0 - 4, right + 6, top + 40);
        for (int i = 0; i < lines.length; i++)
            g.drawString(font, lines[i], right - w, y0 + 10 * i, solution.valid() ? SKIN.ink() : SKIN.dim(), false);
    }

    private static String fmt(float value) {
        return value >= 10f ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void read(Minecraft mc, ItemStack gun) {
        var level = mc.level;
        Circuit c = FireControl.held(gun, FireControl.FIRE_CONTROL, level);
        fireControl = c.live();
        fuse = SmartFuse.fitted(gun, level) ? SmartFuse.mode(gun) : null;
        solution = FireControl.Solution.NONE;
        if (!fireControl)
            return;
        GunFigures f = ((GunItem) gun.getItem()).mechanism().predict(mc.player, gun, level, ClientMaterials.on(level), 0);
        if (f == null)
            return;
        float reach = FireControl.reach(c);
        Vec3 eye = mc.player.getEyePosition(), look = mc.player.getLookAngle(), end = eye.add(look.scale(reach));
        double range = reach + 1;
        Vec3 velocity = Vec3.ZERO;
        HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() != HitResult.Type.MISS)
            range = eye.distanceTo(block.getLocation());
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(mc.player, eye, end, new AABB(eye, end).inflate(1),
                e -> !e.isSpectator() && e.isPickable(), range * range);
        if (hit != null) {
            Entity e = hit.getEntity();
            range = eye.distanceTo(hit.getLocation());
            velocity = e.getDeltaMovement();
        }
        solution = FireControl.solve(c, range, look, velocity, f.muzzleSpeed());
    }
}
