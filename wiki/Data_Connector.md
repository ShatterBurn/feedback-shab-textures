# Data Connector

The tool that links sensors and switchable blocks to a [Controller](Controller.md), so the Controller's [program](Programming.md) can read and drive them.

> **Recipe:** a copper wire on top of two iron rods, in a column.

## Using it

Hold the connector in either hand and every block that is a **data node** shows a small socket with a bright core, outlined through walls. Linked nodes take the color of their link; a node nothing has linked is grey-blue and blinks. A spark runs along each link.

A data node is any block that offers a reading or can be switched: a [Temperature Sensor](Instruments.md#temperature-sensor) or [Current Sensor](Instruments.md#current-sensor) face, a crucible or other vessel, a heater, a [Lathe](Lathe.md), a [Clutch](Clutch.md), [Valve](Valve.md), [Damper](Damper.md), [Breaker](Breaker.md), [Engine](Engine.md), [Thruster](Thruster.md), a [Driver's Seat](Vehicles.md#the-drivers-seat), and the Controllers themselves.

1. **Right-click** the node. You see *"Node selected."* For a sensor attached to a face, click **the face the sensor is on**.
2. **Right-click** the [Controller](Controller.md), [Electric Controller](Electric_Controller.md) or [Computer](Electric_Controller.md#computer). The node is linked to it.

The Controller's program finds the node by what it is, not where it is: see [Programming](Programming.md#the-cards). A Controller binds **8** nodes, and **4** more for each [Card Rack](Controller.md#memory-and-upgrades) fitted.

To color the link, hold a **dye in the other hand** while you make the second click: the link takes that dye's color (16 dyes). With nothing in the other hand the link has no color. A card can ask for a link color, so colors sort several nodes of one kind into groups.

On a [micro cell](Micro_Core.md), the connector picks the single joint voxel you aim at instead of the cell. A card then reads or sets that voxel: a [Piston Joint](Piston_Joint.md)'s **Piston level**, a hinge's **Angle** or **Brake** (see [Rotary Bearing](Rotary_Bearing.md#roles)), a micro [Clutch](Clutch.md#micro-clutch)'s **Engaged**. With the connector in hand, the voxels a card can set (pistons, hinges, clutches) are outlined in the cell.

## Modes

The connector has four modes. Sneak and scroll with it in your main hand to change mode; a strip under the crosshair lists the modes with the current one lit. Changing mode drops a half-made selection.

| Mode | Use on a node |
| --- | --- |
| Link nodes | Select the node, then a second one to join them with wire. This is the mode in effect until you change it. |
| Cut a link | Select one node, then a second that is linked to it: the wire between them is cut. If they are not linked, the bar says "Those aren't linked". |
| Cut every link on a node | Cuts every wire the node carries at once. The bar says "All links cut", or "Nothing connected" when the node had none. |
| Color a link | Select the Controller, then a node linked to it, with a dye in the other hand: the link takes that color. With no dye the color is cleared. The bar says "Link colored", or "Select the Controller, then a node linked to it". |

A cut wire is not refunded.

## Linking nodes

A Controller reaches a node only through a path of linked nodes. Right-click the first node, then right-click the second: the two are linked. After the first click a chip left of the crosshair says *"Linking from"* and the node's name until the link is made, fails or is cancelled. A chain of links works as a path, so a sensor can reach a Controller through other linked nodes. A node reached through another node has no link color.

While you hold the connector in your main hand after selecting the first node, a wire stretches from that node to wherever you aim. It is blue while it hangs in the air, turns green when it snaps to a node under your crosshair, and turns red when the distance is longer than the longest wire reaches.

Each link costs fine wire from your inventory, one piece per block of distance. Creative players pay nothing. The wire is taken from the stack you carry when the link is made, and the link remembers its grade. If you carry too little, the bar message says how much wire the link needs; if the distance is beyond every wire, it says no wire reaches that far.

| Wire | Reach | Reading it carries |
| --- | --- | --- |
| Copper Fine Wire (poor grade) | up to 8 blocks | rounded to one significant figure |
| Redstone-Chromium Fine Wire (good grade) | up to 32 blocks | exact |

A link uses copper wire when it reaches and you have enough, otherwise redstone-chromium. A path is as good as its worst wire. A live [endernet](Data_Port.md#endernet) pair counts as one hop of the best grade, so a path can cross any distance through it.

**Longer links.** A link that ends at a block holding a [circuit](Basic_Electronic_Circuit.md) with gain reaches further: each point of gain adds the wire's plain reach again, and the better of the two ends counts. The blocks that take a circuit are the [Controller](Controller.md#circuit-slot), [Electric Controller](Electric_Controller.md#circuit-slot), [Computer](Electric_Controller.md#circuit-slot), [Data Port](Data_Port.md#circuit), [Power Cutter](Power_Cutter.md#circuit), [Auto Engineer](Auto_Engineer.md#circuit) and [Electrolytic Cell](Electrolytic_Cell.md#circuit). Gain comes from a [Vacuum Tube](Vacuum_Tube.md) on the board and reads at its warmth, so a cold tube gives none. The wire still costs one piece per block, and the grade and quenching rules are unchanged.

Redstone-chromium wire quenches: if either end of a good line carries more than 3 links, the line goes dark for 10 seconds and then recovers undamaged. A reader with no working path reads 0.

The mechanical [Controller](Controller.md) is a data node like the others and is linked with wire in the same way.
