// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.ender;

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.control.endernet.Endernet;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.tube.TubeTuning;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * One end of an entangled data pair (endernet layer 1, {@link Endernet}): a sink that holds a pair id, a tank of teleportant and a
 * bandwidth allowance. A data node linked to it with the Data Connector can be read from the other end of the pair through
 * {@code DataWires.reach}. It carries while both ends are loaded and powered (standing power {@link ControlTuning#ENDERNET_PORT_AMPS} each);
 * an unloaded end pauses the pair, and an ender anchor on the port keeps its chunk loaded. Each signal that crosses costs
 * {@link ControlTuning#ENDERNET_MB_PER_SIGNAL} of teleportant, scaled by the pair's distance as an ender port's is, and one unit of
 * {@link ControlTuning#ENDERNET_SIGNALS_PER_MIN} bandwidth, which refills lazily from the game clock. A dry or saturated port simply does not
 * carry until it recovers; it never destroys anything.
 */
public class DataPortBlockEntity extends ElectricNode implements Endernet.End, PairEnd, io.github.soundgoodizerfan.feedback.control.data.Amplifying,
        io.github.soundgoodizerfan.feedback.control.ChannelSource, io.github.soundgoodizerfan.feedback.control.Steppable,
        io.github.soundgoodizerfan.feedback.control.RemoteChannels {

    @Nullable
    private UUID pair;
    private boolean anchored;
    private float allowance = ControlTuning.ENDERNET_SIGNALS_PER_MIN;
    private long allowanceAt;
    /** Teleportant owed in fractions of a mB. */
    private float owed;
    private boolean unloaded;
    private final io.github.soundgoodizerfan.feedback.machine.CircuitSlot circuit = new io.github.soundgoodizerfan.feedback.machine.CircuitSlot();

    private final FluidTank tank = new FluidTank(ControlTuning.ENDERNET_PORT_TANK_MB, fs -> fs.is(EnderPortBlockEntity.TELEPORTANT)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public DataPortBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DATA_PORT.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return pair == null ? 0f : ControlTuning.ENDERNET_PORT_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    // --- the pair -------------------------------------------------------------------------------

    @Override
    @Nullable
    public UUID pairId() {
        return pair;
    }

    @Override
    public boolean live() {
        return pair != null && !unloaded && !isRemoved() && ElectricRules.rate(getPacketEu(), getAmps(), getWantedAmps(), ratedBand()) >= 0.5f;
    }

    @Override
    public boolean pay(float blocks) {
        if (level == null)
            return false;
        long now = level.getGameTime();
        float cap = bandwidth();
        allowance = Math.min(cap, allowance + cap * (now - allowanceAt) / 1200f);
        allowanceAt = now;
        if (allowance < 1f)
            return false;
        float cost = ControlTuning.ENDERNET_MB_PER_SIGNAL * (1f + blocks / TubeTuning.PAIR_DISTANCE_SCALE);
        float after = owed + cost;
        if (after >= 1f) {
            int whole = (int) after;
            if (tank.drain(whole, IFluidHandler.FluidAction.SIMULATE).getAmount() < whole)
                return false;
            tank.drain(whole, IFluidHandler.FluidAction.EXECUTE);
            after -= whole;
        }
        owed = after;
        allowance -= 1f;
        return true;
    }

    /**
     * Signals a minute this port carries: {@link ControlTuning#ENDERNET_SIGNALS_PER_MIN}, times {@code 1 + CIRCUIT_BANDWIDTH_GAIN x bandwidth} of
     * the circuit seated in it (a resonator's figure). With none, the base.
     */
    public float bandwidth() {
        return ControlTuning.ENDERNET_SIGNALS_PER_MIN
                * (1f + io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_BANDWIDTH_GAIN
                        * io.github.soundgoodizerfan.feedback.process.Circuit.of(circuit.get()).bandwidth());
    }

    @Override
    public float amplifierGain() {
        return level == null ? 0f : circuit.gain(level);
    }

    public io.github.soundgoodizerfan.feedback.machine.CircuitSlot circuit() {
        return circuit;
    }

    // --- a robot's base: the port its leash is bonded to ---------------------------------------------------

    /**
     * The robot whose endernet upgrade is bonded under this port's pair and whose leash can carry now (a live brain, power in its pack,
     * loaded), or null. The port is then that robot's base station, as a mast is for a radio one: a Sensor card linked to it reads the robot's
     * senses, an Actuator card holds its actions, over the pair and with the pair's costs, as far as the leash carries
     * ({@code RobotSpec.leashChannels}, from the brain circuit's selectivity). A port with no robot on it does none of this.
     */
    @Nullable
    public io.github.soundgoodizerfan.feedback.robot.RobotEntity leashed() {
        if (pair == null || level == null || !live())
            return null;
        var r = io.github.soundgoodizerfan.feedback.robot.RobotLeash.get(pair);
        return r != null && r.level() == level && r.leashLive() ? r : null;
    }

    public boolean hasLeash() {
        return pair != null && io.github.soundgoodizerfan.feedback.robot.RobotLeash.get(pair) != null;
    }

    private boolean carries(io.github.soundgoodizerfan.feedback.robot.RobotEntity r, int channel, int length) {
        if (channel < 0 || channel >= length || channel >= r.spec().leashChannels())
            return false;
        return pay((float) Math.sqrt(r.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5)));
    }

    /**
     * The last scan a suit with an endernet upgrade bonded under this port's pair shared, or null: none is fresh, the suit scanned in another
     * dimension (the same-dimension rule of every crossing), or the port is dark. The port then offers the scan as sense channels
     * ({@link io.github.soundgoodizerfan.feedback.suit.SuitScan#CHANNELS}), each read paying the pair's costs over the distance the scan was taken at.
     */
    @Nullable
    private io.github.soundgoodizerfan.feedback.suit.SuitScan.Reading suitScan() {
        if (pair == null || level == null || !live())
            return null;
        var s = io.github.soundgoodizerfan.feedback.suit.SuitScan.get(pair, level.getGameTime());
        return s != null && s.dimension() == level.dimension() ? s : null;
    }

    @Override
    public float read(int channel) {
        var r = leashed();
        if (r == null) {
            var scan = suitScan();
            int length = io.github.soundgoodizerfan.feedback.suit.SuitScan.CHANNELS.length;
            if (scan == null || channel < 0 || channel >= length
                    || !pay((float) scan.at().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))))
                return 0f;
            return scan.channel(channel);
        }
        var senses = io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity.SENSES;
        return !carries(r, channel, senses.length) ? 0f : r.read(senses[channel].ordinal(), 0);
    }

    @Override
    public boolean offersChannels() {
        return leashed() != null || suitScan() != null;
    }

    @Override
    public String[] senseKeys() {
        if (leashed() == null && suitScan() != null) {
            String[] keys = io.github.soundgoodizerfan.feedback.suit.SuitScan.CHANNELS.clone();
            for (int i = 0; i < keys.length; i++)
                keys[i] = "suit.feedback.scan." + keys[i];
            return keys;
        }
        var senses = io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity.SENSES;
        String[] keys = new String[senses.length];
        for (int i = 0; i < keys.length; i++)
            keys[i] = senses[i].langKey();
        return keys;
    }

    @Override
    public String[] channelKeys() {
        var actions = io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity.ACTIONS;
        String[] keys = new String[actions.length];
        for (int i = 0; i < keys.length; i++)
            keys[i] = actions[i].langKey();
        return keys;
    }

    /** Channel 0 of an Actuator card: the robot's first action (roll forward), held while on. A port with no robot on it moves nothing. */
    @Override
    public void setEngaged(boolean engaged) {
        hold(0, engaged);
    }

    @Override
    public boolean isEngaged() {
        var r = leashed();
        return r != null && r.read(io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity.ACTIONS[0].ordinal(), 0) != 0f;
    }

    @Override
    public void pulse(int channel, boolean on) {
        hold(channel, on);
    }

    private void hold(int channel, boolean on) {
        var actions = io.github.soundgoodizerfan.feedback.robot.RobotMastBlockEntity.ACTIONS;
        var r = leashed();
        // out of the leash's reach or unpaid, the action is let go: the robot hears nothing
        if (r != null && channel >= 0 && channel < actions.length)
            r.write(actions[channel].ordinal(), on && carries(r, channel, actions.length));
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }

    public int teleportantMb() {
        return tank.getFluidAmount();
    }

    public boolean anchored() {
        return anchored;
    }

    public void anchor() {
        anchored = true;
        force(true);
        setChanged();
    }

    private void force(boolean on) {
        if (level instanceof ServerLevel sl)
            sl.setChunkForced(worldPosition.getX() >> 4, worldPosition.getZ() >> 4, on);
    }

    /** The pair's state for a hand reading it: unbonded, away (no live partner here), or linked. */
    public String stateKey() {
        if (pair == null)
            return "message.feedback.data_port.unbonded";
        if (!live())
            return "message.feedback.data_port.dark";
        if (leashed() != null)
            return "message.feedback.data_port.robot";
        return Endernet.partner(this).isPresent() ? "message.feedback.data_port.linked" : "message.feedback.data_port.away";
    }

    public void tickServer() {
        Endernet.sync(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        unloaded = false;
        if (level != null && !level.isClientSide && pair != null) {
            Endernet.sync(this);
            PairBook.get(level.getServer()).put(pair, level.dimension(), worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
        Endernet.leave(this);
    }

    @Override
    public void setRemoved() {
        Endernet.leave(this);
        if (!unloaded && level != null && !level.isClientSide && pair != null)
            PairBook.get(level.getServer()).remove(pair, level.dimension(), worldPosition);
        if (!unloaded && anchored && level != null && !level.isClientSide)
            force(false);
        super.setRemoved();
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData data = input.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.contains(EnderPortBlockEntity.PAIR_KEY))
            pair = data.copyTag().getUUID(EnderPortBlockEntity.PAIR_KEY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (pair != null) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID(EnderPortBlockEntity.PAIR_KEY, pair);
            components.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (pair != null)
            tag.putUUID(EnderPortBlockEntity.PAIR_KEY, pair);
        circuit.save(tag, registries);
        tag.putBoolean("Anchored", anchored);
        tag.putFloat("Allowance", allowance);
        tag.putLong("At", allowanceAt);
        tag.putFloat("Owed", owed);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pair = tag.hasUUID(EnderPortBlockEntity.PAIR_KEY) ? tag.getUUID(EnderPortBlockEntity.PAIR_KEY) : null;
        circuit.load(tag, registries);
        anchored = tag.getBoolean("Anchored");
        allowance = tag.contains("Allowance") ? tag.getFloat("Allowance") : ControlTuning.ENDERNET_SIGNALS_PER_MIN;
        allowanceAt = tag.getLong("At");
        owed = tag.getFloat("Owed");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }
}
