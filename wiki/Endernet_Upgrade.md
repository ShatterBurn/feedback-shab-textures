# Endernet Upgrade

An antenna bonded to a [Data Port](Data_Port.md) pair. Fitted to an Electric Controller or a Computer, it makes that block the other end of the pair, so it reads whatever is linked to the far Data Port as though it were linked to itself. Used on a robot or in the air in a suit, it sends that robot's senses or the suit's scan to the far port.

## Crafting

Put one bonded [Data Port](Data_Port.md) and one metal plate in the crafting grid, in any arrangement. The port is used up; the upgrade carries its bond, and the port's partner stays a port. A Data Port with no bond makes nothing.

## Using it

- **Use it on an [Electric Controller](Electric_Controller.md#endernet-upgrade) or a [Computer](Electric_Controller.md#computer).** It fits there; one fits per block. An upgrade with no bond does nothing, and its tooltip says so.
- **Use it on a [robot](Robots.md#endernet-leash)** to leash it to the bonded port, or **in the air while wearing an [Exosuit](Exosuit.md#sharing-a-scan)** to share the suit's scan with it.
- A fitted upgrade draws a small standing current from the block's line. The pair carries while both ends are powered.
- A [Crowbar](Crowbar.md) takes it off, and it comes back as an upgrade with its bond.
- The [Data Port](Data_Port.md#endernet) at the other end pays for each crossing; an upgrade has no tank of its own.

## Reading across the pair

Link a sensor or machine to the far Data Port with the [Data Connector](Data_Connector.md#linking-nodes), and an Input card in the controller's program can read it. A Computer shows the pair and what is linked beyond it in the Network app, and can flip what switches there ([Network](Electric_Controller.md#disk-apps)).

## Channel and address

A fitted upgrade on a Computer has a **channel**, from 1 up to the number its circuit's [Resonator](Resonator.md) separates (1 with no circuit, never more than 16), and an **address** from 1 to 99, or none. You set both in the [Network app](Electric_Controller.md#disk-apps). Computers with an upgrade on one channel in one dimension share that channel's allowance with the [Data Ports](Data_Port.md#channels) tuned to it.

## Sending between Computers

The Network app lists up to 12 other addressed Computers on your channel in the same dimension, each with its address and distance. From it you send:

- **A message** of up to 120 characters.
- **A file**: a program from one of your drive bays, up to 64 nodes long. It lands as a copy on a blank Floppy Disk or USB Drive (see [Media](Programming.md#media)) in a bay of the receiving Computer.

A packet costs one signal for every 16 bytes, at least one. The signals use the channel's allowance, and the sender's pair partner [Data Port](Data_Port.md) pays teleportant and bandwidth for them over the distance. A Computer with no address neither sends nor is listed.

A packet is **lost** when the channel is saturated, the port cannot pay, the far inbox is full (16 mails), or a file finds no blank medium. The sender's console says which. Nothing arrives altered, and what was paid stays paid.

Every other Computer on the channel overhears: it receives the text of a message, marked as overheard, and notes a file's size. Mail is kept in the inbox; mail addressed to the Computer also lands in its console.
