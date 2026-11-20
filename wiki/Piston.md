# Piston

A piston pushes a line of blocks when a signal tells it to and gas drives it. You build it from a cylinder, a rod, a seal and, if you want it to pull, a sticky face.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Piston* in the type row) and build it from its parts. It has these parts:

- **Cylinder**: the bore is the area the gas pushes on, and the wall is the pressure the cylinder stands before it bursts. A wide bore pushes harder and swallows more gas per stroke; a thick wall of strong metal stands higher pressure.
- **Rod**: its length is the reach, from 1 to 3 blocks. A longer rod takes more gas per stroke.
- **Seal**: a soft material (leather, skin or sheet) round the head. A soft seal keeps the gas in; a hard one lets some slip by each stroke.
- **Sticky face** (optional): a slime ball.

The figures bars on the table show the bore area, the reach, the danger, the slip and the stickiness.

## What vanilla does

A redstone signal on a piston or sticky piston pushes it out, and a sticky piston pulls its block back. The signal is all it needs.

## What Feedback does

Redstone only carries a **signal**; it is too weak to move anything. The piston needs the signal, or a [Controller](Controller.md) switch, to say when, and **gas** to do the work. Place a [gas tank](Gas_Tank.md), [pressure vessel](Pressure_Vessel.md), [compressor](Gas_Compressor.md) or [pipe](Pipe.md) against any face of the cylinder except the front. Each stroke out uses gas equal to the bore area swept over the reach, plus what the seal lets slip, and the tank's gauge falls as it does. With a signal but no gas the piston does not move.

Retracting costs no gas. A piston with a **slime face** pulls the block behind its head back with it.

### What it can push

A piston pushes a line of blocks in front of it. The line's total **weight** must be within what the bore and the gas holder's fill can move: a wider bore and a fuller holder move more. Heavier blocks such as stone count for more than dirt.

## Misuse and hazards

- A line too heavy for the piston blows the gas and wears the seal.
- Gas above what the cylinder's wall stands bursts it; see [Hazards](Hazards.md).

## Vanilla pistons

Vanilla pistons and sticky pistons have no recipe and do not appear in the creative tabs. Vanilla pistons found in the world turn into Feedback pistons with a one-block reach and an iron cylinder.
