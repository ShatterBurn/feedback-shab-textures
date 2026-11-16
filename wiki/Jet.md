# Jet

A modular air-breathing engine that pushes a [vehicle](Vehicles.md) along its facing. A compressor draws air in, fuel burns in it, and the nozzle throws the hot gas back.

## Building it

Draw the jet at the [Blueprint Table](Blueprint_Table.md) (pick *Jet* in the type row) and assemble it at the [Engineering Table](Engineering_Table.md). Drawing and assembling it take Aerospace Treatise III: Jets ([Treatise](Treatise.md)).

| Part | What it sets |
| --- | --- |
| **Compressor** (jet compressor) | Its **diameter**, 30 to 120 cm, is the intake: a wider intake swallows more air and burns more fuel. Three variants: 3, 6 or 9 stages. More stages push harder for the same fuel, but weigh more and spin the blade tips faster. Shaped from plate. |
| **Combustor liner** | The wall of the flame. Plate. Its service temperature is held against the fuel's flame. |
| **Turbine blades** | Sit in the hottest gas. Plate. Metals that stand more heat than iron (cobalt, tungsten, cermet) last longer here. |
| **Nozzle** (jet nozzle) | **Converging** gives the same push at any speed. **Divergent** wastes push at rest and gains it as the craft speeds up. |
| **Afterburner** (optional addon) | A plate reheat stage behind the turbine. Adds a second controller level. Its metal must stand the extra heat. |

## Using it

The jet is one full-size block. It pushes along its facing; place it against a face to push away from that face.

- **Fuel.** It takes fuel from a [Pipe](Pipe.md) or a bucket. Any burning fluid works ([Fuels](Fuels.md)); the hotter the flame, the more power.
- **Right-click** to switch between full and idle. The chat line shows the mode and the fuel left in mB.
- **Controller.** A [Controller](Controller.md) card sets an analog throttle (channel 0) and, with an afterburner, the afterburner level (channel 1).

With [Jade](https://modrinth.com/mod/jade) it reads **Off**, **Spooling**, **Running**, **Stalled**, **Flamed out**, **No air** or **Afterburner lit**, and a worn line when the liner, blades or compressor are worn.

## How it works

Thrust is the air the jet swallows times how much faster the exhaust leaves than the air arrives. Thin air costs thrust, and a craft cannot be pushed much past the speed of its exhaust, so thrust falls toward zero as it nears that speed. The afterburner burns much more fuel in the exhaust for extra push.

**Starting.** A jet lights only while a [shaft](Shaft.md) turns it at **20 RPM** or more (a [Dynamotor](Dynamotor.md) run as a motor, a [Hand Crank](Hand_Crank.md)), or while the craft moves fast through the air, which spins the compressor on its own. Once it has spooled to about **20%** it holds itself. Below that, with neither a shaft nor speed turning it, it flames out.

**Air.** A jet needs air: it gives no thrust in the End, in a vacuum or under water, and reads *No air*.

## Misuse and hazards

Misuse only wears the jet, down to **20% condition** with thrust falling with it; it never destroys it. See [Hazards](Hazards.md).

- A flame hotter than the **blades**, the **liner** or the **afterburner** metal stands.
- **Compressor tips** faster than their metal holds.
- **Stall.** Slamming the throttle up on a spooled engine stalls the compressor: no thrust for about 4 seconds, and a little wear. Raise the throttle in steps.

## See also

- [Gas Turbine](Gas_Turbine.md): the shaft-driven relative.
- [Thruster](Thruster.md): burns fuel without drawing air.
- [Propeller](Propeller.md): pulls from a turning shaft.
