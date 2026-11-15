// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import com.mojang.authlib.GameProfile;

import io.github.soundgoodizerfan.feedback.control.controller.ControllerCore;
import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * A robot: a light entity (no Sable body, no tip-over) that runs an ordinary Controller program. Its cards name its own channels
 * ({@link RobotChannel}); the program evaluates every {@link RobotTuning#STEP_TICKS} ticks while movement and work run every tick. The
 * battery is a loose stack (its charge drawn in batches so no per-tick allocation), the program a Punch Card in the core's slot, the
 * cargo nine slots. Picked up or destroyed it comes back as its item with all of that kept.
 *
 * <p>Prior art: PneumaticCraft: Repressurized drones (GPL-3.0), design only: a reduced program step rate against full-rate movement,
 * the real break pipeline's cancellable event, a drop-as-item death carrying program and charge. Rejected: its fake player per drone (one
 * shared per level here), item-id filters, teleporting when stuck, owner UUIDs, pressure as the only power.
 */
public class RobotEntity extends Entity implements ControllerCore.Locals {

    private static final EntityDataAccessor<ItemStack> DESIGN = SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final GameProfile PROFILE = new GameProfile(UUID.nameUUIDFromBytes("feedback:robot".getBytes()), "[Robot]");
    private static final String STATE = "Robot";
    /** The actions a robot starts first to last when the program holds several on. */
    private static final RobotChannel[] ORDER = { RobotChannel.MINE, RobotChannel.PLACE, RobotChannel.PICK_UP, RobotChannel.DROP,
            RobotChannel.TURN_LEFT, RobotChannel.TURN_RIGHT };

    private ItemStack battery = ItemStack.EMPTY;
    private final SimpleContainer cargo = new SimpleContainer(RobotTuning.CARGO);
    private final boolean[] on = new boolean[RobotChannel.values().length];
    private RobotSpec spec = RobotSpec.NONE;
    private final ControllerCore core = new ControllerCore(this::level, this::setChangedFlag, this, this::powered, s -> s.is(FTags.PUNCH_CARDS));

    /** Eu in the battery as of the last flush, and Eu drawn since: the battery stack is written once a step, not every tick. */
    private float chargeEu, pendingEu;
    private RobotChannel doing;
    private int doingTicks;
    private BlockPos doingPos;

    public RobotEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    private void setChangedFlag() {
        // an entity saves with its chunk; nothing to mark
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DESIGN, ItemStack.EMPTY);
    }

    /** The finished robot item this robot is built to. */
    public ItemStack design() {
        return entityData.get(DESIGN);
    }

    public ControllerCore core() {
        return core;
    }

    /** Sets the robot up from the item it was set down from: design, and any battery, program and cargo it carried. */
    public void setup(ItemStack held) {
        ItemStack design = held.copyWithCount(1);
        var custom = design.get(DataComponents.CUSTOM_DATA);
        CompoundTag state = custom == null ? null : custom.copyTag().getCompound(STATE);
        design.remove(DataComponents.CUSTOM_DATA);
        entityData.set(DESIGN, design);
        spec = RobotSpec.of(design);
        if (state != null && !state.isEmpty())
            loadState(state, registryAccess());
        refreshCharge();
    }

    // --- power -----------------------------------------------------------------------------------------

    private boolean powered() {
        return chargeEu - pendingEu > 0f;
    }

    private void refreshCharge() {
        CompoundTag t = BatteryStack.state(battery);
        chargeEu = t == null || !BatteryStack.ready(t) ? 0f : BatteryStack.charge(t);
    }

    /** Writes the Eu drawn since the last step onto the battery stack: one tag copy a step, none between. */
    private void flush() {
        if (pendingEu <= 0f)
            return;
        CompoundTag t = BatteryStack.state(battery);
        if (t != null) {
            CompoundTag next = t.copy();
            BatteryStack.draw(next, pendingEu);
            battery.set(FDataComponents.BATTERY.get(), next);
        }
        pendingEu = 0f;
        refreshCharge();
    }

    private boolean afford(float eu) {
        return chargeEu - pendingEu >= eu;
    }

    /** A share of the battery's charge, 0 to 1; 0 without a battery. */
    public float chargeShare() {
        CompoundTag t = BatteryStack.state(battery);
        if (t == null || !BatteryStack.ready(t))
            return 0f;
        float cap = BatteryStack.effectiveCapacity(t);
        return cap <= 0f ? 0f : Math.max(0f, Math.min(1f, (chargeEu - pendingEu) / cap));
    }

    // --- the program's own channels --------------------------------------------------------------------

    @Override
    public float read(int channel, int argument) {
        RobotChannel c = RobotChannel.of(channel);
        if (c == null)
            return 0f;
        if (c.actuator())
            return on[channel] ? 1f : 0f;
        BlockPos ahead = blockPosition().relative(facing());
        return switch (c) {
            case SOLID_AHEAD -> !level().getBlockState(ahead).getCollisionShape(level(), ahead).isEmpty() ? 1f : 0f;
            case BREAKABLE_AHEAD -> breakable(ahead) ? 1f : 0f;
            case ITEM_AHEAD -> items(ahead).isEmpty() ? 0f : 1f;
            case CARGO_FULL -> cargoFull() ? 1f : 0f;
            case CHARGE -> chargeShare();
            default -> 0f;
        };
    }

    @Override
    public void write(int channel, boolean value) {
        RobotChannel c = RobotChannel.of(channel);
        if (c != null && c.actuator())
            on[channel] = value;
    }

    private boolean cargoFull() {
        for (int i = 0; i < cargo.getContainerSize(); i++)
            if (cargo.getItem(i).isEmpty())
                return false;
        return true;
    }

    private Direction facing() {
        return Direction.fromYRot(getYRot());
    }

    private boolean breakable(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && spec.canBreak(state.getDestroySpeed(level(), pos));
    }

    private java.util.List<ItemEntity> items(BlockPos pos) {
        return level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(0.25));
    }

    // --- tick ------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide)
            return;
        pendingEu += RobotTuning.EU_IDLE_PER_TICK;
        if (tickCount % RobotTuning.STEP_TICKS == 0) {
            flush();
            if (powered())
                core.tickServer();
        }
        boolean live = powered();
        if (!live) {
            doing = null;
            for (int i = 0; i < on.length; i++)
                on[i] = false;
        }
        if (doing != null) {
            if (!on[doing.ordinal()] || !live)
                doing = null;
            else if (--doingTicks <= 0) {
                finish(doing);
                doing = null;
            }
        }
        if (doing == null && live)
            begin();
        boolean walking = doing == null && live && on[RobotChannel.WALK.ordinal()];
        if (walking)
            pendingEu += RobotTuning.EU_MOVE_PER_TICK;
        else if (onGround())
            return; // idle on the ground: nothing to move, nothing allocated
        Direction heading = facing();
        setDeltaMovement(walking ? heading.getStepX() * spec.speed() : 0, onGround() ? 0 : getDeltaMovement().y - 0.08,
                walking ? heading.getStepZ() * spec.speed() : 0);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** Starts the first action the program holds on and the robot can afford. */
    private void begin() {
        for (RobotChannel c : ORDER) {
            if (!on[c.ordinal()])
                continue;
            BlockPos ahead = blockPosition().relative(facing());
            switch (c) {
                case MINE -> {
                    if (!breakable(ahead))
                        continue;
                    float h = level().getBlockState(ahead).getDestroySpeed(level(), ahead);
                    if (!afford(h * RobotTuning.EU_PER_HARDNESS))
                        continue;
                    doingTicks = spec.digTicks(h);
                    doingPos = ahead;
                }
                case TURN_LEFT, TURN_RIGHT -> doingTicks = RobotTuning.TURN_TICKS;
                default -> doingTicks = RobotTuning.PLACE_TICKS;
            }
            doing = c;
            return;
        }
    }

    private void finish(RobotChannel c) {
        BlockPos ahead = blockPosition().relative(facing());
        switch (c) {
            case TURN_LEFT -> setYRot(getYRot() - 90f);
            case TURN_RIGHT -> setYRot(getYRot() + 90f);
            case MINE -> mine(doingPos);
            case PLACE -> place(ahead);
            case PICK_UP -> pickUp(ahead);
            case DROP -> drop(ahead);
            default -> { }
        }
    }

    /** Breaks the block through the real break event (a protection mod may veto it), paying Work from the battery; drops land in the world. */
    private void mine(BlockPos pos) {
        if (!(level() instanceof ServerLevel server) || !pos.equals(blockPosition().relative(facing())) || !breakable(pos))
            return;
        BlockState state = server.getBlockState(pos);
        float eu = state.getDestroySpeed(server, pos) * RobotTuning.EU_PER_HARDNESS;
        if (!afford(eu))
            return;
        var fake = FakePlayerFactory.get(server, PROFILE);
        fake.setPos(getX(), getY(), getZ()); // a claim or protection check sees where the robot stands
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server, pos, state, fake)).isCanceled())
            return;
        pendingEu += eu;
        server.destroyBlock(pos, true, fake);
    }

    private void place(BlockPos pos) {
        if (!(level() instanceof ServerLevel server))
            return;
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (!(s.getItem() instanceof BlockItem block))
                continue;
            var fake = FakePlayerFactory.get(server, PROFILE);
            fake.setPos(getX(), getY(), getZ());
            var hit =new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new BlockPlaceContext(server, fake, InteractionHand.MAIN_HAND, s, hit);
            if (context.canPlace() && block.place(context).consumesAction())
                return;
        }
    }

    private void pickUp(BlockPos pos) {
        for (ItemEntity item : items(pos)) {
            ItemStack left = cargo.addItem(item.getItem().copy());
            if (left.isEmpty())
                item.discard();
            else
                item.setItem(left);
        }
    }

    private void drop(BlockPos pos) {
        for (int i = 0; i < cargo.getContainerSize(); i++) {
            ItemStack s = cargo.getItem(i);
            if (s.isEmpty())
                continue;
            ItemEntity out = new ItemEntity(level(), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, s.copy());
            out.setDeltaMovement(Vec3.ZERO);
            level().addFreshEntity(out);
            cargo.setItem(i, ItemStack.EMPTY);
            return;
        }
    }

    // --- hands -----------------------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level().isClientSide)
            return InteractionResult.SUCCESS;
        if (held.getItem() instanceof DataConnectorItem) {
            selectNext(player);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            pack();
            return InteractionResult.CONSUME;
        }
        if (BatteryStack.is(held) && battery.isEmpty()) {
            battery = held.copyWithCount(1);
            refreshCharge();
            if (!player.getAbilities().instabuild)
                held.shrink(1);
            return InteractionResult.CONSUME;
        }
        if (held.is(FTags.PUNCH_CARDS)) {
            var refusal = core.refusalFor(held);
            if (refusal.isPresent()) {
                player.displayClientMessage(Component.translatable("feedback.programmer.error." + refusal.get()), true);
                return InteractionResult.CONSUME;
            }
            ItemStack old = core.getItem(0);
            core.setItem(0, held.copyWithCount(1));
            if (!player.getAbilities().instabuild)
                held.shrink(1);
            if (!old.isEmpty())
                player.getInventory().placeItemBackInInventory(old);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && !battery.isEmpty() && !player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("robot.feedback.status", Math.round(chargeShare() * 100),
                    Component.translatable(core.getItem(0).isEmpty() ? "robot.feedback.no_program" : "robot.feedback.program")), true);
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && battery.isEmpty()) {
            player.displayClientMessage(Component.translatable("robot.feedback.no_battery"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Each use of the Data Connector steps the player's selection to the next of this robot's channels, for a Programmer card's Link. */
    private void selectNext(Player player) {
        var pending = DataLinkManager.get().peekPending(player);
        int next = pending.filter(p -> p.isLocal()).map(p -> p.local() + 1).orElse(0) % RobotChannel.values().length;
        DataLinkManager.get().selectLocal(player, RobotChannel.of(next).ref());
        player.displayClientMessage(Component.translatable("robot.feedback.selected", Component.translatable(RobotChannel.of(next).langKey())), true);
    }

    /** The robot as its item, its battery, program and cargo inside. */
    private ItemStack asItem() {
        flush();
        ItemStack out = design().copy();
        CompoundTag tag = new CompoundTag();
        tag.put(STATE, saveState(registryAccess()));
        out.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        return out;
    }

    private void pack() {
        ItemStack out = asItem();
        spawnAtLocation(out);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source))
            return false;
        pack();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return design().copy();
    }

    // --- save ------------------------------------------------------------------------------------------

    private CompoundTag saveState(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        if (!battery.isEmpty())
            t.put("Battery", battery.save(provider));
        t.put("Cargo", cargo.createTag(provider));
        CompoundTag c = new CompoundTag();
        core.save(c, provider);
        t.put("Core", c);
        return t;
    }

    private void loadState(CompoundTag t, HolderLookup.Provider provider) {
        battery = t.contains("Battery") ? ItemStack.parseOptional(provider, t.getCompound("Battery")) : ItemStack.EMPTY;
        if (t.contains("Cargo", Tag.TAG_LIST))
            cargo.fromTag(t.getList("Cargo", Tag.TAG_COMPOUND), provider);
        core.load(t.getCompound("Core"), provider);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Design", design().saveOptional(registryAccess()));
        tag.put(STATE, saveState(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ItemStack design = ItemStack.parseOptional(registryAccess(), tag.getCompound("Design"));
        entityData.set(DESIGN, design);
        spec = RobotSpec.of(design);
        loadState(tag.getCompound(STATE), registryAccess());
        refreshCharge();
    }
}
