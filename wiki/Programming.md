# Programming

A [Controller](Controller.md) runs a program, and a program is written on a **programmer**. Programmers differ in what they write onto and how easy it is to change your mind; what a program can say is the same on all of them.

| Programmer | Writes onto | Changing a program |
| --- | --- | --- |
| **Programmer** | Punch Card | Print a new card |


## The Programmer

The desk where you write a program for a [Controller](Controller.md). You lay out cards on a canvas, wire them together, and **print** the result onto a **Punch Card**.

## Crafting

**Programmer**

```
W P W
W S W
W W W
```

W is any Planks, P is Paper and S is a [Shaft](Shaft.md).

**Punch Card**: two Paper side by side make **four** blank cards. Blank cards stack; printed cards don't.

## Opening it

Right-click the Programmer. Put a blank Punch Card in its slot.

## The cards

| Card | In → Out | What it does |
| --- | --- | --- |
| **Input** | → number | The reading of what is linked to the [Controller](Controller.md#linked-nodes). See [Input and Switch cards](#input-and-switch-cards). |
| **Greater Than** | number, number → yes/no | Yes if the left is bigger than the right. |
| **Less Than** | number, number → yes/no | Yes if the left is smaller than the right. |
| **Logic** | yes/no, yes/no → yes/no | **AND**: yes only if both are yes. **OR**: yes if either is yes. **NOT**: yes if its one input is no (the second input disappears). Click the middle to cycle AND, OR, NOT. |
| **Math** | number, number → number | + − × ÷, min or max. Click the middle to cycle; the right input can be a typed number. For vehicles. |
| **Delay** | number or yes/no → the same | Passes its input along after a wait. Click the middle to choose number or yes/no; click the length to step through 1 s, 5 s, 30 s, 2 min and 10 min; click *type a length* to enter any number of ticks from 20 (1 s) to 72000 (1 h). The length reads like `600 t (30 s)`. The length row has a second input pin: wire any number card (sensor, math, read, another Delay) into it and the length follows that number in ticks, rounded and held between 1 and 72000 each step; the length buttons are hidden and the row reads *ticks*. Typed and wired lengths need an **Escapement** fitted to the [Controller](Controller.md#memory-and-upgrades). |
| **Read** | → number or yes/no | The current value of one variable. Click the card to step through your variables; its output is a number or yes/no to match. |
| **Write** | number or yes/no, yes/no → | Stores its first input in one variable on the moment its second input (the *when*) turns yes. Click the card to pick the variable; the first pin follows that variable's kind. |
| **Switch** | yes/no or number → | Drives every linked block of one kind: on for yes, off for no. A number only moves a vehicle part, such as an engine's throttle. |

The second input of *Greater Than* / *Less Than* can be left unwired and given a **number** instead. That's how you write "temperature less than 1380".

A program can have up to **16** cards, more with Card Racks fitted to the Controller (see [Controller](Controller.md#memory-and-upgrades)). A [robot's](Robots.md#program-size) program and the program of a Controller [riding in a vehicle](Vehicles.md#autopilot) are sized by their circuit instead.

## Variables

The panel at the top right of the canvas declares the program's **variables**: named values a program can store with *Write* and read back with *Read*. Type a name (up to 12 letters, digits, spaces, `_` or `-`), pick **number** or **yes/no**, press **+**. A bare Controller holds 4 variables; Memory Drums add more, and so does a [Lattice](Lattice.md) on the Controller's circuit (the panel shows how many you have declared out of the most any Controller can hold).

- Click a variable's name to rename it; Read and Write cards that use it follow.
- Click its kind to switch between number and yes/no; wires the new kind refuses are cut.
- Click **x** to delete it. A variable that a Read or Write card still uses can't be deleted.
- Names must be different from each other.

## Loops

A program may loop back on itself only through a *Delay*, since a Delay's output is what its input read earlier. A Delay's length wire is read before its history, so the data input can still close a loop while the length comes from elsewhere. Any other loop is refused. A *Read* has no input, so a *Write* feeding a *Read* through a variable is not a loop: the *Read* sees the stored value one step later.


## Controls

- **Right-click** the canvas: add a card.
- **Drag from a pin** to another pin: wire them.
- **Alt-click** a wire: cut it.
- **Middle or right-drag**: pan. **Scroll**: zoom. **Home**: fit everything in view.
- **Ctrl+Z / Ctrl+C / Ctrl+V**, **Del**: undo, copy, paste, delete.

## Input and Switch cards

A card names no block. It describes what it wants, and the [Controller](Controller.md#linked-nodes) the card runs on finds it among the nodes linked to it with the [Data Connector](Data_Connector.md). Click a row of the card to choose from a list or to cycle.

An **Input** card has these rows:

- **Signal**: what to read. A list of quantities such as temperature, current, speed or on or off, and a robot's senses.
- **Kind**: a preferred kind of block, or *any kind*. The kinds are Furnace, Vessel, Heater, Clutch, Valve or damper, Engine, Driver seat, Lathe, Breaker or fuse, Electromagnet, Lamp, Mast, Redstone emitter, Controller and Robot.
- **Color**: the color of the link, or *any color*. It tells apart several blocks of one kind, for example two furnaces linked with different dyes.
- **Reduction**: how several matches combine: *average of matches* (the default), *lowest of matches* or *highest of matches*.
- **Input**: for a [Driver's Seat](Vehicles.md#the-drivers-seat), which key to read, 0 to 1 (default **W**). Click the row and press the key.

Once a kind is picked, the Signal list shows the signals that kind reports on its own first, then the other quantities, then a robot's senses (a robot's senses first when the kind is Robot).

A kind is a preference for an Input card. When no linked node of that kind offers the signal, any linked node that does offer it answers, so one program works with a crucible or an arc hearth. A card for *On or off* reads yes while the block is on.

A **Switch** card has a **Kind**, an optional **Color** and, for blocks with several inputs, a **Channel** row. It drives every linked node that matches. It never falls back: if no node of that kind (and color) is linked, it does nothing.

When nothing matches, an Input card reads 0.

A [Robot](Robots.md) carries its own channels. A [Radio Mast](Radio_Mast.md) paired with a robot, or another [Controller](Controller.md#reading-another-controllers-variables), offers its channels on the card's channel row: click it to pick one. A [Power Cutter](Power_Cutter.md#circuit) or [Auto Engineer](Auto_Engineer.md#circuit) with a circuit seated offers Progress, Condition and Output fill the same way.

## Printing

Press **Print**. The program is checked first, and you'll be told if:

- a wire leads nowhere or an input has no wire,
- an Input card has no signal, or a Switch card has no kind,
- the program loops back on itself with no *Delay* in the loop,
- a wire connects the wrong kind of signal (a yes/no into a number input),
- two sensors being compared don't measure the same thing (temperature against current, say),
- there are more than 40 cards or 24 variables (the most any [Controller](Controller.md#memory-and-upgrades) can hold), or
- a Read or Write card names a variable that isn't declared, or a Delay length is out of range.

Print does not know which Controller the card will go in. A Controller refuses to take a card that needs more cards or variables than it has room for, or a typed or wired Delay length without an Escapement fitted, and says why.

A printed Punch Card is **read-only**: *"Printed -- read only."* To change a program, print a new card.

## Media

A mechanical Controller and Programmer take the Punch Card only. The [Electric Controller, Electric Programmer and Computer](Electric_Controller.md) take every medium:

- **Punch Card**: written once.
- **Circuit Board**: written once. Made at the [Engineering Table](Engineering_Table.md) from a Punch Card between two Copper Fine Wires in a column, with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)); it spends no fasteners.
- **Floppy Disk**: made at the Engineering Table with a screwdriver in the tool row and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)), from paper, a Punch Card, magnetite dust and a Wrought Iron Plate:

  ```
  P D P
  M H M
  ```

  P is paper, D is a Punch Card, M is Magnetite Dust and H is a Wrought Iron Plate. It spends no fasteners. Put a printed disk in the Programmer and press **Erase**: the program comes back into the editor, the disk is blank, and Print writes the new program onto the same disk.
- **USB Drive**: a Copper Plate above a [circuit](Basic_Electronic_Circuit.md) above a Ceramic, made at the Engineering Table with a screwdriver in the tool row and Electrical Treatise III: Circuits, with no fasteners. Rewritten the same way as a Floppy Disk.

## Text form

The [Computer's](Electric_Controller.md#computer) Editor window shows a program a second way, as text. The **Text** button switches to it and **Nodes** back. The text is the same program: nothing is stored twice, and the cards and positions on the node canvas are kept.

Switching to Text prints the program in a standard form. Typing recompiles it on every edit. Findings are listed with their **line and column** and underlined in the text:

- **Errors** stop **Apply**: a name that is not declared, a wire of the wrong kind, a loop with no Delay, a number where a card must stand, a limit passed.
- **Warnings** never stop it: an unwired operand, a division by a literal zero, a variable never used, written but never read or read but never written, a value nothing uses.
- A card with no signal or kind, or an unwired input, does not stop **Apply** either; it is reported when you print, as on the canvas.

**Apply**, leaving the Text view or closing the window takes a text that compiles as one undo step and prints it again in the standard form. A text that does not compile never changes the program: it is dropped when you leave. A printed medium's text is read-only. **Tab** completes the word at the caret from what the text declares (statement words, card names, signals, kinds, colors, variables, the two kinds); **Ctrl+Space** opens the list.

Names such as `s1` or `c2` are not kept: after Apply the program is the cards, and the text prints them again under names that follow their kind and order.

### Syntax

One statement per line (a semicolon also ends one). `--` starts a comment. Words are not case sensitive.

| Line | Meaning |
| --- | --- |
| `var armed: boolean` | Declare a variable, `number` or `boolean`. |
| `s1 = sensor(temperature, furnace, red, avg)` | An Input card: the signal, then optionally a kind (a word), a link color (a dye name), a reduction (`avg`, `min`, `max`) and a channel (a number), in any order. `sensor(temperature)` reads any linked node that offers it. |
| `c2 = s1 > 400` | A comparison (`>` or `<`); a bare number on the right is the card's typed number. |
| `g4 = c2 and g10` | A logic card: `and`, `or`, `not`. |
| `m6 = s1 + 10` | A Math card: `+ - * /`, `min(a, b)`, `max(a, b)`. |
| `d5 = delay(c2, 5s)` | A Delay; units are `t` (ticks), `s`, `m` and `h`. |
| `r3 = read(armed)` | A Read card. |
| `a7 = actuate(g4, clutch, blue)` | A Switch card: the value, then a kind, and optionally a link color and a channel. |
| `w8 = write(armed, g4, d5)` | A Write card: the variable, the value and the *when*. |

Linking a sensor or block to the Controller happens in the world with the [Data Connector](Data_Connector.md); the text only says what a card asks for. The program limits (cards and variables) are the same as on the canvas.

## Function Cards

A **Function Card** (a Punch Card and a piece of paper) saves a part of a program. Select cards in the editor, hold a blank Function Card and press **Save fn**: the selected cards go onto it, with any wire that led outside the selection cut. Hold a saved Function Card and press **Stamp**: its cards appear at the cursor as ordinary cards, inputs unwired, with the variables they use declared. Input and Switch cards keep what they ask for. A stamped function is just cards, so it counts against the Controller's card limit like any others.

**Close the Programmer only after printing.** Unprinted edits are lost when you close it.

## Example: thermostat with a damper

- *Input* (crucible temperature) → *Less Than 1380* → *Switch* (bellows clutch). This blows while it's too cool.
- *Input* (same sensor) → *Greater Than 1400* → *Switch* (damper). This vents when it's too hot.

To stop the bellows clutch flapping right at 1380, give it a band: see [Controller](Controller.md#holding-a-band). A *Write* into a yes/no variable, read back by a *Read*, holds the same state without wiring the switch back in.
