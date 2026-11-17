# Power Cutter

A machine that cuts stock fed to it, two cuts a second, repeating a cut recorded on a [Recipe Data Stick](Data_Stick.md).

## Crafting

```
S S S
W C W
S   S
```

S is a Steel Plate, W is Copper Fine Wire and C is Ceramic.

### Heads

The cutter does nothing without a head. The head's material sets the hardness of stock it can take; its tooltip reads "Takes stock up to hardness (n), condition (n)%".

- **Saw Head** cuts cold stock: stone, wood, ceramic, glass, crystal and bone.

```
T T T
T S T
```

T is a Tungsten Carbide Plate and S is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row.

- **Plasma Head** cuts conducting metal plate only, since the arc needs something to conduct.

```
  T
T C T
```

T is a Tungsten Plate and C is Ceramic.

## Using it

- It is an **EHV** sink: about **2.6 A**. See [Electricity](Electricity.md).
- **Right-click** with a head or a data stick to seat it. **Right-click** empty-handed to take out the output, then the stick, then the circuit. **Sneak** and right-click to take the head.
- It has one head slot and one stick slot. A hopper feeds it and empties it: stock goes in, cut pieces come out.
- It idles with no head, no stock or no power.
- **Right-click empty-handed** on a jammed cutter clears the jam; the next empty-handed click takes the output.

## Circuit

A [circuit](Basic_Electronic_Circuit.md) is optional. **Right-click** with one to seat it (a circuit already seated comes back to you). **Right-click empty-handed** takes the output, then the stick, then the circuit.

- **Faster.** A fast circuit makes the cutter play its cuts faster, up to about twice as fast. With none the pace is as given above.
- **More accurate.** A quiet circuit lowers the odds of a jam, blade wear or scrap on stock the stick does not cover, and sometimes makes the cutter hold off from guessing at such stock at all. With none the odds are as described below.
- **Heater.** A circuit with a [tube](Vacuum_Tube.md) takes its heater's current from the cutter's line on top of the cutter's own, and runs slow and noisy until warm ([Warming up](Basic_Electronic_Circuit.md#warming-up)).
- **Readable.** With a live circuit seated, a Sensor card for a [Controller](Controller.md) can read the cutter's **Progress** (how far through the cut), **Condition** (the head's) and **Output fill**. Link the card as for any block ([Programming](Programming.md#linking-cards-to-real-blocks)) and click its channel row to pick one.

## How it cuts

Each piece of stock is cut in turn, two a second.

- **Stock the stick recorded** (same kind of material and same form) gets the recorded cut.
- **Any other stock** (not covered by the stick) rolls an outcome for each piece:
  - **the seeded cut**: a cut the head could make on it, chosen so that the same stock gives the same result every time in a world, and a different one from world to world;
  - **a jam**: the cutter stops until an empty hand on the block clears it, and Jade shows *Jammed*;
  - **blade wear**: the piece is spent and the head wears;
  - **scrap**: the piece turns into scrap of its material, if the material has a scrap form; otherwise it gets the seeded cut.
- **Stock the head cannot cut at all**, or harder than the head's material allows, is voided.

A stick that covers the stock keeps the cutter clear of all but the recorded cut, so record what you mean to cut.

## Head wear

A correct cut wears nothing. A head wears from misuse only:

- stock harder than the head,
- a full output (the cut waits and wears the head each time it retries),
- a supply over the EHV rating.

A worn head cuts slower and never stops. See [Hazards](Hazards.md).

## Attunements

A cutting head takes **Ward** and **Mend** against its misuse wear, **Edge** to cut faster and **Spread** to cut slower, and **Bane**, **Kin** or **Mark** on the creatures around the saw. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
