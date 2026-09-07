# Build/check checklist

1. Install Java 21.
2. Put this project in a folder.
3. Run `gradle build` or generate a Gradle wrapper with your installed Gradle and run `gradlew build`.
4. Use the matching NeoForge 1.21.1 runtime.
5. Install Create 6.0.7, CEE 1.21.1-1.1.1, P&W beta-0.2.3-C6, DragonLib 3.0.28, GeckoLib 4.8.4.
6. Test first with only A/B + D.
7. Then test C1-C4.
8. Then test E.
9. Then enable F.
10. If a crash popup appears, preserve `latest.log` and the crash report; the first `Caused by:` line is the useful failure boundary.
