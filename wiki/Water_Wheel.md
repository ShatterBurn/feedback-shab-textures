# Water Wheel

Free, continuous [rotation](Rotation.md) from flowing water. It's the first source that can actually run machines.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Water Wheel* in the type row); the diameter and bucket count are set there, along with the addons. It looks like a drum ringed with buckets. It needs a blueprint but no treatise. It has one part, the **Wheel Rim**, with three choices:

| Choice | What it sets |
| --- | --- |
| **Diameter** | From 100 to 400 cm, in steps of 20; the default is 200. A bigger wheel catches more water and makes more Su, but turns slower for the same flow. |
| **Buckets** | From 4 to 16; the default is 8. More buckets take more of the flow's push and make more Su. |
| **Wood** | The board or sheet the rim is made from. A heavier wood gives the rim more momentum. |

A 200 cm wheel with 8 buckets makes 256 Su and turns 4 RPM for each flowing side.

Shape the rim from a board (planks) at the [Woodworking Table](Woodworking_Table.md) or from a sheet, then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Water Wheel Blueprint in the blueprint slot judges the fit against your drawing. The Water Wheel comes out with the design of the rim, and the blueprint is kept. Its tooltip shows the Su it makes, the RPM per flowing side and its momentum. Breaking the block gives the same wheel back.

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the wheel or afterward on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). The wheel works without it.

- **Rim ring** (plate): adds momentum by the metal's density, so a denser metal adds more. The wheel is slower to spin up.

## Using it

It is a paddle wheel with spokes on a bronze axle and hub, and it turns about its axle. Its selection box and collision are thin, matching the wheel.

Place it with **flowing water** against it. Its axle runs along the face you place it on, and shafts connect at either end of the axle.

**Speed depends on how many sides have moving water** against them: the RPM per flowing side on the tooltip, times up to four sides. The two axle ends don't count. It needs to be **flowing** water: still source blocks don't push it.

Where you put it matters. A wheel set into a running stream with water on several faces is worth much more than one dipped in a trickle.

## How much it can drive

A water wheel is strong: the Su on its tooltip. A 200 cm, 8-bucket wheel can carry **three [Mechanical Hammers](Mechanical_Hammer.md)** (80 each), but not four. A bigger wheel with more buckets carries more; so do more wheels on the same line.

It's heavy, so a network with a water wheel on it is slow to spin up but steady once it's going, and it coasts a long way when disconnected. A bigger wheel of a heavier wood has more momentum.

## Notes

- Its speed wanders with the water, which is why a [Timer](Timer.md) run off a water wheel doesn't always give the same result twice.

## Creel

A wicker basket trap for fish, with a narrow funnel mouth, set under water.

```
S S S
S . S      S stick
S S S
```

Place it so it sits in a body of water; a puddle catches nothing. Every couple of minutes it draws a catch from the fish and junk the water gives a rod, never treasure. Bait in the top slot shortens the wait and is spent; rotten flesh, spider eyes, bone meal and raw meat and fish are bait. An empty hand empties the basket, and a hopper can take from it. A full basket stops.

## Fish Wheel

A Water Wheel with baskets on its blades. It turns in a current as any wheel does, gives half the power, and catches fish while it turns.

Craft it with a [Creel](#creel) directly above a Water Wheel, one column and two rows. The Fish Wheel is built on that Water Wheel: it has the same rim material, addons and tooltip figures, and keeps them when you break it and pick it up. Place and drive it like a Water Wheel; where the Water Wheel is a bucketed drum, it is a spar carrying two scoop baskets on a small frame. It draws a catch from the same fish the water gives a rod, never treasure, and more blades in the current catch faster. An empty hand empties the baskets, and a hopper can take from them. A full basket stops the catch, not the wheel.

## Attunements

**Ward** and **Mend** on the rim or rim ring handle the wear a wheel takes when held still in flowing water; a worn wheel turns less. **Impulse** on the rim pushes harder from the flow, **Momentum** carries more momentum, and **Bloom**, **Reap**, **Bane**, **Kin**, **Mark** and **Dowse** work on the land and creatures around it. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
