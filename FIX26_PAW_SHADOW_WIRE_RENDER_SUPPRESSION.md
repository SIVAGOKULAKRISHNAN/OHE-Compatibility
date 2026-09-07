# Fix 26 — P&W Shadow Wire Render Suppression

## Purpose

Keep the P&W compatibility connection electrically active in CEE while
preventing it from being drawn as an extra CEE catenary on a native P&W OHE
span.

## Change

`CeeCatenaryVisualMixin` now cancels CEE's client catenary visual only when
the connection uses `ohecompat:paw_shadow` (`PawShadowWireType.SHADOW`).

CEE's catenary renderer constructs a visual independently of the wire model,
so the prior empty shadow model was not a sufficient guarantee that the line
would stay invisible. The new type-specific cancellation is visual-only.

## Preserved behaviour

- The shadow connection remains a real CEE electrical connection.
- P&W continues to own and render the physical P&W OHE wire.
- Native CEE catenaries are not filtered and retain their normal behaviour.
- Exact P&W endpoint matching remains unchanged.
- The bridge model, block textures, two-block insulator, and all bridge
  connectors are unchanged.

## Build note

The bundled Gradle build could not start in this environment because Gradle
could not load Windows `native-platform.dll`. This is an environment startup
failure before Java compilation; run `gradlew.bat clean build` on a normal
Windows Java/Gradle setup to compile and test in-game.
