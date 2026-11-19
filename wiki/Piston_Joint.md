# Piston Joint

A joint, in ordinary blocks or [finespace](Finespace.md), that slides a body along its front axis to a level a [Controller](Controller.md) card sets. It is a linear actuator: a lift, a ram, a retracting leg.

## Crafting

```
S
B
I
```

S is a [Shaft](Shaft.md), B is a [Rotary Bearing](Rotary_Bearing.md) and I is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

It works as a full-size block and as a voxel of [finespace](Finespace.md).

1. Place it against a block, or put it in finespace, with its front along the line the body should slide on.
2. **Use it with an empty hand** to assemble everything joined to its front face into a second body that slides.
3. **Use it again** to put the body back, at the nearest of the 24 ways a cube can be turned.
4. Link the piston to a Controller with a [Data Connector](Data_Connector.md) and add a *Switch* card on its **Piston level** channel. A number from -1 to 1 sets the level; the travel is 0.5 block in all.

## How it works

The piston holds the level the card sets against a load. It wears the joint if a load keeps it from reaching the level for 5 seconds or more. See [Wear](Vehicles.md#wear).

## See also

- [Slider Joint](Slider_Joint.md): slides by a spring instead.
- [Vehicles](Vehicles.md#joints): the other joints.
