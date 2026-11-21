# Coupling Sleeve

A sleeve that gears the piece a [Rotary Bearing](Rotary_Bearing.md) holds to its shaft, so the piece and the line pass torque to each other in both directions instead of the piece acting as a flywheel.

## Crafting

One plate and one rod of the same metal, anywhere in the grid, make one Coupling Sleeve in that metal. Any material with a yield strength works; see [Materials](Materials.md#the-metals).

## Using it

Use it on a full-size Rotary Bearing to fit it. A bearing holds one sleeve; a bearing in finespace cannot take it. A [Crowbar](Crowbar.md) pries it off, and breaking the bearing drops it. Jade shows *Coupled, idle*, *Shaft turning the piece* or *Piece driving the shaft*.

## How it works

The sleeve works on a bearing in role 0, the one that turns with the shaft. In the other roles it sits idle.

Without a sleeve, the held piece counts as a flywheel on the line. With one fitted, the piece leaves the line's inertia and is geared to the shaft:

- The line's sources and loads put torque on the piece, and the shaft turns at the piece's real spin.
- A piece spinning faster than the line speeds the line up, so a spinning body can drive a line, and a loaded line slows the body. The piece's own weight is the flywheel.

The torque the sleeve carries depends on the metal's yield strength. Steel and stronger metals carry the bearing's whole torque limit; weaker metals carry a share of it, iron about 62 percent. The bearing wears only the way a stalled drive does; see [Rotary Bearing](Rotary_Bearing.md#roles).
