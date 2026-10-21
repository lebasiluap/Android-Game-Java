# Rally Club: Overdrive — native Android

This is the native Java / Android SDK edition of the Overdrive redesign. It uses Android Canvas and platform controls, not a WebView. The original prototype is preserved at `../Android Ping Pong Game/Main.java`; this complete application supersedes it.

## Build

Open this `android/` folder in Android Studio, select JDK 17 or newer, install Android SDK Platform 36, and sync. Run the `app` configuration on an Android 8.0+ device or emulator.

Command line: set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK (or create an ignored `local.properties` with `sdk.dir=/path/to/sdk`), then run:

```sh
./gradlew assembleDebug lintDebug
```

The test APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. It uses debug signing and is not a Play Store release. No internet or sensitive permissions are requested.

## Native design and behavior

- Dark Overdrive arena, cyan/pink player identities, lime play button, condensed typography and custom launcher icon.
- Native Canvas grid, animated paddle illustration, ball trails, impact rings, collision particles, score feedback and victory particles.
- Multi-touch paddle control; each pointer stays assigned to its starting side. Solo bot and local two-player modes, three difficulties and targets of 5/7/11.
- FX and sound toggles. Android's animation setting determines the default FX state. Sound starts off and uses the music volume channel.
- Landscape hides the large title and inline setup rows to give the court room. Use `?` → `MATCH SETUP` to change options in landscape.
- Pause/resume, serve countdown, match end, reset, background/focus pause and saved-state restoration. Rotation preserves normalized court positions and pauses for a safe resume.
- Native buttons expose standard accessibility semantics. The Canvas announces points and match results; physical keyboards support W/S, arrows and Space/P. Full nonvisual game control has not been validated.

## Engine checks

`PongEngine` is pure Java, so it can be tested without Android:

```sh
mkdir -p build/engine-checks
javac -d build/engine-checks app/src/main/java/com/paulsackey/rallyclub/PongEngine.java tests/PongEngineChecks.java
java -cp build/engine-checks PongEngineChecks
```

Eight checks cover scoring/winner, fast collisions, walls/bounds, pause/resume, countdown, next serve, frame-rate consistency, and resize/restoration.

## Verification limits

The debug APK was compiled with the installed Android SDK and Gradle. Android lint passed (no errors); remaining warnings cover tool versions and backup configuration. Eight pure-Java engine checks passed. This machine has no configured emulator system image; installation, rendering, audio and multi-touch on a physical Android device have not been tested. A release signing key and Play Store listing are not included.
