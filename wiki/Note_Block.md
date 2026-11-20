# Note Block

A note block plays a note when it gets a redstone signal or is punched. You build it from a **note body** and one or two **noisemakers**; the noisemakers make the sound.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Note Block* in the type row) and build it from its parts:

- **Note body**: its pitch, 0 to 24 semitones, is the note the block plays when placed.
- **Noisemaker**: its shape is the instrument. Its material sets the sound: a harder material rings louder and a denser one plays lower.
- **Second noisemaker** (optional): sounds together with the first.

Each part has its own icon, tinted by its material, and every instrument has its own noisemaker icon. The placed block keeps the look of a vanilla note block.

The instruments are Harp, Basedrum, Snare, Hat, Bass, Flute, Bell, Guitar, Chime, Xylophone, Iron Xylophone, Cow Bell, Didgeridoo, Bit, Banjo and Pling.

## What vanilla does

A note block plays the instrument that matches the block under it, and using it steps the pitch through 25 notes.

## What Feedback does

A note block with no noisemaker is silent. The noisemakers set the instrument and the body sets the note.

- **Use with an empty hand** to step the note and hear it.
- A **rising redstone signal** or a **punch** plays it. The block above it must be air.

Vanilla note blocks found in the world turn into Feedback note blocks and keep their pitch.
