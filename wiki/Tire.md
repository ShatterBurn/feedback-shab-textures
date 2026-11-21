# Tire

A ring of rubber that is fitted to a [Tire Hub](#tire-hub), the part that makes a body a wheel. Each rubber makes its own tire, such as a *Natural Rubber Tire* or a *Synthetic Rubber Tire*, and the wheel grips, wears and rolls by that rubber's figures. The item is a ring, tinted by the rubber.

## Obtaining

Shape an ingot of a material with the **tire** trait into a tire with a blow step at the shaping tables ([Metalworking](Metalworking.md)). Natural rubber and synthetic rubber have the trait ([Materials](Materials.md#polymers)).

## Using it

- **Use** a tire on a [Tire Hub](#tire-hub) to fit it.
- **Sneak-use** the hub with an empty hand to take the tire off.
- Breaking the hub gives the tire back.
- Use a tire on the [Rotary Bearing](Rotary_Bearing.md) of a wheel to replace the tire and reset the tread wear.

## How it works

The rubber's friction sets the grip. Its hardness sets how soft the tire is: a softer tire wears faster and rolls harder. See [Wheels](Vehicles.md#wheels) for wear, braking and the effect of the ground.

## Tire Hub

The part that makes a body turned by a [Rotary Bearing](Rotary_Bearing.md) a wheel. It comes in three sizes, **Small**, **Medium** and **Large Tire Hub**, and sets the wheel's radius.

The hub is a round flange on a short bronze stub axle along its axis, with a ring of bolt heads painted on the flange: four on the small, six on the medium, eight on the large. The large flange is steel, the others iron. The outline follows the flange and axle.

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

S is a [Shaft](Shaft.md). P is a Wrought Iron Plate for every hub.

To make a wheel:

1. Build the body a role 0 Rotary Bearing turns, and put the hub on its axle: as a block in ordinary space, or as a voxel in [finespace](Finespace.md). The hinge is what makes the wheel.
2. Use a tire on the hub to fit it. A hub comes with no tire.
3. Assemble the bearing as usual. The body is a wheel.

**Sneak-use** the hub with an empty hand to take the tire off. Breaking the hub gives the tire back.

A hub on a body fixed to its vehicle, with no Rotary Bearing turning it, is not a wheel: it does nothing, has no ring to roll on, and slides along with the body. Four hubs fixed on a block slide.

The hub declares the wheel's radius, from the axle to the tread:

| Hub | In ordinary space | In finespace |
| --- | --- | --- |
| Small | about 0.75 blocks | 0.25 blocks |
| Medium | 1 block | 0.375 blocks |
| Large | 1.5 blocks | 0.5 blocks |

The tire is drawn as a true cylinder about the axle. How the wheel grips, wears and rolls is in [Wheels](Vehicles.md#wheels).

The hub has no suspension: the wheel is wherever it is mounted. For a smoother ride, build a [Slider Joint](Joints.md#slider-joint) into the mount.

## Tire Rubber

A block of pressed [synthetic rubber](Materials.md#polymers): a soft, grippy building block that collides like any other. It is not a wheel; a wheel is a [Tire Hub](#tire-hub) fitted with a tire.

```
RR
RR
```

R is a Synthetic Rubber Ingot. The recipe makes 1. Place it as a block, or cut it into [motes](Finespace.md#motes) and place them in [finespace](Finespace.md). Rubber has a friction of 1.2, so it grips more than most blocks.
