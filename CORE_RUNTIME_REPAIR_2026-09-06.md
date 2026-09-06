# Core runtime repair — 2026-09-06

## Runtime issues addressed

1. **P&W wire hit on the three-block bridge assembly**
   - P&W wire selection now resolves clicks on either internal insulator to the real OHE Power Bridge connector block.
   - The actual P&W endpoint remains the bridge's opposite-face `BasicConnectorDataProvider` offset.

2. **CEE detached-node markers**
   - CEE detached-node rendering is cancelled at `DetachedNodeRenderer.render`.
   - This is deterministic for old/new compatibility nodes and does not delete the server-side electrical nodes.

3. **Invisible CEE shadow wire**
   - `WireRenderer.renderWire` and `forceRenderWire` cancel only `PawShadowWireType.SHADOW`.
   - The electrical shadow connection remains on the server. P&W remains the visible OHE geometry.

4. **Stray bridge terminal texture**
   - Removed the asymmetric west-face CEE terminal element from the bridge model.
   - Canonical terminal layout is now CEE on the FACING side and P&W on the exact opposite side after the blockstate Y rotation.

## Existing assembly rules retained

- Four horizontal placement directions use Y-only model rotation.
- The CEE and P&W terminals remain exactly opposite.
- The two insulators inherit the bridge facing.
- Breaking any assembly part is intended to remove the complete three-block assembly and produce one bridge item.
- P&W owns visible wire geometry; CEE owns hidden electrical simulation.
