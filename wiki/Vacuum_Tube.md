# Vacuum Tube

A glass bulb with a heated cathode, a plate and any number of grids, pumped empty and sealed. Mounted on a [trace board](Trace_Board.md) it is a switching element that amplifies, and a [circuit](Basic_Electronic_Circuit.md) that carries one works at its best only once a heater has warmed it.

## Parts

A tube is four parts, each shaped from stock at a part bench ([Composed Parts](Composed_Parts.md)). A part takes only materials that can do its job: an envelope must insulate, stand heat and resist corrosion, a cathode must have a melting point and an electrode potential.

| Part | Made from | Set when shaping it |
| --- | --- | --- |
| Tube Envelope | tubing or ingot | **Bulb** diameter, 15 to 60 mm |
| Tube Cathode | wire or rod | **Emitter** length, 5 to 30 mm |
| Tube Grid | fine wire or wire | **Grids**, 1 to 3, and **pitch**, 20 to 100% |
| Tube Plate | plate or foil | **Area**, 20 to 100% |

The grid is optional: a tube without one is a diode. Drawing and assembling a tube take Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

## Putting one together

Lay the envelope, the cathode, the plate and, if you want one, the grid in a crafting grid. What you get is a Vacuum Tube **open to the air**: its tooltip says *Open to the air. Seal it in a vacuum dryer before it goes on a board.* An open tube mounts as nothing. Taking a tube apart and building it again opens it.

## Sealing

Use the open tube on a [Vacuum Dryer](Potion_Salts.md#sealing-a-vacuum-tube). It comes out sealed, its name says its character (*Vacuum Tube (Triode)*) and its tooltip says *Sealed under a hard vacuum*, *good vacuum* or *soft vacuum, gas left in*.

Three things set how good the vacuum is:

- **Depth**: the faster the pump's shaft turns, the deeper the chamber.
- **Bake**: the hotter the chamber, the better, up to a little under what the glass can stand.
- **The glass**: an inert, stiff glass is tight; a runny or reactive envelope leaks and caps the vacuum.

A shallow or cool seal leaves gas in. A chamber hotter than the envelope can stand slumps the glass and seals no vacuum at all. Neither destroys the tube: you get a gassy, poor tube.

## Character

The tube's character follows from what you built and how well it was sealed. It is never chosen.

| Character | What makes it |
| --- | --- |
| **Diode** | no grid; it passes current one way and amplifies nothing |
| **Triode** | one grid |
| **Tetrode** | two grids; noisier than a triode |
| **Pentode** | three grids; quieter and faster than a tetrode |
| **Thyratron** | a grid and a seal with gas left in; it adds a large gain in a flash but is slow and noisy |

## What the materials change

- **Cathode metal**: a cathode must be held hot to give off electrons. A metal that melts before it gets hot enough (copper) gives almost nothing; a refractory metal such as tungsten gives full emission. It also sets how hot the bulb runs and how much heater power the tube takes.
- **Emitter length**: a longer emitter gives more emission and gain, and takes more heater power.
- **Grid count and pitch**: more grids and a finer pitch give more gain; each grid costs a little speed. A grid metal that conducts poorly loses gain.
- **Plate area and metal**: a bigger plate handles more power and slows the tube. A plate metal that sheds heat poorly slows it further.
- **Envelope**: the glass sets how good a vacuum it holds and how hot it may be baked. A wider bulb warms more slowly.
- **Emission**: a poor emitter adds noise, and so does gas left in the seal.

## On a board

Mount a sealed tube in a crafting grid like any other part (see [Making one](Basic_Electronic_Circuit.md#making-one)). A board with a tube gives the circuit:

- **Gain**, which a [Component Tester](Instruments.md#component-tester) reads. It is the sum over the tubes; a circuit with none has no gain.
- **Speed** from the tube's geometry, and **noise** added to the circuit's.
- A **heater**: the tube takes standing power from whatever host holds the circuit. See [Warming up](Basic_Electronic_Circuit.md#warming-up).

## Misuse and hazards

A chamber hotter than the glass stands gives a gassy tube, never a destroyed one. A board carrying a tube lifts its traces under heat like any other circuit ([Overrun](Basic_Electronic_Circuit.md#overrun)); the heater stops short of that limit.
