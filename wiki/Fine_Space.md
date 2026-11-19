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

## Joints

The [Ball Joint](Ball_Joint.md), [Locked Joint](Locked_Joint.md), [Slider Joint](Slider_Joint.md), [Piston Joint](Piston_Joint.md), Rotary Bearing and Ring Gear are used with an empty hand once placed as voxels. Clicking a joint voxel to choose its joint, or a bearing voxel to choose its role, shows a strip under the crosshair with every option and the current one lit; see [Vehicles](Vehicles.md#joints).

## See also

- [Vehicles](Vehicles.md): lifting a build, wheels and wings.
