# Redstone Bridges

Two blocks join Feedback machines to vanilla redstone, and every machine with a switch answers to redstone on its own.

## Redstone on a switch

Any Feedback machine a [Controller](Controller.md) can start and stop also starts and stops from redstone. A redstone signal arriving at the machine switches it on; the signal dropping switches it off. Nothing needs to be fitted. A machine that a controller is also driving takes whichever change arrived last.

## Redstone Emitter

Turns a data signal into redstone. A controller, timer or sensor program switches it like any other machine; when it is on it puts a full redstone signal out of every face. Use it to drive a piston, lamp or door from a [sensor](Sensors.md).

```
P P P
W Y R      P wrought iron plate, W copper wire, Y relay, R redstone
```

Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)).

## Analog Adapter

A fitting for a machine with a variable setting. Right-click a face of the machine with it; redstone power arriving at that face then sets the setting from none to full in fifteen even steps: power 15 is full, power 0 is none. The [Valve](Valve.md) has one, its opening, which scales the flow it passes; an engine has its throttle, and a transmission its gear. Take the adapter off with a crowbar and the machine returns to full. A machine with no variable setting ignores it.

```
P R P
W C P      P wrought iron plate, R redstone, W copper wire, C circuit
```

Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and Electrical Treatise III: Circuits ([Treatise](Treatise.md)); it spends no fasteners. The circuit is any [circuit](Basic_Electronic_Circuit.md).

## See also

- [Controller](Controller.md) for starting and stopping machines from sensors.
- [Sensors](Sensors.md) for what a controller can read.
