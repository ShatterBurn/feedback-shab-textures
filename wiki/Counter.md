# Counter

A shop counter with a nine-slot shelf. Stock it and villagers walk over and buy at your prices, or feed it goods by hopper to hand to a villager as a delivery.

```
P E P
P H P      P oak planks, E emerald, H hopper
```

A Counter has a facing. Its front is the customer side and its back is the keeper side.

## Using it

- **Front or top, right-click:** the buy menu. Click a shelf item to buy one, Shift-click to buy a stack. A price tag hangs on a string under each shelf item, and a purchase knocks on the counter: a heavier thud and a bigger thump for a whole stack. You pay in emeralds from your inventory, and they go into the Counter's till.
- **Back, right-click:** the keeper menu. Stock the shelf, set a price in emeralds for each slot with the **+** and **-** buttons (Shift changes it by 8), and take emeralds from the till slots and goods from the returns slots. A slot's tooltip shows the market price, the highest price any villager pays for that item.
- **Either side, right-click:** the catchment readout: villagers in reach, how many want something here, errands pending and villagers out of reach.

Jade on a Counter shows the same catchment, plus villagers who find the price too dear, walk-ins turned away when the till is full, the road tiles that carry the reach, and sales today.

### Hoppers and belts

- The shelf fills from the back and the top.
- The left, right and bottom faces are the intake, the till and the returns.
- The front has no inventory access.

## Walk-in customers

A villager comes to a stocked Counter when it:

- is idle or meeting,
- wants something the shelf holds,
- has errands left for the day (one plus half its level, rounded down), and
- has a walkable path to the front face.

It walks over and buys at the keeper's price. The default price of a slot is the villager's line price for that item. A villager sealed in a cell has no path and never comes. When more than one stocked Counter is in reach, the nearest wins.

A villager pays above the market price up to one and a half times it (at least 1 emerald more), and gossips against the keeper for it. Above that it refuses.

### Reach

A villager reaches a Counter within a straight radius of 12 blocks. A street of road tiles carries the reach farther, about 48 blocks along the road. Path, gravel, cobblestone, stone bricks, bricks, planks, wooden slabs and similar blocks count as road.

### Freshness

Food is priced by freshness: fresh food pays in full, food going off pays half, and rotten food is refused. Food on the shelf spoils in the shelf's air. A stove or a cold box touching the Counter pulls its air. See [Food](Food.md).

### Regional prices

A village's biome type shifts what it pays for some goods. Snow and taiga villages pay more for food, wool and coal; desert villages pay less. Some shop lines are limited to certain biome types.

## Hand-over

Goods fed into the intake are handed to the nearest villager within 8 blocks as a delivery at their screen would be:

- A made piece is judged against the orders the villager has posted today. If it satisfies one, the Counter takes it and pays emeralds by its grade.
- Anything else is offered to what the villager buys. A want sold this way pays 75% of the line price.

Emeralds come out of the till slots, and whatever the villager did not take comes out of the returns slots.

## Misuse

The Counter does not choose or read what it is given. Poor work pays little, and a run of similar pieces fills the villager's saturation for days. With nothing sorting upstream, rejected goods fill the return slots and the intake stalls until they are emptied.

Raiders go for a Counter whose shelf holds 20 or more emeralds' worth of stock. See [Ledger Board](Ledger_Board.md#raids).

## Depot

One end of a trade road. A linked pair of depots swaps goods by convoy each market day.

### Crafting

A depot is a clean wooden tray with a ring and an emerald mark.

```
P E P
P B P      P any plank, E emerald, B barrel
P P P
```

The **Depot Link Card** is a column of black dye, paper and an emerald, top to bottom.

### Using it

1. Use a link card on one depot. The card records it.
2. Use the card on a second depot to link the two. A message confirms the link. If the recorded depot is gone or too far, the card keeps what it recorded.
3. Right-click a depot to open it. The **outgoing** shelf takes goods; the **incoming** shelf is take-only. The screen says whether the depot is linked and how many parcels are on the road.

Hoppers fill the outgoing shelf and empty the incoming one.

### How it works

On each [market day](Villages.md#market-cross), a convoy carries the outgoing goods to the linked depot, provided a road of at least 12 tiles leaves the depot and the other end is in the same dimension. Each linked depot sends its own. Road blocks are the same as for a [Counter](#reach).

The trip takes about 12 ticks per block of distance, and at least a minute. A donkey courier is seen leaving when a player is within 64 blocks.

#### Raiders

Each stack on the road is lost to raiders with a 20% chance. Each bell within 12 blocks of either depot lowers the chance by 15%; up to 3 bells count, and the chance never falls below 3%.

Villages pay differently for the same goods (see [Regional prices](#regional-prices)): carry what the other village wants.

## See also

- [Villagers](Villagers.md) for orders, grades and saturation.
- [Village Stalls](Villages.md#stalls-and-plazas) for Counters the village keeps.
- [Ledger Board](Ledger_Board.md) for contracts a Counter can fill.
