#!/bin/sh
# Run against an already-booted Android emulator. No third-party test libraries.
set -eu
cd "$(dirname "$0")"
task_serial="${1:-emulator-5554}"
task_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -n "$task_sdk" ]; then task_adb="$task_sdk/platform-tools/adb"; else task_adb="$(command -v adb)"; fi
./gradlew assembleDebug assembleDebugAndroidTest
"$task_adb" -s "$task_serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$task_adb" -s "$task_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
task_report="$(mktemp)"
trap 'rm -f "$task_report"' EXIT
"$task_adb" -s "$task_serial" shell am instrument -w com.paulsackey.rallyclub.test/com.paulsackey.rallyclub.RuntimeChecks | tee "$task_report"
grep -q 'PASSED=14 FAILED=0' "$task_report"
