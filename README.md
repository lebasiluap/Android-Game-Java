# Rally Club: Overdrive

A responsive neon arcade Pong game by Paul Sackey, rebuilt from the original Java / Processing-style prototype.

**[Play live](https://bigmindhub.netlify.app/games/rally-club/)** · [Portfolio](https://bigmindhub.netlify.app/#projects)

## Play

Open `Play-Rally-Club.html` directly in a modern browser, or serve this folder with `python3 -m http.server 8770` and visit `http://localhost:8770`. No dependencies or API keys are needed. Optional Google Fonts have local fallbacks.

- **VS Machine:** W/S, arrow keys, mouse, or touch and drag. Three difficulty levels.
- **Local Duel:** left player W/S, right player arrow keys; touchscreens support one pointer per side.
- **Space / P:** start, pause, resume while the court has focus. Changing tabs pauses automatically.
- Choose first to 5, 7, or 11. FX, sound and full-screen controls are available.

## Design and engine

Custom arcade HUD, cyan/pink player identities, animated paddle illustration, ambient light, glowing trails, collision particles and rings, score feedback, rally-energy meter, and victory particles. FX can be disabled; reduced-motion preferences are respected.

Time-based simulation, small physics steps, directional paddle collisions, speed cap, angle-based returns, bounded input, one-point scoring, countdowns and complete match states. The canvas adapts to viewport size and pixel density.

## Development

`node --test tests/*.test.js` runs the 8 engine checks. `node build.mjs` regenerates the portable single-file game after editing source files. `index.html`, `style.css`, `app.js`, and `engine.js` run directly on static hosting. The original Java file remains in `Android Ping Pong Game/Main.java`.

## Native Android edition

The same Overdrive art direction is now implemented in a complete **native Java / Android SDK application** in [`android/`](android/README.md), with platform controls and Canvas rendering. It includes multi-touch, solo/duel modes, neon effects, optional sound, pause/resume, adaptive court sizing and saved-state restoration. It is not a WebView.

[Download the Android debug APK](https://bigmindhub.netlify.app/downloads/Rally-Club-Overdrive-debug.apk). This is a test build, not a Play Store release. The APK compiles and eight pure-Java engine checks pass; physical-device/emulator UI and multi-touch have not been tested.
