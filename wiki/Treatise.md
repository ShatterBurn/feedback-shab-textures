# Treatise

A treatise is a numbered book on one **school** of engineering. Holding a volume lets the [Blueprint Table](Blueprint_Table.md) draw blueprints of the types that volume covers and lets the [Engineering Table](Engineering_Table.md) assemble them. It is kept, never used up.

## Volumes

Each school has one item per volume. Every volume is a subject of its own: holding Weapons Treatise II does not give you Weapons Treatise I, and nothing in one school opens a type in another. A volume opens **types** (drawn at the [Blueprint Table](Blueprint_Table.md) and assembled at the [Engineering Table](Engineering_Table.md)) and **machines and parts** (crafted at the Engineering Table).

| Treatise | Types it opens | Machines and parts it opens |
| --- | --- | --- |
| Weapons Treatise I: Blades and Heads | maul, spear, cleaver | [Knuckleblaster](Knuckleblaster.md), [Whiplash](Whiplash.md) |
| Weapons Treatise II: Firearms | gun, launcher, crossbow, [fluid caster](Fluid_Caster.md) | [Magazines](Magazine.md), ammo belt, [Bullet Mold](Ammunition.md#bullets), [Reloading Press](Reloading_Press.md) |
| Weapons Treatise III: Electric Guns | electric gun | |
| Mechanical Treatise I: Engines, Flywheels and Dynamotors | engine, flywheel, [dynamotor](Dynamotor.md) | [Steam Engine](Steam_Engine.md), [Steam Turbine](Steam_Turbine.md), [Mechanical Hammer](Mechanical_Hammer.md), [Mechanical Press](Mechanical_Press.md), [Grapple Winch](Grapple.md) |
| Mechanical Treatise II: Transmission | transmission | [Gearbox](Cogs.md#gearbox), [Differential](Differential.md), Derrick Base |
| Aerospace Treatise I: Propellers | propeller | |
| Aerospace Treatise II: Thrusters | thruster | |
| Aerospace Treatise III: Jets | [jet](Jet.md) | |
| Robotics Treatise I: Suit Modules | suit module, and the servo, gauge, sensor head, battery bay and upgrade fittings of armor ([Exosuit](Exosuit.md)) | |
| Robotics Treatise II: Robots | [robot](Robots.md) | [Robot Arm](Robot_Arm.md), [Robot Beacon](Robot_Beacon.md), [Robot Dock](Robot_Dock.md), [Radio Mast](Radio_Mast.md), [Tether Reel](Tether_Reel.md), Tether Cable, [Sorting Station](Tube_Station.md#sorting-station) |
| Chemical Treatise I: Pressure Vessels | pressure vessel, boiler, gas tank | [Mixer](Mixer.md), [Vacuum Pump](Pneumatic_Tube.md#vacuum-pump) |
| Chemical Treatise II: Heat and Compression | heat exchanger, gas compressor | [Cryocooler](Cryocooler.md), [Refrigeration Unit](Refrigeration_Unit.md), [Vacuum Dryer](Potion_Salts.md#vacuum-dryer) |
| Chemical Treatise III: Explosives | demolition charge | |
| Electrical Treatise I: Cells and Static Charge | [battery](Battery.md), [galvanic cell](Galvanic_Cell.md), [Leyden jar](Leyden_Jar.md) | [Electrostatic Generator](Electrostatic_Generator.md) |
| Electrical Treatise II: Coils and Magnets | [heating coil](Heating_Coil.md), [linear motor](Pneumatic_Tube.md#linear-motor-ring), [electromagnet](Electromagnet.md) | [Relay](Relay.md), [Rectifier](Rectifier.md), [Redstone Coherer](Redstone_Coherer.md), Photocell, [Breaker](Breaker.md) |
| Electrical Treatise III: Circuits | [vacuum tube](Vacuum_Tube.md), [resonator](Resonator.md), [lattice](Lattice.md) | Circuit Board, Evacuated Tube, [Computer and Electric Controller](Electric_Controller.md), Memory Drum, [Micro Core](Micro_Core.md), [Control Rod Assembly](Control_Rod.md), [Feedbacker](Feedbacker.md) |
| Materials Treatise I: Machine-Shop Consumables | roll, wire die, lathe bit, drill bit, abrasive wheel ([Machine Heads](Machine_Heads.md)) | [Saw Head](Power_Cutter.md) |
| Materials Treatise II: Metallurgy and Heat Treatment | | [Annealing Furnace](Furnaces.md) |
| Nuclear Treatise I: Reactors and Fuel | [gas centrifuge](Gas_Centrifuge.md) | [Reactor](Reactor.md), [Reactor Core](Fission_Reactor.md), [fuel rods and Breeder Rod](Nuclear_Fuel.md) |

### Machines that need two volumes

A machine can name several volumes; it needs all of them held. Taking it apart teaches the first one named.

| Machine | Volumes |
| --- | --- |
| [Lathe](Lathe.md), [Drill Press](Drill_Press.md), [Rolling Mill](Rolling_Mill.md), [Wire Drawer](Wire_Drawer.md) | Mechanical I and Materials I |
| [Electrolyser](Electrolyser.md), [Electrolytic Cell](Electrolytic_Cell.md) | Chemical I and Electrical I |
| [Charging Gantry](Charging_Gantry.md) | Electrical II and Robotics I |
| [Arc Hearth](Arc_Furnace.md#the-arc-hearth) | Electrical II and Materials II |
| [Reflow Oven](Reflow_Oven.md) | Electrical III and Materials I |

### Study prerequisites

Some volumes can be researched at the [Research Table](Research_Table.md) only while volumes of other schools are held:

| Volume | Also needs |
| --- | --- |
| Aerospace II | Mechanical II |
| Robotics I | Mechanical I and Electrical II |
| Robotics II | Mechanical II and Electrical III |
| Nuclear I | Chemical II and Electrical II |

Types outside every volume (tools, armor, water wheel, thermal store, pump, fluid burner, lamp, belt and the rest) need no treatise.

## Obtaining

- **Write one.** Craft a **Blank Treatise**, put it in the blank slot on the Treatise tab of the Engineering Table, put an item of that volume's types, or a machine that names it first, in the specimen slot and press **Take apart**, which opens the learning game: calipers measure each part of the specimen (click to open the jaws, click again when they just touch the part; too tight crushes the reading, too loose is a guess) and the readings are inked onto a notebook page. How well you read it is the quality of the **notes** the treatise keeps (guessed, rough, fair, clean, exact); you always get the volume. The item is taken apart and the Blank Treatise becomes that volume. Most types that need a treatise have a [specimen](#specimens) you can craft to take apart first; a maul, spear, cleaver or charge is made the usual way.
- **Research one.** A Blank Treatise can be written at the [Research Table](Research_Table.md) with a specimen built from the volume before it (a first volume takes a plain thing) and the [study prerequisites](#study-prerequisites) held, by playing rows of concept cards for a target score on the notebook page; other volumes you hold boost the score (see [Treatise research](Research_Table.md#treatise-research)).
- **Find one.** A treatise of a random school and volume turns up, rarely, in dungeon, mineshaft, desert temple, stronghold and bastion chests.
- **Buy one.** Weaponsmiths, toolsmiths, cartographers, librarians, clerics and masons sell volume I of their school at a high trade level, and the later volumes at higher levels; clerics sell Chemical I and II, and weaponsmiths all three Weapons volumes. See [Villagers](Villagers.md#blueprints).

### Blank Treatise

```
Paper + Paper + Leather + Ink Sac (shapeless)
```

### Specimens

A specimen is one ready-made item of a type, built to a fixed cheap design, crafted in the crafting grid. It is not a good tool; it is something to take apart on the Engineering Table's [Treatise tab](Engineering_Table.md#treatise-tab) with a Blank Treatise. A specimen is made of the metal or wood named after it. Layouts are shown as in a grid; blank spaces are empty.

```
Gun (iron)                 Launcher (iron)           Crossbow (wood)
I I G                      I I S                     S I S
  P S                      P C I                     T P T
    S                          S                       S

I iron ingot, G gunpowder, P any plank, S stick, C piston, T string

Electric gun (copper)      Engine (iron)             Transmission (iron)
C C R                      I P I                     I I I
P R S                      I C I                     P C P
    S                      I B I                     I I I

C copper ingot, R redstone, P any plank, S stick
Engine: I iron ingot, P piston, C chain, B bucket
Transmission: I iron ingot, P piston, C comparator

Flywheel (iron)            Propeller (wood)          Thruster (iron)
I I I                      P   P                     I B I
I B I                        I                       I C I
I I I                      P   P                       N

Flywheel: I iron ingot, B iron block
Propeller: P any plank, I iron ingot
Thruster: I iron ingot, B bucket, C blaze powder, N iron bars

Suit module (copper)       Pressure vessel (iron)    Gas tank (iron)
C R C                      I I I                     I I I
I D I                      I B I                     I G I
C R C                      I G I                     I I I

Suit module: C copper ingot, R redstone, I iron ingot, D diamond
Pressure vessel: I iron ingot, B bucket, G iron nugget
Gas tank: I iron ingot, G glass pane

Boiler (iron)              Heat exchanger (copper)
I I I                      C C C
I C I                      I B I
I B I                      C C C

Boiler: I iron ingot, C cauldron, B bucket
Heat exchanger: C copper ingot, I iron bars, B copper block

Gas compressor (iron)
I P I
I H I
I I I

I iron ingot, P piston, H hopper

Linear motor (copper)      Heating coil (copper)     Electromagnet (copper)
C R C                      C R C                     C I C
I R I                      C   C                     I R I
C R C                      C R C                     C I C

Linear motor: C copper ingot, R redstone, I iron ingot
Heating coil: C copper ingot, R redstone
Electromagnet: C copper ingot, I iron ingot, R redstone

Dynamotor (copper)
C R C
I B I
C R C

C copper ingot, R redstone, I iron ingot, B Bearing

Battery (copper)           Galvanic cell (copper)    Leyden jar (copper)
I C I                      C I C                     G C G
C R C                      G G G                     G   G
I C I                                                G G G

Battery: C copper ingot, I iron ingot, R redstone
Galvanic cell: C copper ingot, I iron ingot, G glass
Leyden jar: C copper ingot, G glass

Vacuum tube (copper)       Resonator (copper)        Lattice (copper)
G G G                      Q R Q                     C I C
C I C                      I C                       I Q I
  G G                                                C I C

Vacuum tube: G glass, C copper ingot, I iron ingot
Resonator: Q quartz, R redstone, I iron ingot, C copper ingot
Lattice: C copper ingot, I iron ingot, Q quartz

Robot (iron)               Gas centrifuge (iron)
P R P                      P S P
I D I                      P R P
P R P                      P S P

Robot: P wrought iron plate, R redstone, I iron ingot, D diamond
Gas centrifuge: P wrought iron plate, S shaft, R redstone
```

## Using it

A treatise counts as held, at the Blueprint Table and at the Engineering Table, when it is:

- loose in your inventory,
- inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in your inventory, or
- inside a [Treatise Shelf](Treatise_Binder_and_Shelf.md) touching the table, or
- inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in the Engineering Table's binder slot.

Without a volume, the types it covers are left out of the Blueprint Table's type row and list altogether, so nothing of them can be picked or drawn, and the Engineering Table does not offer to assemble them: its status line names the volume it needs. A machine that needs several volumes is crafted only with all of them held; the status line names the first one missing. The treatise's tooltip lists every type it opens and names the [age](Ages.md) its specimen belongs to ("Its specimen is the work of the Iron Age"); reading is never gated by age. The same holding rule decides which part shapes the [Smithing Table](Smithing_Table.md) fan (and the Anvil, Woodworking Table, Stonecutter and Prep Table) lists, with a binder in your inventory or a shelf touching the table counting: parts of a gated type stay out of the fan until its volume is held. Treatises stack to sixteen. Copying a blueprint needs no treatise, and neither do repairing an item and taking it apart.
