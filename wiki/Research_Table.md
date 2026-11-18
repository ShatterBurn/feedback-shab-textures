# Research Table

The Research Table is where what is not yet known is found out. It writes a [Blank Treatise](Treatise.md#blank-treatise) into a treatise volume, writes a [Rite](Enchanting_Table.md#rites) for an attunement, or writes an app onto a blank Floppy Disk. A treatise is worked out by playing rows of concept cards for a score on a notebook page, an attunement by purifying a grid of murk, and an app by breaking into a terminal.

## Crafting

```
P P P
W L W
W W W
```

P is paper, W is any plank, L is a lectern.

## Using it

The screen is an open book. The left page is a list with three tabs, **Treatises**, **Rites** and **Apps**; each entry has a spinning icon. Click an entry to pick it. The right page shows the picked entry's two-line blurb, what it asks for, its slots and the **Research** button, which costs 40 experience points. It opens the notebook page for a treatise, the purify grid for an attunement and the terminal for an app.

A volume has a slot for one Blank Treatise and a slot for one **specimen**. An attunement has a slot for an **offering**, a slot for a **reagent** and a slot for a **Blank Rite**. The text on the right page says what is missing.

### Specimens

A **first volume** takes a plain thing:

| Volume | Specimen |
| --- | --- |
| Weapons I | sword, knife, axe or hafted tool |
| Mechanical I | hand crank or water wheel |
| Aerospace I | water wheel |
| Robotics I | dynamotor or electromagnet |
| Electrical I | battery or galvanic cell |
| Chemical I | crucible or thermal store |
| Materials I | hand hammer or file |

A volume after the first takes an item built from the volume before it, and that earlier treatise must be held: in your pack, or on a [Treatise Shelf](Treatise_Binder_and_Shelf.md) beside the table.

| Volume | Specimen |
| --- | --- |
| Weapons II | cleaver, maul or spear |
| Weapons III | crossbow, gun or launcher |
| Mechanical II | engine or flywheel |
| Aerospace II | propeller |
| Chemical II | boiler, gas tank or pressure vessel |
| Chemical III | compressor or heat exchanger |

### Attunements

The Rites tab lists every attunement, and any of them can be researched again and again. Each attunement is about one material property: hardness, toughness, strength, weight, heat resistance, conduction or heat capacity. Its power sets a bar. The **offering** is any stock of a material, and it must rank at least that far up the property among all materials; the screen calls the bar modest, good, fine or exceptional. The further the offering is over the bar, the more ink it gives, up to 4 extra. **Mend** is about toughness and asks for an offering in the top tenth of materials by toughness.

The **reagent** is an optional brewed potion.

### Apps

The Apps tab lists every app a [Computer](Electric_Controller.md#apps) can run from a disk. An app has a slot for a **disk**, a blank [Floppy Disk](Programming.md#media) with no program and no app on it, and a slot for **About**, the thing the app is about: a spyglass for Scope, a compass for Link Map, a book for Notes, a clock for Converter, redstone for Calculator, a redstone lamp for Power, a note block for Arcade, paper for Blueprint, and an Ender Pearl or Synthetic Pearl for Network.

The **Research** button opens a green phosphor terminal. A column of hex addresses sits beside a dump of junk characters, two columns of 16 rows. Hidden in the dump are 8 to 12 words of equal length, all from the app's own vocabulary (Arcade: ARCADE, SCORES, TOKENS and the like). One of them is the answer. The longer and more alike the words, the harder the app. A log column at the right records what you try, and pips show the tries left.

Click a word to try it. The terminal answers **LIKENESS=n**, how many of its letters match the answer in the same place, and spends a try. A wrong word gives ENTRY DENIED. Use the likeness to narrow the words down. Finding the answer writes the app onto the disk with a long tone; the About item is only studied and stays in its slot. Running out of tries locks the terminal: the 40 experience points are spent and the disk stays blank.

A matched pair of brackets on one row, such as `( )`, `[ ]`, `{ }` or `< >`, can be clicked as a **hack**, once per pair. Three times in four it removes a wrong word, shown as dots. One time in four it refills the tries. A stamp grades the notes by whether the answer was found, the tries left and the hacks unused. The grade does not change the app.

A plain run has 3 tries and no brackets. Each ink above the base of seven adds a try, up to 6, and every 2 ink above the base adds a bracket pair. The extra ink comes from having done the real thing before. Doing it enough times gives the bonus:

| App | Done before | Extra ink |
| --- | --- | --- |
| Arcade | crafted 8 note blocks | 2 |
| Blueprint | crafted 32 paper | 2 |
| Calculator | crafted 8 repeaters | 2 |
| Converter | crafted 2 clocks | 3 |
| Link Map | crafted 2 compasses | 2 |
| Network | used 8 ender pearls | 3 |
| Notes | crafted 4 books | 2 |
| Power | crafted 4 redstone lamps | 2 |
| Scope | crafted 1 spyglass | 3 |

A three-step guide in terminal capitals shows over the terminal the first time you play it in a game session; click to dismiss it, and the **?** button in the top right corner brings it back.

Keys, a low buzz on a denied word, a rising tone on a hack and the long tone on success sound as you play.

### Blank Rite

A Blank Rite is shapeless: 2 paper + lapis lazuli + ink sac. It is used up when the research writes the rite.

Some attunements, Heat and Ward among them, name a potion. A brewed potion carrying the named effect, still potent, qualifies by itself and gives 3 ink, which gives the rite game more ink and leans its tray (see [Purifying a rite](#purifying-a-rite)). If the offering is not enough, the screen says it needs an offering that is good enough, or the potion it names.

One offering is used up per run, and the potion with it. No specimen is worn.

## Treatise research

Each volume has its own story, goal concept, concepts and traits. The notebook page shows the volume's goal, the hands and discards left, and your score against the **target score**. Below lies a hand of 5 concept cards, each showing its **traits** under its name, such as force, thin, hard or pressure. Two concepts link when they share a trait. Hover a card to see its traits and whether it is raised by a treatise you hold or by the goal.

Click cards to lay a row of 1 to 5, then press **Play row**. The page shows what the row is worth as you build it, as chips times a multiplier, and the row is judged as a set:

- A card that shares a trait with another card of the row scores. A lone card is safe and scores as a single.
- The most cards sharing one trait name the **combo**: single, pair, triple, quad or five. A bigger combo has more base chips and a bigger multiplier.
- Each scoring card adds chips.
- A row of 3 or more cards that links card to card in the order laid adds a **chain** bonus to chips and multiplier, so the order of the row matters.
- A scoring card that carries a trait of the goal adds to the multiplier. A green pip marks it.
- Other treatise volumes you hold act as **jokers**. Each lends the traits of its school, and a scoring card carrying one of those traits adds more to the multiplier, once for each such volume. A gold pip marks it. A volume counts when it is in your pack, in a binder or on a shelf touching the table (see [Treatise](Treatise.md#using-it)); the volume being researched and the one before it do not.

Reaching the target score ends the run. The page then replays what was found, asks one closing question, and a stamp grades the notes.

A card that shares nothing with the rest of a row of 2 or more burns. The play is spent and, on a volume, the specimen **strains**: a few burning plays wear it, so it comes back worn and can be repaired, and many take it apart into its parts, which go to you. A tougher specimen forgives more. Burned, played and discarded cards come back on the next reshuffle. A run that spends its hands before the target is reached still gives notes, at a low grade.

A run has 4 hands and 2 discards. A discard throws chosen cards back for fresh ones and costs no hand. There is no clock.

A three-step protocol window shows over the notebook page the first time you play it in a game session; click to dismiss it, and the **?** button in the top right corner brings it back.

### Target and deals

The volume's number sets the target score: 160 for volume 1, plus 50 per volume after. It also sets how many linking cards each deal is guaranteed to hold: 2 in volume 1, 1 in volume 2, none after.

### Ink

A run starts with a base of seven ink. A volume adds up to three more from a specimen in good condition, and a bonus if you have already done the real thing a good number of times: fired a crossbow many times before researching guns, or forged with a hammer before Materials I. Each ink above the base adds a discard, and every 3 ink above the base adds a hand.

### Grade

The notes' quality adds up from four parts:

- 0.35 for reaching the target,
- up to 0.3 for the score over the target,
- up to 0.15 for hands and discards left,
- 0.2 for the closing answer.

A run that does not reach the target gives 0.05 to 0.25 by how close the score came.

## Purifying a rite

The Rites tab's **Research** button opens a 5 by 5 grid. Its cells are **murk**, the raw effect, each of one of six elements, drawn as its own tile: fire, water, earth, air, light and dark. Fire and water, earth and air, and light and dark are opposites. A few **anchors**, cells of the effect's own element, sit in the grid. Anchors never clear, and they count as filled when a line is checked.

Under the grid is a tray of 3 pieces, each of a single element and 1 to 4 cells. Press a piece and drag it: it lifts onto the pointer at full size. Release it over the grid to set it on the nearest cells where it fits, centered on the pointer. While you hold it, the cells it would cover show green where it fits and red where it does not, and any row or column it would complete glows. Released elsewhere, or where it does not fit, the piece flies back to its tray slot (with a shake if it was over the grid). Clicking a cell places nothing. Pieces do not rotate.

When a row or column is full, it clears and your own pieces in it vanish. A murk cell in the line is cleansed only if the line also holds the opposite of that murk's element, in any cell, anchors and other murk included. A full line without it clears your pieces and leaves the murk.

A grid is done when all its murk is gone. A rite takes 1 to 3 grids depending on the attunement, and each grid is thicker than the one before. There is no clock.

A three-step guide in a wizard's flourishing voice shows over the grid the first time you play it in a game session; click to dismiss it, and the **?** button in the top right corner brings it back.

- **Draw again** costs 1 ink and deals a whole new tray. The ink comes from the base, the offering and the reagent, as for a treatise.
- **Take the residue** ends the run at once with what has been cleansed, at a lower grade.

The grade is the purity (the grids cleansed, plus the share of the one in hand) times a bonus for few pieces and few redraws.

The **offering** shapes the grids. The further its property ranks over the attunement's bar, the thinner the murk and the tidier the pieces. A **reagent** potion leans the tray toward the element that cleanses the thickest murk.

## What comes out

An app is written onto the disk as it is, whatever the grade. A treatise carries the quality as its notes. An attunement gives a [Rite](Enchanting_Table.md#rites) whose quality is the quality of the purify game. The experience is spent either way, and a scrawl still turns the Blank Treatise into the volume. Pressing Esc before closing the notebook spends nothing.

## See also

- [Treatise](Treatise.md): the volumes and the other ways to get one.
- [Enchanting Table](Enchanting_Table.md#rites): shelving a rite to write its attunement.
- [Engineering Table](Engineering_Table.md#treatise-tab): taking a specimen apart for a volume.
