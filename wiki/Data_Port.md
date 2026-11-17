# Data Port

One end of a bonded pair that carries data across any distance with no wire between the two ends. A sensor or machine linked to one port can be read from the other.

## Crafting

Put one Ender Pearl or [Synthetic Pearl](Synthetic_Pearl.md), two metal plates and one length of metal fine wire (any metals, any arrangement) in the crafting grid. The result is **two** Data Ports that share one bond. Ports from different crafts are not bonded to each other.

## Using it

- **Power it.** A Data Port is a sink on an [MV](Electricity.md#bands-and-amps) line and draws a small standing current while it is bonded. A pair carries only while both ends are powered; an unpowered end leaves the pair dark.
- **Link things to it** with the [Data Connector](Data_Connector.md#linking-nodes), as to any other node: a sensor, a machine or a [Computer](Electric_Controller.md#computer).
- **Read from the other end.** Whatever is linked to one end can be reached from the other end, and from any [Electric Controller](Electric_Controller.md) or [Computer](Electric_Controller.md#computer) that carries the bonded [Endernet Upgrade](Endernet_Upgrade.md), as if it were wired there. A [Sensor card](Programming.md#linking-cards-to-real-blocks) linked to a far node reads it.
- **Right-click** a port empty-handed to read its tank of teleportant, whether its chunk is anchored and the state of the pair: *carrying*, *far end not loaded or not powered*, *not powered* or *none (unbonded)*.
- Use a [Chunk Anchor](Ender_Port.md#chunk-anchor) on a port to keep its chunk loaded, so the pair works while nobody is near. An end in an unloaded chunk pauses the pair.
- A [Tuned Eye](Tuned_Eye.md) tuned to a port reads the distance, direction and height of the other end.

## Endernet

A bonded pair is an **endernet** link. It is the one place data crosses a gap with no wire: the pair counts as a single hop of the best grade, so a reading that crosses it comes out as exact as the wire on either side allows. The rules:

- **One dimension.** Both ends must be in the same dimension.
- **A port at one end at least.** One end of every pair must be a Data Port, because the port's tank pays for the crossing. The other end may be a second port or an Endernet Upgrade; a pair with an upgrade at both ends carries nothing.
- **Each crossing costs teleportant.** Every signal that crosses spends a little [Liquid Teleportant](Liquid_Teleportant.md) from the port's tank, more at a greater distance, and uses up a share of the port's **bandwidth**, which refills over about a minute. A pipe fills the tank, which holds 500 mB and takes only teleportant.
- **Dry or saturated means dark.** A pair whose port is out of teleportant or bandwidth carries nothing until it recovers. Nothing is lost or destroyed.

A Computer can list and switch what is linked to the far end in its [Network app](Electric_Controller.md#disk-apps).
