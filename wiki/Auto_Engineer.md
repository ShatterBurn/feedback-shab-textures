# Auto Engineer

A machine that repeats one assembly you made by hand at the [Engineering Table](Engineering_Table.md). It reads the assembly from a [Recipe Data Stick](Data_Stick.md), takes parts from a bay, wears its own hand tools and spends [fasteners](Fasteners.md) from its stock.

## Crafting

```
S A S
E M C
S S S
```

S is a Steel Plate, A is a [Robot Arm](Robot_Arm.md), E is an Engineering Table and M is a [Dynamotor](Dynamotor.md) and C is a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which the finished machine keeps. Craft it at the Engineering Table with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 6 machine bolts ([Fasteners](Fasteners.md)). It needs Robotics Treatise II: Robots ([Treatise](Treatise.md)).

## Recording

Hold a blank Recipe Data Stick in your **off hand** while you take an Engineering Table craft from the output slot. The stick keeps what you made, the kinds of stock you used (a kind and a form, such as a metal plate, or the exact item for anything else), which hand tools the craft wore, how many fasteners it spent and the treatise volumes you held. Vanilla crafts, engineering recipes and modular assemblies are all recorded alike. A stick holds one recording or one [Power Cutter](Power_Cutter.md) cut, and another recording overwrites it.

## Using it

- **Right-click** with a stick to seat it. **Right-click** with a hand tool to put it in one of the four tool slots, with a fastener to fill the fastener stock, and with anything else to fill the parts bay.
- **Right-click** empty-handed to take the output, then the stick. **Sneak** and right-click empty-handed to take back the tools, fasteners and parts.
- A **hopper** can fill the bay and the fastener stock, and empty the output. The tool slots are filled by hand.

It is an [EHV](Electricity.md#bands-and-amps) sink, about **2.6 A** while parts are in the bay. An assembly takes about 3 seconds.

## How it assembles

When the bay holds stock of every kind the stick recorded, in at least the recorded amounts, the machine builds what the stick recorded. The stock may be a different material from the one you used, so the same recording turns out the same item in another metal. It lays the parts out as you did, wears each tool the recording called for one point and spends the fasteners from its stock. There is no hand-work game, and the item starts no better than the floor start: fasteners below the floor lower it. A [blueprint](Blueprint_Table.md) is not used.

It waits, spending nothing, when a tool is missing or broken, or the fastener stock is short.

### Stock the stick does not cover

Parts that do not match the recording are not refused. The machine picks one thing the whole bay could legally form, the same thing every time in a world, and consumes the bay to make it. If nothing could be formed, it scraps one stack to nuggets of its material, with half the material lost. Stock with no material or no nugget is lost. This never wears the tools and spends no fasteners.

## Circuit

The [circuit](Basic_Electronic_Circuit.md) the machine was crafted with is built in and cannot be taken out or swapped. Without one the machine does not run. A placed machine keeps its circuit, and so does the item it drops when broken. The [Engineering Table's Repair tab](Engineering_Table.md#repair-tab) takes the machine apart and returns the circuit.

- **Faster.** A fast circuit makes each assembly come sooner, up to about twice as fast. A slow one leaves an assembly at about the time given above.
- **Fewer improvised or scrapped results.** A quiet circuit makes the machine sometimes hold off from guessing at parts the stick does not cover: it waits instead of improvising or scrapping them.
- **Longer links.** A circuit with gain lengthens a [Data Connector](Data_Connector.md#linking-nodes) link that ends at the machine.
- **Heater.** A circuit with a [tube](Vacuum_Tube.md) takes its heater's current from the machine's line on top of the machine's own, and runs slow and noisy until warm ([Warming up](Basic_Electronic_Circuit.md#warming-up)).
- **Readable.** An Input card on a [Controller](Controller.md) can read **Progress** (how far through the assembly), **Condition** (the machine's) and **Output fill**. Link the machine to the Controller with the [Data Connector](Data_Connector.md) and click the card's channel row to pick one (see [Programming](Programming.md#input-and-switch-cards)).

## Wear

A correct assembly never wears the machine. It wears from stock too hard for it, from a full output slot, from scrapping and from a supply in a band above its rating. A worn machine works slower and never stops. [Calipers](Instruments.md#calipers) read its condition while it is stopped.
