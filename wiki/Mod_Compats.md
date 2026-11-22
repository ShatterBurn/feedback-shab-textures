# Mod Compats

The mods Feedback works with, and what each one gives you. Some are required, some are optional, and Thaumaturge changes nothing without it.

| Mod | Status | What it does for you |
|---|---|---|
| [Sable](#sable-dimensional-sable-and-sable-scaling) | Required | Moving physics bodies for [Vehicles](Vehicles.md) |
| [Dimensional Sable](#sable-dimensional-sable-and-sable-scaling) | Required | Carries a craft between dimensions |
| [Sable Scaling](#sable-dimensional-sable-and-sable-scaling) | Required | Scales a craft |
| [Curios](#curios) | Required | Worn slots for gloves and combat gear |
| [Better Combat](#better-combat) | Required | Swing animations and attack arcs |
| [Flywheel](#flywheel) | Required | Rendering on the client |
| [JEI](#jei) | Optional | Recipe, process and info pages |
| [Jade](#jade) | Optional | Readouts on blocks and villagers |
| [Thaumaturge](#thaumaturge) | Optional | Aspects on Feedback items |

## Sable, Dimensional Sable and Sable Scaling

**Required.** Sable turns a build into one moving physics body, which is what a [Lift](Vehicles.md#the-lift) does to a vehicle. Dimensional Sable lets such a body cross between dimensions, as a craft does when it climbs to [The Gap](The_Gap.md) and returns. Sable Scaling resizes a body. All three must be installed.

## Curios

**Required.** [Leather Gloves](Hazards.md#gloves) sit in Curios' hands slot, and the three pieces of [Combat Gear](Combat_Gear.md) each take a slot that the mod adds to the Curios screen.

## Better Combat

**Required.** Weapon swings play Better Combat's attack animations and arcs, which the [Combat](Combat.md) page builds on. Better Combat needs Cloth Config installed alongside it.

## Flywheel

**Required** on the client. It draws moving machine parts and bodies. A dedicated server does not need it, and it adds no items or screens of its own.

## JEI

**Optional.** With Just Enough Items installed, the recipe browser covers Feedback's tables as well as its crafting recipes:

- Cards for thermal processes (melting, smelting, reactions, boiling, quenching, dips, electrolysis, freezing), deformation and removal, alloy mixes, fuels, ore veins and fluid deposits, villager goods, brewing ingredients, soldering, guns and tool and part benches. [Heat](Heat.md) describes the thermal cards.
- A **Material Stats** card for every material, and a **Separation** card per material that a separator can tell apart. See [Materials](Materials.md).
- Info pages on machines, stations and items that have no recipe card, saying what they do. They never give away a puzzle's answer.
- Crafting cards for recipes that take any material, cycling through every material that fits.
- Vanilla smelting, blasting and smoking cards, vanilla ores and the vanilla beds are hidden, because Feedback replaces them.

The [Engineering Table](Engineering_Table.md) screen and the [Computer](Electric_Controller.md) screen give JEI room: a Computer window in fullscreen hides JEI's lists.

## Jade

**Optional.** With Jade installed, looking at a block shows a readout of its state: rotation on [shafts and machines](Rotation.md), temperature on [heat machines](Heat.md), the band on [electric lines](Electricity.md), food equipment, [experience](Experience.md) stores, the [Fission Reactor](Fission_Reactor.md), a worn or workpiece-holding block, and a villager's standing with you (see [Villages](Villages.md)).

Readouts follow the instrument rule: a requirement printed on a part shows exactly, but a state such as a temperature, a speed or a band shows as a word until you carry an [instrument](Instruments.md) that reads it. Then it shows a figure.

## Thaumaturge

**Optional.** With the **Thaumaturge** mod installed (a Thaumcraft port with aspects and a Thaumometer for scanning), nearly every Feedback item carries Thaumaturge aspects, so scanning them works as it does for any other item. Amounts differ from item to item. Without Thaumaturge, nothing changes.

### Materials

Every form of a material (ingot, dust, block, plate, nugget and the rest) carries aspects worked out from what the material is and how it behaves:

| Property of the material | Aspect |
|---|---|
| Metal | Metallum (metal) |
| Mineral | Terra (earth) |
| Glass and gems | Vitreus (crystal) |
| Fluids | Aqua (water) or Aer (air) |
| Plants | Herba (plant) |
| Animals | Bestia (beast) and Victus (life) |
| Light animal fiber, such as [feather down](Materials.md#feather-down-and-rabbit-hide) | Volatus (flight) |
| Electrical conductivity | Potentia (energy) |
| High reflectivity | Lux (light) |
| Flammable or burning | Ignis (fire) |
| Toxicity | Mortuus (death) |
| Hardness and alloys | Ordo (order) |
| Magnetism | Motus (motion) |
| Powders | Perditio (entropy) |
| Plastics, rubbers and composites | Fabrico (craft) |
| Insulating plastics and rubbers | Praemunio (ward) |
| Elastic rubber, such as tire rubber | Motus (motion) |
| Bonded composites | Ordo (order) |

Amounts follow the size of the form: a nugget carries less than an ingot, a block more. They also follow the material's figures, so a denser metal carries more Metallum and a better conductor more Potentia. See [Materials](Materials.md) for the properties themselves.

### Foods

Every [food](Food.md) carries Victus (life) and Herba (plant), plus an aspect for what it does to the eater:

| Effect on the eater | Aspect |
|---|---|
| Warming and hearth foods | Ignis (fire) |
| Cooling foods | Gelum (ice) |
| Night sight | Lux (light) and Sensus (senses) |
| Sobering | Cognitio (mind) |
| Quickening | Motus (motion) |
| Antitoxin | Alkimia (alchemy) |
| Mending | Victus (life) |

### Machines and hardware

Machines, devices and carried hardware carry aspects that match their function:

- Mechanisms, [belts](Belt_Modules.md), moving parts and [vehicle](Vehicles.md) hardware such as engines: Machina (mechanism) and Motus.
- Tools and workshop machines: Instrumentum (tool) and Fabrico (craft). Tool heads and hand tools carry the same pair.
- [Instruments](Instruments.md): Sensus (senses) and Instrumentum.
- [Guns](Guns.md): Aversio (conflict).
- [Armor](Armor.md) and [Combat Gear](Combat_Gear.md): Praemunio (ward).
- Heat machines: Ignis. Cold machines: Gelum (ice). See [Heat](Heat.md).
- Electrical parts: Potentia, and with electronics Cognitio (mind). See [Electricity](Electricity.md).
- Flight hardware: Volatus (flight). This covers [thrusters](Thruster.md) with their nozzle and the suit thruster module of the [Exosuit](Exosuit.md), [jets](Jet.md) with their nozzle and compressor, [propellers](Propeller.md), the Levitation Plate and the Levitation Sleeve (see [Levitation](Levitation.md)). Light things that lift, such as feather down and the vanilla Phantom Membrane, carry it too. The gas cells of an [Airship](Airship.md) carry Aer (air) and Volatus.
- Sensors: Sensus (senses).
- Controllers and computers: Cognitio (mind). See [Controller](Controller.md).
- Treatises and other books: Cognitio.
- Experience, ender and sculk gear: Praecantatio (magic). See [Experience](Experience.md) and [Sculk](Sculk.md).
- Reactor parts: their own aspects, set by what they do. See [Fission Reactor](Fission_Reactor.md).
- [Robot](Robots.md) parts (chassis, drive, head, loader, radio, sensor, tether): Machina, Cognitio and Motus.

Part items that carry their material on the stack have no aspects of their own, and Thaumaturge works theirs out from their recipes. Creative and debug items have none.
