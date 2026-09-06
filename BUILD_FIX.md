# Build fix

The first build failed because the Create Maven coordinate was incomplete:
`6.0.10` is the human-facing release version, but the Create Maven artifact for
Minecraft 1.21.1 uses a build-numbered version such as `6.0.10-280`.

This project now uses:

`com.simibubi.create:create-1.21.1:6.0.10-280:slim`

It also now declares the actual NeoForge runtime dependencies required by P&W:

- DragonLib 3.0.28 (Curse Maven file 8176502)
- GeckoLib 4.8.4 NeoForge (Curse Maven file 7707149)

Do not change the Create version back to `6.0.10` without the `-280` build suffix.
