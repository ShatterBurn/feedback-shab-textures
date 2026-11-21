# Heating Coil

Turns [electricity](Electricity.md) into [heat](Heat.md). Set it against a [Crucible](Crucible.md), [Boiler](Boiler.md) or anything else you'd put on a [Firebox](Firebox.md), and it heats it.

## Crafting

A Heating Coil is a [modular](Tools_and_Weapons.md) block made from one part, a **Coil Element**. Cut the element at the [Blueprint Table](Blueprint_Table.md) or a Stonecutter from wire or fine wire of a metal ([Wire Drawer](Wire_Drawer.md)), then assemble it. Drawing and assembling it take Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)). The element's metal sets how the coil behaves, and two measures set its build:

- **Gauge**, 20 to 100 percent (50 by default): the thickness of the wire. A thicker wire draws more current, and carries more: the coil is rated for the lesser of the current its turns and gauge ask for and the current its metal can carry over that thickness (the **Carries** bar on the [Blueprint Table](Blueprint_Table.md)). A metal that carries little, such as aluminum, limits a thick coil.
- **Turns**, 1 to 4 (2 by default): more turns draw less current and weigh more.

A metal that resists current (a high electrical drop, like nichrome) turns more of the current into heat than one that does not. Copper hardly heats; iron heats well.

The coil takes the color of its element's metal. Recipes that ask for a Heating Coil take any of them.

## Using it

The coil can be rotated: it radiates away from the face you click when placing it, so a coil set on the floor heats the block above it. The block it faces gets its full heat, the blocks beside that face get less, and the block behind gets none ([Heat](Heat.md#heat-sources-have-a-facing)). Put the vessel in front and run [cable](Copper_Cable.md) to the coil. The wire's gauge and turns set the current it asks for (2 A at the defaults). It has no switch; put a [Breaker](Breaker.md) in its line to turn it on and off by hand or with a [Controller](Controller.md).

The coil is rated for LV. The power it turns into heat is the packet it receives times its amps, so it is as hot as its supply. At 2 A:

| Line | Default crucible settles near |
| --- | --- |
| LV (a [Galvanic Cell](Galvanic_Cell.md)) | 70 Tu |
| MV (a [Dynamotor](Dynamotor.md) with a winding of 2 turns at a [Steam Engine](Steam_Engine.md)'s speed) | 210 Tu |
| HV | 790 Tu |

The element runs a little hotter than what it's heating, and heats its vessel the same way a fire does: fast while there's a big difference, slower as they get close.

## Not like a fire

- **It stops almost instantly.** The element is light, so cut the power and it has almost nothing left to give. A fire's character is its fuel; a coil's is its supply.
- **It's steady and controllable.** No fuel to vary, no air to manage.
- **It isn't hot enough to smelt.** It's for low, precise jobs: tempering, preheating, raising steam. To smelt, use an [Induction Coil](Induction.md#induction-coil) or an [Arc Furnace](Arc_Furnace.md).

## The element's limit

The coil holds as hot as its wire metal's service temperature. Above that the element **wears**, faster the further over, down to half condition. A worn element puts out less heat. It never breaks. A metal with a higher service temperature gives a coil that can run hotter.

## Fed too high a band

A coil fed a higher band than it is rated for draws more current, since it is a resistance. Once that current passes what its wire carries, the element wears the same way, faster the further past. Fed as rated, it never wears this way.
