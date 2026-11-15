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
- **Robot Drive**: the wheels and gearing. A lighter robot on the same drive moves faster.
- **Robot Tool Head**: the working end. Its material decides what the robot can dig and how fast.

## Using it

- **Right-click a block** with the Robot item to set it down as a robot standing there.
- **Sneak and right-click with an empty hand** to pick it up. It keeps its battery, program and cargo.
- **Right-click with a [Battery](Battery.md) item** to fit one. Without a battery the robot has no power and does nothing.
- **Right-click with a Punch Card** to load a program. It is an ordinary [Controller](Controller.md) program written at the [Programmer](Programming.md).
- **Right-click with an empty hand** to read its charge and whether a program is loaded.

The robot holds nine slots of cargo.

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

Readings (an *Input* card reads them):

| Channel | Reads |
| --- | --- |
| Blocked ahead | Whether a solid block is in front |
| Breakable ahead | Whether the tool head can break the block in front |
| Items ahead | Whether dropped items lie in front |
| Cargo full | Whether all nine cargo slots are full |
| Battery charge | The charge left, as a share |

## How it works

- **Speed** falls as the robot gets heavier: a heavy frame or tool head slows it.
- **Digging** depends on the tool head's material. Its yield strength decides which blocks it can break and how fast. Breaking a block costs battery charge in proportion to the block's hardness.
- **A flat battery** stops the robot where it stands.
- **Damage** drops the robot as its item.
- A robot in an unloaded chunk halts, as any other creature does.

## Tips and automation

Read *Breakable ahead* and *Battery charge* to decide when to dig and when to stop before the battery runs flat.
