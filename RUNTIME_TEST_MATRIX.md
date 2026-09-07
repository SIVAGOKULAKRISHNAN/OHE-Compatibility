# Runtime test matrix

## Basic loading
- [ ] Game starts with CEE + P&W + OHE Compatibility.
- [ ] Dedicated server starts.
- [ ] Client starts.
- [ ] Removing P&W/CEE produces a clear dependency error (they are required for this build).

## Existing behavior preservation
- [ ] CEE-only OHE behaves exactly as before.
- [ ] P&W-only OHE behaves exactly as before.
- [ ] Existing CEE train power still works.
- [ ] Existing P&W pantograph still animates/contact-detects P&W wires.

## P&W -> CEE
- [ ] P&W pantograph is captured as a CEE TrainPantographEntry.
- [ ] P&W catenary edge creates a CEE shadow catenary connection.
- [ ] Train can draw CEE current from the shadow network.
- [ ] P&W wire remains physically unchanged.

## C1-C4
- [ ] C1 attaches to supported cantilever arrangement.
- [ ] C2 detects direct-in-span placement.
- [ ] C3 uses short-neutral geometry.
- [ ] C4 combines C3 with cantilever placement.
- [ ] Each side can be isolated.
- [ ] Pantograph can pass mechanically.
- [ ] Left-feed/right-feed behavior works.
- [ ] Parallel mode is disabled by default.

## D
- [ ] CEE HV switch animation/arc state works.
- [ ] Create wrench works.
- [ ] Two OHE endpoints map to CEE nodes.
- [ ] Suitable CEE and P&W support mounting works.

## E/F
- [ ] E uses CEE transformer electrical behavior.
- [ ] E compact physical arrangement does not create a second simulation.
- [ ] F transfers power through CEE transformer behavior.
- [ ] No fixed distance range is used.

## Persistence
- [ ] Save/reload preserves shadow topology.
- [ ] Removing a P&W wire removes its shadow connection.
- [ ] Removing a P&W node removes orphan shadow nodes.
- [ ] No duplicate shadow nodes after repeated syncs.
