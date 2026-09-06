# Fix 30 Final Audit

Date: 2026-09-05

## Source merge
Two supplied project archives were compared. They contain the same source tree except for `CeeDetachedNodeRendererMixin.java`. The newer archive already contains the preferred Fix 30 `WireRenderer.setNodeData` interception, so it was used as the master.

## Fixes applied in this final source package
- Preserved the Fix 29 exact P&W endpoint architecture and CEE shadow electrical simulation.
- Preserved the client-side suppression of compatibility-labelled CEE detached-node markers (`ohecompat:*`).
- Corrected OHE Power Bridge terminal coordinate mapping so CEE and P&W endpoints are on opposite faces for every horizontal direction.
- Corrected the P&W-side bridge node calculation in the BlockConnectorNodeData compatibility mixin to match the same geometry.
- Added player-horizontal-facing placement for the OHE Power Bridge, C1-C4 section assemblies, and D switch assembly.
- Propagated bridge facing to both insulator blocks when the three-block bridge assembly is created.
- Added a distinct bottom-insulator model with a square mounting plate; the lower block uses `separated=false`, the upper block uses `separated=true`.
- Preserved whole-assembly destruction: breaking the bridge or either insulator removes the other two blocks and produces one bridge item through the bridge destruction path.
- Completed the CEE HV switch adapter's native state toggle using CEE's existing `HVSwitchDevice`/`HVSwitchBlockEntity` state fields rather than creating a duplicate electrical switch.
- Removed the duplicate/typo switch language key.
- Verified recipe result IDs are local `ohecompat:*` IDs and ingredient namespaces used by the recipes are present in the supplied CEE/P&W jars.

## Static/API validation performed
- Parsed all 78 JSON resource files successfully.
- Checked required source/resource files: PASS.
- Checked Fix 30 detached-node suppression source: PASS.
- Checked bridge player-facing placement source: PASS.
- Checked corrected bridge/P&W coordinate mappings: PASS.
- Inspected the supplied CEE 1.21.1-1.1.1 jar with `javap` for ConnectorBlock, TransformerBlock, HVSwitchBlock, HVSwitchDevice, HVSwitchBlockEntity and DevicesSavedData APIs.
- Inspected the supplied P&W 1.21.1-beta-0.2.3-C6 jar for WireNode, BlockConnectorNodeData, ConnectorDataProvider, BasicConnectorDataProvider and relevant item/block resources.
- Compared both supplied archives: no source-tree differences other than the detached-node renderer mixin.

## Build limitation
A clean Gradle build was attempted in the isolated Linux validation environment. The project wrapper attempted to download the official Gradle 8.13 distribution from `services.gradle.org`, but that environment has no DNS/internet access. Therefore a new binary JAR could not be compiled here.

The supplied master archive contained a previously successful verified JAR (`cee-paw-ohe-compat-0.2.0-alpha.jar`, 95,359 bytes, timestamp 2026-09-04 17:21 UTC), but it predates the source edits in this final package. It is intentionally not included as a claimed final binary.

## Runtime limitation
A real Minecraft/Prism Launcher runtime cannot be launched from this validation environment. The runtime matrix therefore remains a required final check on the user's Windows Minecraft instance. No runtime result is fabricated.

## Final architecture
P&W owns visible OHE wire geometry, tension and pantograph contact.
CEE owns electrical simulation.
Compatibility CEE shadow nodes/wires are internal and compatibility-labelled client markers are suppressed.
No proximity-based P&W-to-OHE endpoint matching is used.
