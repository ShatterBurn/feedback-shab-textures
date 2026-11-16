# Machine Heads

The part of a shop machine that does the work and wears: a **Roll** for the [Rolling Mill](Rolling_Mill.md), a **Wire Die** for the [Wire Drawer](Wire_Drawer.md), a **Lathe Bit** for the [Lathe](Lathe.md), a **Drill Bit** for the [Drill Press](Drill_Press.md) and an **Abrasive Wheel** for the [Grinding Wheel](Grinding_Wheel.md). The hammer that heads the [Mechanical Hammer](Mechanical_Hammer.md) and [Mechanical Press](Mechanical_Press.md) is a [Hammer](Hammer.md#in-machines).

## Making one

A head is a modular item. Draw it at the [Blueprint Table](Blueprint_Table.md) while you hold Materials Treatise I ([Treatise](Treatise.md)), shape its part at a part bench ([Smithing Table](Smithing_Table.md)), and put the parts together in a crafting grid. Pick the material, the axis and the variant at the Blueprint Table; the bars there show the **ceiling** (*Driven up to*) and, for a bit, wheel or die, the **edge hardness**.

Each type has a first specimen recipe in iron, crafted in the crafting grid:

```
Roll              Wire Die        Lathe Bit     Drill Bit     Abrasive Wheel
I I I             I B I           I             N             G I G
S I S             B   B           N             N             I B I
I I I                             N             I             G I G
```

I is an Iron Ingot, S is a [Shaft](Shaft.md), B is Iron Bars (in the wire die) or a [Bearing](Bearing.md) (in the abrasive wheel), N is an Iron Nugget and G is Gravel.

Each head has an **edge** slot as well as a body: the edge material is what the cut is made with, so a head with a harder edge cuts harder stock.

| Head | Axis | Variants |
| --- | --- | --- |
| Roll | roll diameter | broad roll, narrow roll |
| Wire Die | die bore | short bearing, long bearing |
| Lathe Bit | none | negative rake, positive rake |
| Drill Bit | drill flute | twist flute, spade flute |
| Abrasive Wheel | wheel grit | straight wheel, cup wheel |

A wider roll carries more; a wider die bore leaves a thinner wall that carries less. A finer grit cuts gently and sheds slowly. The Blueprint Table shows what each variant changes.

## Using one

- **Right-click** the machine holding the head to seat it. The head it replaces comes back to you.
- **Right-click with an empty hand** to take things out: the workpiece first, then the head. The Rolling Mill gives up its skin before the workpiece. The Drill Press pushes the bit instead while a workpiece is in; take that out by right-clicking with an item.
- With no head seated the machine does nothing.
- Breaking the machine drops the head.

The head's condition stays on the head, so a worn one moved to another machine is still worn.

## What the head sets

- The **ceiling**: how hard the machine may drive the head. Its material, mass and shape set it. A heavier or stronger head lifts it, a light or soft one lowers it. The machine's own setting (slow or fast, light or heavy) can only work up to it.
- The **edge hardness**, for a lathe bit, drill bit or abrasive wheel: what it can cut. Stock harder than the edge is not cut. JEI shows each metal's hardness under *Tool must beat*.

## Wear

A stroke, pass or push that lands nothing (metal too hard, too cold or already finished) wears the **head**, not the machine. A worn head carries less; wear stops at half condition and the head is never broken by it. Replace a spent head to start fresh.

The **Abrasive Wheel** is different: its grit is used up by **every cut**, not only by a misuse. A fully worn wheel has no grit left and cuts nothing.
