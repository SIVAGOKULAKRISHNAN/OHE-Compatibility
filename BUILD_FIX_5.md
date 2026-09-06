# Build fix 5

The previous build reached `processResources` and failed because Gradle's `expand()`
was applied to the entire `neoforge.mods.toml`. Groovy interpreted TOML `${...}`
content as template properties and produced:

`Missing property (file) for Groovy template expansion.`

The global template expansion has been removed. The build now copies the TOML normally
and then replaces only the exact Create dependency range string.

Run:

`gradlew.bat clean build`
