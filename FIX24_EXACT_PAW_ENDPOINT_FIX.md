# Fix 24 — Exact P&W Endpoint / No Proximity Bridge

## Problem found in Fix 23

Fix 23 used `PawPowerBridgeManager` to search for the nearest P&W shadow node within 4.5 blocks and connect it to the bridge's CEE node. This could create an unwanted/invisible CEE electrical connection between the bridge and an unrelated P&W line.

## Fix

- Removed the proximity-based bridge search from the active connection path.
- `PawPowerBridgeManager.syncBridge()` is now a no-op legacy shim.
- P&W-to-bridge electrical handoff is synchronized from the native P&W graph.
- A P&W graph endpoint is accepted for the bridge only when its actual attachment position matches the bridge's external P&W connector within a small 0.08-block tolerance.
- Bridge placement/removal is synchronized even when the P&W graph signature itself has not changed.
- Stale P&W-shadow → OHE Power Bridge CEE-node links created by older builds are removed when they are no longer an exact endpoint match.
- Native P&W physical wire rendering remains owned by P&W.
- The CEE holder compatibility path remains exact-position based and is not a nearest-node search.

## Intended result

`P&W physical wire -> exact P&W bridge endpoint -> internal electrical handoff -> CEE bridge node`

There should be no automatic bridge connection merely because another P&W node is nearby.

## Build

On Windows, from the project root:

```bat
gradlew.bat clean build
```

Then test in the normal NeoForge client.
