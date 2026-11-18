// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

import io.github.soundgoodizerfan.feedback.control.controller.ControllerCore;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The arm: a robot fixed on a mount, run off an electrical line instead of a battery (electricity moves work; the line pays, so an action
 * costs the arm no charge of its own). It runs the same kind of program as a robot, through the same {@link RobotWorker}, on the space
 * ahead of it; turning swings it a quarter on its mount. It carries cargo as a robot does and has a fixed tool head.
 */
public class RobotArmBlockEntity extends ElectricNode implements ControllerCore.Locals, RobotWorker.Body {

    private static final RobotSpec BARE = new RobotSpec(0f, 0f, RobotTuning.ARM_HEAD_ST, "", RobotSpec.WHEELS, false, false, 0f);
    private static final RobotChannel[] ORDER = { RobotChannel.MINE, RobotChannel.PLACE, RobotChannel.PICK_UP, RobotChannel.DROP,
            RobotChannel.TURN_LEFT, RobotChannel.TURN_RIGHT, RobotChannel.GIVE, RobotChannel.TAKE, RobotChannel.USE };

    private final SimpleContainer cargo = new SimpleContainer(RobotTuning.CARGO);
    private final boolean[] on = new boolean[RobotChannel.values().length];
    private final ControllerCore core = new ControllerCore(this, this, () -> getAmps() > 0f, s -> s.is(FTags.PUNCH_CARDS)).cardCap(() -> spec().cards());
    private final BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
    private RobotChannel doing;
    private int doingTicks;
    private BlockPos doingPos;
    private boolean switched;
    /** The circuit seated in the arm, whose figures are its dexterity. */
    private ItemStack brain = ItemStack.EMPTY;
    private RobotSpec spec = BARE;

    /** Client only: the angle the arm's head is drawn at (degrees clockwise from north, as the blockstate's y rotation) and the time it was drawn. */
    private float visualYaw = Float.NaN;
    private float visualLast = -1f;

    /** Degrees a tick the head swings toward a new facing. */
    private static final float SWING_DEGREES = 14f;

    public RobotArmBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROBOT_ARM.get(), pos, state);
        cargo.addListener(c -> setChanged());
    }

    /**
     * The head's angle at this frame (client): the facing is a blockstate and changes at once, so the head swings round to it over a few
     * ticks by the short way instead of snapping.
     */
    public float visualYaw(float partialTick) {
        float target = (facing().toYRot() + 180f) % 360f;
        if (level == null || Float.isNaN(visualYaw)) {
            visualYaw = target;
            return visualYaw;
        }
        float now = level.getGameTime() + partialTick;
        float dt = visualLast < 0f ? 0f : Math.max(0f, now - visualLast);
        visualLast = now;
        float diff = ((target - visualYaw + 540f) % 360f) - 180f;
        float step = SWING_DEGREES * dt;
        visualYaw = Math.abs(diff) <= step ? target : (visualYaw + Math.signum(diff) * step + 360f) % 360f;
        return visualYaw;
    }

    public ControllerCore core() {
        return core;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return core.getItem(0).isEmpty() ? 0f : RobotTuning.ARM_AMPS;
    }

    // --- body ------------------------------------------------------------------------------------------

    @Override
    public Level level() {
        return level;
    }

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    @Override
    public Direction facing() {
        return getBlockState().getValue(RobotArmBlock.FACING);
    }

    @Override
    public Vec3 centre() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    public RobotSpec spec() {
        // The brain is read at the warmth it is at now.
        return brain.isEmpty() || level == null ? spec : BARE.withBrain(brain, level);
    }

    private void setBrainQuietly(ItemStack circuit) {
        brain = circuit;
        spec = brain.isEmpty() ? BARE : BARE.withBrain(brain);
    }

    public ItemStack brain() {
        return brain;
    }

    /** Seats a circuit as the arm's brain (replacing any); its figures become the arm's dexterity. */
    public void setBrain(ItemStack circuit) {
        brain = circuit;
        spec = brain.isEmpty() ? BARE : BARE.withBrain(brain);
        setChanged();
    }

    @Override
    public SimpleContainer cargo() {
        return cargo;
    }

    @Override
    public long owner() {
        return worldPosition.asLong();
    }

    @Override
    public boolean afford(float eu) {
        return getAmps() > 0f;
    }

    @Override
    public void spend(float eu) {
        // the line pays
    }

    // --- program channels ------------------------------------------------------------------------------

    @Override
    public float read(int channel, int argument) {
        RobotChannel c = RobotChannel.of(channel);
        if (c == null)
            return 0f;
        if (c.actuator())
            return on[channel] ? 1f : 0f;
        float shared = RobotWorker.sense(this, c, scan);
        return Float.isNaN(shared) ? 0f : shared;
    }

    @Override
    public void write(int channel, boolean value) {
        RobotChannel c = RobotChannel.of(channel);
        if (c != null && c.actuator())
            on[channel] = value;
    }

    // --- tick ------------------------------------------------------------------------------------------

    public void tickServer() {
        if (level == null)
            return;
        boolean live = getAmps() > 0f;
        if (level.getGameTime() % RobotTuning.STEP_TICKS == 0 && live) {
            core.tickServer();
            boolean want = on[RobotChannel.SWITCH.ordinal()];
            if (want != switched) {
                switched = want;
                if (level.getBlockEntity(worldPosition.relative(facing())) instanceof io.github.soundgoodizerfan.feedback.control.Switchable sw)
                    sw.setEngaged(want);
            }
        }
        if (!live) {
            if (doing == RobotChannel.MINE)
                RobotClaims.release(level, doingPos, owner());
            doing = null;
            return;
        }
        if (doing != null) {
            if (!on[doing.ordinal()]) {
                if (doing == RobotChannel.MINE)
                    RobotClaims.release(level, doingPos, owner());
                doing = null;
            } else if (--doingTicks <= 0) {
                finish(doing);
                doing = null;
            }
        }
        if (doing == null)
            begin();
    }

    private void begin() {
        BlockPos ahead = null;
        for (RobotChannel c : ORDER) {
            if (!on[c.ordinal()])
                continue;
            if (ahead == null)
                ahead = worldPosition.relative(facing());
            int ticks = RobotWorker.begin(this, c, ahead);
            if (ticks < 0)
                continue;
            doing = c;
            doingTicks = ticks;
            doingPos = ahead;
            return;
        }
    }

    private void finish(RobotChannel c) {
        switch (c) {
            case TURN_LEFT -> level.setBlock(worldPosition, getBlockState().setValue(RobotArmBlock.FACING, facing().getCounterClockWise()), 3);
            case TURN_RIGHT -> level.setBlock(worldPosition, getBlockState().setValue(RobotArmBlock.FACING, facing().getClockWise()), 3);
            default -> RobotWorker.finish(this, c, worldPosition.relative(facing()));
        }
    }

    // --- items -----------------------------------------------------------------------------------------

    /** Drops what the arm holds: its cargo and its program card. */
    public java.util.List<ItemStack> contents() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < cargo.getContainerSize(); i++)
            if (!cargo.getItem(i).isEmpty())
                out.add(cargo.getItem(i));
        if (!core.getItem(0).isEmpty())
            out.add(core.getItem(0));
        if (!brain.isEmpty())
            out.add(brain);
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cargo", cargo.createTag(registries));
        CompoundTag c = new CompoundTag();
        core.save(c, registries);
        tag.put("Core", c);
        if (!brain.isEmpty())
            tag.put("Brain", brain.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cargo", Tag.TAG_LIST))
            cargo.fromTag(tag.getList("Cargo", Tag.TAG_COMPOUND), registries);
        core.load(tag.getCompound("Core"), registries);
        if (tag.contains("Brain"))
            setBrainQuietly(ItemStack.parseOptional(registries, tag.getCompound("Brain")));
    }
}
