# Gas Centrifuge

A shaft-driven machine that separates natural uranium into **Enriched Uranium** and **Depleted Uranium**. The enriched ingots make [Enriched Fuel Rods](Nuclear_Fuel.md#enriched-fuel-rod); the depleted ones make [Breeder Rods](Nuclear_Fuel.md#breeder-rod).

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Gas Centrifuge* in the type row); drawing and assembling it take Nuclear Treatise I: Reactors and Fuel ([Treatise](Treatise.md)). It has one part, the **Centrifuge Rotor**, shaped from plate of a metal (stainless steel by default); the centrifuge is built from it at the Stonecutter. The rotor has one measure, its **radius**, from 10 to 50 cm in steps of 5 (25 by default). The block and its item take the color of the rotor's metal.

## Using it

Put the centrifuge on a [shaft](Shaft.md) (the shaft crosses the block). Right-click with Uranium Ingots to load them (up to four batches), and with a bucket of hydrofluoric acid, or a pipe, to fill its tank. Right-click with an empty hand to take what it has made, or to read its feed, acid and progress.

## How it works

A batch takes **4 ingots and 50 mB of hydrofluoric acid** and makes **1 Enriched Uranium and 3 Depleted Uranium ingot**. Nothing happens below a minimum shaft speed, **24 RPM** for the default 25 cm rotor. Above it the batch advances with the square of the speed, so a shaft at twice the speed finishes four times as fast. It draws a steady load from the shaft. The machine keeps its feed, acid and finished ingots until you take them.

### The rotor

- **Radius.** A wider rotor starts separating at a lower shaft speed, but it weighs more on the shaft, so it draws a heavier load. A denser metal weighs more still.
- **Rim limit.** The rim holds only as fast as its metal's strength against its density allows, and less the wider the rotor is. A strong, light metal holds a faster rim.
- **Tearing apart.** Spin the shaft past the rim limit and the rotor tears itself apart: the block breaks. Match the rotor's radius and metal to the shaft's speed.
