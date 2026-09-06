# Create 6.x build profiles

The compatibility mod cannot safely promise one binary for arbitrary Create 6.x APIs.
Instead, the build has a profile switch. Each profile compiles against the exact Create
API line used by the selected CEE/P&W target and produces a JAR with a matching
NeoForge dependency range.

Use:
`gradle -Pcreate_profile=create_6_0 build`

For a new Create 6.x release, add its concrete Maven coordinate and run the API
compatibility check before enabling the profile. This prevents a misleading
dependency range from causing a Minecraft popup/crash.
