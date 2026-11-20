# Tube Station

The door of a [tube](Pneumatic_Tube.md) run. Items go in, are packed into a [carrier](Carrier.md) stamped with an address and sent into the tube the station faces; arriving carriers are emptied out.

## Crafting

```
S S S
S T S
S S S
```

S is a Wrought Iron Plate and T is a Pneumatic Tube.

## Using it

The station is a wrought iron plate box with an open copper tube mouth on the side it faces. It faces the tube it loads into, in any of the six directions.

- **Feed it** with a hopper or the end of a [belt](Conveyor_Belt.md). The station packs a carrier when its buffer is full, or when it has stood idle for a moment.
- **Stock it**: use carriers on the station. It keeps carriers and pods separately.
- **Use a dye** to set the address, the color stamped on every carrier it sends. **Sneak with an empty hand** to clear it.
- Arriving carriers empty into an output buffer that pushes into neighboring inventories.
- **Right-click** with an empty hand to board a stocked [pod](Carrier.md#pod), or to read the station's state if there is none.

## Sorting at the station

Sorting happens once, here, not in the tube. With a **Sort Gauge** fitted, the station reads each unit and sends it to one address if it is in the chosen band and another if not. Units with the same address share a carrier.

**Sort Gauge**:

```
W
P
P
```

W is Copper Wire and P is a Copper Plate.

- **Use the gauge on a station** to step its last stage through the bands, then to the next property.
- **Sneak and use it** to add a stage, or to clear the stages when full.
- **Sneak with a dye** to set the address for units that are in the band. A plain dye sets the address for everything else.

## Sorting Station

A station with up to three sort stages. Each stage is a separate reader (a magnet, an optical head or a weigher) that a unit must pass. A unit that passes every stage goes to the in-band address; a unit it cannot place goes to the **reject address** (an off-hand dye sets it). It runs at the pace of its slowest stage; a weigher is the slowest and a magnet the quickest.

Crafting, at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 3 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)):

```
E L G
  C
  T
```

E is an [Electromagnet](Electromagnet.md), L is a [Glowstone Lamp](Lamp.md), G is a Sort Gauge, C is a [circuit](Basic_Electronic_Circuit.md) and T is a Tube Station.

The readers share the weaknesses of the [belt sorters](Belt_Modules.md#sorting).

## Sorting by kind

The gauge's **Kind** property sorts units as metallic, stony, granular, organic, polymer or other. A station does it by a **spark and flame test**, which is a guess made from a sample and is noisy even on a plain case. Some stock fools it:

- an **alloy** gives muddy sparks;
- a **soft metal** gives no sparks at all;
- a **polymer**, or an organic that does not char, reads like the wrong kind, as does a **mineral that burns** (coal, say);
- a **plated** stack hides what is under the plating.

A station with more than one stage reads kind more cleanly. JEI's **Separation** card shows a **Spark and flame test** row for each material: *Reads true*, *Muddy (an alloy)* or *Fooled (soft metal, look-alike)*.
