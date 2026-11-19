# Fine Space

The inside of a block, divided into 4 by 4 by 4 voxels, each a quarter of a block on a side. It is where small mechanisms, thin arms, fine models and the bodies of [vehicles](Vehicles.md) are built. The ordinary block grid is coarse space.

## Making fine space

[Cut](Mote.md#obtaining) a whole block into motes, then right-click a block or an empty spot with a [Mote](Mote.md). Where there was only air, the click makes the **Fine Space** block there and puts the mote in the voxel in front of the face you clicked. A Fine Space block holds up to 64 voxels.

- **Mine a voxel** with the right tool for its block to take it out and get its mote back. Mining Fine Space takes one voxel at a time, the one you look at.
- A face on the edge of a block of Fine Space is hidden when the voxel in the next block covers it.
- A block of Fine Space weighs its voxels by what they are made of. A build in fine space lifts on a [Lift](Vehicles.md#the-lift) like any other.

The [Fine Gauge](Fine_Gauge.md) sets single, line, box, mirror or wedge placement. The [Fine Wedge](Fine_Wedge.md) is a four-step ramp voxel.

## Rotation in fine space

A [Shaft](Shaft.md), [Bearing](Bearing.md), [Cog](Cogs.md), Gearbox, [Clutch](Clutch.md#fine-clutch), [Flywheel](Flywheel.md), [Rotary Bearing](Rotary_Bearing.md) or [Ring Gear](Ring_Gear.md) can be a voxel. Hold the part and right-click, as with a mote. A shaft lies along the face you click and joins the voxels it touches end to end. A cog takes the axis of the shaft you click it onto. Cogs mesh by the same rules as full-size [Cogs](Cogs.md), at a quarter of the size.

Only a [Rotation Adapter](Rotation_Adapter.md) joins rotation in fine space to full-size rotation.

## Machines in fine space

Any modular machine item, such as a dynamotor, thruster, jet, gas tank, heat exchanger, water wheel or gas centrifuge, can be a voxel. Start fine space with a [Mote](Mote.md), then right-click an existing fine-space cell with the machine. It takes one voxel, and that voxel is the machine: the design it was built with, running by itself and drawn at a quarter of a block. A thruster in a voxel pushes its body. One machine item pays for one voxel.

The machine is scaled to a voxel's volume when placed. Figures that follow volume are a 64th of the full-size machine: mass, power, force, burn, capacity, tank, flow, push, pull, lift, current, supply, stored energy, slots, heat, work, intake, leak and seep. Rotational inertia is a 1024th. Figures that belong to the material, such as temperatures, speeds, efficiencies and voltage bands, stay as built.

Mine the voxel to get the machine back as an item with its figures as built. Breaking the cell or replacing the voxel drops it.

A hosted machine runs alone. You cannot fill its tank, open its menu or use a crowbar on it. It does not join pipes, cables, heat or the rotation network; shafts, cogs and joints are their own parts and join as described above. A machine with a menu or screen cannot be a voxel. A machine in an assembled arm drops as an item when the arm is put back.

## Joints

The [Ball Joint](Ball_Joint.md), [Locked Joint](Locked_Joint.md), [Slider Joint](Slider_Joint.md), [Piston Joint](Piston_Joint.md), Rotary Bearing and Ring Gear are used with an empty hand once placed as voxels. Clicking a joint voxel to choose its joint, or a bearing voxel to choose its role, shows a strip under the crosshair with every option and the current one lit; see [Vehicles](Vehicles.md#joints).

## See also

- [Vehicles](Vehicles.md): lifting a build, wheels and wings.
