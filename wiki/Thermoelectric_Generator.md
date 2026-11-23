# Thermoelectric Generator

A block of two metal legs that turns a temperature difference into a trickle of [electricity](Electricity.md): up to about **2 A in the LV band**, for as long as one side is hot and the other is cold.

## Building it

Shape the **Thermo Legs** at a part bench from two different metals; they are the part's two slots (see [Composed Parts](Composed_Parts.md)). The generator is a modular block, so the legs carry their design into it. The legs have two measures and a shape:

- **Section**: from 20 to 100 percent (50 by default). A wider section carries more current.
- **Stubby legs**: more current for the section, but they conduct the heat straight through and hold less of the temperature difference.
- **Slender legs**: less current for the section, but they hold more of the temperature difference.

## Using it

Place it touching [Copper Cable](Copper_Cable.md) or a machine. There is no screen and no off switch.

- **Hot side**: whatever heat source or hot vessel touches the block, such as a [Crucible](Crucible.md), a [Boiler](Boiler.md), a fire or a [Heating Coil](Heating_Coil.md).
- **Cold side**: a [Cryocooler](Cryocooler.md)'s cold head touching it, else water or ice touching it, else the air.

Its tooltip and Jade show the **Seebeck gap**, the **supply** in amps and the **max heat** the legs stand.

## How it works

The metals decide the voltage a degree of difference drives: the gap between their Seebeck figures. Copper against constantan is a good pair; the same metal twice gives nothing. The pair is the design.

Current rises with the square of that voltage: twice the temperature difference gives four times the current, up to what the section carries. Stubby legs lose some of the difference to conduction, so they need a bigger one to reach the same current; slender legs keep more of it but carry less.

## Misuse and hazards

Past the **max heat** of the weaker of the two metals the legs fail and the block gives nothing until it cools. See [Hazards](Hazards.md) for heat.

## Tips and automation

It is a weak source, in the same class as a [Galvanic Cell](Galvanic_Cell.md): several on one line pool and share the load, and a higher band on the line outbids it ([Electricity](Electricity.md)).
