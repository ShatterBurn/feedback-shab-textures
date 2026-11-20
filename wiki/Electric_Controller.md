# Electric Controller

A [Controller](Controller.md) powered from a cable instead of a shaft. It runs the same programs, with the same upgrades, but takes the Memory Board and the Floppy Disk, two of the [program media](Programming.md#media). A mechanical Controller and Programmer take the Punch Card only.

## Crafting

```
A B
D C
```

A is a [Controller](Controller.md), B is a Memory Board, D is a [circuit](Basic_Electronic_Circuit.md) and C is Copper Wire. The finished controller keeps the circuit. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

## Using it

Right-click with a medium to put it in; right-click with an empty hand to take it out. Link what it reads and switches to it with the [Data Connector](Data_Connector.md): select a node, then use the connector on the Electric Controller. It binds **8** nodes, and **4** more for each Card Rack fitted. A node it reaches only through a path of linked nodes ([Linking nodes](Data_Connector.md#linking-nodes)); with no path the reading is 0, and the wire's grade sets how exact the reading is. A card asks for a signal, kind and link color rather than a block: see [Programming](Programming.md#input-and-switch-cards). It evaluates only while the line delivers at least half of the current it wants; with less the outputs, variables and delay clocks hold, as a mechanical Controller's do on a dead shaft.

## Circuit slot

The [circuit](Basic_Electronic_Circuit.md) the controller was crafted with is built in and cannot be taken out or swapped. Without one the controller does not run. The program steps at the circuit's clock rate, as with the mechanical [Controller](Controller.md#circuit-slot): a fast circuit takes it up to 20 times a second, a slow one stays near the bottom, and a circuit's noise makes sensor readings wobble. A placed controller keeps its circuit, and so does the item it drops when broken. The [Engineering Table's Repair tab](Engineering_Table.md#repair-tab) takes it apart and returns the circuit.

A circuit with a [Resonator](Resonator.md) steadies the clock and the readings. A circuit with a [Lattice](Lattice.md) adds up to 8 program variables, learns repeated readings and, unless its lattice [holds](Lattice.md#power-loss), forgets the extra variables after a long time without power. A circuit with gain lengthens a [Data Connector](Data_Connector.md#linking-nodes) link that ends at the controller. The Computer's circuit works the same way.

A powered Electric Controller warms a circuit with a [tube](Vacuum_Tube.md) with its heater, and the heater's current is added to what the controller wants from the line. See [Warming up](Basic_Electronic_Circuit.md#warming-up).

## Endernet upgrade

An [Endernet Upgrade](Endernet_Upgrade.md) makes an Electric Controller one end of an [endernet](Data_Port.md#endernet) pair, so that it reads what is linked to the pair's [Data Port](Data_Port.md) as though linked to itself. Use the upgrade on the controller to fit it. One fits per controller, it draws a small standing current while fitted, and a [Crowbar](Crowbar.md) takes it off.

## Electric Programmer

A [Programmer](Programming.md) that takes the Memory Board and the Floppy Disk. Crafted at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)):

```
  C
W P W
```

C is a [circuit](Basic_Electronic_Circuit.md), W is Copper Wire and P is a Programmer. **Erase** works on a printed Memory Board or Floppy Disk (see [Media](Programming.md#media)). It needs no power.

## Computer

An Electric Programmer and an Electric Controller in one block, with a desktop. Crafted from a [Phosphor Screen](Phosphor_Screen.md) above an Electric Controller above an Electric Programmer, in a column, at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

It looks like a 1990s desktop PC: a beige case with drive slots and lamps, a CRT monitor with a blinking cursor, and a keyboard and mouse on the desk in front. The screen faces you when you place it.

The Computer takes the Electric Controller's circuit, which is built in and works as the Electric Controller's [circuit](#circuit-slot). It takes an [Endernet Upgrade](#endernet-upgrade) and a [USB Port](#usb-port).

Its drive bays, running slot and editor take the Floppy Disk, and the USB Drive once a USB Port is fitted. A Punch Card or a Memory Board does not go in; the Computer runs programs from its internal disk through a card it makes itself.

Right-click it to open the desktop, a fixed panel drawn as a terminal. The colors, wallpaper and screen effects are set in the **Settings** window and are the same for everyone who opens this Computer.

The desktop has no inventory slots. Down the left are icons for **drive bays A: to D:**, the **RUN** slot and **INT**, the internal disk (see [Internal disk](#internal-disk)); each drive icon shows what its medium holds. Click an icon to open its **Drive** window, which shows a memory bar for the medium. Right-click a drive icon for its menu.

**Sneak** and right-click the Computer with an empty hand to open the **drive way** instead: an ordinary slot screen with the four bays (A to D), the running slot and your inventory, where Floppy Disks (and USB Drives, once a USB Port is fitted) go in and out by hand, program and app disks alike.

The Drive window offers:

- **Edit** opens the medium in an **Editor** window on the desktop. It holds the same node editor as the [Programmer](Programming.md): cards, wires, the variables panel, **Print**, **Erase**, **Stamp** and **Save fn**. Closing the Editor window saves the edit and returns the medium to its drive. The Editor also has a **Text** view that shows the same program as written text; see [Text form](Programming.md#text-form).
- **Run** moves the medium to the RUN slot, where it runs while the Computer is powered (the Controller's limits and upgrades apply). **Stop** puts it back in a free bay.
- **Eject** takes it into your inventory.

### Windows

Drive, Editor and every app open as windows. Drag a window by its title bar and resize it by dragging its right or bottom edge. Each has a minimize button, a maximize button (**o**), a **fullscreen** button (**[]**) and a close button (**x**). The focused window has the bright title. Windows stay inside the desktop area, clear of the drive icons, except in fullscreen.

Drag a title bar to the left or right edge of the screen area to snap the window to that half, or to the top edge to maximize it. Dragging a maximized or snapped window away restores its size. Double-click a title bar to maximize or restore.

**Fullscreen** makes the window fill the whole game screen, over the Computer's panel; its own title bar stays. JEI's item lists are hidden while it is on. The fullscreen button, the **Fullscreen** entry of the right-click menu (**Leave fullscreen** while it is on) and the same menu's restore entry, maximize, a double-click on the title bar, dragging the title bar and snapping all bring the window back to its normal size.

With a window focused:

- **Alt+Tab** cycles through the windows (Shift reverses).
- **Alt+Left** and **Alt+Right** snap to a half.
- **Alt+Up** maximizes.
- **Alt+Down** restores, or minimizes a window that is already restored.
- **Ctrl+W** closes.

Right-click opens a menu. On a title bar or a task button: minimize or restore, maximize, fullscreen, snap left, snap right, close. On the wallpaper: Files, Settings, Next wallpaper, Show desktop. On a drive icon or a Files row: Open, Edit, Run or Stop, Copy, Eject, Erase (it asks first), and **Save to internal disk** for a program or **Install on internal disk** for an app disk.

Short **toasts** appear in the top-right corner of the work area for about 6 seconds and vanish when clicked. They report an app disk inserted and Console events: a program started or stopped, a medium ejected, copied or erased, power on or lost, and refusals.

A bottom panel holds the **Menu** launcher, one task button per open window, and a tray. The Menu lists every app that is available, in two columns. Clicking the focused window's task button minimizes it; clicking another raises or restores it. The tray shows a lamp for each bay and the RUN slot, a power lamp, and the clock. The sliver at the far right of the panel minimizes every window.

### Apps

**Drive**, **Editor**, **Files**, **Settings**, **Monitor**, **Links**, **Diagram** and **Console** are built in. Every other app comes on an **app disk**, a Floppy Disk that carries the app. An app runs while its disk sits in one of the four drive bays or is installed on the [internal disk](#internal-disk). Pull the disk and the app disappears from the Menu and its window closes; the disk can go into another Computer and the app is there. An app disk is not a program medium: the Computer will not edit, run or copy onto it, and a [Programmer](Programming.md) will not take it. In a Drive window an app disk shows that it runs while in a drive. The creative tab lists each app disk.

Each app disk is researched at the [Research Table](Research_Table.md#apps): a blank Floppy Disk (no app, no program) plus the thing the app is about, won by breaking into a terminal.

| About item | App disk |
| --- | --- |
| Spyglass | Scope App Disk |
| Compass | Link Map App Disk |
| Book | Notes App Disk |
| Clock | Converter App Disk |
| Redstone | Calculator App Disk |
| Redstone Lamp | Power App Disk |
| Note Block | Arcade App Disk |
| Paper | Blueprint App Disk |
| Ender Pearl or Synthetic Pearl | Network App Disk |

#### Built-in apps

A Computer with power shows sensors linked straight to it (made with the [Data Connector](Data_Connector.md): Sensor fittings, Flight Sensors, channel sources such as a Driver's Seat) in the Monitor, Diagram and Scope apps with no program running. These reads travel the data wire like an Input card's: wire reach, a poor wire's rounding and circuit read noise all apply.

- **Monitor**: the running program's variables and the last reading of each of its Input cards. A card whose request matches no linked sensor shows a dimmed *no match* in place of a reading. A **Linked sensors** section lists the sensors [linked](Data_Connector.md) straight to the Computer, up to 8, whether or not a program reads them.
- **Links**: the blocks linked to the Computer, with their positions and link colors.
- **Diagram**: the [Contraption Diagram](Instruments.md#contraption-diagram) of the vehicle a Flight Sensor is on, for any Flight Sensor linked straight to the Computer. **Next sensor** steps between linked sensors; the angle, merge and assembly controls are the same as on the sensor's own screen.
- **Console**: a log of what the Computer did, newest at the bottom: programs started and stopped, things its program switched on and off, switches you flipped over an endernet pair (*flipped over the pair*), power lost and back, media ejected, edited or copied, and media it refused.
- **Files**: in the Menu. Lists drives A to D and RUN with what each holds. Click a row to pick it, double-click to open its Drive window. The buttons are **Open**, **Edit**, **Run** (**Stop** for the running medium) and **Eject**. Drag a written program onto another drive row, or use **Copy** in the right-click menu, to copy it onto a blank Floppy Disk or USB Drive sitting alone in that bay; the blank medium is used up as the cost. Drag it onto the **Trash** box to erase it: it asks first, and the medium is left blank and reusable.
- **Internal disk**: opened from the **INT** icon. See [Internal disk](#internal-disk).
- **Settings**: see below.

#### Disk apps

- **Scope**: a trace for each Input card of the running program, then for each sensor linked straight to the Computer, up to four traces in all, each scaled to its own range. **Pause** freezes the traces, **1x**, **2x** and **4x** set the time scale, **Clear** wipes them. Readings arrive a few times a second, so the traces are stepped.
- **Link Map**: a plan view with the Computer in the middle and every linked block around it. Inputs are squares, outputs are crosses, and a block in an unloaded chunk is drawn hollow. Hover a mark for the block, its position and its distance.
- **Notes**: a text pad of up to 2000 characters, saved on the block and the same for everyone.
- **Converter**: converts between units in three tabs. Metal covers block, ingot, Qu and mB of melt (1 Qu is a nugget, 9 Qu make an ingot, 81 a block, and molten metal is 16 mB per Qu). Volume covers bucket (1000 mB), ingot (144 mB) and mB. Time covers day (24000 ticks), minute (1200), second (20) and tick.
- **Calculator**: handles + - * / ^ and brackets, from the buttons or the keyboard. Enter or = evaluates, and `ans` recalls the last result.
- **Power**: whether the Computer is powered, the band of its line, its current and what it wants (read to the precision of the instrument you carry), the band it is rated for, a bar showing how much of a rated packet arrives, and a half-minute trace of the packet.
- **Arcade**: Snake (arrows or WASD, space to start) and Minesweeper (click to dig, right-click to flag; 14 mines on a 14x8 field). The best Snake length and the fastest cleared field are kept on the block.
- **Blueprint**: the [Blueprint Table](Blueprint_Table.md)'s drafting board in a window, in the desktop's colors: draw designs from the types, pick parts, materials and shapes, read the predicted figures, compare and pin up to three drafts, and run the test rig. The type selector, edge selector and search stay with the window, and the board takes the window's size, showing more types when the window is wide. It cannot print onto paper and has no sample slot. The window scrolls with Ctrl plus the wheel, or by dragging the strip down its right edge.
- **Network**: the Computer's [Endernet Upgrade](Endernet_Upgrade.md) and its [channel and address](Endernet_Upgrade.md#channel-and-address). It needs the Network App Disk in a bay and a fitted upgrade; without an upgrade the window says so. It has four tabs.
  - **Pair**: the pair (a short tail of its id), its state (*carrying*, *far end away* or *not powered*), the far end's distance and compass side, and the count of channels the Computer's circuit can separate ([Resonator](Resonator.md)). Below, a list of what is linked to the far [Data Port](Data_Port.md), each row with its kind (machine, computer, robot or port) and, for something that switches, its state (on or off) and a **Flip** button that toggles it. The list refreshes about once a second and scrolls. Flip works only while the pair is carrying.
  - **Tune**: **-** and **+** step the channel (1 up to the count the circuit separates) and the address (none, then 1 to 99).
  - **Send**: the addressed Computers on your channel, each with its address and distance. Pick one, then **Send message** with the text you typed, or **Send file** with the bay (A to D) the button shows. See [Sending between Computers](Endernet_Upgrade.md#sending-between-computers) for costs and losses.
  - **Inbox**: the mail that arrived, with how many of the 16 places are used. Messages for you read *From*; ones you overheard read *Heard*, and files show the program's node count. **Clear** empties it.

### Internal disk

Every Computer has an internal disk of **16 files**, each at most **16 KiB**. The files stay with the Computer: break it and place the dropped item again and the files are there; a copy of the Computer gets a copy of the files.

The Internal disk window lists the files, each with a **Run**, **Edit** and **Delete** button. **Edit** opens the program in an Editor window, and closing the Editor saves it back. A right-click menu offers **Copy to** and **Move to** a drive, which needs a blank Floppy Disk (or USB Drive, with a USB Port fitted) alone in that bay. An app is installed, not copied: saving an app disk leaves the disk blank, and an app can only be moved back onto a blank disk. A program or app disk goes onto the internal disk from its drive icon's menu (**Save to internal disk**, **Install on internal disk**). An app installed on the internal disk runs with no disk in a bay. A program received over the network is kept on the internal disk when there is room.

Media have a capacity (Punch Card 1, USB Drive 4, Floppy Disk 8), but a medium still holds one program or one app.

### Settings

- **Phosphor**: the screen color, Green, Amber, White or Blue.
- **Wallpaper**: Grid, Scans, Circuit, Horizon (a retro perspective plain) or Blank. **Next wallpaper** in the wallpaper's right-click menu steps through them.
- **Screen effects**: Scanlines, Glow, Rolling band (a slow bright band down the glass) and Boot line (a bright line that grows to fill the screen as it powers up), each on or off.
- **Desktop widgets**: Clock (the time with the day count), Scope (traces the running program's first sensor reading) and Memory (a bar per drive), each on or off.
- **Icons right**: moves the drive icons to the right side; it takes effect when the desktop is reopened.

Right-click with a medium in hand puts it straight into the RUN slot; the drive way takes it out. The editor works without power; the program runs only while the cable delivers.

### USB Port

A socket you fit to a Computer. With it fitted, the Computer's drive bays, running slot and editor take [USB Drives](Programming.md#media) as well as Floppy Disks.

```
K K K
K P K
```

K is a Ceramic and P is a Copper Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 2 machine bolts ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)). Use it on a Computer to fit it. A Computer carries one USB Port.
