// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.entity.EntityTypeTest;
import java.util.List;

/**
 * Everything alive in the Gap shrinks with the bodies there (docs/specs/feedback_space_spec.md 2.4): size, reach, step height, jump,
 * walking speed and the fall that hurts. One transient multiplier on each attribute, so it is never saved: a person who logs out
 * in the Gap comes back at full size and is scaled again by the next sweep. The sweep runs once a second in the Gap and costs one
 * attribute write per living thing there; a crossing applies and clears at once.
 */
public final class ScaleAboard {
    private static final ResourceLocation MODIFIER = Feedback.id("body_scale");
    private static final List<Holder<Attribute>> SCALED = List.of(Attributes.SCALE, Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE, Attributes.STEP_HEIGHT, Attributes.JUMP_STRENGTH, Attributes.MOVEMENT_SPEED,
            Attributes.SAFE_FALL_DISTANCE);

    private ScaleAboard() {
    }

    /** Ticks between sweeps of the Gap. */
    public static final int SWEEP_TICKS = 20;

    /** The sweep: everything alive in the Gap is at the Gap's scale (a body there was scaled by the crossing; a person off any body is as small). */
    public static void sweep(ServerLevel gap) {
        for (LivingEntity e : gap.getEntities(EntityTypeTest.forClass(LivingEntity.class), x -> true))
            set(e, SpaceTuning.GAP_SCALE);
    }

    /** Puts {@code e} at scale {@code k} (1 removes the multiplier). */
    public static void set(LivingEntity e, double k) {
        for (Holder<Attribute> a : SCALED) {
            AttributeInstance inst = e.getAttribute(a);
            if (inst == null)
                continue;
            AttributeModifier now = inst.getModifier(MODIFIER);
            if (Math.abs(k - 1) < 1e-9) {
                if (now != null)
                    inst.removeModifier(MODIFIER);
            } else if (now == null || Math.abs(now.amount() - (k - 1)) > 1e-9) {
                inst.removeModifier(MODIFIER);
                inst.addTransientModifier(new AttributeModifier(MODIFIER, k - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }
}
