// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.control.ChannelSource;
import io.github.soundgoodizerfan.feedback.control.Switchable;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.DelayNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramEvaluator;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramVariable;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.control.program.ValueType;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.SensorFitting;
import io.github.soundgoodizerfan.feedback.fitting.SidedFitting;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.CardRackFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.EscapementFitting;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.MemoryDrumFitting;
import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.function.BooleanSupplier;

/**
 * The controller's whole behaviour, held apart from any block entity so the mechanical Controller, the Electric Controller and the
 * Computer share it: one Punch Card (or other medium) slot, the upgrades, the variables and delay history, and the evaluation of the
 * printed graph. The host says whether there is motive force ({@code powered}) and supplies the level, position and change
 * notification; everything else is here. Not itself a {@link DataNode}: a card carries its own {@link DataNodeRef}.
 */
public class ControllerCore implements Container, Fittable {

    private static final String CARD = "Card";
    private static final String VARIABLES = "Variables";
    private static final String DRUMS = "Drums";
    private static final String RACKS = "Racks";
    private static final String ESCAPEMENT = "Escapement";
    private static final String CIRCUIT = "Circuit";
    private static final String ENDERNET = "Endernet";

    private ItemStack card = ItemStack.EMPTY;

    /** The circuit in this host's circuit slot (a host that has one: {@link #withCircuitSlot}). It sets the clock rate and the read noise; none still runs, slower and noisier. */
    private ItemStack circuit = ItemStack.EMPTY;
    private boolean circuitSlot, circuitHeater, endernetCapable;
    /** A host whose program size is something other than the upgrades' (a robot's brain): the cards it may hold. */
    @Nullable
    private java.util.function.IntSupplier cardCap;
    /** Steps owed: a tick adds the clock rate over twenty and a step is taken at one. Not persisted. */
    private float stepCredit;
    /** Ticks this host has stood without power, and what an adapting circuit has learned of each sensor's reading (not persisted). */
    private int offTicks;
    private float[] learned = new float[0];

    /** Fitted upgrades; stateless, so a count or flag per kind is the whole of their persistence. */
    private final List<UpgradeFitting> upgrades = new ArrayList<>();

    /**
     * The compiled {@link #lastGraph}, or null if it has a cycle or is not {@link #runnable}. Built
     * only when the graph changes, with the scratch arrays below sized to its sensors.
     */
    @Nullable
    private ProgramEvaluator.Plan plan;
    private SensorNode[] sensors = new SensorNode[0];
    /**
     * This tick's reading and the last evaluated reading for each sensor, by index into
     * {@link #sensors} -- spec §2.5: the graph is only re-evaluated when one of these actually
     * moves. Not persisted; losing them on reload just costs one extra evaluation, which is harmless.
     */
    private float[] current = new float[0];
    private float[] last = new float[0];
    private ProgramGraph lastGraph = ProgramGraph.EMPTY;
    /** Whether {@link #lastGraph} passes {@link ProgramGraph#findEditError} against {@link #limits()}; recomputed whenever the graph or upgrades change. */
    private boolean runnable;

    /** Variable values by name, each a Float or a Boolean. Persisted. */
    private final Map<String, Object> variables = new HashMap<>();
    /** Writes made this step; committed at its end so a Read sees them next step. */
    private final Map<String, Object> pendingWrites = new HashMap<>();
    /** Delay history by node id. Not persisted: after a reload each delay restarts from its current input. */
    private final Map<Integer, DelayHistory> delays = new HashMap<>();
    /** Ticks this controller has been turning; the delay clock. Frozen with the shaft. */
    private long clock;
    /** Clock at which a delay's output next changes; {@link Long#MAX_VALUE} if none is pending. */
    private long nextDue = Long.MAX_VALUE;
    /** The count each delay used on its last evaluated step, by node id: a wired count moves, so the due time follows it. */
    private final Map<Integer, Integer> delayTicks = new HashMap<>();
    /** A variable changed: run another step so Reads see it. */
    private boolean dirty;
    private boolean priming;

    /** The block entity hosting this core, or null when a robot carries it. */
    @Nullable
    private final BlockEntity host;
    private final java.util.function.Supplier<Level> levelOf;
    private final Runnable changed;
    /** A robot's own channels, answering the {@code DataNodeRef.local} cards; null on a block host. */
    @Nullable
    private final Locals locals;
    private final BooleanSupplier powered;
    private final java.util.function.Predicate<ItemStack> takes;

    /** What a robot answers for the cards naming its own channels: a reading, and a write that says whether it was taken. */
    public interface Locals {
        float read(int channel, int argument);

        void write(int channel, boolean on);
    }

    public ControllerCore(BlockEntity host, BooleanSupplier powered, java.util.function.Predicate<ItemStack> takes) {
        this.host = host;
        this.levelOf = host::getLevel;
        this.changed = host::setChanged;
        this.locals = null;
        this.powered = powered;
        this.takes = takes;
    }

    /** A core in a block entity that also answers the robot channels (the robot arm): wires read as for any block host, local cards go to {@code locals}. */
    public ControllerCore(BlockEntity host, Locals locals, BooleanSupplier powered, java.util.function.Predicate<ItemStack> takes) {
        this.host = host;
        this.levelOf = host::getLevel;
        this.changed = host::setChanged;
        this.locals = locals;
        this.powered = powered;
        this.takes = takes;
    }

    /** A core carried by a robot: it has no block entity; {@code locals} answers the robot's own channels. */
    public ControllerCore(java.util.function.Supplier<Level> level, Runnable changed, Locals locals, BooleanSupplier powered,
            java.util.function.Predicate<ItemStack> takes) {
        this.host = null;
        this.levelOf = level;
        this.changed = changed;
        this.locals = locals;
        this.powered = powered;
        this.takes = takes;
    }

    private Level level() {
        return levelOf.get();
    }

    /**
     * Gives this host the circuit slot: it then steps at its circuit's clock rate (the base rate with none) and reads sensors through its
     * noise. A host with no slot (a robot, the arm, the lift) steps every tick and reads true, as before. {@code heater}: the host has
     * power for a tube's heater and warms the circuit it holds with it, which it pays for ({@link #heaterAmps}).
     */
    public ControllerCore withCircuitSlot(boolean heater) {
        circuitSlot = true;
        circuitHeater = heater;
        return this;
    }

    /** Lets this host take an endernet upgrade (an electric host: both ends of a pair need power). */
    public ControllerCore withEndernet() {
        endernetCapable = true;
        return this;
    }

    /** The pair id of the fitted endernet upgrade, or null for none. */
    @Nullable
    public java.util.UUID endernetPair() {
        for (UpgradeFitting u : upgrades)
            if (u instanceof io.github.soundgoodizerfan.feedback.fitting.upgrade.EndernetFitting e)
                return e.pair();
        return null;
    }

    /** Sets the cards this host's program may hold from {@code cap} instead of the Card Racks (a robot's brain decides); a negative answer leaves the Card Racks in charge. */
    public ControllerCore cardCap(java.util.function.IntSupplier cap) {
        cardCap = cap;
        return this;
    }

    public boolean hasCircuitSlot() {
        return circuitSlot;
    }

    /** The circuit held, as a stack (empty for none). */
    public ItemStack circuit() {
        return circuit;
    }

    /** Whether a stack may go in the circuit slot: a circuit item, and the slot empty. */
    public boolean canFitCircuit(ItemStack stack) {
        return circuitSlot && circuit.isEmpty() && stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.CircuitItem;
    }

    public void fitCircuit(ItemStack stack) {
        circuit = stack;
        stepCredit = 0f;
        setChanged();
    }

    /** Whether the circuit can come out: refused while the program in the slot needs the variables its lattice holds. */
    public boolean canTakeCircuit() {
        return limits(CIRCUIT_OUT).exceededBy(card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY)).isEmpty();
    }

    public ItemStack takeCircuit() {
        ItemStack out = circuit;
        circuit = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /** Amps the circuit's tube heater wants from a line of {@code packetEu}: its standing power over the packet. 0 with none, with a host that has no heater. */
    public float heaterAmps(float packetEu) {
        if (!circuitHeater || circuit.isEmpty() || packetEu <= 0f)
            return 0f;
        return Circuit.of(circuit).heaterPower() / packetEu;
    }

    /** Program steps a second the held circuit gives at its warmth now. */
    public float clockRate() {
        return Circuit.of(circuit, level()).clockRate();
    }

    /** What the sensor reads multiply by: noise of the circuit at its warmth now, or 1 for none. */
    private float readNoise() {
        Circuit c = Circuit.of(circuit, level());
        return c.noise() * (1f - io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_STABLE_SAMPLING * c.stability());
    }

    /**
     * What an adapting circuit (a sculk lattice) makes of a reading: it remembers the last reading of each sensor and holds it while the new one
     * stays within {@code CIRCUIT_ADAPT_BAND x adaptation x CONTROLLER_READ_JITTER} of it (relative), learning toward the new one
     * {@code CIRCUIT_ADAPT_LEARN} a read, so noise the size of the read jitter is filtered and a real change passes at once. Two-state and
     * zero readings pass. Deterministic: the same reads give the same output.
     */
    private float adapt(int i, float v) {
        if (i >= learned.length)
            return v;
        float a = circuitSlot ? Circuit.of(circuit, level()).adaptation() : 0f;
        float held = learned[i];
        if (a <= 0f || v == 0f || v == 1f || held == 0f) {
            learned[i] = v;
            return v;
        }
        float band = io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_ADAPT_BAND * a * ControlTuning.CONTROLLER_READ_JITTER * Math.abs(held);
        if (Math.abs(v - held) <= band) {
            learned[i] = held + (v - held) * io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_ADAPT_LEARN;
            return learned[i];
        }
        learned[i] = v;
        return v;
    }

    /**
     * Power returned after a long dark: a circuit whose lattice does not hold unpowered has lost the memory variables beyond what the Memory
     * Drums hold (the later declared ones), which read zero or false again.
     */
    private void forgetUnheld() {
        Circuit c = Circuit.of(circuit);
        if (c.memory() <= 0 || c.line().holds())
            return;
        List<ProgramVariable> all = declared();
        for (int k = limits(CIRCUIT_OUT).variables(); k < all.size(); k++)
            variables.put(all.get(k).name(), all.get(k).type().zero());
        dirty = true;
        setChanged();
    }

    /** What a circuit seated here makes of the program size on a vehicle: the base and a share per element (the autopilot's brain). */
    public int vehicleCards() {
        return Math.min(ControllerLimits.MAX.nodes(), ControlTuning.VEHICLE_CARDS_BASE
                + ControlTuning.VEHICLE_CARDS_PER_ELEMENT * Math.max(0, circuitSlot ? Circuit.of(circuit).elements() : 0));
    }

    /** The gain of the held circuit at its warmth: 0 with none or a host with no slot. */
    public float gain() {
        return circuitSlot ? Circuit.of(circuit, level()).gain() : 0f;
    }

    /** Channels the fitted endernet upgrade can be tuned to: one with no circuit, more for what a resonator on the circuit separates, never over the cap. */
    public int endernetChannels() {
        return Math.min(ControlTuning.ENDERNET_MAX_CHANNELS, Math.max(ControlTuning.ENDERNET_CHANNELS, circuitSlot ? Circuit.of(circuit).selectivity() : 0));
    }

    private int tunedChannel = 1, tunedAddress;

    /** The channel the endernet upgrade is tuned to, 1 to {@link #endernetChannels}. */
    public int endernetChannel() {
        return Math.max(1, Math.min(tunedChannel, endernetChannels()));
    }

    /** The upgrade's address, 0 for none. */
    public int endernetAddress() {
        return tunedAddress;
    }

    /** Steps the tuned channel by {@code delta}, wrapping within the channels the circuit allows. */
    public void tuneChannel(int delta) {
        int n = endernetChannels();
        tunedChannel = Math.floorMod(endernetChannel() - 1 + Integer.signum(delta), n) + 1;
    }

    /** Steps the address by {@code delta}, wrapping between 0 (none) and {@link ControlTuning#ENDERNET_ADDRESS_MAX}. */
    public void tuneAddress(int delta) {
        tunedAddress = Math.floorMod(tunedAddress + Integer.signum(delta), ControlTuning.ENDERNET_ADDRESS_MAX + 1);
    }

    /** A quantity read through this host's circuit: jittered by its noise; a two-state or zero reading carries none. */
    private float jitter(float value) {
        if (!circuitSlot || value == 0f || value == 1f || level() == null)
            return value;
        return value * (1f + (level().random.nextFloat() * 2f - 1f) * readNoise() * ControlTuning.CONTROLLER_READ_JITTER);
    }

    @Override
    public void setChanged() {
        changed.run();
    }

    /** The variables the loaded program declares, in declaration order: what another controller's Sensor card can pick between. */
    public List<ProgramVariable> declared() {
        return card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).variables();
    }

    /** The value of the {@code index}th declared variable as a number (a boolean reads 1 or 0); 0 for none. */
    public float variableValue(int index) {
        List<ProgramVariable> all = declared();
        if (index < 0 || index >= all.size())
            return 0f;
        Object v = variables.get(all.get(index).name());
        return v instanceof Float f ? f : v instanceof Boolean b && b ? 1f : 0f;
    }

    /**
     * Another controller sets the {@code index}th declared variable: a boolean variable takes the value, a number variable 1 or 0. It reads
     * next step, as the controller's own writes do. A switch, never a dial: only a boolean crosses.
     */
    public void writeVariable(int index, boolean on) {
        List<ProgramVariable> all = declared();
        if (index < 0 || index >= all.size())
            return;
        ProgramVariable v = all.get(index);
        Object value = v.type() == ValueType.NUMBER ? (Object) (on ? 1f : 0f) : (Object) on;
        if (!value.equals(variables.put(v.name(), value))) {
            dirty = true;
            setChanged();
        }
    }

    /** The program's variables and their values (a Float or a Boolean each), as last saved or synced; for a monitor. */
    public Map<String, Object> variables() {
        return java.util.Collections.unmodifiableMap(variables);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** The last reading of each sensor card of the running program, in the plan's sensor order; for a monitor. */
    public float[] readings() {
        return current.clone();
    }

    /** Whether this host takes the medium at all: a mechanical Controller takes the Punch Card only. */
    public boolean takesMedium(ItemStack stack) {
        return takes.test(stack);
    }

    /** What the fitted upgrades let a card use. */
    public ControllerLimits limits() {
        return limits(null);
    }

    private ControllerLimits limits(@Nullable UpgradeFitting without) {
        int drums = 0, racks = 0;
        boolean escapement = false;
        for (UpgradeFitting u : upgrades) {
            if (u == without)
                continue;
            if (u instanceof MemoryDrumFitting)
                drums++;
            else if (u instanceof CardRackFitting)
                racks++;
            else if (u instanceof EscapementFitting)
                escapement = true;
        }
        ControllerLimits base = ControllerLimits.of(drums, racks, escapement, circuitSlot && without != CIRCUIT_OUT ? Circuit.of(circuit).memory() : 0);
        int cap = cardCap == null ? -1 : cardCap.getAsInt();
        return cap < 0 ? base : new ControllerLimits(cap, base.variables(), base.typedDelays());
    }

    private static final UpgradeFitting CIRCUIT_OUT = new EscapementFitting();

    /** Why {@code stack} may not go in (an error key), or empty if it may. Blank cards always fit. */
    public Optional<String> refusalFor(ItemStack stack) {
        return limits().exceededBy(stack.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY));
    }

    public void tickServer() {
        if (level() == null || level().isClientSide)
            return;
        if (!powered.getAsBoolean()) {
            offTicks++;
            return; // no motive force, no switching -- outputs, variables and delays hold
        }
        if (offTicks > 0) {
            if (circuitSlot && offTicks > io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_HOLD_GRACE_TICKS)
                forgetUnheld();
            offTicks = 0;
        }

        clock++;
        if (circuitSlot) {
            if (!circuit.isEmpty() && clock % ControlTuning.CIRCUIT_HEAT_EVERY_TICKS == 0) {
                if (circuitHeater)
                    Circuit.warm(circuit, level(), ControlTuning.CIRCUIT_HEAT_EVERY_TICKS, 1f);
                if (clock % 20 == 0)
                    Circuit.scorch(circuit, level());
            }
            // The clock: a step is taken when a tick brings the credit to one. At most one a tick, so a program never steps twice in a tick.
            // A circuit short of full speed accumulates fractions of a step; a steady (resonator) clock keeps them even, an unsteady one wanders.
            float rate = clockRate();
            float wobble = rate >= io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_MAX_STEPS_PER_SECOND ? 0f
                    : (level().random.nextFloat() * 2f - 1f) * io.github.soundgoodizerfan.feedback.process.ProcessTuning.CIRCUIT_CLOCK_JITTER
                            * (1f - Circuit.of(circuit, level()).stability());
            stepCredit = Math.min(1f, stepCredit + rate / 20f * (1f + wobble));
            if (stepCredit < 1f)
                return;
            stepCredit -= 1f;
        }
        ProgramGraph graph = card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY);
        if (graph.nodes().isEmpty())
            return;

        // Reference first: the card's component is the same object tick to tick, so the deep
        // compare runs only when something swapped it.
        boolean sameGraph = graph == lastGraph;
        if (!sameGraph && graph.equals(lastGraph)) {
            lastGraph = graph;
            sameGraph = true;
        }
        if (!sameGraph)
            loadGraph(graph);
        if (plan == null)
            return;

        boolean moved = false;
        for (int i = 0; i < sensors.length; i++) {
            float v = adapt(i, readSensor(sensors[i]));
            current[i] = v;
            if (Float.compare(v, last[i]) != 0)
                moved = true;
        }

        // The Controller already ticks every turning tick, so a due delay is a clock compare here;
        // a scheduled tick would add nothing.
        if (!(!sameGraph || dirty || clock >= nextDue || moved))
            return;
        dirty = false;

        // Evaluate on the readings just taken -- resolving every sensor a second time would cost
        // a second world lookup each, and could see a different value than the change test did.
        if (needsPriming(graph)) {
            priming = true;
            plan.evaluate(reader, NO_WRITE, state);
            priming = false;
        }
        plan.evaluate(reader, actuatorWriter, state);
        System.arraycopy(current, 0, last, 0, current.length);
    }

    /** Checks and compiles a graph new to this controller; leaves {@link #plan} null if it cannot run. */
    private void loadGraph(ProgramGraph graph) {
        lastGraph = graph;
        // The full edit check, not just the limits: a creative or datapack card never met the Programmer.
        runnable = graph.findEditError(limits()).isEmpty();
        Optional<List<Integer>> order = runnable ? ProgramEvaluator.topologicalOrder(graph) : Optional.empty();
        plan = order.map(o -> new ProgramEvaluator.Plan(graph, o)).orElse(null);
        sensors = plan == null ? new SensorNode[0] : plan.sensors();
        current = new float[sensors.length];
        learned = new float[sensors.length];
        last = new float[sensors.length];
    }

    private final ProgramEvaluator.SensorReader reader = (node, index) -> current[index];
    private final ProgramEvaluator.ActuatorWriter actuatorWriter = this::writeActuator;
    /** Told (key, argument) when the program switches something; the Computer's console listens. Never null. */
    private java.util.function.BiConsumer<String, String> events = (key, arg) -> { };

    public void events(java.util.function.BiConsumer<String, String> events) {
        this.events = events;
    }
    private static final ProgramEvaluator.ActuatorWriter NO_WRITE = (target, channel, value) -> { };

    private boolean needsPriming(ProgramGraph graph) {
        for (ProgramNode node : graph.nodes())
            if (node instanceof DelayNode d && !delays.containsKey(d.id()))
                return true;
        return false;
    }

    private final ProgramEvaluator.State state = new ProgramEvaluator.State() {
        @Override
        public Object read(String variable) {
            return variables.get(variable);
        }

        @Override
        public void write(String variable, Object value) {
            if (!priming)
                pendingWrites.put(variable, value);
        }

        @Override
        public Object delayed(DelayNode node, int ticks) {
            delayTicks.put(node.id(), ticks);
            DelayHistory h = delays.get(node.id());
            if (h == null)
                return node.type().zero();
            float v = h.valueAt(clock - ticks);
            return node.type() == ValueType.NUMBER ? (Object) v : (Object) (v > 0.5f);
        }

        @Override
        public void record(DelayNode node, Object input) {
            float v = input instanceof Float f ? f : input instanceof Boolean b && b ? 1f : 0f;
            DelayHistory h = delays.get(node.id());
            if (h == null) {
                // Seeded as if the input had always been this: the output starts at the current input.
                h = new DelayHistory();
                delays.put(node.id(), h);
                h.push(clock - delayTicks.getOrDefault(node.id(), node.ticks()), v);
            } else if (!priming) {
                h.pushIfChanged(clock, v);
            }
        }

        @Override
        public void endStep() {
            if (priming) {
                pendingWrites.clear();
                return;
            }
            boolean moved = false;
            for (Map.Entry<String, Object> write : pendingWrites.entrySet())
                if (!write.getValue().equals(variables.put(write.getKey(), write.getValue())))
                    moved = true;
            pendingWrites.clear();
            if (moved) {
                dirty = true;
                setChanged();
            }
            nextDue = Long.MAX_VALUE;
            for (Map.Entry<Integer, DelayHistory> entry : delays.entrySet()) {
                int ticks = ticksOf(entry.getKey());
                nextDue = Math.min(nextDue, entry.getValue().nextChange(clock, ticks));
            }
        }
    };

    private int ticksOf(int nodeId) {
        Integer last = delayTicks.get(nodeId);
        return last != null ? last
                : lastGraph.find(nodeId).map(n -> n instanceof DelayNode d ? d.ticks() : 0).orElse(0);
    }

    /** One reading of {@code ref} as a Sensor card would take it (wire reach, poor-wire rounding, read noise); for the Computer's attached sensors. */
    public float readAttached(DataNodeRef ref, int channel) {
        return readSensor(new SensorNode(0, ref, null, channel));
    }

    private float readSensor(SensorNode sensor) {
        DataNodeRef ref = sensor.target() == null || level() == null ? null
                : sensor.target();
        if (level() == null || ref == null)
            return 0f;
        if (ref.isLocal())
            return locals == null ? 0f : locals.read(ref.local(), sensor.channel());
        DataNode node = DataNode.resolveOrNull(level(), ref);
        // An electric host reads only down a wire: no path of links, no reading; poor wire rounds it.
        if (node != null && node.acceptsLinks() && host instanceof DataNode self) {
            var wire = io.github.soundgoodizerfan.feedback.control.data.DataWires.reach(self, node);
            if (wire.isEmpty())
                return 0f;
            return io.github.soundgoodizerfan.feedback.control.data.DataWires.deliver(readNode(node, sensor), wire.get());
        }
        return readNode(node, sensor);
    }

    private float readNode(DataNode node, SensorNode sensor) {
        if (node instanceof ChannelSource source && source.offersChannels())
            return jitter(source.read(sensor.channel()));
        // An actuator's own state, read back: 1 engaged, 0 not. ProgramEvaluator turns it into a boolean.
        if (node instanceof Switchable sw)
            return sw.isEngaged() ? 1f : 0f;
        return node instanceof SensorFitting fitting ? jitter(fitting.readRaw()) : 0f;
    }

    /**
     * A Boolean switches a {@link Switchable}; a Float sets an {@link Analog} part's channel. A
     * number reaching a plain switch moves nothing -- a factory controller is a switch, never a
     * dial (philosophy 14) -- and a Boolean reaching a part that is only analog sets it full or off.
     */
    private void writeActuator(@Nullable DataNodeRef ref, int channel, Object value) {
        if (level() == null || ref == null)
            return;
        if (ref.isLocal()) {
            if (locals != null && value instanceof Boolean on)
                locals.write(ref.local(), on);
            return;
        }
        DataNode target = DataNode.resolve(level(), ref).orElse(null);
        if (value instanceof Boolean pulse && channel > 0 && target instanceof io.github.soundgoodizerfan.feedback.control.Steppable stepper) {
            stepper.pulse(channel, pulse);
            return;
        }
        if (value instanceof Float amount && target instanceof Analog analog)
            analog.setLevel(channel, amount);
        else if (value instanceof Boolean on) {
            if (target instanceof Switchable sw) {
                boolean was = sw.isEngaged();
                sw.setEngaged(on);
                if (was != on)
                    events.accept(on ? "switch_on" : "switch_off", level().getBlockState(ref.pos()).getBlock().getDescriptionId());
            } else if (target instanceof Analog analog)
                analog.setLevel(channel, on ? 1 : 0);
        }
    }

    /**
     * Change events of one delay's input, in a fixed ring: the input held {@code vals[i]} from
     * clock {@code times[i]} on. Changes only are stored, so a slow delay costs a few entries
     * rather than a sample per tick; a full ring forgets its oldest event (the output then holds that
     * event's value for earlier times -- shortcut: an input flipping more than the ring size inside
     * one delay length reads slightly wrong).
     */
    private static final class DelayHistory {
        private final long[] times = new long[ControlTuning.DELAY_HISTORY_EVENTS];
        private final float[] vals = new float[ControlTuning.DELAY_HISTORY_EVENTS];
        private int head, count;

        private int at(int i) {
            return (head + i) % times.length;
        }

        void push(long time, float value) {
            if (count == times.length) {
                head = (head + 1) % times.length;
                count--;
            }
            int slot = at(count++);
            times[slot] = time;
            vals[slot] = value;
        }

        void pushIfChanged(long time, float value) {
            if (count == 0 || Float.compare(vals[at(count - 1)], value) != 0)
                push(time, value);
        }

        /** The input as of {@code time}: the latest event at or before it, else the oldest known. */
        float valueAt(long time) {
            float result = vals[at(0)];
            for (int i = 0; i < count; i++) {
                if (times[at(i)] > time)
                    break;
                result = vals[at(i)];
            }
            return result;
        }

        /** The clock at which the output next changes, given a delay of {@code ticks}. */
        long nextChange(long now, int ticks) {
            for (int i = 0; i < count; i++)
                if (times[at(i)] + ticks > now)
                    return times[at(i)] + ticks;
            return Long.MAX_VALUE;
        }
    }

    // --- Container (one Punch Card slot) ---------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return card.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return card;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = card.split(count);
        if (!result.isEmpty())
            cardChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = card;
        card = ItemStack.EMPTY;
        cardChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        card = stack;
        card.limitSize(getMaxStackSize(card));
        keepMatchingVariables();
        cardChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return card.isEmpty() && takes.test(stack) && refusalFor(stack).isEmpty();
    }

    /** A new card keeps the values of variables it declares with the same name and type; the rest are dropped. */
    private void keepMatchingVariables() {
        if (card.isEmpty())
            return; // taking the card out leaves the values for the same card coming back
        Map<String, Object> kept = new HashMap<>();
        for (ProgramVariable v : card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).variables()) {
            Object old = variables.get(v.name());
            if (v.type() == ValueType.NUMBER ? old instanceof Float : old instanceof Boolean)
                kept.put(v.name(), old);
        }
        variables.clear();
        variables.putAll(kept);
    }

    /**
     * The Lift carried blocks to new positions: every card target at a position in {@code moves} follows
     * it, so a controller riding in a vehicle keeps naming the same parts after it is lifted or lowered.
     */
    public void retarget(Map<net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> moves) {
        ProgramGraph graph = card.get(FDataComponents.PROGRAM_GRAPH);
        if (graph == null)
            return;
        List<io.github.soundgoodizerfan.feedback.control.program.ProgramNode> out = new ArrayList<>();
        boolean changed = false;
        for (var n : graph.nodes()) {
            if (n instanceof io.github.soundgoodizerfan.feedback.control.program.SensorNode s && s.target() != null && !s.target().isLocal() && moves.containsKey(s.target().pos())) {
                n = new io.github.soundgoodizerfan.feedback.control.program.SensorNode(s.id(),
                        new DataNodeRef(moves.get(s.target().pos()), s.target().side(), s.target().voxel()), s.quantity(), s.channel());
                changed = true;
            } else if (n instanceof io.github.soundgoodizerfan.feedback.control.program.ActuatorNode a && a.target() != null && !a.target().isLocal() && moves.containsKey(a.target().pos())) {
                n = new io.github.soundgoodizerfan.feedback.control.program.ActuatorNode(a.id(), a.input(),
                        new DataNodeRef(moves.get(a.target().pos()), a.target().side(), a.target().voxel()), a.channel());
                changed = true;
            }
            out.add(n);
        }
        if (changed) {
            card.set(FDataComponents.PROGRAM_GRAPH, graph.withNodes(out));
            cardChanged();
        }
    }

    /** Saves, and forgets the cached graph so a new card is evaluated on its first turning tick. */
    private void cardChanged() {
        lastGraph = ProgramGraph.EMPTY;
        delays.clear();
        delayTicks.clear();
        pendingWrites.clear();
        nextDue = Long.MAX_VALUE;
        dirty = false;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return host == null || Container.stillValidBlockEntity(host, player);
    }

    @Override
    public void clearContent() {
        card = ItemStack.EMPTY;
        cardChanged();
    }

    // --- Fittable (upgrades only) -------------------------------------------------------------

    @Nullable
    @Override
    public SidedFitting getSidedFitting(Direction side) {
        return null;
    }

    @Override
    public void setSidedFitting(Direction side, @Nullable SidedFitting fitting) {
    }

    @Override
    public boolean canMount(SidedFitting fitting, Direction side) {
        return false;
    }

    @Override
    public boolean canAdd(UpgradeFitting upgrade) {
        long same = upgrades.stream().filter(u -> u.getClass() == upgrade.getClass()).count();
        if (upgrade instanceof MemoryDrumFitting)
            return same < ControlTuning.MEMORY_DRUM_MAX;
        if (upgrade instanceof CardRackFitting)
            return same < ControlTuning.CARD_RACK_MAX;
        if (upgrade instanceof io.github.soundgoodizerfan.feedback.fitting.upgrade.EndernetFitting)
            return endernetCapable && same < 1;
        return upgrade instanceof EscapementFitting && same < 1;
    }

    /** Refused while the inserted program needs what it would take away. */
    @Override
    public boolean canRemove(UpgradeFitting upgrade) {
        return limits(upgrade).exceededBy(card.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY)).isEmpty();
    }

    @Override
    public List<UpgradeFitting> getUpgrades() {
        return upgrades;
    }

    @Override
    public boolean addUpgrade(UpgradeFitting upgrade) {
        boolean added = upgrades.add(upgrade);
        if (added)
            upgradesChanged();
        return added;
    }

    @Override
    public void removeUpgrade(UpgradeFitting upgrade) {
        if (upgrades.remove(upgrade))
            upgradesChanged();
    }

    private void upgradesChanged() {
        lastGraph = ProgramGraph.EMPTY; // re-check the card against the new limits
        setChanged();
    }

    // --- persistence --------------------------------------------------------------------------

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!card.isEmpty())
            tag.put(CARD, card.save(registries, new CompoundTag()));
        if (!circuit.isEmpty())
            tag.put(CIRCUIT, circuit.save(registries, new CompoundTag()));
        ListTag list = new ListTag();
        variables.forEach((name, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("N", name);
            if (value instanceof Float f)
                entry.putFloat("F", f);
            else
                entry.putBoolean("B", Boolean.TRUE.equals(value));
            list.add(entry);
        });
        tag.put(VARIABLES, list);
        tag.putInt(DRUMS, (int) upgrades.stream().filter(MemoryDrumFitting.class::isInstance).count());
        tag.putInt(RACKS, (int) upgrades.stream().filter(CardRackFitting.class::isInstance).count());
        tag.putBoolean(ESCAPEMENT, upgrades.stream().anyMatch(EscapementFitting.class::isInstance));
        java.util.UUID pair = endernetPair();
        if (pair != null) {
            tag.putUUID(ENDERNET, pair);
            tag.putInt(ENDERNET + "Channel", tunedChannel);
            tag.putInt(ENDERNET + "Address", tunedAddress);
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        card = tag.contains(CARD)
                ? ItemStack.parse(registries, tag.getCompound(CARD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        circuit = tag.contains(CIRCUIT)
                ? ItemStack.parse(registries, tag.getCompound(CIRCUIT)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        variables.clear();
        for (Tag t : tag.getList(VARIABLES, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            variables.put(entry.getString("N"), entry.contains("F") ? (Object) entry.getFloat("F") : (Object) entry.getBoolean("B"));
        }
        upgrades.clear();
        for (int i = 0; i < Math.min(tag.getInt(DRUMS), ControlTuning.MEMORY_DRUM_MAX); i++)
            upgrades.add(new MemoryDrumFitting());
        for (int i = 0; i < Math.min(tag.getInt(RACKS), ControlTuning.CARD_RACK_MAX); i++)
            upgrades.add(new CardRackFitting());
        if (tag.getBoolean(ESCAPEMENT))
            upgrades.add(new EscapementFitting());
        if (endernetCapable && tag.hasUUID(ENDERNET)) {
            upgrades.add(new io.github.soundgoodizerfan.feedback.fitting.upgrade.EndernetFitting(tag.getUUID(ENDERNET)));
            tunedChannel = Math.max(1, tag.getInt(ENDERNET + "Channel"));
            tunedAddress = Math.max(0, Math.min(ControlTuning.ENDERNET_ADDRESS_MAX, tag.getInt(ENDERNET + "Address")));
        }
        lastGraph = ProgramGraph.EMPTY;
        delays.clear();
        delayTicks.clear();
        nextDue = Long.MAX_VALUE;
    }
}
