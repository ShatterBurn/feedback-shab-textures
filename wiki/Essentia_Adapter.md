# Essentia Adapter

A one-way fitting that turns essentia from a Thaumaturge essentia store into gas. Set beside a reservoir or jar, it fills its own tank with air, steam or hot air. It needs [Thaumaturge](Mod_Compats.md#flux-scrubbing-and-rust); without that mod it does nothing.

## Crafting

```
  T  
S S S
S V S
```

T is Glass Tubing, S is a Wrought Iron Plate and V is a [Valve](Valve.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 screws ([Fasteners](Fasteners.md)). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

## Using it

Place it touching an essentia store, on any side. The gas leaves through its fluid port: a [pipe](Pipe.md) or a [gas tank](Gas_Tank.md) beside it draws the gas out. Nothing can be pushed into it.

## How it works

About once a second it draws up to **4 essentia** from the store, and each unit fills its **4000 mB** tank with **40 mB** of gas.

| Aspect | Gas |
|---|---|
| Aer | Air |
| Aqua | Steam |
| Ignis | Air at 600 Tu |

Every other aspect stays in the store untouched. The adapter never puts anything back: there is no way from gas into essentia.

## Misuse and hazards

A tank that cannot hold what was drawn spills the rest into the chunk as flux, **0.1** for each essentia unit lost. A full tank on a store that keeps feeding it fouls the aura. Keep the gas drawn off, or let a [Scrubbing Bed](Scrubbing_Bed.md) take the flux back out; flooded chunks [rust bare machines](Mod_Compats.md#flux-scrubbing-and-rust).

## See also

- [Essentia Vent](Essentia_Vent.md): bleeds a full essentia store into the chunk as flux.
