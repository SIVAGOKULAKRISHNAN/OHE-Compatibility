# Build Validation — 2026-09-07

A duplicate `syncFeederBridgeLinks` / `findFeederBridgeAtEndpoint` definition was removed from `PawShadowCatenaryManager.java`; this was a real Java compile blocker in the uploaded master source.

A local Gradle 8.13 invocation was attempted with the supplied Gradle distribution and `--offline`.

Result: build could not reach Java compilation because the offline cache does not contain the `net.neoforged.moddev:2.0.107` Gradle plugin. This is an environment/dependency-cache limitation, not a claimed successful build.

The repository should be built on the user's Windows/Java 21 setup with:

    gradlew.bat clean build

The resulting CI/runtime test is still required before calling the mod fully verified.
