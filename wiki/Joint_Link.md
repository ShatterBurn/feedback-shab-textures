# Joint Link

A reusable tool that ties a joint voxel to a body that is already lifted, without making a new body. It closes chains of [joints](Vehicles.md#joints) into loops.

## Crafting

```
R
B
R
```

R is a Wrought Iron Rod and B is a [Rotary Bearing](Rotary_Bearing.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

1. Click a joint voxel that has nothing in front of its front face. A chip left of the crosshair says *"Linking from"* and the joint's name until the link is made, fails or is cancelled.
2. Click a voxel of another lifted body within 1.5 blocks of that face.

That body is held to the joint, as a body assembled from the blocks in front of it would be. Nothing is made and nothing is used up.

**Right-click the joint voxel with an empty hand** to let the body go.

## How it works

A chain of joints closed this way is a loop. The [Lift](Vehicles.md#the-lift) reports loops when it lifts the vehicle. A Lift refuses a vehicle that has a link until the link is let go.

## See also

- [Rotary Bearing](Rotary_Bearing.md): the joint most often linked.
- [Vehicles](Vehicles.md#joints): the other joints.
