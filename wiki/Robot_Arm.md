# Robot Arm

A [robot](Robots.md) fixed on a mount and run off an [electrical](Electricity.md) line instead of a battery. It works the space it faces.

## Crafting

```
 P W P
   P  
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a [Copper Plate](Mechanical_Hammer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

## Using it

It faces the way you looked when you placed it.

- **Right-click with a Punch Card** to load a program.
- **Sneak and right-click with a Punch Card** to take the loaded program back out into your inventory.
- **Right-click with a circuit** to seat it as the arm's brain. It gives the arm dexterity and sets its program size as on a [robot](Robots.md#the-brain); an arm with no circuit is clumsy and runs a short program. A circuit already seated comes back to you.
- **Right-click with any other item** to put it in its cargo.
- **Sneak and right-click with an empty hand** to take the last stack out.

Breaking the block drops its cargo and its circuit.

## How it works

It breaks, places, picks up and drops, uses the first carried stack on the block ahead ([Robots](Robots.md#using-the-held-item)), puts items into and takes them out of a container or machine, and switches a machine on or off, with the same channels and program as a robot. Turning swings it a quarter on its mount. It shares the readings *Blocked ahead*, *Breakable ahead*, *Items ahead*, *Cargo full* and *Dock ahead*. Its tool head has a fixed strength.

It acts only while its line feeds it.
