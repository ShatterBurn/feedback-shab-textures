# Charger

Electrical blocks that charge a [Battery](Battery.md) from an [electrical](Electricity.md) line: the **Charger** is a clip that charges one Battery item, and the **Charging Gantry** is a frame that charges the battery of a worn [Exosuit](Exosuit.md).

## Charger

A clip on an electrical line that charges one Battery item.

### Crafting

```
  W
F P F
```

W is [Copper Wire](Wire_Drawer.md), F is [Copper Foil](Mechanical_Hammer.md) and P is a [Copper Plate](Mechanical_Hammer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

### Using it

- **Right-click with a Battery** to seat it (one at a time, and only while the clip is empty). It charges as a placed battery does: it draws its own charge current while the line is in the battery's band or a higher one, and a line over its case's insulation band wears it. A lower band never charges it.
- **Right-click with an empty hand** to take out what is in it.

Connect it to a line.

## Charging Gantry

A frame on an [electrical](Electricity.md) line. Stand in it wearing an [Exosuit](Exosuit.md) and the suit's [Battery](Battery.md) charges.

### Crafting

```
P W P
P   P
P C P
```

W is [Copper Wire](Wire_Drawer.md), P is a Steel Plate and C is a [Charger](#charger). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md). It needs Electrical Treatise II: Coils and Magnets and Robotics Treatise I: Suit Modules ([Treatise](Treatise.md)).

### Using it

Connect it to a line like any electrical block. Stand in the frame; only its floor plate is solid. While the line is in the battery's band or a higher one, the battery in the worn suit's chestplate bay takes charge as a placed battery does. Nothing is drawn when you are not wearing a suit or the bay is empty.

### Misuse and hazards

A line over the battery's case band wears the battery, as it does in a placed one. See [Hazards](Hazards.md).
