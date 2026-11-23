# Scrubbing Bed

A packed bed that takes **flux** out of the chunk it stands in. It is for keeping a chunk clean when [Thaumaturge](Mod_Compats.md#flux-scrubbing-and-rust) is installed; without that mod there is no flux and the bed does nothing.

## Crafting

```
  A
S D S
S S S
```

S is a Wrought Iron Plate, D is a Drilled Wrought Iron Plate and A is an Air Intake. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

- **Right-click** with a bed item to pack it. Any item whose material works as a catalyst bed works, such as charcoal; the bed it replaces comes back. The [Reactor](Reactor.md#catalysts) page lists the catalyst materials.
- **Right-click with an empty hand** to read the bed in words: no bed, taking flux (under half full), loaded (over half), saturated, or burnt out.
- **Sneak and right-click with an empty hand** to take the bed out. Breaking the block drops it.

## How it works

Once packed, the bed takes flux out of its chunk, about once a second, and holds it. A more active catalyst material takes flux faster and holds more. Its **burn clean** and **sinter** temperatures are the same ones the Reactor page lists for that material.

- **Saturated.** A full bed takes nothing and lets a trickle of flux back into the chunk.
- **Regenerating.** A fire directly beneath the bed, facing up into it (a [Firebox](Firebox.md) or other [heat source](Heat.md)), burns the load off once the flame is past the material's burn clean temperature. The bed takes no flux while it still has load to burn.
- **Ruined.** A flame past the material's sinter temperature burns away some of the bed's activity for good, for as long as it stays there. A ruined bed takes nothing and has to be repacked.

## See also

- [Essentia Vent](Essentia_Vent.md): what bleeds into the chunk as flux that a bed can take back out.
- [Taint Glaze](Taint_Glaze.md): protects single machines from the corrosion that flooded chunks cause.
