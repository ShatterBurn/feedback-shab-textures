# Tire Hub

The part that makes a body turned by a [Rotary Bearing](Rotary_Bearing.md) a wheel. It comes in three sizes, **Small**, **Medium** and **Large Tire Hub**, and sets the wheel's radius.

The hub is a round flange on a short bronze stub axle along its axis, with a ring of bolt heads painted on the flange: four on the small, six on the medium, eight on the large. The large flange is steel, the others iron. The outline follows the flange and axle.

## Crafting

Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, machine bolts ([Fasteners](Fasteners.md)) and Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

Small (4 machine bolts):

```
 P
PSP
 P
```

Medium (6 machine bolts):

```
PPP
PSP
PPP
```

Large (8 machine bolts), the same grid as the medium:

```
PPP
PSP
PPP
```

S is a [Shaft](Shaft.md). P is a Wrought Iron Plate for the small and medium hub and a Steel Plate for the large.

## Using it

1. Build the body a role 0 Rotary Bearing turns, and put the hub on its axle: as a block in ordinary space, or as a voxel in [finespace](Finespace.md).
2. Use a [tire](Tire.md) on the hub to fit it. A hub comes with no tire.
3. Assemble the bearing as usual. The body is a wheel.

**Sneak-use** the hub with an empty hand to take the tire off. Breaking the hub gives the tire back.

## How it works

The hub declares the wheel's radius, from the axle to the tread:

| Hub | In ordinary space | In finespace |
| --- | --- | --- |
| Small | about 0.75 blocks | 0.25 blocks |
| Medium | 1 block | 0.375 blocks |
| Large | 1.5 blocks | 0.5 blocks |

The tire is drawn as a true cylinder about the axle. How the wheel grips, wears and rolls is in [Wheels](Vehicles.md#wheels).

The hub has no suspension: the wheel is wherever it is mounted. For a smoother ride, build a [Slider Joint](Slider_Joint.md) into the mount.
