RALLY CLUB: OVERDRIVE — Paul Sackey
A complete browser rebuild of the original Processing-style Java Pong prototype.

PLAY WITHOUT INSTALLING ANYTHING
Open Play-Rally-Club.html in a current browser. The game, graphics and sound
are bundled into that file. Optional Google Fonts need an internet connection;
local fallback fonts work without one. No account, API key or audio download.

DEVELOP / PREVIEW
From this folder: python3 -m http.server 8770
Then open http://localhost:8770
Optional Node.js commands:
  npm test       Run 8 game-engine checks (no npm install needed).
  node build.mjs Refresh the standalone HTML after editing source files.

CONTROLS
Solo: W/S, Up/Down, mouse movement on the court, or touch and drag.
Two players: left uses W/S, right uses Up/Down. On a touch device,
players drag their own half simultaneously; the touch stays assigned to that side.
Space or P: start / pause / resume when the court has focus.
Reset: return to the lobby. Changing mode, difficulty or target resets the match.
First to 5, 7 or 11 wins. Difficulty controls the solo bot's movement speed.
FX toggles trails, particles, ambient motion and animation. Reduced-motion
preferences are respected. Expand opens full screen where supported.
Sound is opt-in. Switching tabs or opening help pauses the game.

WHAT WAS FIXED
Time-based movement, physics substeps to prevent fast-ball tunnelling,
directional paddle collisions to prevent repeated reversals, one score per point,
serve countdown, point delay, bounded paddle movement, ball speed cap,
edge-dependent bounce angles, pause/resume and end-of-match state.
Rendering adapts to screen size and pixel density. Effects respect reduced motion.

FILES
index.html / style.css / app.js: interface, rendering, sound and input.
engine.js: independently testable game simulation.
tests/engine.test.js: automated physics and match-state checks.
Play-Rally-Club.html: portable, bundled game.
original/Main.java: unchanged original source for reference.
icon.svg: original vector favicon.

DEPLOY
Upload index.html, style.css, app.js, engine.js and icon.svg to any static host
(Netlify, GitHub Pages, Vercel, etc.). Or deploy the standalone HTML as index.html.
No build tools or server-side services required. Do not describe this version as
a native Android SDK application: it is a responsive HTML Canvas browser game.
The separate native Java / Android SDK edition is in android/. Its debug APK
compiles, lint passes and eight engine checks pass; device UI testing is pending.

VERIFICATION
8 automated engine tests: pause/countdown; single scoring and winner;
next serve; fast paddle collisions; edge angles/speed cap; wall/paddle bounds;
frame-rate consistency; bot speed and reset.
Desktop and mobile browser checks are recorded in VERIFICATION.txt.

Live game: https://bigmindhub.netlify.app/games/rally-club/
Source: https://github.com/lebasiluap/Android-Game-Java
Portfolio source: https://github.com/lebasiluap/My-Portfolio
