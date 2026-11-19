# Finespace

The inside of a block, divided into 4 by 4 by 4 voxels, each a quarter of a block on a side. It is where small mechanisms, thin arms, fine models and the bodies of [vehicles](Vehicles.md) are built. The ordinary block grid is coarsespace.

## Making finespace

[Cut](Mote.md#obtaining) a whole block into motes, then right-click a block or an empty spot with a [Mote](Mote.md). Where there was only air, the click makes the **Finespace** block there and puts the mote in the voxel in front of the face you clicked. A Finespace block holds up to 64 voxels.

- **Mine a voxel** with the right tool for its block to take it out and get its mote back. Mining Finespace takes one voxel at a time, the one you look at.
- A face on the edge of a block of Finespace is hidden when the voxel in the next block covers it.
- A block of Finespace weighs its voxels by what they are made of; a hosted machine weighs by its design (see [Machines in finespace](#machines-in-finespace)). A build in finespace lifts on a [Lift](Vehicles.md#the-lift) like any other.

The [Fine Gauge](Fine_Gauge.md) sets single, line, box, mirror or wedge placement. The [Fine Wedge](Fine_Wedge.md) is a four-step ramp voxel.

## Rotation in finespace

A [Shaft](Shaft.md), [Bearing](Bearing.md), [Cog](Cogs.md), Gearbox, [Clutch](Clutch.md#fine-clutch), [Flywheel](Flywheel.md), [Rotary Bearing](Rotary_Bearing.md) or [Ring Gear](Ring_Gear.md) can be a voxel. Hold the part and right-click, as with a mote. A shaft lies along the face you click and joins the voxels it touches end to end. A cog takes the axis of the shaft you click it onto. Cogs mesh by the same rules as full-size [Cogs](Cogs.md), at a quarter of the size.

Only a [Rotation Adapter](Rotation_Adapter.md) joins rotation in finespace to full-size rotation.

## Machines in finespace

Any modular machine item, such as a dynamotor, thruster, jet, gas tank, heat exchanger, water wheel or gas centrifuge, can be a voxel. Start finespace with a [Mote](Mote.md), then right-click an existing finespace cell with the machine. It takes one voxel, and that voxel is the machine: the design it was built with, drawn at a quarter of a block. A thruster in a voxel pushes its body. One machine item pays for one voxel.

The machine is scaled to a voxel's volume when placed. Figures that follow volume are a 64th of the full-size machine: mass, power, force, burn, capacity, tank, flow, push, pull, lift, current, supply, stored energy, slots, heat, work, intake, leak and seep. Rotational inertia is a 1024th. Figures that belong to the material, such as temperatures, speeds, efficiencies and voltage bands, stay as built.

Mine the voxel to get the machine back as an item with its figures as built. Breaking the cell or replacing the voxel drops it.

A hosted machine answers its own use. Click it with an item or an empty hand and the machine decides, exactly as the full-size block would: fill a tank with a bucket, work a machine's controls. The click is mapped into the voxel.

Fluid and item pipes, hoppers and the like see a hosted machine. The cell answers fluid and item connections for the machines it holds, and the machine on the face they touch first takes the connection.

A hosted machine weighs a 64th of the mass figure of its design, not the stuff its block is made of.

A hosted rotation machine, such as a dynamotor on a shaft or a flywheel-driven or water-driven machine, joins the finespace rotation network. It couples to the shafts and cogs beside its voxel by the same shaft and tooth rules as the full-size block, so a finespace shaft can turn it or be turned by it.

A hosted electric machine, such as a dynamotor or motor, joins the cable run: the cell is on the cable it touches like the full-size block at that spot. A hosted heat source, such as a heater or coil, heats the blocks and vessels round the cell the way the full-size block would.

A cell takes part in the electric network and in heating through one machine each: the first electric machine it holds and the first heat source it holds. A second electric machine in the same cell is not on the cable. A hosted machine reaches what the cell's block touches, not the voxel beside it. When a hosted machine's figures ride the machine's standard figures they are a 64th of the full-size ones; a dynamotor's and a pressure vessel's own figures stay as the full-size machine's.

A machine that toggles its own state (lamp, burner, damper, valve) cannot be hosted. A machine in an assembled arm blocks the joint from assembling or putting back; the message says to take it out first. A machine that opens a screen is unchecked.

## Joints

The [Ball Joint](Ball_Joint.md), [Locked Joint](Locked_Joint.md), [Slider Joint](Slider_Joint.md), [Piston Joint](Piston_Joint.md), Rotary Bearing and Ring Gear are used with an empty hand once placed as voxels. Clicking a joint voxel to choose its joint, or a bearing voxel to choose its role, shows a strip under the crosshair with every option and the current one lit; see [Vehicles](Vehicles.md#joints).

## See also

- [Vehicles](Vehicles.md): lifting a build, wheels and wings.
