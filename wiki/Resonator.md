# Resonator

A crystal that rings between two electrodes. Soldered to a [circuit](Basic_Electronic_Circuit.md) as an element, it steadies the clock and the readings of a host that holds the circuit, and gives an [endernet](Data_Port.md#endernet) link more channels and a [Data Port](Data_Port.md) more bandwidth.

## Crafting

A Resonator is two parts ([Composed Parts](Composed_Parts.md)), each shaped at a part bench:

- **Resonator Crystal**: from a crystal plate or a shard. Its material must ring: **quartz** rings best, then **amethyst**, then tourmaline. Two axes: **size** (4 to 20 mm) and **cut** (0 to 100%).
- **Resonator Electrode**: from foil or fine wire. Its material must conduct; a better conductor couples the crystal to its wire better.

Drawing and assembling a resonator take Electrical Treatise III: Circuits ([Treatise](Treatise.md)). Lay them side by side in the grid:

```
C E
```

C is a Resonator Crystal, E is a Resonator Electrode.

Then solder the Resonator to a [Trace Board](Trace_Board.md) as one of its parts, as in [Making one](Basic_Electronic_Circuit.md#making-one): board plus Resonator in a crafting grid, then paste and solder. It counts as one of the board's four parts and adds a little noise of its own.

## What it gives the circuit

A [Component Tester](Instruments.md#component-tester) held against a circuit reads three figures from it, and the circuit's tooltip says it carries a resonator.

| Figure | Set by | What it does for a host |
| --- | --- | --- |
| **Stability** | a bigger crystal, a stronger ring | A steadier program clock and less wobble in sensor readings. Like a quartz plate on the board, it steadies the rest of the circuit. |
| **Channels** | a narrower cut (higher cut) | How many channels an [Endernet Upgrade](Endernet_Upgrade.md#channel-and-address) on the host can be tuned to, and a [Data Port](Data_Port.md#channels) with the circuit, from 1 up to 16. The [Network app](Electric_Controller.md#disk-apps) shows the count, and a [robot's](Robots.md#the-brain) endernet leash offers more of its senses and actions with more channels. |
| **Bandwidth** | a stronger ring, a better conductor, a smaller crystal, a wider cut | More signals a minute through a [Data Port](Data_Port.md) that holds the circuit. |

A bigger crystal is steadier and carries less; a narrower cut separates more channels and carries less. The circuit's best figure of each kind counts when a board carries more than one.

## Where it works

- A [Controller](Controller.md#circuit-slot), [Electric Controller](Electric_Controller.md#circuit-slot) or [Computer](Electric_Controller.md#circuit-slot) steps more evenly and reads steadier.
- A [Data Port](Data_Port.md#circuit) takes the circuit for bandwidth.
- The host works without a Resonator; it is steadier and carries more with one.

See [Lattice](Lattice.md) for the circuit element that remembers.
