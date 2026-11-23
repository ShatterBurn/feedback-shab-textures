# Thermocouple Probe

A [sensor](Sensors.md) that reads the temperature of the block it is placed against and needs no power. A [Controller](Controller.md) card reads it like any other sensor.

## Building it

Shape the **Thermo Wires** at a part bench from two different metals; they are the part's two slots (see [Composed Parts](Composed_Parts.md)). The probe is a modular block, so the wires carry their design into it. It is assembled with the same electrical knowledge as the [Thermoelectric Generator](Thermoelectric_Generator.md).

## Using it

Place it with its tip against the block you want to watch; it looks at the face it was placed against. Link it to a Controller with the [Data Connector](Data_Connector.md) and read it with an Input card. Right-click it with an empty hand to see the banded reading as a percentage.

It reads a hot vessel's own temperature, or else the heat reaching that block from a fire or other source nearby.

## How it works

The two metals decide the scale:

- **Top of the scale**: the service temperature of the weaker wire. A block hotter than that reads full. A low-melting wire such as copper tops out before a hot furnace is read.
- **Bands**: the gap between the two metals' Seebeck figures sets how many steps the reading has. A wider gap reads finer. Copper against constantan reads in about eight bands; the range is 2 to 16.

## Misuse and hazards

The same metal twice has no gap and gives only the coarsest reading. See [Hazards](Hazards.md) for heat.
