# Valve

An on/off cutoff that you put in the middle of a line. It works on **air** (from a [Powered Bellows](Powered_Bellows.md)) and on **fluids**, including **steam** from a [Boiler](Boiler.md).

## Crafting

```
P R P
P C P
```

P is a Wrought Iron Plate, R is a Wrought Iron Rod and C is a [Copper Plate](Mechanical_Hammer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Placing

A valve has a direction. **Whatever is behind it feeds in, and whatever is in front of it gets fed.** When you place it, the front points away from the block you clicked. The four other sides don't connect to anything.

Typical uses:

- **Bellows → Valve → Firebox**: cut the air to a fire without breaking anything.
- **Boiler → Valve → [Steam Engine](Steam_Engine.md)**: shut off steam so the engine stops pulling it.

## Using it

**Right-click** to open or close it. You'll hear a trapdoor-like clunk. A valve is placed **open**.

- **Open**: everything passes straight through.
- **Closed**: air blown into it is simply lost, and anything trying to draw fluid through it gets nothing.

The valve doesn't hold anything itself, so there's nothing to drain and nothing to lose when you break it.

## Automating it

A [Controller](Controller.md) can open and close a valve. Link it to the Controller with the [Data Connector](Data_Connector.md) and ask for it with a *Switch* card of kind *Valve or damper* in your [Programmer](Programming.md) program, the same way you would a [Clutch](Clutch.md). Pair that with a [Temperature Sensor](Instruments.md#temperature-sensor) and you have a thermostat that cuts the air to a fire when it gets too hot.

Keep in mind that closing a valve doesn't put out a fire or empty a boiler. It only stops *more* from arriving, and what's already there carries on until it runs down.
