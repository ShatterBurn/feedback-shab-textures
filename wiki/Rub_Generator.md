# Rub Generator

Friction electricity by hand. Rub a rod of an insulator and it holds a charge, which the generator gives to a line as a small low-voltage burst. Wired to a [Leyden Jar](Leyden_Jar.md), the bursts add up.

## Crafting

```
  F
P W K
P P P
```

P is a Wrought Iron Plate, W is White Wool, K is a Copper Plate and F is Copper Fine Wire. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 screws ([Fasteners](Fasteners.md)). It needs Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

## Using it

- **Right-click** with a rod to seat it. Any item of an insulating material rated above LV works; amber is the classic one. The rod it replaces comes back.
- **Right-click with an empty hand** to rub the rod. Each rub leaves charge on it. When the rod holds all it can, a message says so.
- **Sneak and right-click with an empty hand** to take the rod out. Breaking the block drops it.

## How it works

Only an insulator holds a charge on its surface, so the better the rod's insulation band, the more each rub leaves ([Materials](Materials.md#polymer-insulation) lists the bands). The charge leaks away slowly on its own.

The generator is an LV source. It draws its charge off into the line it is wired to, up to 2 A, while the rod holds charge. It is a burst, not a supply: it charges a Leyden jar, or runs something small for the moment the rod lasts. See [Electricity](Electricity.md).
