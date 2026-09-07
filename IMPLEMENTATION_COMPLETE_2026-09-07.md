# OHE Compatibility component implementation — 2026-09-07

This package is the next implementation pass for Minecraft 1.21.1 / NeoForge 21.1.219 / Create 6.0.10-280 / CEE 1.1.1 / P&W beta-0.2.3-C6.

## Component scope completed

- OHE Power Bridge: CEE native connector + native P&W catenary connector, horizontal placement, exact opposite CEE/P&W endpoints, three-block assembly, one-item whole-assembly break behavior.
- C1: cantilever-mounted section/neutral assembly.
- C2: direct-in-wire section/neutral assembly.
- C3: short PTFE-style direct-in-wire neutral section.
- C4: combined cantilever-mounted short-neutral section.
- D: OHE switch/sectioning assembly; P&W connector shell and CEE HV-switch operating adapter. The existing CEE HV switch remains the electrical authority.
- E: compact OHE substation remains a physical compact arrangement using the existing CEE TransformerBlock/TransformerBlockEntity behavior.
- F: OHE booster/autotransformer remains a separate CEE transformer topology.

## Compatibility behavior implemented

### CEE
- C1-C4 expose two real CEE electrical nodes.
- C1-C4 can be electrically open or internally connected through an invisible compatibility wire.
- OHE Power Bridge remains a native CEE ConnectorBlock node and therefore accepts native CEE feeder/power wires.
- No separate feeder-support block is added. Existing CEE feeder/feeding-post infrastructure remains authoritative.

### P&W
- OHE Power Bridge, C1-C4, and D expose native P&W connector interfaces and P&W WireConnectorBlockEntity instances.
- The Catenary Wire Item compatibility hook uses native BlockConnectorNodeData.
- Connector attach offsets are applied by P&W exactly once.
- P&W remains the owner of wire geometry, tension, and pantograph contact.

### Shadow electrical translation
- P&W wire graph is mirrored into CEE using the hidden shadow wire type.
- Power Bridge links use exact P&W attachment coordinates.
- C1-C4 links use exact section endpoint coordinates.
- D links, when present, match a P&W endpoint to an actual CEE HV-switch node coordinate; there is no nearest-node electrical discovery.
- Existing CEE visual shadow markers/wires remain suppressed.

## Visual design completed

Distinct 3D block models were added for C1, C2, C3, C4, D, compact substation, and booster. C1-C4 have open/closed variants; D has handle-down/handle-up variants. The existing Power Bridge/insulator models are retained, with the Power Bridge blockstate rotation corrected so the visible CEE-facing side follows the inherited CEE FACING state.

## Important safety/compatibility decisions

- The Power Bridge subclass does NOT redeclare CEE's `FACING` property. It uses the inherited full-direction CEE property and restricts placement to horizontal directions.
- The internal section insulator has no item drop; it is a part of the Power Bridge assembly.
- No guessed CEE/P&W recipe IDs are shipped in this pass. Recipes will be added only after their source item IDs are verified against the target mod versions.
- No new feeder-support equipment block is added.

## Validation performed in this environment

- JSON syntax: PASS.
- Blockstate -> block model references: PASS.
- Item model -> block model references: PASS.
- Power Bridge source has no horizontal-only FACING shadow declaration: PASS.
- C1-C4 implement both CEE ConnectorBlock behavior and native P&W connector interfaces: PASS by source inspection.
- P&W catenary endpoint mixin covers Power Bridge, C1-C4, D, and CEE catenary holders: PASS by source inspection.

## Build status

A full Gradle build was attempted in the isolated build environment. It stopped before Java compilation because the NeoForge ModDev Gradle plugin artifact was not available in that environment's offline Gradle cache. The source package is therefore **not claimed as a compiled JAR yet**. The included GitHub Actions workflow is the authoritative build path.

## Runtime test order after a successful build

1. Load exact target versions.
2. Place Power Bridge at North/East/South/West player facings and verify the CEE and P&W terminals remain exactly opposite.
3. Connect CEE supply to the OHE Power Bridge for OHE, or to the OHE Feeder Bridge for P&W Energy Wire feeder supply.
4. Connect a native P&W catenary wire to the opposite side.
5. Verify no second visible CEE wire appears beside the P&W wire.
6. Test C1, C2, C3 and C4 with P&W-only, CEE-only, and mixed endpoints.
7. Toggle section isolation with the Create wrench and verify the physical P&W wire remains while the CEE electrical path opens/closes.
8. Test D against a real CEE HV switch and verify the Create wrench toggles the existing switch.
9. Test compact substation and booster using native CEE transformer behavior.
10. Break the Power Bridge, lower insulator, and upper insulator separately; each test must remove the complete three-block assembly and produce only one Power Bridge item.
11. Restart the world and repeat the bridge/section/switch tests to validate persistence and cleanup.
