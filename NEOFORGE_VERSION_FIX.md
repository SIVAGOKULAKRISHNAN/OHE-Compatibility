# NeoForge version fix

The development client was launching with NeoForge 21.1.174, but the runtime
dependencies in this project require a newer NeoForge.

The runtime log showed:
- Ponder 1.0.82 requires NeoForge >= 21.1.206.
- Create 6.0.10 requires NeoForge >= 21.1.219.

This profile is therefore pinned to NeoForge 21.1.219 for Minecraft 1.21.1.

Run:

gradlew.bat clean build

Then:

gradlew.bat runClient
