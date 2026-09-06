# Create 6.x.x compatibility

The OHE Compatibility mod now declares **Create 6.x.x** as its supported Create major-version range:

- minimum: 6.0.0
- maximum: below 7.0.0
- target Java/Minecraft: Java 21 / Minecraft 1.21.1

The project compiles against Create 6.0.7 because that is the concrete API baseline used by the supplied CEE/P&W target JARs.

Important: accepting a Create 6.x.x version in this mod's dependency metadata does not override the dependency requirements inside CEE or P&W themselves. The selected CEE and P&W releases must also declare that Create version compatible. P&W explicitly identifies its `C6` builds as Create 6 compatible.

If a future Create 6.x release changes a method/class used by CEE or P&W, that is a dependency-level incompatibility and must be handled by the corresponding upstream mod or by a version-specific adapter. This project therefore does not pretend that one compiled binary can guarantee every future Create 6.x API change.
