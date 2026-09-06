# Recheck report

## Verified against supplied JAR APIs
- CEE HVSwitchBlock/HVSwitchDevice/HVSwitchBlockEntity APIs used by D.
- CEE TransformerBlock/TransformerBlockEntity APIs used by E/F.
- CEE InfrastructureSavedData node/connection APIs.
- CEE IPantographBlock + TrainPantographEntry capture path.
- P&W WireGraph/WireNode/WireEdge APIs.
- P&W NewWireCollision and WireBlockCollision APIs used for C2/C3 placement detection.
- P&W required dependency versions from its own `neoforge.mods.toml`.
- CEE required dependency versions from its own `neoforge.mods.toml`.

## Problems fixed in this recheck
- Section placed before a wire existed could remain permanently unlinked: now retried.
- Removed wire could leave a stale section link: now cleared and recaptured.
- Old C1-C4 left/right shadow nodes could survive after reconnecting: now cleaned before rebuilding.
- Project had no `settings.gradle`: added.
- Dependency manifest was implicit: added explicit manifest.
- P&W required DragonLib/GeckoLib versions are pinned in the build configuration.

## Still requiring a live Minecraft test
- Actual Gradle build and runClient.
- Final D physical mounting geometry and bottom handle animation.
- Exact runtime CEE switch discovery from the D assembly in a world.
- P&W direct wire graph split if/when a true physical span insertion is required.
- Actual CEE electrical current reaching a P&W pantograph through the shadow network.
- E/F transformer topology and correct ratio/ports in-world.
- Client/server sync and world reload.
- Renderer/model polish.

These are not hidden behind a false "100% complete" claim.
