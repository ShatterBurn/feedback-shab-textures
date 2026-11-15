# Robots

A small battery-powered crawler you build from three parts. It rolls, digs, places blocks and moves items by itself, following a program you load from a Punch Card.

## Crafting

A **Robot** is made from three parts ([Composed Parts](Composed_Parts.md)), each shaped from plate stock at a part bench. Lay them in a column in a crafting grid, chassis on top:

```
 C 
 D 
 T 
```

C is a Robot Chassis, D is a Robot Drive, T is a Robot Tool Head.

- **Robot Chassis**: the frame. It carries the drive, the battery and the program, and sets how much the robot weighs.
- **Robot Drive**: what moves it. A lighter robot on the same drive moves faster. It comes in three kinds, see [Drives](#drives).
- **Robot Tool Head**: the working end. Its material decides what the robot can dig and how fast.

Two optional parts go below the tool head in the column, in either order after it: a **Robot Sensor** and a **Robot Chunk Loader** (see [Sensors and the chunk loader](#sensors-and-the-chunk-loader)). A robot may carry one of each.

```
 C 
 D 
 T 
 S 
 L 
```

S is a Robot Sensor, L is a Robot Chunk Loader.

## Using it

- **Right-click a block** with the Robot item to set it down as a robot standing there.
- **Sneak and right-click with an empty hand** to pick it up. It keeps its battery, program and cargo.
- **Right-click with a [Battery](Battery.md) item** to fit one. Without a battery the robot has no power and does nothing.
- **Right-click with a Punch Card** to load a program. It is an ordinary [Controller](Controller.md) program written at the [Programmer](Programming.md).
- **Right-click with an empty hand** to read its charge and whether a program is loaded.

The robot holds nine slots of cargo.

Use a [Tether Cable](Tether_Reel.md) on a robot to tie it to a reel. A robot set down where its chunk or dimension already holds the most robots the server allows is refused, with a message in the action bar; see [Server limits](#server-limits).

## Drives

- **Wheels**: roll over flat ground and climb a slab.
- **Legs**: step up a full block, and are slower than wheels.
- **Rotor**: lifts the robot off the ground if it is light enough, and is slower than wheels. It draws power steadily to hover. A robot too heavy for its rotor stays on the ground.

## Sensors and the chunk loader

A **Robot Sensor** comes in three kinds. Each gates the channels listed for it; without it those channels read zero.

- **Probe**: reads *Hardness ahead*.
- **Rangefinder**: reads *Distance ahead*, the blocks to the first solid thing in front, up to a short reach.
- **Inertial**: reads *Heading* and cuts the position error that wheels gather as they roll (see [Position](#position)).

A robot with no sensor still feels what is directly ahead through its other readings.

A **Robot Chunk Loader** keeps the chunk the robot stands in loaded while it runs a program, so it does not halt when you walk away. It drains the battery, and the server limits how many chunks may be held this way.

## Programming a robot

A robot has its own channels. Point a card at them with the [Data Connector](Data_Connector.md): each right-click on the robot steps the selection to the next channel and the bar shows *"Selected"* and its name. Then press **link** on the card in the Programmer, as for any other block ([Linking cards to real blocks](Programming.md#linking-cards-to-real-blocks)).

Switches (a *Switch* card turns them on and off):

| Channel | What it does |
| --- | --- |
| Roll forward | Drives forward |
| Turn left | Turns the robot left |
| Turn right | Turns the robot right |
| Break ahead | Digs the block in front |
| Place ahead | Sets a block from cargo in front |
| Pick up ahead | Takes dropped items in front into cargo |
| Drop ahead | Puts cargo out in front |
| Put into container ahead | Moves the first stack of cargo into the container or machine in front |
| Take from container ahead | Takes the first stack the container in front lets out |
| Switch machine ahead | Holds a switchable machine in front engaged while on, released while off |
| Climb | Rises while on; only a rotor robot light enough to fly answers |
| Descend | Sinks while on |

Readings (an *Input* card reads them):

| Channel | Reads |
| --- | --- |
| Blocked ahead | Whether a solid block is in front |
| Breakable ahead | Whether the tool head can break the block in front |
| Items ahead | Whether dropped items lie in front |
| Cargo full | Whether all nine cargo slots are full |
| Battery charge | The charge left, as a share |
| Distance ahead | Blocks to the first solid thing in front (rangefinder) |
| Hardness ahead | How hard the block in front is (probe) |
| Heading | Which way it faces, in quarter turns from south (inertial unit) |
| Beacon near | Whether a [Robot Beacon](Robot_Beacon.md) is within reach |
| Dock ahead | Whether a [Robot Dock](Robot_Dock.md) or Tether Reel is the block in front |
| Position east | Blocks east of where it was set down, by dead reckoning |
| Position south | Blocks south of where it was set down, by dead reckoning |
| Height above start | Blocks above the block it was set down on |

## How it works

- **Speed** falls as the robot gets heavier: a heavy frame or tool head slows it.
- **Digging** depends on the tool head's material. Its yield strength decides which blocks it can break and how fast. Breaking a block costs battery charge in proportion to the block's hardness.
- **Claims**: a robot claims the block it is breaking for the time it takes, so several robots on one face spread out instead of digging the same block.
- **A flat battery** stops the robot where it stands.
- **Damage** drops the robot as its item.
- A robot in an unloaded chunk halts, as any other creature does, unless it carries a chunk loader.

### Position

A robot tracks where it is by counting what it rolls. The error grows with every block, so *Position east* and *Position south* drift. A [Robot Beacon](Robot_Beacon.md) within a few blocks wipes it.

### Charging

A [Robot Dock](Robot_Dock.md) charges the robot standing on it, and a [Tether Reel](Tether_Reel.md) feeds a robot tied to it, both off an electrical line. A [Robot Arm](Robot_Arm.md) is a robot fixed in place and run off a line.

### Server limits

The server holds a limit on robots per chunk, robots per dimension and chunks kept loaded by robots, set in `feedback-server.toml`.

## Tips and automation

Read *Breakable ahead* and *Battery charge* to decide when to dig and when to stop before the battery runs flat. Read *Dock ahead* to know when a charging point is in front, and *Beacon near* to know when the position has been corrected.
