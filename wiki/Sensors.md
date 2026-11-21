# Sensors

Five blocks that look at the face they were placed against and report what they see to a [Controller](Controller.md). They sense and control nothing. Right-click one with an empty hand to read it as a percentage.

| Sensor | Reads | Reads in |
| --- | --- | --- |
| Ripeness Probe | how grown the crop in front is, from the crop's own age | quarters |
| Photocell | the brighter of daylight and block light in front | fifths |
| Presence Plate | whether a creature or a dropped item stands in front | on or off |
| Fill Gauge | how full the inventory or tank in front is | eighths |
| Observer | a change in the state of the block in front, for half a second after it | on or off |

## Using them

Place a sensor with its face against what you want to watch, then link it to a Controller with the [Data Connector](Data_Connector.md), which costs fine wire by distance, and read it with an Input card. The reading is quantized, as the table lists: a probe reports a crop as unripe, a quarter, half, three quarters or ripe. A quiet circuit in a Fill Gauge or Ripeness Probe cuts the reading into up to about twice as many bands.

A Ripeness Probe reads any block that has an age, so a modded crop works too. A Photocell reads its own lamp's light, so a lamp it switches can switch it back; keep the lamp out of its sight. An Observer reports a change a moment late. It is the Feedback version of the vanilla observer: a vanilla observer found in the world turns into an Observer. Where vanilla's observer pulses redstone, a controller card reads this one.

A Ripeness Probe, Photocell, Presence Plate or Fill Gauge can also be seated in a [Pivot Mast](Pivot_Mast.md) as a sensing head, reading the cell under the arm tip.

## Crafting

**Photocell**, at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)); it spends no fasteners:

```
P G P
W X W
```

P is a Wrought Iron Plate, G is Glass, W is [Copper Wire](Wire_Drawer.md) and X is Galena.

**Fill Gauge and Ripeness Probe**, at the Engineering Table:

```
P X P
W C W
```

P is a Steel Plate and C is a [circuit](Basic_Electronic_Circuit.md), which the finished sensor keeps. X is a Brass Plate and W is Copper Wire for the Fill Gauge; X is a Copper Rod and W is Copper Fine Wire for the Ripeness Probe.

**Presence Plate and Observer**, at the Engineering Table:

```
P X P
W   W
```

P is a Wrought Iron Plate and W is Copper Fine Wire. X is a Pivot Arm (Short) for the Presence Plate and a Photocell for the Observer.

Each of the four takes a screwdriver in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)). The [Data Sensor](Sculk.md#data-sensor) has its own grid and takes Electrical Treatise II: Coils and Magnets.
