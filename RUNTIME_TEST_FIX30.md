# Fix 30 Runtime Test Order

Use Minecraft 1.21.1 + NeoForge 21.1.219 + Create 6.0.10-280 + the pinned CEE/P&W versions from `DEPENDENCIES.md`.

1. Launch with CEE + P&W + this mod. Confirm no mixin crash.
2. Create a flat test world.
3. Place an OHE Power Bridge. Confirm the assembly is exactly three blocks tall.
4. Place the bridge while facing north/east/south/west. Confirm CEE and P&W terminals remain exactly opposite.
5. Connect a native P&W wire to the P&W side. Confirm no visible CEE node marker appears at the endpoint.
6. Confirm no visible CEE shadow wire appears along the P&W wire.
7. Break the top bridge block. Confirm all three blocks disappear and exactly one bridge item drops.
8. Repeat by breaking the upper insulator and lower insulator. Confirm the same result.
9. Test C1, C2, C3 and C4 on P&W wire. Toggle isolation with the Create wrench and confirm the electrical shadow gap appears/disappears without changing P&W wire geometry.
10. Test D with an already-placed native CEE HV switch. Confirm the OHE switch assembly's wrench action toggles the existing CEE switch state/animation.
11. Test E with CEE transformer behavior. Confirm no second electrical simulation is created.
12. Test F with the booster/transformer topology. Confirm power transfer follows the CEE transformer network.
13. Save, exit, reload and verify shadow links persist without duplicate nodes/connections.
14. Remove a P&W wire/node and verify its compatibility shadow data is cleaned on subsequent server ticks.

If any runtime step fails, save the latest `logs/latest.log` and report the exact numbered step that failed.


## Fix 30 Core Repair Retest

1. Start the development client with `gradlew.bat runClient`.
2. Create a fresh superflat test world.
3. Place four OHE Power Bridges while facing North, East, South and West.
4. Confirm each complete three-block assembly rotates as one unit.
5. Confirm the CEE side and P&W side are always opposite.
6. Use the native P&W catenary wire item: click the OHE bridge endpoint, then a P&W cantilever/connector.
7. Repeat in all four orientations.
8. Confirm the wire is rendered by P&W and no visible CEE node/line is created.
9. Break the top, middle and bottom assembly blocks in separate tests. Confirm the entire three-block assembly disappears and only the intended bridge item drops.
10. If a connection fails, capture the exact chat message and latest `logs/latest.log` section from the failed click; do not change the world before recording the result.

## 2026-09-05 Core repair test

1. Place OHE Power Bridge while facing NORTH, EAST, SOUTH, WEST. Confirm the enclosure/terminal faces rotate horizontally only; no terminal should point upward/downward.
2. Use the P&W catenary wire and connect a normal P&W endpoint to the bridge's P&W-facing terminal.
3. Complete a P&W wire through the bridge and confirm the wire is native P&W geometry.
4. Confirm no CEE detached node marker appears at the bridge or ordinary P&W junctions.
5. Break the top bridge block: all three bridge/insulator blocks disappear and only one OHE Power Bridge item drops.
6. Place again and break the upper insulator: same result.
7. Place again and break the lower insulator: same result.
8. Repeat the connection test in all four horizontal orientations.
