# Robot Beacon

A landmark that corrects a [robot's](Robots.md) idea of where it is.

## Crafting

```
  W  
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a [Copper Plate](Mechanical_Hammer.md).

## How it works

A robot counts the blocks it rolls to know its position, and the error grows with every block. A robot within a few blocks of a beacon has that error wiped. A robot reads *Beacon near* while it is within reach. See [Position](Robots.md#position).
