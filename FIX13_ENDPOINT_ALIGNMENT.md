# Fixed13 — CEE/P&W shared endpoint alignment

This revision changes the compatibility endpoint model used for a CEE catenary holder.

- The P&W WireNode for a CEE holder is now stored at CEE's actual `getNodePosition(..., 0)` world position.
- The P&W contact connector offset is now zero, so the P&W contact endpoint is exactly the same coordinate as the CEE catenary node.
- The P&W node is updated to the same CEE position after graph reload/update.
- The existing CEE block entity is untouched; no second visible P&W connector block is created.
- The P&W catenary tension attachment remains a small offset above the shared contact point.
- Existing CEE electrical and P&W shadow-network bridging remains unchanged.

Build note: source compilation was not executed in this environment because the Gradle 8.13 distribution host was unavailable. The user should run `gradlew.bat clean build` locally.


# Fixed14 follow-up

- CEE holders now expose a basic zero-offset P&W connector instead of a P&W cantilever tension attachment. This prevents an extra/floating P&W line at the CEE holder.
- The P&W-to-CEE electrical bridge now locates the CEE holder by matching the actual CEE node position instead of flooring the elevated node Y coordinate.
- Duplicate CEE visual suppression now compares P&W endpoints to the CEE holder's real node position, so a shared CEE/P&W endpoint is recognized correctly.
- Native P&W cantilevers are untouched, including their adjustable size/settings.
