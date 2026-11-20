# Bearing Sensor

A block that reads which way the End lies from a craft in the [Gap](The_Gap.md#the-end-by-rocket), and how far, and hands the figure to a [Controller](Controller.md) so a program can steer toward it.

## Crafting

Made at the [Engineering Table](Engineering_Table.md) with a screwdriver and 3 screws, on an electrical volume of 2:

```
P G P
C W C
C P C
```

P is a wrought iron plate, G is glass (the lens at the front), C is a [Photocell](Sensors.md) (four, one in each quadrant) and W is copper wire (it joins them).

## Using it

Place it on the craft with its front the way it faces. **Sneak-right-click** steps what it reads; a strip under the crosshair lists the readings with the current one lit:

| Reading | What it is |
| --- | --- |
| **Yaw** | Degrees to turn right to face the End (negative is left), to the nearest two. |
| **Pitch** | Degrees to tip the front up to face the End (negative is down), to the nearest two. |
| **Distance** | Blocks to the End, in the Gap's own blocks, to the nearest four. |

It reads only in the Gap. In any other dimension it reads zero.

Link it to the Controller with a [Data Connector](Data_Connector.md) and read it with an *Input* card on the [Programmer](Programming.md). The card carries that kind of reading, so a program cannot compare distance with an angle. A fitted circuit or a poor wire changes how fine the reading is, as for the [Flight Sensor](Instruments.md#flight-sensor).

## How it works

The End is a fixed point in the Gap, marked in the sky by the gas giant. The sensor measures from its own place on the craft to that point and compares it with the way its front points. A coarse reading costs a longer trip, not a refusal: a craft that is a few degrees off still closes, and the next reading corrects it.
