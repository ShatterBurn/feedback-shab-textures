# Lattice

A living mesh in an insulating frame. Soldered to a [circuit](Basic_Electronic_Circuit.md) as an element, it gives a host extra program variables, lets it learn repeated readings, and gives a [robot's brain](Robots.md#remembered-places) places to remember.

## Crafting

A Lattice is two parts ([Composed Parts](Composed_Parts.md)), each shaped at a part bench:

- **Lattice Body** (the mesh): from a vein, a shard or a block. Its material must hold experience: **sculk** and **echo shard** do. Two axes: **cells** (1 to 8) and **mesh** (0 to 100%).
- **Lattice Frame**: from a plate or foil. Its material must insulate; a better insulator keeps what the mesh holds when power is lost.

Lay them side by side in a crafting grid:

```
L F
```

L is a Lattice Body, F is a Lattice Frame.

Then solder the Lattice to a [Trace Board](Trace_Board.md) as one of its parts, as in [Making one](Basic_Electronic_Circuit.md#making-one). It counts as one of the board's four parts and adds a little noise of its own.

## What it gives the circuit

A [Component Tester](Instruments.md#component-tester) reads three figures; the circuit's tooltip says it carries a lattice.

| Figure | Set by | What it does for a host |
| --- | --- | --- |
| **Memory** | more cells, a finer mesh | Extra program variables, up to 8 in all across the board's lattices. |
| **Adaptation** | a coarser mesh, a conducting mesh | The host learns repeated sensor readings: a reading that stays close to the last one it held is held, so noise does not show, while a real change passes at once. |
| **Retention** | a mesh that leaks little, a good insulating frame | Whether the variables survive a long time without power. |

More cells and a finer mesh hold more variables and learn less; a coarser mesh learns more and holds fewer.

## Power loss

A host that has lost power for a long while sets the extra variables a lattice gives back to zero or false when power returns, unless its lattice **holds**. The circuit's tooltip says which: *Its lattice holds what it remembers with no power*, or *Its lattice needs a standing supply to remember*. A sculk mesh in a glass frame needs a standing supply; an echo shard mesh holds.

The variables a [Memory Drum](Controller.md#memory-and-upgrades) gives are kept either way.

## Where it works

- A [Controller](Controller.md#circuit-slot), [Electric Controller](Electric_Controller.md#circuit-slot) or [Computer](Electric_Controller.md#circuit-slot) gets the extra variables, which the [Programmer](Programming.md#variables) counts. A program that uses them does not run without the circuit, so the circuit cannot be taken out while it does.
- A [robot](Robots.md#remembered-places) or vehicle [autopilot](Vehicles.md#autopilot) keeps places and waypoints in them.

See [Resonator](Resonator.md) for the circuit element that rings.
