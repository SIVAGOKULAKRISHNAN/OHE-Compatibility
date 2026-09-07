# Gradle build bootstrap

`gradlew.bat` is included for Windows and `gradlew` for macOS/Linux.

They provision the pinned **Gradle 8.13** distribution from the official Gradle
distribution server and then invoke it. This project uses ModDevGradle 2.0.107;
NeoForged documents Gradle 8.8 compatibility for ModDevGradle, so 8.13 is used
as the project bootstrap version.

Run on Windows:

```bat
gradlew.bat build
```

or to launch the test client:

```bat
gradlew.bat runClient
```

The normal official Gradle Wrapper consists of `gradlew`, `gradlew.bat`, and
`gradle/wrapper/gradle-wrapper.jar`. This environment could not download the
official wrapper JAR, so the included scripts use the same core idea—provision
the pinned Gradle distribution and execute it—without pretending the missing
JAR was generated.
