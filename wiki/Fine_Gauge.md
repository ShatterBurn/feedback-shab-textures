# Fine Gauge

A gauge for building in [finespace](Finespace.md). It places nothing itself: held in either hand while you place or mine [motes](Mote.md), it sets how they go.

## Crafting

```
RP
PR
```

R is a Wrought Iron Rod and P is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

## Using it

**Sneak-use** the gauge, on a block or in the air, to cycle the mode. A strip under the crosshair lists every mode with the current one lit, and a line says what it does.

| Mode | What a click does |
| --- | --- |
| Single | Places one voxel. |
| Line | Click a start, then an end: a line of voxels joins them. |
| Box | Click one corner, then the other: the box between them fills. At most 4096 voxels in one go. |
| Mirror | Use the gauge on a face of the build to set a mirror plane through it; use it on the same plane again to remove it. Up to 3 planes. |
| Wedge | The mote in your other hand is placed as a ramp of itself; see [Fine Wedge](Fine_Wedge.md#wedge-mode). |

With mirror planes set, every voxel you place is copied across each plane, with its facing flipped.

Line and box modes also work for mining. The first mining click marks a corner and the second removes the span between. Mirror planes remove the mirrored voxels too.
