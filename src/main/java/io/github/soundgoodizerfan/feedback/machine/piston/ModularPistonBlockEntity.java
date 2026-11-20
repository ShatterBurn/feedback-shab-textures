// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.piston;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidNetwork;
import io.github.soundgoodizerfan.feedback.core.hazard.PressureBurst;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.machine.Mendable;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureHolder;
import io.github.soundgoodizerfan.feedback.modular.bake.FigureStore;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The modular piston's working state: how many blocks of rod are out, whether it is asked to be out, the seal's condition and the figures its
 * design baked. It does nothing while idle: a signal or a switch schedules one block tick per block of stroke ({@link PistonTuning#STEP_TICKS} apart)
 * and the tick does one step. An extending step draws gas from a handler touching the cylinder on any face but its front (the swept volume of the
 * bore, plus what the seal lets by), pushes the line in front of the head one block if its summed mass is within what the bore and the supply's
 * pressure can move (a heavier line stalls: the gas is spent and the seal wears), and moves the head on. Gas over the cylinder's danger line
 * bursts it. A retracting step draws nothing (the spring returns it); a slime face pulls the one block behind the head back with it. Redstone says
 * when, like a lever; a controller switch ({@link Switchable}) says the same. Neither moves anything without gas.
 * <p>
 * Pushing runs on vanilla's moving-block pieces ({@link MovingPistonBlock}) but with its own line rule (mass, not vanilla's count of 12).
 */
public class ModularPistonBlockEntity extends BlockEntity implements Switchable, DataNode, Mendable, FigureHolder {

    private final FigureStore figures = new FigureStore();
    private int out;
    private boolean redstone;
    private boolean switched;
    private float condition = 1f;
    private boolean scheduled;

    public ModularPistonBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.MODULAR_PISTON.get(), pos, state);
    }

    @Override
    public FigureStore figures() {
        return figures;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        figures.set(input.get(FDataComponents.BLOCK_FIGURES.get()), input.get(FModular.DESIGN.get()), input.get(FModular.FIGURES.get()));
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        figures.collect(components);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        figures.save(tag, registries);
        tag.putInt("Out", out);
        tag.putBoolean("Redstone", redstone);
        tag.putBoolean("Switched", switched);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        figures.load(tag, registries);
        out = tag.getInt("Out");
        redstone = tag.getBoolean("Redstone");
        switched = tag.getBoolean("Switched");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        figures.save(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- figures ------------------------------------------------------------------------------

    /** Blocks of stroke the rod gives. */
    public int reach() {
        return Math.max(1, Math.round(figures.get("Reach", 1f)));
    }

    /** A slime (or other sticky) face pulls the block behind the head back with it. */
    public boolean sticky() {
        return figures.get("Sticky", 0f) > 0f;
    }

    private float area() {
        return figures.get("Area", PistonTuning.DEFAULT_AREA_CM2);
    }

    private float leak() {
        return figures.get("Slip", PistonTuning.DEFAULT_LEAK) * (1f + (1f - condition) * PistonTuning.LEAK_WORN_GAIN);
    }

    private Pu danger() {
        return new Pu(figures.get("Danger", PistonTuning.DEFAULT_DANGER_PU));
    }

    public int out() {
        return out;
    }

    // --- asking -------------------------------------------------------------------------------

    /** A neighbour changed or the block was placed: look again next tick. */
    public void poke() {
        if (level == null || level.isClientSide || scheduled)
            return;
        scheduled = true;
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override
    public boolean isEngaged() {
        return switched;
    }

    @Override
    public void setEngaged(boolean engaged) {
        if (switched == engaged)
            return;
        switched = engaged;
        setChanged();
        kick();
    }

    private boolean want() {
        return redstone || switched;
    }

    /** Schedules the next step if the rod is not where it is asked to be. */
    public void kick() {
        boolean pending = want() ? out < reach() : out > 0;
        if (level == null || level.isClientSide || scheduled || !pending)
            return;
        scheduled = true;
        level.scheduleTick(worldPosition, getBlockState().getBlock(), PistonTuning.STEP_TICKS);
    }

    /** The scheduled tick: one block of stroke, then the next if there is one to go and this one moved. */
    public void step() {
        scheduled = false;
        if (!(level instanceof ServerLevel server))
            return;
        redstone = server.hasNeighborSignal(worldPosition);
        boolean moved = false;
        if (want() && out < reach())
            moved = extendOne(server);
        else if (!want() && out > 0)
            moved = retractOne(server);
        if (moved)
            kick();
    }

    // --- stroke -------------------------------------------------------------------------------

    private Direction facing() {
        return getBlockState().getValue(ModularPistonBlock.FACING);
    }

    private boolean extendOne(ServerLevel level) {
        Direction f = facing();
        BlockPos head = worldPosition.relative(f, out + 1);
        if (!level.isLoaded(head))
            return false;
        // The line in front of the head, and what it weighs.
        List<BlockPos> push = new ArrayList<>();
        List<BlockPos> destroy = new ArrayList<>();
        float mass = 0f;
        for (int i = 0; ; i++) {
            BlockPos p = head.relative(f, i);
            BlockState s = level.getBlockState(p);
            if (s.isAir())
                break;
            if (!level.isInWorldBounds(p) || !level.getWorldBorder().isWithinBounds(p) || i >= PistonTuning.MAX_LINE
                    || !PistonBaseBlock.isPushable(s, level, p, f, true, f))
                return false; // walled in: nothing moves, nothing wears
            if (s.getPistonPushReaction() == PushReaction.DESTROY) {
                destroy.add(p);
                break;
            }
            push.add(p);
            mass += PistonTuning.massOf(s, level, p);
        }
        // Gas, and the force it gives.
        int mb = Math.max(1, Math.round(area() * PistonTuning.MB_PER_CM2 * (1f + leak())));
        float fill = draw(level, f, mb);
        if (fill < 0f)
            return false; // no air: no stroke, no wear
        Pu pressure = new Pu(PistonTuning.SUPPLY_PU * Math.max(PistonTuning.MIN_FILL, fill));
        if (PressureBurst.fails(pressure, danger(), new Pu(danger().value() * PistonTuning.CRITICAL_OVER_DANGER), level.random)) {
            PressureBurst.burst(level, worldPosition, PistonTuning.BURST_POWER);
            return false;
        }
        float budget = pressure.value() * area() * PistonTuning.MU_PER_PU_CM2;
        if (mass > budget)
            return stall(level); // gas was drawn and blown: the seal wears
        for (int j = destroy.size() - 1; j >= 0; j--) {
            BlockPos p = destroy.get(j);
            BlockState s = level.getBlockState(p);
            Block.dropResources(s, level, p, level.getBlockEntity(p));
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 18);
        }
        List<BlockState> states = new ArrayList<>();
        for (BlockPos p : push)
            states.add(level.getBlockState(p));
        List<BlockPos> vacated = new ArrayList<>(push);
        for (int k = push.size() - 1; k >= 0; k--) {
            BlockPos to = push.get(k).relative(f);
            vacated.remove(to);
            BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, f);
            level.setBlock(to, moving, 68);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(to, moving, states.get(k), f, true, false));
        }
        for (BlockPos p : vacated) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 82);
            level.updateNeighborsAt(p, Blocks.AIR);
        }
        // The old head becomes rod, and the head moves on.
        if (out > 0)
            level.setBlock(worldPosition.relative(f, out), ModularPistonHeadBlock.state(f, true, sticky()), 3);
        level.setBlock(head, ModularPistonHeadBlock.state(f, false, sticky()), 3);
        out++;
        level.setBlock(worldPosition, getBlockState().setValue(ModularPistonBlock.EXTENDED, true), 3);
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.25f + 0.6f);
        setChanged();
        return true;
    }

    private boolean retractOne(ServerLevel level) {
        Direction f = facing();
        BlockPos old = worldPosition.relative(f, out);
        // Only our own rod comes out; if something else is there the rod was taken from outside, so the count just catches up.
        if (level.getBlockState(old).getBlock() instanceof ModularPistonHeadBlock)
            level.setBlock(old, Blocks.AIR.defaultBlockState(), 18);
        out--;
        if (out > 0)
            level.setBlock(worldPosition.relative(f, out), ModularPistonHeadBlock.state(f, false, sticky()), 3);
        if (sticky()) {
            BlockPos behind = old.relative(f);
            BlockState s = level.getBlockState(behind);
            if (!s.isAir() && PistonBaseBlock.isPushable(s, level, behind, f.getOpposite(), false, f)
                    && s.getPistonPushReaction() != PushReaction.DESTROY && PistonTuning.massOf(s, level, behind) <= PistonTuning.SUPPLY_PU * area() * PistonTuning.MU_PER_PU_CM2) {
                BlockState moving = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, f);
                level.setBlock(behind, Blocks.AIR.defaultBlockState(), 82);
                level.setBlock(old, moving, 68);
                level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(old, moving, s, f, false, false));
            }
        }
        level.setBlock(worldPosition, getBlockState().setValue(ModularPistonBlock.EXTENDED, out > 0), 3);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.15f + 0.6f);
        setChanged();
        return true;
    }

    /** A push that cannot move its line: the gas is blown, the seal wears, and the head stays where it was. */
    private boolean stall(ServerLevel level) {
        condition = Math.max(0f, condition - PistonTuning.SEAL_WEAR_PER_STALL);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.3f, 1.6f);
        setChanged();
        return false;
    }

    /**
     * Takes {@code mb} of gas from a holder touching the cylinder on any face but the front; returns how full the holder was (0 to 1) before,
     * or -1 when none has it.
     */
    private float draw(ServerLevel level, Direction front, int mb) {
        for (Direction side : Direction.values()) {
            if (side == front)
                continue;
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (handler == null)
                continue;
            FluidStack probe = handler.drain(mb, IFluidHandler.FluidAction.SIMULATE);
            if (probe.getAmount() < mb || !FluidNetwork.isGas(probe))
                continue;
            float fill = 1f;
            if (handler.getTanks() > 0 && handler.getTankCapacity(0) > 0)
                fill = Math.min(1f, (float) handler.getFluidInTank(0).getAmount() / handler.getTankCapacity(0));
            handler.drain(probe.copyWithAmount(mb), IFluidHandler.FluidAction.EXECUTE);
            return fill;
        }
        return -1f;
    }

    /** Takes the rod and head with it when the cylinder goes. */
    public void clearRod(Level level) {
        Direction f = facing();
        for (int i = 1; i <= out; i++)
            if (level.getBlockState(worldPosition.relative(f, i)).getBlock() instanceof ModularPistonHeadBlock)
                level.setBlock(worldPosition.relative(f, i), Blocks.AIR.defaultBlockState(), 18);
        out = 0;
    }

    // --- Mendable -----------------------------------------------------------------------------

    @Override
    public float mendableCondition() {
        return condition;
    }

    @Override
    public void mend(float by) {
        condition = Math.min(1f, condition + by);
        setChanged();
    }

    // --- DataNode: so a controller card can switch it -----------------------------------------

    @Override
    public BlockPos getNodePos() {
        return worldPosition;
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }
}
