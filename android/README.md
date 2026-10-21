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

## Emulator runtime checks

Boot an Android 16 emulator, then run `./run-runtime-checks.sh emulator-5554` (substitute its serial). The helper builds and installs both APKs and fails unless all 14 checks pass. The custom runner uses platform Instrumentation and real injected single- and two-pointer touch events; no third-party test dependencies are needed.

The suite covers initial rendering and controls, match setup, countdown/play, drag mapping, pause/resume, two-pointer crossing and release, rendered-frame differences with FX on/off, impact/victory effects, background/foreground, rotation, activity recreation, sound toggle initialization, 320dp phones and a tablet viewport. Screenshots and `report.txt` are written to the test app's external-files `runtime-checks` folder. See `RUNTIME-VERIFICATION.txt` for the verified build and results.

## Verification and limits

Version 2.0.1 built successfully and passed Android lint, eight pure-Java engine checks, and all 14 runtime checks on an Android 16 / API 36 ARM64 emulator. Phone, landscape, small-phone and tablet screenshots were inspected. The compact screen uses a shorter title, a larger court and a gear button for settings. The landscape overlay stays within the court. Selected match settings are shown in the scoreboard.

Physical devices and older Android OS versions have not been tested. Audio initialization and mute were checked, but audio output was not listened to or measured. The APK uses debug signing; a release signing key and Play Store listing are not included.
