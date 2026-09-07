# Fix 28 — P&W + CEE Bridge Foundation

## Implemented foundation

- Native P&W catenary endpoint at the centre of the external P&W-facing side.
- Separate external CEE electrical node on the opposite side.
- P&W wires remain native P&W wires; the CEE compatibility graph is internal.
- Existing saved compatibility links are migrated to `ohecompat:paw_shadow`,
  which the client does not render as a CEE catenary.
- Exact endpoint matching only; a nearby P&W or CEE node cannot connect merely
  because it is close.
- P&W graph topology is mirrored edge-for-edge, so independent P&W feeder
  sections are not joined by the bridge. Native P&W feeders, catenary holders,
  and sectioning posts remain the source of their own topology.
- The CEE/P&W connector sides now have two fixed physical-side indicators:
  green on the CEE (south) side and blue on the P&W (north) side. They rotate
  with the placed component, not with the player camera.
- The OHE Power Bridge item name, two-block ceramic/copper insulator, and
  wrench removal of the full assembly are preserved.

## Explicitly deferred

C1, C2, C3, C4, and D-specific compatibility are reserved for the next stage.
This fix does not alter their existing blocks or introduce new feeder blocks.

## Test order

1. Build and launch the client.
2. Open an existing test world once, then restart/reload it to complete any
   saved shadow-wire migration.
3. Test a P&W feeder through a P&W catenary to the bridge: only the native P&W
   wire should be visible.
4. Test the separate CEE side: its native CEE wire must remain visible.
5. Test a native P&W sectioning arrangement: the bridge must not join the two
   separate P&W sections.
