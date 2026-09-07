# Final build notes

This is the clean source package for the requested CEE + P&W OHE compatibility project.

## D
D is NOT a new HV electrical switch.
The existing CEE `high_voltage_switch` is the electrical component.
The new `ohe_switch_assembly` is the railway mounting/operating part and its handle is at the bottom.
Using a Create Wrench on the assembly actuates the nearest existing CEE HV Switch.

## A/B
Native CEE and P&W OHE are not replaced.

## C1-C4
These are new section/neutral assemblies. Their electrical effect is represented in CEE's network shadow; P&W physical wire geometry/tension is left native.

## E
The compact substation uses CEE's TransformerBlock/TransformerBlockEntity behavior.

## F
The booster is a separate transformer-based topology. It is not a 40/50 km range mechanic and does not create a second electricity system.

## Popup/crash prevention
- Exact Minecraft/NeoForge/Create/CEE/P&W dependency ranges are declared.
- CEE/P&W target JARs are included as dev runtime files.
- GeckoLib and DragonLib are declared required because P&W declares them as required.
- No guessed recipe IDs are used.
- D no longer subclasses or registers a duplicate CEE HV switch.
- The cross-mod layer fails closed when a native bridge operation is unavailable.

## Build
NeoForge's documented 1.21.1 workflow uses Java 21 and `gradlew build`; this package is structured for that workflow. A full Minecraft build/runtime launch still needs the normal Gradle/NeoForge network dependencies and the required P&W dependencies (DragonLib/GeckoLib) available in the development environment.
