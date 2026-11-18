# Robot Beacon

A landmark that corrects a [robot's](Robots.md) idea of where it is.

## Crafting

```
  W  
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a [Copper Plate](Mechanical_Hammer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

## How it works

A robot counts the blocks it rolls to know its position, and the error grows with every block. A robot within a few blocks of a beacon has that error wiped. A robot reads *Beacon near* while it is within reach. See [Position](Robots.md#position).
