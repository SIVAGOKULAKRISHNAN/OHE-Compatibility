# OHE Power Bridge — first in-game test

Target: Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10 / CEE 1.1.1 / P&W beta-0.2.3-C6.

## What this first component does

`ohecompat:ohe_power_bridge` is a placeable CEE electrical connector with one stable CEE node.
It automatically joins that CEE node to the nearest P&W shadow catenary node within 4.5 blocks.

The hand-off uses the compatibility mod's invisible CEE shadow wire. Therefore:

- The P&W OHE wire remains the only visible OHE wire.
- No ordinary CEE wire is rendered between the bridge and the P&W OHE.
- The player can still use a native CEE wire on the bridge as the feeder connection.
- P&W cantilever geometry, wire tension, pantograph contact and adjustable cantilever behavior are not replaced.
- CEE remains the electrical/train-power authority.

## Test

1. Start the dev client with CEE + P&W + this mod.
2. Make a normal P&W catenary span using native P&W cantilevers/wires.
3. Make sure the P&W wire has a real connected endpoint near the location where the bridge will be placed.
4. Give the component:

   `/give @s ohecompat:ohe_power_bridge`

5. Place the OHE Power Bridge within about 4 blocks of the P&W catenary endpoint.
6. Connect a native CEE feeder wire from a CEE power source to the OHE Power Bridge node.
7. Wait a few ticks. The bridge should silently connect its CEE node to the P&W shadow catenary network.
8. Verify visually that there is **no second ordinary wire** between the bridge and P&W OHE.
9. Put a P&W pantograph under the P&W wire and verify that the CEE train electrical system can see the energized OHE.
10. Test the CEE electric motor on the train.

## Important scope for this test

This is deliberately a power-path test first. It does not attempt to make a CEE native catenary wire visually become a P&W OHE wire, and it does not require a CEE cantilever holder to accept a P&W wire. Those were the failure points seen in the earlier mixed-wire tests.
