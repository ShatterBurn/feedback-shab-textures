# The Shelf

The Shelf is the dimension above the Overworld's sky: a void with no gravity, no air, great cold and the sky's radiation. A craft reaches it by climbing out of the air.

## Getting there and back

Air in the Overworld thins with height and is gone by y 312 (see [air density](Vehicles.md#air-density)). A [vehicle](Vehicles.md) that stays in air of zero density for about a second leaves for the Shelf, and the screen clears from black. Below the Shelf's floor the vehicle returns to the Overworld, and the screen clears from white-orange. Wings, [propellers](Propeller.md) and air-breathing [thrusters](Thruster.md) fade as the air thins and give nothing at the top.

## Living in it

- **Weight.** You have no weight, and you keep your speed.
- **Air.** There is none. Breathe from a [Respirator](Respirator.md) with an air cylinder, or from a sealed cabin with an [Air Valve](Air_Valve.md) (see [thin air](Hazards.md#thin-air)).
- **Heat.** It is cold, and a vessel keeps all the heat it has. A [Radiator Panel](Radiator_Panel.md) sheds it.
- **Radiation.** Your dose rate gains the sky's own. See below.
- **Map.** Below the craft the explored Overworld shows as a flat map at the chunks' real places, one pixel for each chunk, in the colors of the map.

## Radiation

In the Shelf the sky's radiation adds to the [dose](Radiation.md) you carry, once a second. Armor shields it by density, as for any source, and so do the dense plates of a hull around you: more open sides to the sky means more dose.

## Commands

Operators can move a craft by hand: `/feedback space send`, `/feedback space back` and `/feedback space scale <k>` act on the craft you stand on or look at (send, back, and set its scale).
