# Controller

A mechanical logic box. It reads sensors, compares numbers, and switches things **on or off**, following a program punched into a **Punch Card**. It takes the Punch Card only; the [Electric Controller](Electric_Controller.md) takes the Memory Board and the Floppy Disk, and the [Computer](Electric_Controller.md#computer) takes the Floppy Disk and USB Drive ([program media](Programming.md#media)).

It's the step up from a [Timer](Timer.md) or [Bimetallic Strip](Bimetallic_Strip.md): it can watch more than one thing, compare against any number you like, and switch several things at once.

## Crafting

```
P W P
C E C
P S P
```

P is a [Copper Plate](Mechanical_Hammer.md), W is a Wrought Iron Rod, C is a [Small Cog](Cogs.md), E is an [Escapement](#crafting-the-parts) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise II: Transmission ([Treatise](Treatise.md)).

## Setting it up

1. Write a program onto a Punch Card at the [Programmer](Programming.md).
2. **Right-click the Controller with the printed card** to put it in. Right-click with an empty hand to take it out.
3. **Link what it reads and switches** to the Controller with the [Data Connector](Data_Connector.md): select a sensor or switchable block, then use the connector on the Controller. The Controller is a data node itself, so the link is made with wire like any other.
4. **Connect the Controller to a turning shaft.** It's mechanical, and it only thinks while it's being turned. It connects on any side and takes **2 Su**; even a [Hand Crank](Hand_Crank.md) (12 Su) is enough while you're turning it.

If the shaft stops, the controller stops making decisions, and everything it controls **stays as it was last set**.

## Circuit slot

A [circuit](Basic_Electronic_Circuit.md) is optional. **Right-click the Controller with a circuit** to seat it; an empty hand takes out the card first, then the circuit.

- **Pace.** A Controller with no circuit steps its program about **4 times a second**. A fast circuit raises that, up to **20 a second**; a slow, cold or dead one stays near the bottom.
- **Reading.** A circuit's noise makes the numbers an Input card reads wobble by a few percent; a quiet circuit reads steadily, and with none the reading is as noisy as a circuit can be. A yes/no reading never wobbles.
- **Steadiness.** A [Resonator](Resonator.md) on the circuit makes the clock step evenly and cuts the wobble of readings further.
- **Extra variables.** A [Lattice](Lattice.md) on the circuit adds program variables on top of the Memory Drums, up to 8 more. A program that uses them does not run without the circuit, so the circuit stays in while it does.
- **Learned readings.** A lattice also holds a reading that stays close to the last one, so noise does not show, and lets a real change through at once.
- **Forgetting.** After a long time with the shaft stopped, the extra variables go back to zero or false unless the lattice [holds](Lattice.md#power-loss). The variables of Memory Drums are kept.
- **Longer links.** A circuit with gain lengthens a [Data Connector](Data_Connector.md#linking-nodes) link that ends at the Controller.

The Controller has no power of its own to heat a [tubed](Vacuum_Tube.md) circuit; see [Warming up](Basic_Electronic_Circuit.md#warming-up).

## What it can read

An Input card reads any node linked to the Controller that offers the signal it asks for:

- [Temperature Sensor](Instruments.md#temperature-sensor) on a crucible or Improved Furnace
- [Current Sensor](Instruments.md#current-sensor) on a cable
- a [Lathe](Lathe.md) (how much it has cut off its workpiece)
- anything it can switch (below): **whether it's on**, as a yes/no. See [Holding a band](#holding-a-band).
- a [Driver's Seat](Vehicles.md#the-drivers-seat): how far a key is pressed
- another Controller's declared variables (see [Reading another Controller's variables](#reading-another-controllers-variables))
- a robot, through a [Radio Mast](Radio_Mast.md): its senses

## What it can switch

A *Switch* card drives every linked node of the kind it asks for. These turn on and off:

- [Clutch](Clutch.md) and [Overspeed Trip](Clutch.md#overspeed-trip)
- [Valve](Valve.md)
- [Damper](Damper.md)
- [Engine](Engine.md)
- [Thruster](Thruster.md)
- [Breaker](Breaker.md)
- a robot's actions, through a [Radio Mast](Radio_Mast.md)
- another Controller's variables, as 1/0 or true/false (see [Reading another Controller's variables](#reading-another-controllers-variables))

A **true** signal switches the target **on**: clutch engaged, valve open, damper open, engine or thruster running, breaker closed. **False** switches it **off**.

## Linked nodes

A Controller binds **8** linked nodes, and **4** more for each [Card Rack](#memory-and-upgrades) fitted. A card never names a block. An *Input* card asks for a signal, such as temperature, and may prefer a kind of block and a link color; the Controller answers it from what is linked to it. How cards choose among the linked nodes is on [Programming](Programming.md#the-cards). Nothing matching reads 0, and a *Switch* card with nothing matching does nothing.

## It controls energy, not machines

A controller never runs a machine. It can only connect to the things that let energy through (a clutch passes rotation, a valve passes air or steam, a breaker passes current) and open or close them. The hammer, the crucible and the bellows don't know it exists.

In a factory those are all **on/off switches**, so the controller's whole answer is on or off. There's no "half open". If your crucible swings back and forth around the target, that isn't a programming problem. Fix it physically: a heavier vessel, insulation, less air per stroke. See [Bimetallic Strip](Bimetallic_Strip.md#it-overshoots-and-thats-expected).

**Car parts** are the other thing it can connect to, and the rule is the same there. An engine's throttle is how much fuel and air the engine gets, so it's a flow of energy too. It just takes a number instead of on/off, which is why a *Math* card's number can set it. A number sent to a clutch or valve does nothing.

## Holding a band

A single comparison ("bellows on below 1380") flips the switch every time the reading crosses the line, which can be many times a second. That's hard on anything that wears when switched under load, like a [Breaker](Breaker.md).

The fix is a **band** (a deadband): turn on below the bottom, turn off above the top, and in between **leave it as it is**. The switch itself remembers, so read it back:

> **on** if (temperature < 1360) **OR** (temperature < 1400 **AND** the bellows clutch is already on)

An *Input* card asking for *On or off* of kind *Clutch* reads yes while the clutch is engaged. Inside the band the clutch keeps doing whatever it was doing.

## Reading another Controller's variables

Any Controller's declared [variables](Programming.md#variables) can be shared. Link the other Controller to this one with the [Data Connector](Data_Connector.md) and ask for kind *Controller* on a card:

- An **Input** card reads one of its variables. Click the card's channel row to pick which; a yes/no variable reads 1 or 0.
- A **Switch** card sets one of its variables, as 1 or 0 for a number and true or false for a yes/no. Click the channel row to pick which. The first choice sets nothing.

The other Controller sees the value on its next step, as with its own *Write*. A fleet of robots, or several Controllers, can share state this way.

## Memory and upgrades

A bare Controller runs a program of up to **16 cards** with **4 variables** (named numbers or yes/no values a program can set and read back), and offers Delay cards the lengths 1 s, 5 s, 30 s, 2 min and 10 min. Three parts, fitted by **right-clicking the Controller with them**, stretch that:

| Part | Gives | Up to |
| --- | --- | --- |
| **Memory Drum** | 4 more variables | 3 |
| **Card Rack** | 8 more cards and 4 more linked nodes | 3 |
| **Escapement** | Delay cards may use a typed or wired length | 1 |

A card whose program needs more than the fitted parts give is refused when you try to put it in. A [Crowbar](Crowbar.md) will not pry off a part the card in the Controller needs. Variable values survive a reload; Delay history does not, and a Delay restarts from whatever its input reads. A stopped shaft freezes variables and delays along with everything else.

When a new card goes in, variables it declares with the same name and kind keep their values; the rest are cleared.

### Crafting the parts

```
Memory Drum        Card Rack        Escapement
P M P              I A I            S
W S W              P A P            C
P P P              I A I            R
```

- Memory Drum: P is a Wrought Iron Plate, M Magnetite Dust, W [Copper Wire](Wire_Drawer.md) and S a [Shaft](Shaft.md). It is crafted at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)); it spends no fasteners.
- Card Rack: P is any planks, I a wrought iron rod, A paper.
- Escapement: S is a steel plate, C a small cog, R a wrought iron rod.
