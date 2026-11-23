# Aura Sensor

A pickup coil in a block. It reads the aura of the chunk it stands in and gives a [Controller](Controller.md) that reading, so a program can react to vis or flux. It needs the Thaumaturge mod; see [Mod Compats](Mod_Compats.md#thaumaturge).

## Crafting

At the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)):

```
P T P
W W W
```

P is a Brass Plate, T is Glass Tubing and W is Copper Fine Wire. The wire is wound on the tube as a coil, and the aura bends the field through it.

## Using it

Link it to a Controller with the [Data Connector](Data_Connector.md) and read it with an Input card, as with the other [Sensors](Sensors.md). It reads the chunk it stands in, whichever face it is placed against.

- Channel 0 is the chunk's vis as a share of its base aura, full at the top and staying full above it.
- Channel 1 is how full of flux the chunk is, from empty to a chunk at its limit.
- Channels 2 to 15 switch on one by one as the flux climbs past each fourteenth of that limit.

It only measures. What a program does with the reading, such as running a [Scrubbing Bed](Scrubbing_Bed.md) before flux overruns the chunk, is up to the Controller.
