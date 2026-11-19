# Clutch

A shaft coupling you can disconnect. Everything past it stops being driven.

## Crafting

```
P K P
S S
```

P is a [Copper Plate](Mechanical_Hammer.md), K is a [Coil Spring](Lathe.md#what-it-makes) of any metal and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Placing

Put it inline on a shaft. The side you place it **against** is the **driving** side; the far side is the output.

## Using it

**Right-click** to disengage or re-engage it. A clutch is placed **engaged**.

You can see the state. Engaged, the copper pressure plate presses against the disc and both halves turn together. Disengaged, the output half slides away, leaving a gap, and turns only with whatever is downstream as it coasts, or stands still.

A [Timer](Timer.md) touching it, or a [Controller](Controller.md) with a *Switch* card, can switch it for you.

## What disengaging actually does

A clutch **doesn't brake**. It stops *driving* the far side. Whatever is downstream keeps turning on its own momentum and coasts down, quickly if it's loaded, slowly if it's light or has heavy things on it. A [hammer](Mechanical_Hammer.md) behind a clutch can land a blow or two after you throw it, so allow for that.

The disengaged clutch stays part of the driving side, so the power source keeps running normally.

## Fine clutch

A Clutch can be a voxel of [finespace](Finespace.md). A [Controller](Controller.md) card switches a fine clutch, engaged or not. Link the clutch voxel to the Controller with a [Data Connector](Data_Connector.md) and add a card on its **Engaged** channel.

## Uses

- Stopping one machine without stopping the whole line.
- Automating stops with a [Timer](Timer.md) or [Controller](Controller.md).
- An automatic version that trips on overspeed is the [Overspeed Trip](Overspeed_Trip.md).
- A coupling that lets the output run free whenever it outpaces the input is the [Freewheel](Freewheel.md).
