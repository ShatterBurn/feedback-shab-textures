# Wire Drawer

Pulls metal through a [Wire Die](Machine_Heads.md), again and again for as long as it's turning. Rods become wire, wire becomes fine wire, and plates become cartridge cases.

## Crafting

```
I B I
  S
```

I is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors and Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

The die is a [machine head](Machine_Heads.md): draw it at the Blueprint Table and put it together in a crafting grid.

## Setting it up

The drawer sits **directly on a shaft line**. Connect it from any side; you don't need a [Crank Linkage](Crank_Linkage.md). The faster the shaft turns, the faster it draws.

- **Right-click with a Wire Die** to seat it; the die it replaces comes back.
- **Right-click with an item** to put a workpiece in (or swap it for what's there).
- **Right-click with an empty hand** to take the workpiece out, then the die.
- **Sneak-right-click with an empty hand** to switch between **slow** and **fast** draw.

## What it makes

| From | Becomes | Then |
| --- | --- | --- |
| Rod | Wire | Fine Wire |
| Plate | Case | |

With no die the drawer does nothing. Like every machine, it doesn't stop when the wire is done. Leave copper wire in and it becomes **fine wire**. See [Metalworking](Metalworking.md).

Rods come from the [Rolling Mill](Rolling_Mill.md). Copper wire is used all over the mod: [cable](Copper_Cable.md), [electromagnets](Electromagnet.md), instruments. Cases are for [ammunition](Ammunition.md).

## Slow vs. fast

The die's ceiling is the most a draw can pull with. **Slow** takes about a third of it; **fast** takes all of it.

- **Slow** is gentle and completely safe, and it's all a soft metal like copper needs. A metal that needs more than a third of the die's ceiling, steel for one, **can't** be drawn on slow at all.
- **Fast** pulls with the die's full ceiling, enough for **steel** with a good die. But every draw that actually moves the metal has a **5%** chance of **snapping the wire**, and then the workpiece is **gone**. You'll hear it go.

Steel wire is only possible on fast, so you have to accept losing the odd piece. A steel rod takes about 7 draws to become wire, so about one steel rod in three is lost along the way.

The drawer is a **30 Su** load.

## Circuit

A [circuit](Basic_Electronic_Circuit.md) is optional; the drawer works without one. **Right-click the drawer with a circuit** to seat it. Taking it out with an empty hand gives the die first, then the circuit. A quiet circuit lowers the chance that a fast draw snaps the wire; a noisy one helps little. The drawer has no heater, so a tube in the circuit runs cold.

## Wear

A draw that can't move the metal goes into the **die** instead and wears it ([Machine Heads](Machine_Heads.md#wear)). That happens when the metal is too hard for the setting, when the piece is cold-worked to its limit and needs an anneal ([Work hardening](Metalworking.md#work-hardening)), or there's nothing left to draw (fine wire is as far as it goes). Correct use never wears the die.

## Steel Cable

Three Steel Wire (drawn on the fast setting) stacked vertically in a crafting grid make one **Steel Cable**.
