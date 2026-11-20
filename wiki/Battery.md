# Battery

A rechargeable battery, lead-acid, iron-nickel (Edison) or lithium-ion: cells in series, rated a band, that store charge in Eu. It charges from a line in its band or a higher one and gives its own band back when the line is lower, at full current until almost empty.

## Crafting

Shape the parts at a bench and put them together as for any [modular tool](Tools_and_Weapons.md); drawing and assembling a battery take Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)). It has two parts:

- **Battery Plates**, shaped from plates of a metal that states an electrode potential or cell figures: lead, iron as anode with nickel as cathode (the Edison cell), or for a lithium-ion cell [graphite](Materials.md#graphite) as the anode with [cobalt](Materials.md#the-metals), [manganese](Materials.md#the-metals) or [vanadium](Materials.md#the-metals) as the cathode, or any other pair of such metals. The plates have an **anode** and a **cathode** slot, so the two electrodes can be different metals; the cathode picker lists only metals that qualify, and clicking a figure bar such as capacity sorts it, as in the anode picker, to find good pairs. A pair where both metals state a charge figure (lead, iron, nickel) holds the mean of the two per ingot. Any other pair holds charge per ingot in proportion to the gap between the two metals' potentials, whichever is anode; a pair with no gap, such as the same metal twice, makes no battery. Two steppers set the size: **cell size** (1 to 4), how much each cell holds, and **cell count** (1 to 12), how many cells sit in series.

- **A case**: a pane, shard or sheet of an insulating material such as glass, ceramic or a polymer. The highest band the case holds off is the band the battery is **rated**: it charges from a line of that band or higher, and a line above it wears the battery.

The plate metal sets how much charge a plate holds, how it fares in the cold, and how much of the charge comes back. Capacity is in Eu: the cells in series add up what the battery holds, but not the current it can give, which is one cell's. The blueprint sheet shows the rated band and the capacity.

| Plate metal | Charge per plate | Charge returned | Cold | Gassing |
| --- | --- | --- | --- | --- |
| Lead | standard | about 85% | half the capacity at 0 Tu | yes |
| Iron anode, nickel cathode (Edison) | half | about 65% | most of it kept at 0 Tu | none |
| Lithium-ion (graphite anode, cobalt, manganese or vanadium cathode) | set by the pair of metals | about 95% | most of it kept at 0 Tu | none |

A metal with no stated cell chemistry acts as a plain plate: it gasses like lead, has no cold cliff, cannot run away and is not sealed.

The charge per plate of a lithium-ion cell follows the pairing: the further apart the two metals sit in the reactivity order, the more an ingot holds. The cathode metal also sets how hot the case may get before the cell runs away (see below): cobalt 120 Tu, manganese 180 Tu, vanadium 320 Tu.

## Setting up

Place the battery and fill it from a bucket or a [pipe](Pipe.md):

- **Electrolyte**: 250 mB per cell of any fluid that carries ions (see [The electrolyte](#the-electrolyte)). Without it the battery is dry and does nothing.
- **Water**: a 1000 mB tank, topped up the same way, for every cell that takes a water solution. A sealed lithium-ion battery has no water tank and takes no water. See [Water](Water.md).

A lithium-ion battery is sealed. Keep rain off its top and water out of the blocks beside it.

Wire it to a line like any other electrical block; see [Electricity](Electricity.md). Breaking it keeps its charge, water, electrolyte and condition in the item.

## The electrolyte

The tank takes any fluid whose material carries ions: sulfuric, hydrochloric and hydrofluoric acid, lye, alkaline water, brine, sodium aluminate, sodium tungstate, hypochlorite, the metal sulfate and chloride baths and thorium hydroxide, which are water solutions, and [Battery Electrolyte](Chemicals.md#battery-electrolyte) and cryolite bath, which are not water. A sealed cell (lithium-ion, or any with a graphite anode) takes only a non-water electrolyte; every other cell takes only a water solution. The first fill fixes the fluid until the tank is drained.

The electrolyte eats the plates. A filled battery loses condition at a rate set by the less resistant plate metal and by how far the fluid's **pH** is from 7 (see [Materials](Materials.md#heat-and-other-figures)). A base bites a quarter as hard as an acid. Plates that resist, such as lead, nickel, cobalt and graphite (corrosion resistance 0.9 or more), take no wear. Iron plates wear in acid far faster than in lye, so lye suits an iron-nickel cell.

## How it works

The battery switches between three states on its own, shown in Jade:

- **Charging**: the line is in a band above the battery's, or the battery is empty on a line in its own band. It draws current and fills, from empty to full in about 2.5 minutes at its limit, or about 1 minute beside a powered [Rectifier](Rectifier.md).
- **Supplying**: the battery holds charge and the line is in its own band or a lower one. It is a source of its own band, and a full battery can run flat in about 5 minutes at its limit.
- **Holding**: nothing flows, for example a full battery on a line above its band, or a battery on no line.

A line in a lower band never charges it.

### As a reserve

A battery on a line with a generator of the same band, such as a [Dynamotor](Dynamotor.md), is a **reserve**. The generator carries the load first. The battery supplies only the shortfall when the load is more than the generator can give, and charges from any surplus when it is less. With no generator of its band on the line, the battery supplies as any source does. Jade shows *Supplying* for a charged battery in either case. The current it gives stays full while charge lasts and falls away below 10% charge. The share of the energy put in that comes back is in the table above; the rest heats the case. A battery loses about 1% of its charge a day when idle.

### Reading the charge

Jade shows the charge in words: **Low**, **Part charged**, **Nearly full**, **Full**. A [Hydrometer](Instruments.md#hydrometer) reads it to 5%.

## Misuse and hazards

See [Hazards](Hazards.md) for the general rules.

- **Overcharge.** On a lead battery, a line well above its full strength, or charging past full, boils water off as hydrogen. Iron-nickel cells do not gas. Jade shows *Weeping gas*. A neighboring [Gas Tank](Gas_Tank.md) takes the hydrogen; otherwise a flame beside more than 40 mB of it blasts.
- **Electrolyte wear.** A fluid that eats the plates costs condition for as long as the battery holds it; see [The electrolyte](#the-electrolyte).
- **Low water.** Jade shows *Low water*. Exposed plates cut capacity; top the water up.
- **Wear.** **Ward** on the plates or case slows all of the wear below ([Enchanting Table](Enchanting_Table.md#on-a-machine)).
- **Overdraw.** Drawing more than the limit heats the case and costs condition.
- **Heat.** A case over 50 Tu costs condition. See [Heat](Heat.md).
- **Water on a lithium-ion cell.** Rain falling on an open top, or water beside the case, makes hydrogen and costs condition. If the cell holds charge, it starts a runaway.
- **Too high a band.** A line above the case's insulation band costs condition.
- **Cold.** Capacity falls as the body cools below 20 Tu, to half at 0 Tu for lead and much less for iron-nickel, as in an [Ice Box](Ice_Box.md).

### Thermal runaway

A lithium-ion cell that is misused can run away. Jade shows *Running away*. Three things set it off:

- The case reaches the cathode's limit (cobalt 120 Tu, manganese 180 Tu, vanadium 320 Tu).
- Overcharge, or a line over the case's band: the excess turns to heat.
- A heavy draw from a cell that was charged while below 0 Tu.

A cell running away burns its charge as heat, faster the hotter it gets, so a full cell burns longest. It lights the air beside it and passes heat to neighboring batteries, so a stack of cells cascades. Sand beside it eases it. Water makes hydrogen and makes it worse. It ends when the charge is gone, and costs condition meanwhile. Keep cells cool, on a line in their band, and spaced apart or walled with sand.

A charged battery with a case rated MV or more shocks whoever touches it; see [Hazards](Hazards.md#shock).

An [Arc Welder](Arc_Welder.md) draws its charge from a Battery in your inventory. A [Charger](Charger.md) takes one Battery item, and the chestplate of an [Exosuit](Exosuit.md) holds the Battery that runs the suit; a [Charging Gantry](Charging_Gantry.md) charges it while worn.

## Attunements

**Mend** on the plates or case closes the wear. **Charge** on the plates or case holds more charge, and a seated **Piezo** head or knocking engine beside the battery tops it up a little per stroke. The case also takes **Ground**, **Spring**, **Reservoir** and **Grudge**. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
