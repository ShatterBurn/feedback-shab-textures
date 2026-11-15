// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import com.mojang.authlib.GameProfile;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * What a robot does to the world, shared by the {@link RobotEntity} that rolls about and the arm on its mount ({@code RobotArmBlockEntity}):
 * each works from a {@link Body} that says where it stands, which way it faces, what it carries and what it can pay. Breaking goes through
 * the real break event on one shared fake player per level; the drops are the block's own loot, whatever the tool.
 */
public final class RobotWorker {

    private RobotWorker() {
    }

    /** The thing doing the work: a robot or an arm. */
    public interface Body {
        Level level();

        BlockPos pos();

        Direction facing();

        Vec3 centre();

        RobotSpec spec();

        SimpleContainer cargo();

        /** An id that tells this body's block claims from another's. */
        long owner();

        boolean afford(float eu);

        void spend(float eu);
    }

    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("feedback:robot".getBytes()), "[Robot]");

    // --- what it senses ahead --------------------------------------------------------------------------

    public static boolean solid(Body b, BlockPos pos) {
        return !b.level().getBlockState(pos).getCollisionShape(b.level(), pos).isEmpty();
    }

    public static boolean breakable(Body b, BlockPos pos) {
        BlockState state = b.level().getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && b.spec().canBreak(state.getDestroySpeed(b.level(), pos));
    }

    public static List<ItemEntity> items(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.25));
    }

    public static boolean cargoFull(SimpleContainer cargo) {
        for (int i = 0; i < cargo.getContainerSize(); i++)
            if (cargo.getItem(i).isEmpty())
                return false;
        return true;
    }

    /** Blocks to the first solid thing ahead, as far as a rangefinder reaches. */
    public static float range(Body b, BlockPos.MutableBlockPos scan) {
        for (int i = 1; i <= RobotTuning.RANGE_MAX; i++) {
            scan.set(b.pos()).move(b.facing(), i);
            if (!b.level().getBlockState(scan).getCollisionShape(b.level(), scan).isEmpty())
                return i;
        }
        return RobotTuning.RANGE_MAX;
    }

    public static float hardness(Body b, BlockPos pos) {
        BlockState state = b.level().getBlockState(pos);
        if (state.isAir())
            return 0f;
        float h = state.getDestroySpeed(b.level(), pos);
        return h < 0f ? 100f : h;
    }

    /**
     * The senses a robot and an arm share, or NaN for a channel that is not one of them. A sense that needs a fitted sensor reads 0 without it.
     */
    public static float sense(Body b, RobotChannel c, BlockPos.MutableBlockPos scan) {
        BlockPos ahead = b.pos().relative(b.facing());
        RobotSpec spec = b.spec();
        return switch (c) {
            case SOLID_AHEAD -> solid(b, ahead) ? 1f : 0f;
            case BREAKABLE_AHEAD -> breakable(b, ahead) ? 1f : 0f;
            case ITEM_AHEAD -> items(b.level(), ahead).isEmpty() ? 0f : 1f;
            case CARGO_FULL -> cargoFull(b.cargo()) ? 1f : 0f;
            case DOCK_AHEAD -> b.level().getBlockState(ahead).is(FTags.ROBOT_DOCKS) ? 1f : 0f;
            case RANGE_AHEAD -> spec.sensor().equals(RobotSpec.RANGEFINDER) ? range(b, scan) : 0f;
            case HARDNESS_AHEAD -> spec.sensor().equals(RobotSpec.PROBE) ? hardness(b, ahead) : 0f;
            default -> Float.NaN;
        };
    }

    // --- what it does ----------------------------------------------------------------------------------

    /**
     * Ticks the action takes, or -1 if it cannot start now (nothing to break, no money, a block another robot has claimed). A mine claims
     * the block for those ticks.
     */
    public static int begin(Body b, RobotChannel c, BlockPos ahead) {
        return switch (c) {
            case MINE -> {
                if (!breakable(b, ahead))
                    yield -1;
                float h = b.level().getBlockState(ahead).getDestroySpeed(b.level(), ahead);
                if (!b.afford(h * RobotTuning.EU_PER_HARDNESS))
                    yield -1;
                int ticks = b.spec().digTicks(h);
                if (!RobotClaims.claim(b.level(), ahead, b.owner(), b.level().getGameTime() + ticks + RobotTuning.STEP_TICKS))
                    yield -1;
                yield ticks;
            }
            case TURN_LEFT, TURN_RIGHT -> RobotTuning.TURN_TICKS;
            case PLACE, PICK_UP, DROP, GIVE, TAKE -> RobotTuning.PLACE_TICKS;
            default -> -1;
        };
    }

    /** Carries out a finished action on the space {@code ahead}. Turning is the body's own. */
    public static void finish(Body b, RobotChannel c, BlockPos ahead) {
        switch (c) {
            case MINE -> mine(b, ahead);
            case PLACE -> place(b, ahead);
            case PICK_UP -> pickUp(b, ahead);
            case DROP -> drop(b, ahead);
            case GIVE -> give(b, ahead);
            case TAKE -> take(b, ahead);
            default -> { }
        }
    }

    private static net.neoforged.neoforge.common.util.FakePlayer actor(Body b, ServerLevel server) {
        var fake = FakePlayerFactory.get(server, PROFILE);
        fake.setPos(b.centre().x, b.centre().y, b.centre().z); // a claim or protection check sees where the robot stands
        return fake;
    }

    /** Breaks the block through the real break event (a protection mod may veto it), paying Work from the battery; drops land in the world. */
    private static void mine(Body b, BlockPos pos) {
        RobotClaims.release(b.level(), pos, b.owner());
        if (!(b.level() instanceof ServerLevel server) || !breakable(b, pos))
            return;
        BlockState state = server.getBlockState(pos);
        float eu = state.getDestroySpeed(server, pos) * RobotTuning.EU_PER_HARDNESS;
        if (!b.afford(eu))
            return;
        var fake = actor(b, server);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, fake)).isCanceled())
            return;
        b.spend(eu);
        server.destroyBlock(pos, true, fake);
    }

    private static void place(Body b, BlockPos pos) {
        if (!(b.level() instanceof ServerLevel server))
            return;
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (!(s.getItem() instanceof BlockItem block))
                continue;
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new BlockPlaceContext(server, actor(b, server), InteractionHand.MAIN_HAND, s, hit);
            if (context.canPlace() && block.place(context).consumesAction())
                return;
        }
    }

    private static IItemHandler port(Body b, BlockPos pos) {
        return b.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, b.facing().getOpposite());
    }

    /** Offers the first stack it carries to the item port ahead; what the port will not take stays in the cargo. */
    private static void give(Body b, BlockPos pos) {
        IItemHandler handler = port(b, pos);
        if (handler == null)
            return;
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (s.isEmpty())
                continue;
            cargo.setItem(i, ItemHandlerHelper.insertItem(handler, s, false));
            return;
        }
    }

    /** Takes the first stack the item port ahead lets out, as much as the cargo has room for. */
    private static void take(Body b, BlockPos pos) {
        IItemHandler handler = port(b, pos);
        if (handler == null)
            return;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack out = handler.extractItem(i, 64, false);
            if (out.isEmpty())
                continue;
            ItemStack left = b.cargo().addItem(out);
            if (!left.isEmpty())
                ItemHandlerHelper.insertItem(handler, left, false);
            return;
        }
    }

    private static void pickUp(Body b, BlockPos pos) {
        for (ItemEntity item : items(b.level(), pos)) {
            ItemStack left = b.cargo().addItem(item.getItem().copy());
            if (left.isEmpty())
                item.discard();
            else
                item.setItem(left);
        }
    }

    private static void drop(Body b, BlockPos pos) {
        SimpleContainer cargo = b.cargo();
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (s.isEmpty())
                continue;
            ItemEntity out = new ItemEntity(b.level(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, s.copy());
            out.setDeltaMovement(Vec3.ZERO);
            b.level().addFreshEntity(out);
            cargo.setItem(i, ItemStack.EMPTY);
            return;
        }
    }

    /** Each use of the Data Connector steps the player's selection to the next of a robot's channels, for a Programmer card's Link. */
    public static void selectNext(Player player) {
        var pending = DataLinkManager.get().peekPending(player);
        int next = pending.filter(p -> p.isLocal()).map(p -> p.local() + 1).orElse(0) % RobotChannel.values().length;
        DataLinkManager.get().selectLocal(player, RobotChannel.of(next).ref());
        player.displayClientMessage(Component.translatable("robot.feedback.selected", Component.translatable(RobotChannel.of(next).langKey())), true);
    }
}
