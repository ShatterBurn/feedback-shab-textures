# Clutch

A shaft coupling you can disconnect. Everything past it stops being driven.

## Crafting

```
P K P
S S
```

P is a [Bronze Plate](Mechanical_Hammer.md), K is a [Coil Spring](Lathe.md#what-it-makes) of any metal and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

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
- Starting and stopping the feed of a [Drill Press](Drill_Press.md#linkage-feed) by putting the clutch upstream of its [Crank Linkage](Crank_Linkage.md).
- Automating stops with a [Timer](Timer.md) or [Controller](Controller.md).
- An automatic version that trips on overspeed is the [Overspeed Trip](#overspeed-trip).
- A coupling that lets the output run free whenever it outpaces the input is the [Freewheel](#freewheel).

## Freewheel

An overrunning clutch: one shaft in, one shaft out. The input drives the output only while it turns the faster; otherwise the output turns on its own.

At the Engineering Table, with a screwdriver in the tool row, 2 machine bolts and Mechanical Treatise II: Transmission ([Treatise](Treatise.md)):

```
C K C
S   S
```

C is a [Small Cog](Cogs.md), K is a Clutch and S is a [Shaft](Shaft.md).

Put it inline on a shaft. The face you place it **against** is the **input**; the far face is the output.

- While the input turns faster than the output, in the same sense, the freewheel drives the output.
- When the output turns faster (a flywheel coasting, a load that keeps spinning), the output is let go and runs at its own speed.
- The output never drives the input back.

Nothing is switched by hand: the faster side decides.

## Overspeed Trip

A Clutch with spinning weights inside that **throws itself open** if its shaft turns too fast. It is a safety cutoff for gear trains that might run away.

Two [Steel Ingots](Guide_Making_Steel.md) above a Clutch.

```
S S
 C
```

Place it inline on a shaft exactly like a clutch. The side you place it against is the driving side. Normally it just passes rotation through. If the driving shaft spins **faster than the trip is built for**, the weights fly out and it **disengages**, the same as if you had right-clicked a clutch. Everything downstream is cut off from the power and coasts down on its own momentum.

It **stays open** until someone resets it:

- **Right-click** it to close it again, or
- have a [Controller](Controller.md) flip it with a *Switch* card.

If the shaft is still spinning too fast when you reset it, it trips again immediately. That is your hint that whatever caused it is still going on.

It is built with a **light spring** that trips it at **48 RPM**. **Sneak and right-click** it to swap the spring: light (48 RPM), medium (120 RPM) or heavy (300 RPM), then back to light. A message shows the speed it now trips at. Three springs only; there is no finer setting.

A shaft turning at the speed of its own source (a [Hand Crank](Hand_Crank.md) at 32, a [Steam Engine](Steam_Engine.md) at 16, a [Water Wheel](Water_Wheel.md) at up to 16) never trips the light spring. A [Steam Turbine](Steam_Turbine.md) at 240 RPM trips the light and medium springs at once and needs the heavy one. It lets go when you have **geared up** past the fitted spring with [cogs](Cogs.md) or [pulleys](Pulleys.md).

It does not slow anything down or hold a speed. It is all-or-nothing, like a fuse for rotation. Tripping does it no harm at all; it never wears out, however often it fires.
