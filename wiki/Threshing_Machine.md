# Threshing Machine

A drum on a shaft under a hopper that beats wheat apart into seed, straw and chaff. It runs off a shaft and sorts the three into trays.

## Crafting

```
  P
R S R
B W B
```

P and W are any plank, R is a Wrought Iron Rod, S is a [Shaft](Shaft.md) and B is a [Bearing](Bearing.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [hammer](Hammer.md) in the tool row and 2 [fasteners](Fasteners.md). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

It sits **directly on a shaft line**. Put wheat in the hopper on top, by hand or from a [Hopper](Hopper.md). Turn the shaft at 16 RPM or more and it threshes one plant a second at 32 RPM, faster at more RPM. Each plant gives 2 wheat seeds, 1 [Straw](Materials.md#straw-and-chaff) and 1 Chaff, each into its own tray: seed, straw and chaff. Take them out with an empty hand, or let a hopper below draw them.

It is a **40 Su** load.

## How it works

- **Speed.** Above 64 RPM the seed is thrown onto the ground instead of landing in its tray.
- **Full tray.** A tray that is full stalls the drum until you empty it.
- **Soft items** that are not grain are shredded to nothing.
- **Hard items** jam it. Anything harder than the teeth (hardness 4) stops the drum and wears the teeth by the excess. Worn teeth work slower, down to 40% of full speed, and never stop. Running the drum never wears them.
- **Clearing a jam.** Right-click with an empty hand: the stuck piece comes back to you.

Straw and chaff are loose fibers that burn and insulate; straw also pads a [bed](Bed.md).
