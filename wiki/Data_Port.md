# Data Port

One end of a bonded pair that carries data across any distance with no wire between the two ends. A sensor or machine linked to one port can be read from the other.

## Crafting

Put one Ender Pearl or [Synthetic Pearl](Synthetic_Pearl.md), two metal plates and one length of metal fine wire (any metals, any arrangement) in the crafting grid. The result is **two** Data Ports that share one bond. Ports from different crafts are not bonded to each other.

## Using it

- **Power it.** A Data Port is a sink on an [MV](Electricity.md#bands-and-amps) line and draws a small standing current while it is bonded. A pair carries only while both ends are powered; an unpowered end leaves the pair dark.
- **Link things to it** with the [Data Connector](Data_Connector.md#linking-nodes), as to any other node: a sensor, a machine or a [Computer](Electric_Controller.md#computer).
- **Read from the other end.** Whatever is linked to one end can be reached from the other end, and from any [Electric Controller](Electric_Controller.md) or [Computer](Electric_Controller.md#computer) that carries the bonded [Endernet Upgrade](Endernet_Upgrade.md), as if it were wired there. A [Sensor card](Programming.md#linking-cards-to-real-blocks) linked to a far node reads it.
- **Right-click** a port empty-handed to read its tank of teleportant, whether its chunk is anchored and the state of the pair: *carrying*, *far end not loaded or not powered*, *not powered* or *none (unbonded)*. The same click steps the port's [channel](#channels): off, then 1 up to the number its circuit separates, then off again, and the message names the channel it landed on.
- Use a [Chunk Anchor](Ender_Port.md#chunk-anchor) on a port to keep its chunk loaded, so the pair works while nobody is near. An end in an unloaded chunk pauses the pair.
- A [Tuned Eye](Tuned_Eye.md) tuned to a port reads the distance, direction and height of the other end.

## Circuit

Right-click a port with a [circuit](Basic_Electronic_Circuit.md) to seat it; sneak and right-click with an empty hand takes it out. The port works without one.

- **Bandwidth.** A circuit with a [Resonator](Resonator.md) raises the signals a minute the port carries, in proportion to the resonator's bandwidth. The far end has no say: the port pays.
- **Channels.** The resonator's selectivity sets how many [channels](#channels) the port can be tuned to: 1 with no circuit, up to 16 with the best.
- **Reach.** A circuit with gain lengthens a data link made to the port with the [Data Connector](Data_Connector.md#linking-nodes). A port has no heater, so a [tube](Vacuum_Tube.md) in the circuit gives gain only while something else keeps it warm; a cold tube gives none.

## Endernet

A bonded pair is an **endernet** link. It is the one place data crosses a gap with no wire: the pair counts as a single hop of the best grade, so a reading that crosses it comes out as exact as the wire on either side allows. The rules:

- **One dimension.** Both ends must be in the same dimension.
- **A port at one end at least.** One end of every pair must be a Data Port, because the port's tank pays for the crossing. The other end may be a second port or an Endernet Upgrade; a pair with an upgrade at both ends carries nothing.
- **Each crossing costs teleportant.** Every signal that crosses spends a little [Liquid Teleportant](Liquid_Teleportant.md) from the port's tank, more at a greater distance, and uses up a share of the port's **bandwidth**, which refills over about a minute. A pipe fills the tank, which holds 500 mB and takes only teleportant.
- **Dry or saturated means dark.** A pair whose port is out of teleportant or bandwidth carries nothing until it recovers. Nothing is lost or destroyed.

A Computer can list and switch what is linked to the far end in its [Network app](Electric_Controller.md#disk-apps).

## Channels

A port can be tuned to a **channel**, a plain number. Tuning is off until you right-click it. Every port in one dimension tuned to the same channel shares it, with no names and no addresses.

- **One shared allowance.** A channel carries 1200 signals a minute for everything on it, however many ports and Computers share it. A busy channel starves its own listeners.
- **Four signal slots.** A tuned port that is carrying, and whose pair holds no robot or suit, offers four slots. An Actuator card linked to the port writes a slot; a [Sensor card](Programming.md#linking-cards-to-real-blocks) linked to any port tuned to the same channel reads it. A value that is not rewritten goes stale after about a minute.
- **Anyone tuned hears it.** A slot has no address, so every port on the channel can read it.
- **Each signal costs** a share of the channel's allowance, plus the port's own teleportant and bandwidth over the distance, as any crossing does.
- **Robots and suits come first.** A port whose pair carries a [robot's leash](Robots.md#endernet-leash) or a [suit's scan](Exosuit.md#sharing-a-scan) gives its channels to that and offers no slots.

A [Computer](Endernet_Upgrade.md#sending-between-computers) sends messages and programs over a channel as well.
