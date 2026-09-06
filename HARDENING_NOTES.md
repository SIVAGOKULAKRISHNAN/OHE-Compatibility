# v0.2 hardening

## Current problems solved
1. The OHE switch now derives from the actual CEE HVSwitchBlock, reusing native CEE node layout, switching logic and electrical device type. A dedicated block entity type hosts the inherited CEE HVSwitchBlockEntity.
2. E now derives from the actual CEE TransformerBlock, so the compact substation is a physical adaptation of the existing CEE transformer behavior.
3. F is a distinct new OHE transfer/booster block built on the CEE transformer simulation.
4. P&W pantographs are exposed to CEE's native IPantographBlock capture path by a conditional integration mixin, avoiding a second train electrical system.
5. P&W catenary is mirrored into CEE's InfrastructureSavedData as an electrical shadow. P&W remains the physical source of truth.
6. Shadow nodes are labelled with P&W UUIDs, so the mapping can be recovered after world reload.
7. Stale shadow connections/nodes are removed when P&W topology disappears.
8. C1-C4 are separate craftable block types and have a server-side link record to a P&W wire collision.
9. An isolated section removes the shadow electrical connection and creates two live sides with an intentionally dead gap. The P&W physical wire remains continuous for pantograph passage.
10. No artificial 40/50 km distance rule exists.
11. Invalid placeholder recipes were removed; recipes will be added only after exact item IDs are confirmed in a live registry test.

## Remaining runtime-only risks
- NeoForge/Gradle compilation must be run in a real 1.21.1 development environment.
- P&W collision data and CEE shadow simulation must be exercised with real world saves.
- A renderer should be registered for the CEE-derived switch/transformer block entities for the final visual pass.
- Section left-feed/right-feed and controlled paralleling need a live CEE node-controller layer; the dead-gap isolation is implemented at the shadow level first.
