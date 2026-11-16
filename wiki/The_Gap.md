# The Gap

The Gap is the dimension above the Overworld's sky: a void with no gravity, no air, great cold and the sky's radiation. A craft reaches it by climbing out of the air.

## Getting there and back

Air in the Overworld thins with height and is gone by y 312 (see [air density](Vehicles.md#air-density)). A [vehicle](Vehicles.md) that stays in air of zero density for about a second leaves for the Gap, and the screen clears from black. Below the Gap's floor the vehicle returns to the Overworld, and the screen clears from white-orange. Below the floor, a player standing on no vehicle falls out into the Overworld too: same x and z, at y 300 in thin air, with the same speed and fall distance, and on fire for 10 s. A player on a vehicle goes with it. Wings, [propellers](Propeller.md) and air-breathing [thrusters](Thruster.md) fade as the air thins and give nothing at the top.

## Living in it

- **Weight.** You have no weight, and you keep your speed.
- **Air.** There is none. Breathe from a [Respirator](Respirator.md) with an air cylinder, or from a sealed cabin with an [Air Valve](Air_Valve.md) (see [thin air](Hazards.md#thin-air)).
- **Heat.** It is cold, and a vessel keeps all the heat it has. A [Radiator Panel](Radiator_Panel.md) sheds it.
- **Radiation.** Your dose rate gains the sky's own. See below.
- **Map.** Below the craft the explored Overworld shows as a flat map at the chunks' real places, one pixel for each chunk, in the colors of the map.

## Radiation

In the Gap the sky's radiation adds to the [dose](Radiation.md) you carry, once a second. Armor shields it by density, as for any source, and so do the dense plates of a hull around you: more open sides to the sky means more dose.

## Commands

Operators can move a craft by hand: `/feedback space send`, `/feedback space back` and `/feedback space scale <k>` act on the craft you stand on or look at (send, back, and set its scale).
