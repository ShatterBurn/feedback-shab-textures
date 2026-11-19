// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * A mote: one finespace voxel of a block, cut from it (64 to a block). It carries the block as the {@code feedback:mote} component, so motes
 * of one block stack and a mote is whatever it was cut from. Placing one is {@link MicroPlacer}'s: a click on any block puts the mote in the
 * voxel in front of the face and makes the spot finespace if it was air.
 */
public class MoteItem extends Item {

    /** Blocks that are never cut into motes, whatever their shape. */
    public static final TagKey<Block> BLOCKED = TagKey.create(Registries.BLOCK, Feedback.id("mote_blocked"));
    /** Blocks that are cut into motes although they fail the shape test. */
    public static final TagKey<Block> FORCED = TagKey.create(Registries.BLOCK, Feedback.id("mote_forced"));

    public MoteItem(Properties properties) {
        super(properties);
    }

    /** Motes of {@code block}. */
    public static ItemStack of(Block block, int count) {
        ItemStack stack = new ItemStack(FItems.MOTE.get(), count);
        stack.set(FDataComponents.MOTE.get(), BuiltInRegistries.BLOCK.getKey(block));
        return stack;
    }

    /** The state the mote is made of, or null for one that carries none. */
    @Nullable
    public static BlockState state(ItemStack stack) {
        ResourceLocation id = stack.get(FDataComponents.MOTE.get());
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).map(Block::defaultBlockState).orElse(null);
    }

    /**
     * Whether a block can be cut into motes: a solid, collidable whole cube drawn from a model, with no block entity and nothing of its own to
     * do (a rotation part or a wedge is a part, placed as itself). The shape is the test, not the id; the two block tags override it.
     */
    public static boolean eligible(BlockState state) {
        if (state.is(BLOCKED) || state.isAir() || !state.getFluidState().isEmpty())
            return false;
        if (state.is(FORCED))
            return true;
        Block block = state.getBlock();
        return !(block instanceof MicroRotation) && !(block instanceof MicroWedgeBlock) && !state.hasBlockEntity()
                && state.getRenderShape() == RenderShape.MODEL && state.getDestroySpeed(EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) >= 0
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockState state = state(stack);
        return state == null ? super.getName(stack) : Component.translatable("item.feedback.mote.of", state.getBlock().getName());
    }
}
