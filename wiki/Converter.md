# Solid-State Converter

Steps a line's band down one, down two or up one, with a Converter Tap on the other side. It changes band with no moving parts, at up to about 95 percent efficiency.

## Crafting

```
C E C
A A A
```

**Solid-State Converter**: C is an [Induction Coil](Induction.md#induction-coil), E is a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which the finished converter keeps, and A is an Aluminum Plate.

```
C R C
A A A
```

**Converter Tap**: C is an [Induction Coil](Induction.md#induction-coil), R is a [Rectifier](Rectifier.md) and A is an Aluminum Plate.

Both are crafted at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 screws ([Fasteners](Fasteners.md)). They need Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

The **Heatsink** is crafted in the plain [Crafting Table](Crafting_Table.md):

```
A A
A A
A A A
```

**Heatsink**: A is an Aluminum Plate.

## Using it

1. Wire the line you step **from** to the converter.
2. Set a **Converter Tap** against the converter and wire the line you step **to** to the tap.
3. **Right-click the converter with an empty hand** to cycle its step (a strip under the crosshair lists the options with the current one lit): *down one band*, *down two bands*, *up one band*. A step that would leave the list of bands (down from LV, up from Trunk) gives nothing.

The two lines never share a run: the converter is a load on the first, and the tap is a source on the second. A tap with no converter beside it supplies nothing.

## How it works

The converter passes a fixed amount of power, so a lower band out means more amps out, and a higher band out means fewer. Asked for more than that, it gives no more. A starved input line shrinks the output in proportion.

With a full-speed circuit about 5 percent of the power is lost, and the loss is **heat in the converter block**. A hot converter loses condition, down to a floor, and a worn one wastes more of what passes. It never refuses a line and is never destroyed.

### The circuit

The converter's [circuit](Basic_Electronic_Circuit.md) is built in and cannot be taken out. Its speed sets the efficiency: about 95 percent with a circuit at full speed, down to about 85.5 percent with a speed-0 circuit. Its traces set the amps the converter passes: printed traces carry little, etched traces more, and a converter with no circuit passes nothing. A placed converter keeps its circuit, and so does the item it drops when broken. The [Engineering Table's Repair tab](Engineering_Table.md#repair-tab) takes it apart and returns the circuit.

### Heatsinks

Set up to four aluminum **Heatsinks** against the converter. Each sheds heat to the air, so the converter runs cooler under load.

## See also

- [Electricity](Electricity.md#changing-band) for the other way to change band, a motor-generator set.
- [Substation](Substation.md) for a converter as the head of a branch.
