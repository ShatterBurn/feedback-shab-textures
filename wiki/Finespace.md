# Finespace

The inside of a block, divided into 4 by 4 by 4 voxels, each a quarter of a block on a side. It is where small mechanisms, thin arms, fine models and the bodies of [vehicles](Vehicles.md) are built. The ordinary block grid is coarsespace.

## Making finespace

[Cut](#motes) a whole block into motes, then right-click a block or an empty spot with a [Mote](#motes). Where there was only air, the click makes the **Finespace** block there and puts the mote in the voxel in front of the face you clicked. A Finespace block holds up to 64 voxels.

- **Mine a voxel** with the right tool for its block to take it out and get its mote back. Mining Finespace takes one voxel at a time, the one you look at.
- A face on the edge of a block of Finespace is hidden when the voxel in the next block covers it, or when a full-size block beside it does. A hosted machine hides no neighbor's face.
- **Jade** names the voxel under the cursor: "Stone Mote" for a stone voxel.
- A [Wrench](Wrench.md) on a voxel turns that voxel by its block's own rules (facing or axis), keeping its look, wear and any hosted machine. Joints do not turn.
- A block of Finespace weighs its voxels by what they are made of; a hosted machine weighs by its design (see [Machines in finespace](#machines-in-finespace)). A build in finespace lifts on a [Lift](Vehicles.md#the-lift) like any other.

The [Fine Gauge](#fine-gauge) sets single, line, box, mirror or wedge placement. The [Fine Wedge](#fine-wedge) is a four-step ramp voxel.

## Motes

A **Mote** is one voxel of a block: a 64th of it, a quarter of a block on a side. Motes are what you build with in finespace.

Cut a whole block into 64 motes at the [Engineering Table](Engineering_Table.md) with a [Sawblade](Sawblade.md) in a tool slot. No fasteners are needed, and it needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

```
B
```

B is any plain, solid, whole block: stone, planks, a metal block, wool. A [shaft](Shaft.md), [cog](Cogs.md), [joint](Joints.md) or [tire hub](Tire.md#tire-hub) can be cut too, into motes of itself. A block with a menu or machinery, a half block, a fluid or anything that is not a full cube cannot be cut. The motes are named for their block ("Stone Mote") and motes of one block stack. Every cuttable block has its own mote in the **Feedback: Motes** creative tab. Mining a voxel with the right tool gives its mote back.

Right-click any block or any piece of finespace with a mote in hand. It goes in the voxel in front of the face you clicked, at the exact spot you pointed. While you hold a mote, an outline shows the voxel it fills. Blocks place the way they do full size: a log lies along its axis, a stair takes the half you clicked. A mote in the inventory, in hand and on the ground is a small cube of its block's texture. In the world each voxel is drawn as its block's own model at a quarter of the size, and you walk on and bump into the exact voxels. A glowing block lights the space around it.

Parts that work in finespace ([shafts](Shaft.md), [cogs](Cogs.md), [joints](Joints.md), tire hubs) are cut into motes like any block, and each mote is one voxel of the part: a fine shaft, a fine cog. [Modular machines](#machines-in-finespace) are placed as themselves, one item per voxel.

## Fine Gauge

A gauge for building in finespace. It places nothing itself: held in either hand while you place or mine motes, it sets how they go.

```
RP
PR
```

R is a Steel Rod and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

**Sneak-use** the gauge, on a block or in the air, to cycle the mode. A strip under the crosshair lists every mode with the current one lit, and a line says what it does.

| Mode | What a click does |
| --- | --- |
| Single | Places one voxel. |
| Line | Click a start, then an end: a line of voxels joins them. |
| Box | Click one corner, then the other: the box between them fills. At most 4096 voxels in one go. |
| Mirror | Use the gauge on a face of the build to set a mirror plane through it; use it on the same plane again to remove it. Up to 3 planes. |
| Wedge | The mote in your other hand is placed as a ramp of itself; see [Wedge mode](#wedge-mode). |

With mirror planes set, every voxel you place is copied across each plane, with its facing flipped.

Line and box modes also work for mining. The first mining click marks a corner and the second removes the span between. Mirror planes remove the mirrored voxels too.

## Fine Wedge

A four-step ramp voxel, a quarter of a block long, falling away from you.

```
S 
SS
```

S is a Smooth Stone Slab. The recipe makes 2.

Hold it and right-click a block or a piece of finespace as you would place a mote. The ramp falls away from you. Click the upper half of a face to hang it from above instead.

Each of the four steps is 1/16 of a block. What you see is what collides and what the outline shows: the same four steps. The wedge has a single look.

### Wedge mode

Set a Fine Gauge to **Wedge** mode, hold it in one hand and a mote in the other, and the mote goes in as a ramp of itself. It takes each face's own texture and tint (a grass wedge is green on top), and mining it gives the mote back. Rotation parts and joints cannot be cut to a wedge.

## Voxel Case

A hinged iron case that carries one whole block of finespace, with everything in it.

```
PP
RP
```

R is a Steel Rod and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

Use it empty on a block of Finespace: every voxel, its look, wear and spin, and every hosted machine with the item it was made from, goes into the case and the block becomes air. Use it full on the clicked block if it can be replaced, otherwise on the spot beside the face you clicked: the block comes back exactly as it was. The case holds one block and does not stack. A full case glints, and its tooltip says how many voxels and machines it holds.

A block with a joint or a belt cannot be carried: the message says it is tied to something outside the block.

## Rotation in finespace

A [Shaft](Shaft.md), [Bearing](Bearing.md), [Cog](Cogs.md), Gearbox, [Clutch](Clutch.md#fine-clutch), [Flywheel](Flywheel.md), [Rotary Bearing](Rotary_Bearing.md) or [Ring Gear](Ring_Gear.md) can be a voxel. Hold the part's mote and right-click. A shaft lies along the face you click and joins the voxels it touches end to end. A cog takes the axis of the shaft you click it onto. Cogs mesh by the same rules as full-size [Cogs](Cogs.md), at a quarter of the size.

Only a [Rotation Adapter](Rotation_Adapter.md) joins rotation in finespace to full-size rotation.

## Machines in finespace

Any modular machine item, such as a dynamotor, thruster, jet, gas tank, heat exchanger, water wheel or gas centrifuge, can be a voxel. Right-click an existing finespace cell with the machine, or a block whose neighbor across the face you click is a finespace cell, so a machine can sit against a block. It takes one voxel, and that voxel is the machine: the design it was built with, drawn at a quarter of a block. A thruster in a voxel pushes its body. One machine item pays for one voxel.

The machine is scaled to a voxel's volume when placed. Figures that follow volume are a 64th of the full-size machine: mass, power, force, burn, capacity, tank, flow, push, pull, lift, current, supply, stored energy, slots, heat, work, intake, leak and seep. Rotational inertia is a 1024th. Figures that belong to the material, such as temperatures, speeds, efficiencies and voltage bands, stay as built.

Mine the voxel to get the machine back as an item with its figures as built. Breaking the cell or replacing the voxel drops it.

A hosted machine answers its own use. Click it with an item or an empty hand and the machine decides, exactly as the full-size block would: fill a tank with a bucket, work a machine's controls. The click is mapped into the voxel.

Fluid and item pipes, hoppers and the like see a hosted machine. The cell answers fluid and item connections for the machines it holds, and the machine on the face they touch first takes the connection.

A hosted machine weighs a 64th of the mass figure of its design, not the stuff its block is made of.

A hosted rotation machine, such as a dynamotor on a shaft or a flywheel-driven or water-driven machine, joins the finespace rotation network. It couples to the shafts and cogs beside its voxel by the same shaft and tooth rules as the full-size block, so a finespace shaft can turn it or be turned by it.

Every hosted electric machine is its own point on the cable run. It connects to the voxel beside it, whether that is in the same cell, in the next cell or a whole block's face, so two machines in one cell are each on the line. Every hosted heat source, such as a heater, coil or burner, is likewise its own: two heaters in one cell heat separately, and a hosted burner heats the voxel beside it.

A hosted crucible, boiler or thermal store reads fire from the voxels round it: the voxel beside it, a voxel of the next cell, or a whole burner beside the cell. Only a voxel on the cell's bottom row has lava under it. When a hosted machine's figures ride the machine's standard figures they are a 64th of the full-size ones; a dynamotor's and a pressure vessel's own figures stay as the full-size machine's.

A machine that toggles its own state shows it on its voxel: a burner lit, a lamp, a crucible or a boiler glowing, an electromagnet lit, a damper or a valve open. Hosted machines keep animating while they run. A machine that opens a screen, such as a crucible or a thermal vessel, opens it and keeps it open.

A machine in a cell that a [Rotary Bearing](Rotary_Bearing.md) or joint turns travels with the arm's voxels and lands in the voxel the arm puts it in when the arm is put back, keeping its contents.

## Fine wire

A **Fine Wire** of a conductor metal ([Wire Drawer](Wire_Drawer.md)) is a bare conductor of one voxel. Right-click a finespace cell with it: one fine wire fills one voxel. The voxel is drawn whole in the metal's color.

A wire voxel joins the electric run to every electric voxel beside it, in the same cell or the next. It carries like a [standard cable](Copper_Cable.md#how-much-it-carries) and loses a quarter of a standard cable's loss per voxel. It is bare: it arcs across exactly one empty voxel to another fine wire in a straight line, across cell edges too ([Bare cable arcs](Copper_Cable.md#bare-cable-arcs)), but not to a full-size cable. It gives no shock to a hand.

Overload heats it ([Overloading](Copper_Cable.md#overloading)); when it burns out, only its voxel is emptied. Mine it to get the fine wire back.

## Joints

The [Ball Joint](Joints.md#ball-joint), [Locked Joint](Joints.md#locked-joint), [Slider Joint](Joints.md#slider-joint), [Piston Joint](Joints.md#piston-joint), Rotary Bearing and Ring Gear are used with an empty hand once placed as voxels. Clicking a joint voxel to choose its joint, or a bearing voxel to choose its role, shows a strip under the crosshair with every option and the current one lit; see [Vehicles](Vehicles.md#joints).

## See also

- [Vehicles](Vehicles.md): lifting a build, wheels and wings.
