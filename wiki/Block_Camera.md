# Block Camera

A traffic camera on a steel pole, found standing in the world and most often beside a road. It watches for players carrying Bad Omen and reports them the way a village would, once.

## Obtaining

Block Cameras and their Camera Pole lengths cannot be crafted. They stand in the overworld, rarely in open country and hundreds of times more often beside roads: dirt paths, cobblestone, stone bricks and rails. Poles are 3 to 5 blocks tall. Both blocks are in the operator creative tab.

Breaking the camera, or any pole length under it, brings the whole stack down. The camera drops, each by its own chance, whether or not it ever fired:

- a [Battery](Battery.md), about half the time
- 1 to 3 copper wire, usually
- a [Trace Board](Trace_Board.md), more often than not
- an optical glass lens, a little under half the time

## How it works

A player carrying Bad Omen who comes within 16 blocks is locked on to. The head turns to face them, a red lamp blinks for 3 seconds and a shutter click sounds.

If they still carry Bad Omen after the blink, the camera reports them. Their Bad Omen becomes Raid Omen (30 seconds) and the vanilla raid comes to the camera, or to the village center if a village is within 64 blocks.

The camera reports once. Afterwards its lens is dark and it never fires again. If the player moves 32 blocks away or loses the omen before the report, the camera stands down and can lock on again.

The camera does nothing on Peaceful, with raids turned off by the game rule, or in dimensions without raids.
