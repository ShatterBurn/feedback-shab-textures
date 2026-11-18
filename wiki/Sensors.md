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

Place a sensor with its face against what you want to watch, then link it to a Controller with the [Data Connector](Data_Connector.md), which costs fine wire by distance, and read it with an Input card. The reading is quantized: a probe reports a crop as unripe, a quarter, half, three quarters or ripe, never a finer figure.

A Ripeness Probe reads any block that has an age, so a modded crop works too. A Photocell reads its own lamp's light, so a lamp it switches can switch it back; keep the lamp out of its sight. An Observer reports a change a moment late. It is the Feedback version of the vanilla observer: a vanilla observer found in the world turns into an Observer. Where vanilla's observer pulses redstone, a controller card reads this one.

A Ripeness Probe, Photocell, Presence Plate or Fill Gauge can also be seated in a [Pivot Mast](Pivot_Mast.md) as a sensing head, reading the cell under the arm tip.

## Crafting

**Photocell**, at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)); it spends no fasteners:

```
P G P
W X W
```

P is a Wrought Iron Plate, G is Glass, W is [Copper Wire](Wire_Drawer.md) and X is Galena.

**Fill Gauge, Ripeness Probe, Presence Plate and Observer**, at the Engineering Table:

```
P X P
W C W
```

P is a Wrought Iron Plate and C is a [circuit](Basic_Electronic_Circuit.md). W is Copper Wire for the Fill Gauge and Copper Fine Wire for the others.

| Sensor | X |
| --- | --- |
| Fill Gauge | Copper Plate |
| Ripeness Probe | Copper Rod |
| Presence Plate | Pivot Arm (Short) |
| Observer | Photocell |

Each takes a screwdriver in the tool row, 2 screws ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)). The [Data Sensor](Sculk_Culture.md#data-sensor) is made the same way, with a Copper Plate for P and a Sculk Sensor for X.
