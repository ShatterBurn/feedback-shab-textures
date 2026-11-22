# Winch

A drum on a shaft in a frame. Tie a fixed line to it and, turned, it draws in whoever hangs on the line, or pays them out.

## Crafting

```
P _ P
R S R
P _ P
```

P is a Wrought Iron Plate, R a Wrought Iron Rod and S a [Shaft](Shaft.md).

## Using it

Set it on a [rotation](Rotation.md) shaft: the shaft runs through both ends of the block. Tie a fixed line to it with a [grapple](Grapple.md#fixed-lines) as you would to a [Piton](Piton.md); the other end is a piton or another winch.

## How it works

A drive turning one way draws whoever hangs on the line toward the winch, faster the faster it turns (about 0.05 blocks a second for each RPM). The other way pays them out.

What hangs on the line is a load on the shaft, in proportion to its mass. A weak drive stalls under it: the line stays where it is and takes the weight, and it strains hard. A stronger drive lifts.

The winch is bolted fast and holds any load. The line is rated by its material ([The line has a rating](Grapple.md#the-line-has-a-rating)), and breaking the winch cuts its line.
