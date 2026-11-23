# Energy Fitting

The one block that gives Forge Energy (FE, the energy other mods use) from a Feedback [electrical](Electricity.md) line to another mod's machines, at a loss.

## Crafting

```
W R W
P P P
```

W is [Copper Wire](Wire_Drawer.md), R is a [Rectifier](Rectifier.md) and P is a Copper Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws, holding Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

## Using it

Wire it to an MV line like any other load; it draws nothing from an LV line. Place it touching blocks from other mods that take FE. It pushes its stored FE into each of them, and anything that draws FE from it can do so too. It never takes FE in.

It asks the line for **2 A** while its buffer has room. The buffer holds **20000 FE** and gives up to **400 FE** a tick.

## How it works

Each Eu taken from the line becomes 4 FE, and four fifths of that is kept; the rest is lost as heat. A full buffer stops it asking the line for current, so a load that stops leaves the line unharmed.

See [Mod Compats](Mod_Compats.md#energy-fitting) for how it sits among the mods Feedback works with.
