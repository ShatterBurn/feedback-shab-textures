# Robot Dock

A pad on an [electrical](Electricity.md) line that charges the [robot](Robots.md) standing on it.

## Crafting

```
 W P W
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a [Copper Plate](Mechanical_Hammer.md).

## Using it

Join the dock to an electrical line and stand a robot on it. The robot's [Battery](Battery.md) is charged the way a [Charger](Charger.md) charges one: the dock asks for the battery's charge current while the line is at its band or above. A robot with no battery takes nothing.

## Tips and automation

A robot reads *Dock ahead* when a dock is the block in front, so a program can drive onto it to recharge. A [Tether Reel](Tether_Reel.md) charges a robot that stays away from the pad.
