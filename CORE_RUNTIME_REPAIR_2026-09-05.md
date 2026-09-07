# Core runtime repair — 2026-09-05

## Confirmed from the user's runtime screenshots

1. OHE Power Bridge visual terminal/node textures were rotated onto the wrong faces.
2. P&W catenary wire still failed to attach to the OHE Power Bridge.
3. The three-block insulator/bridge assembly still needed robust whole-assembly break behavior.
4. CEE compatibility node markers remained visible on P&W connection points.

## Root causes found in the version-locked P&W API

### 1. Bridge P&W node was moved to the face and then given a face offset
P&W `BlockConnectorNodeData` intentionally creates its graph node at the connector block's integer position. The physical wire attachment is represented separately by `ConnectorDataProvider#getAttachOffset()`.

The previous compatibility mixin changed the bridge graph node itself to the external face and then supplied the same face offset again. That double-applied the offset and could make the P&W edge validation fail.

The bridge is now allowed to use P&W's native `BlockConnectorNodeData` node creation. Its `IWireConnector#getConnectorData()` supplies the exact opposite-face offset.

### 2. Wrong blockstate rotation
The bridge model's neutral orientation has CEE terminal on SOUTH and P&W terminal on NORTH. Therefore:

- SOUTH = model rotation 0°
- WEST = model rotation 270°
- NORTH = model rotation 180°
- EAST = model rotation 90°

The old state table used X rotations for horizontal states, which moved terminal textures/geometry vertically. The horizontal variants are now corrected to Y-only rotations.

### 3. Insulator was a component but could still drop separately
The insulator blocks are internal pieces of the OHE Power Bridge assembly. Their drops are now suppressed. Breaking either insulator finds the bridge two/one blocks above and destroys the bridge; the bridge's own destruction removes both insulators and supplies the single bridge drop.

### 4. CEE detached markers
CEE must retain compatibility nodes for electrical simulation, but those nodes are not physical OHE objects. New `ohecompat:` node data is suppressed at creation and a render-time purge removes stale compatibility entries before CEE's renderer draws them.

## Intended runtime result

`P&W wire -> P&W connector -> OHE Power Bridge -> P&W connector -> P&W wire`

with CEE electrical compatibility hidden internally.

The four horizontal orientations must keep the CEE and P&W terminals exactly opposite (180°) while the entire three-block assembly rotates together.
