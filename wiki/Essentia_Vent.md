# Essentia Vent

A relief valve with a built-in gauge for Thaumaturge's essentia stores. Set against a reservoir or other essentia store, it opens when the store is nearly full and lets essentia out. It needs [Thaumaturge](Mod_Compats.md#flux-scrubbing-and-rust); without that mod it does nothing.

## Crafting

```
  G
S K S
S V S
```

S is a Wrought Iron Plate, G is a Pressure Gauge, K is a Coil Spring of any material and V is a [Valve](Valve.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

Place it touching an essentia store, on any side. **Right-click with an empty hand** to read it in words: open, closed, or touching no store.

## How it works

The vent reads how much essentia the store beside it holds, about once a second. Above a high fill it opens and draws essentia out, with a hiss and a puff of vapor. It stays open until the store has fallen well below that fill, and only then reseats, so it does not flutter on the line.

Nothing it bleeds is lost: the essentia goes into the chunk as flux. A [Scrubbing Bed](Scrubbing_Bed.md) in the chunk takes that flux back out; without one, the chunk's flux rises, and flooded chunks [rust bare machines](Mod_Compats.md#flux-scrubbing-and-rust).
