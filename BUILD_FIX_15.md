# Build 15 — CEE wire ↔ P&W cantilever endpoint bridge

Changes:
- Added a CEE-side detached electrical endpoint for native P&W cantilevers.
- Endpoint position comes from P&W `defaultWireAttachPoint`, so P&W cantilever size/configuration remains authoritative.
- Added CEE WireSpool integration so CEE wires can start/end on native P&W cantilevers.
- Existing P&W catenary → CEE shadow bridge remains separate and invisible.
- Existing P&W cantilever behavior, sizing, geometry, and P&W wire graph are not replaced.
- Compatibility endpoint positions are refreshed each server tick so cantilever configuration changes move the CEE endpoint with the native P&W attachment point.
- Endpoint is removed when the source cantilever no longer exists and has no remaining CEE connections.

Validation required on the target machine:
1. `gradlew.bat clean build`
2. `gradlew.bat runClient`
3. CEE wire → P&W cantilever
4. P&W cantilever → CEE wire
5. P&W catenary → CEE cantilever
6. CEE catenary → P&W cantilever
7. P&W cantilever resize after a CEE connection
8. P&W → P&W and CEE → CEE regression tests
