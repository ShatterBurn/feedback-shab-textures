# Drill Press

A spinning [Drill Bit](Machine_Heads.md) over a workpiece. The shaft keeps the bit turning, but **nothing gets drilled until you push it**. It's hand-fed, one push per click.

The first part cut on a drill press enters the [Machine Age](Ages.md) for you and any player nearby.

## Crafting

```
  S
  B
I I I
```

I is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md). It needs Mechanical Treatise I: Engines, Flywheels and Dynamotors and Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

The bit is a [machine head](Machine_Heads.md): draw it at the Blueprint Table and put it together in a crafting grid.

## Using it

1. Put it on a shaft line (a bronze shaft stub shows on each side; any side couples) and get it turning. It's a **35 Su** load.
2. **Right-click with a Drill Bit** to seat it; the bit it replaces comes back.
3. **Right-click with a workpiece** to lay it on the base plate under the bit.
4. **Right-click with an empty hand** to push the bit down once. Each push cuts a little.
5. **Right-click with an item** on an occupied press to take the workpiece back. With the workpiece out, an empty hand takes the bit.

**Sneak-right-click** with an empty hand switches between **slow feed** and **fast feed**.

The bit has to be spinning. Pushing a stopped bit does nothing, and a press with no bit does nothing.

## What it makes

**Plate → Drilled Plate → Swarf**

Keep pushing after the holes are done and you drill the plate away into swarf. Bits of swarf also fly off the top of the press while you cut (15% of cuts).

It also drills most [gun parts](Gun_Parts.md): a turned rod into a **Gun Barrel** (and wider), an ingot into a **Receiver**, a ground ingot into a **Cylinder**, a rod into a **Magazine Tube**, a magazine tube into a **Muzzle Brake**, a nugget into a **Relief Vent**.

An ingot of any metal or polymer drills into a [Pipe](Pipe.md) of that material.

Two Wrought Iron Drilled Plates make a **Bullet Mold** (see [Ammunition](Ammunition.md#bullets)).

## Cutting is different from hammering

Hammering is about **force**. Cutting is about the **tool being harder than the metal**. The drill bit's **edge hardness** decides what it cuts: a bit with an edge harder than the metal cuts it, one that is not does nothing but wear. Steel is 15, hardened steel 69, netherite 130 and tungsten carbide 157. A harder edge, or the [Grinding Wheel](Grinding_Wheel.md), reaches the hard stock. JEI shows each metal's hardness under *"Tool must beat"*.

## Slow vs. fast feed

- **Slow** (22 St) cuts less per push and never jams.
- **Fast** (48 St) cuts more than twice as much per push, but **one push in twenty** the bit **binds**: you hear a clank and that push is wasted. Nothing is damaged.

Each push removes strength ÷ hardness in work. A drilled steel plate (about 13 Wu) is about 5 fast pushes or 13 slow ones.

## Circuit

A [circuit](Basic_Electronic_Circuit.md) is optional; the press works without one. **Right-click the press with a circuit** to seat it. Taking it out with an empty hand gives the bit first, then the circuit. A quiet circuit lowers the chance that a fast push binds the bit; a noisy one helps little. The press has no heater, so a tube in the circuit runs cold.

## Wear

Pushing when the bit can't cut (metal too hard, or nothing left) wears the **bit**, not the press. See [Machine Heads](Machine_Heads.md#wear).
