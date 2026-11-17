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

Link a sensor or machine to the far Data Port with the [Data Connector](Data_Connector.md#linking-nodes), and a Sensor card in the controller's program can read it. A Computer shows the pair and what is linked beyond it in the Network app, and can flip what switches there ([Network](Electric_Controller.md#disk-apps)).
