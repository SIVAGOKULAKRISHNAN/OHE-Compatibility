# Fix 22 — P&W wire visibility, external node alignment, and bridge texture

Based on Fix 21.

Changes:
- Restored the working multi-texture OHE Power Bridge model instead of applying the 32x32 SH-breaker atlas to every face.
- Kept the visible/player name `OHE Power Bridge` and internal id `ohecompat:ohe_power_bridge`.
- Added native P&W `ICatenaryWireConnector` implementation to the Power Bridge so the native P&W Catenary Wire Item can recognize the bridge directly.
- Kept the P&W connector data on the external face opposite the CEE terminal.
- Moved the visible P&W connector housing to the centered north/P&W face so it matches the native P&W endpoint.
- Kept the CEE external node on the south/CEE face.
- Kept the existing two-block insulator assembly and wrench removal behavior.
- No changes to other OHE component names or recipes.

Build on Windows:
`gradlew.bat clean build`

Runtime test:
`gradlew.bat runClient`

Give the bridge:
`/give @s ohecompat:ohe_power_bridge`
