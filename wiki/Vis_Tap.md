# Vis Tap

A coil that turns vis from a Thaumaturge vis relay network into current on an [electrical](Electricity.md) line. It needs the Thaumaturge mod; see [Mod Compats](Mod_Compats.md#thaumaturge).

## Crafting

```
  T  
W W W
P L P
```

T is Glass Tubing, W is Copper Fine Wire, P is a Brass Plate and L is [Copper Wire](Wire_Drawer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)).

## Using it

Place it within reach of a linked Thaumaturge vis relay and wire it into a line like any other source. It offers MV current, up to **2 A**, from a buffer of **400 Eu**. The line draws the buffer down; the tap refills it from the network.

Every second it draws **4 hundredths of vis** of ignis from the network, and each hundredth fills 5 Eu of the buffer. Without Thaumaturge it does nothing.

## How it works

The tap cannot refuse what the network pushes at it. While a load keeps the buffer from filling, all the vis goes into current. With no load the buffer fills, and the vis that cannot be held backs up as a slow trickle of flux into the chunk. A tap left running with nothing connected fouls the aura instead of switching off; a load that stays connected keeps it clean.

Flux in a chunk is read by the [Aura Sensor](Aura_Sensor.md) and cleared by a [Scrubbing Bed](Scrubbing_Bed.md). High flux also corrodes bare metal machines; see [Flux, scrubbing and rust](Mod_Compats.md#flux-scrubbing-and-rust).
