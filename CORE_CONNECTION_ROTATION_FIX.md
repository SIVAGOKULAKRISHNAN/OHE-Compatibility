# Fix 30 Core Connection + Rotation Repair

## Runtime problems addressed

The first Fix 30 runtime test showed that the OHE Power Bridge could be placed, but P&W did not reliably accept it as a catenary endpoint and the bridge endpoint/orientation could become inconsistent.

### Root cause found in the P&W API

`BlockConnectorNodeData.toWorldPos()` returns the block origin by default. P&W calls this method while validating the second wire point and when processing the wire point data. The compatibility layer previously corrected the graph node position, but did not correct `toWorldPos()`.

The compatibility hook also created the corrected P&W graph node without persisting P&W's required `WireConnectorBlockEntity -> NodeId` association.

## Changes

1. `PawCeeHolderBlockConnectorNodeDataMixin`
   - Adds a `toWorldPos()` hook for the OHE Power Bridge.
   - Returns the exact P&W external face contact point.
   - Preserves P&W `WireConnectorBlockEntity` node IDs.
   - Reuses an existing stored P&W node when available.
   - Creates and stores a new `NodeId` when needed.

2. The bridge keeps the four Minecraft horizontal orientations:
   - NORTH
   - EAST
   - SOUTH
   - WEST

3. The P&W endpoint remains exactly opposite the CEE endpoint for every orientation.

4. The complete three-block assembly keeps one shared orientation.

## Expected endpoint table

| Bridge FACING | CEE endpoint | P&W endpoint |
|---|---|---|
| NORTH | north face | south face |
| EAST | east face | west face |
| SOUTH | south face | north face |
| WEST | west face | east face |

The visible P&W wire remains P&W-owned. CEE compatibility data remains internal/electrical and must not create a visible CEE wire or detached compatibility marker.

## Build status

The supplied offline environment has Gradle 8.13 available, but the NeoForge ModDev plugin artifact is not cached locally, so a complete Gradle build cannot be truthfully reported from this environment. The source/resource validation succeeds and the final Windows build must be run in the user's development environment.
