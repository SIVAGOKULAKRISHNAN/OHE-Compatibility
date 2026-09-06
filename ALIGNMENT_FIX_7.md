# Alignment Fix 7

## Purpose
Fix the CEE shadow OHE endpoint alignment with native Create: Pantographs & Wires (P&W) OHE.

## Root cause
P&W `WireNode#getPos()` identifies the connector/node block position. For connector blocks such as cantilevers, the physical OHE wire endpoint is offset from that node position. The previous compatibility layer used the node position directly for the CEE shadow endpoint, causing the CEE line to be displaced from the P&W OHE line.

## Fix
For every P&W edge endpoint, the compatibility layer now reads `WireEdge#getWireConnectionData()` and uses `BasicConnectorDataProvider#getAttachOffset()`:

`physical OHE endpoint = WireNode position + connector attach offset`

The CEE shadow nodes, CEE shadow wire length, section midpoint, and section gap axis are all based on those physical endpoints.

The signature also includes the calculated endpoint positions, so connector geometry changes trigger a resync instead of leaving stale alignment.

## Scope
- CEE remains the electrical/shadow network authority.
- P&W native OHE geometry is not modified.
- P&W native extra/tension wire is not removed or replaced.
- The compatibility wire uses the exact P&W OHE attachment coordinates for its endpoints.
- Section electrical isolation continues to operate on the shadow CEE network.

## Build note
The project could not be built in the restricted environment because Gradle 8.13 is not cached and the wrapper attempted to download it from `services.gradle.org`. Build on Windows with:

`gradlew.bat clean build`
