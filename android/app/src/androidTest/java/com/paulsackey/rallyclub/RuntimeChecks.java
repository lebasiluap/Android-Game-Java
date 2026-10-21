package com.paulsackey.rallyclub;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/** Dependency-free on-device integration checks. Run with adb shell am instrument -w. */
public final class RuntimeChecks extends Instrumentation {
  private volatile MainActivity activity;
  private int passed, failed;
  private final StringBuilder report = new StringBuilder();
  private File output;

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      Application app = (Application) getTargetContext().getApplicationContext();
      app.registerActivityLifecycleCallbacks(
          new Application.ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity a, Bundle b) {
              if (a instanceof MainActivity) activity = (MainActivity) a;
            }

            public void onActivityStarted(Activity a) {}

            public void onActivityResumed(Activity a) {}

            public void onActivityPaused(Activity a) {}

            public void onActivityStopped(Activity a) {}

            public void onActivitySaveInstanceState(Activity a, Bundle b) {}

            public void onActivityDestroyed(Activity a) {}
          });
      activity =
          (MainActivity)
              startActivitySync(
                  new Intent(getTargetContext(), MainActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      output = getTargetContext().getExternalFilesDir("runtime-checks");
      if (output == null) throw new AssertionError("No output directory");
      output.mkdirs();
      waitFor(
          () -> main(() -> court().getWidth() > 0 && court().getHeight() > 0),
          5000,
          "initial layout");
      SystemClock.sleep(500);
      check(
          "portrait layout and native controls",
          () -> {
            main(
                () -> {
                  PongView v = court();
                  RectF f = field();
                  require(
                      f.left >= 0
                          && f.top >= 0
                          && f.right <= v.getWidth() + 1
                          && f.bottom <= v.getHeight() + 1,
                      "Court outside view");
                  require(
                      button("VS MACHINE").isShown() && button("LOCAL DUEL").isShown(),
                      "Mode controls missing");
                  require(v.engine.state == PongEngine.State.READY, "Initial state");
                  return null;
                });
            shot("01-portrait-ready");
          });
      check(
          "compact match setup changes settings",
          () -> {
            click("⚙");
            tapAccessible("Cycle target");
            require(main(() -> court().engine.target) == 11, "Target setting was not applied");
            click("⚙");
            tapAccessible("Cycle difficulty");
            require(
                main(() -> court().engine.difficulty) == 2, "Difficulty setting was not applied");
            require(
                main(() -> court().engine.state) == PongEngine.State.READY,
                "Setting should reset the match");
          });
      check(
          "tap starts countdown and match",
          () -> {
            tapStart();
            waitFor(
                () -> main(() -> court().engine.state == PongEngine.State.COUNTDOWN),
                1000,
                "countdown");
            waitFor(
                () -> main(() -> court().engine.state == PongEngine.State.PLAYING),
                4500,
                "playing");
            shot("02-playing");
          });
      check(
          "solo touch drag reaches both court ends",
          () -> {
            drag(.2f, .25f, .2f, .75f);
            float actual = main(() -> court().engine.paddles[0] + court().engine.paddleHeight / 2);
            float expected = main(() -> court().engine.height * .75f);
            require(
                Math.abs(actual - expected) < 3,
                "Solo drag mapping: " + actual + " vs " + expected);
          });
      check(
          "pause freezes physics and resumes",
          () -> {
            click("PAUSE");
            float x = main(() -> court().engine.x), y = main(() -> court().engine.y);
            SystemClock.sleep(250);
            require(main(() -> court().engine.state) == PongEngine.State.PAUSED, "Pause state");
            require(
                main(() -> court().engine.x) == x && main(() -> court().engine.y) == y,
                "Physics moved while paused");
            shot("03-paused");
            click("RESUME");
            require(main(() -> court().engine.state) == PongEngine.State.PLAYING, "Resume state");
          });
      check(
          "two simultaneous pointers stay assigned across center",
          () -> {
            click("LOCAL DUEL");
            tapStart();
            twoFingers();
            shot("04-two-finger-duel");
          });
      check(
          "FX changes actual rendered frames and OFF is stable",
          () -> {
            click("RESET");
            if (!main(() -> court().effects)) click("FX");
            SystemClock.sleep(150);
            Bitmap a = capture("05-motion-a");
            SystemClock.sleep(350);
            Bitmap b = capture("06-motion-b");
            int motion = changed(a, b);
            a.recycle();
            b.recycle();
            require(motion > 30, "No visible animation: " + motion);
            click("FX");
            SystemClock.sleep(180);
            a = capture("07-fx-off-a");
            SystemClock.sleep(350);
            b = capture("08-fx-off-b");
            int still = changed(a, b);
            a.recycle();
            b.recycle();
            require(!main(() -> court().effects), "FX flag");
            require(still == 0, "FX OFF frame still changes: " + still);
            log("Animation pixels changed=" + motion + ", FX-off=" + still);
          });
      check(
          "impact effects and victory feedback run",
          () -> {
            if (!main(() -> court().effects)) click("FX");
            main(
                () -> {
                  PongView v = court();
                  v.engine.reset();
                  v.engine.solo = false;
                  v.engine.state = PongEngine.State.PLAYING;
                  v.engine.x = 70;
                  v.engine.y = v.engine.paddles[0] + v.engine.paddleHeight / 2;
                  v.engine.vx = -500;
                  v.engine.vy = 0;
                  return null;
                });
            waitFor(() -> main(() -> court().engine.rally == 1), 1500, "paddle impact");
            require(main(() -> collection("sparks").size()) > 0, "No hit particles");
            require(main(() -> collection("rings").size()) > 0, "No impact ring");
            main(
                () -> {
                  PongEngine e = court().engine;
                  e.scores[0] = e.target - 1;
                  e.x = 1008;
                  e.vx = 500;
                  return null;
                });
            waitFor(
                () -> main(() -> court().engine.state == PongEngine.State.OVER), 1500, "winner");
            require(main(() -> court().engine.winner) == 0, "Wrong winner");
            require(main(() -> collection("sparks").size()) >= 50, "No win particles");
            shot("09-win");
            tapStart();
            require(main(() -> court().engine.scores[0]) == 0, "Replay did not reset scores");
          });
      check(
          "background pauses and return preserves the match",
          () -> {
            main(
                () -> {
                  court().engine.state = PongEngine.State.PLAYING;
                  court().engine.scores[0] = 2;
                  activity.moveTaskToBack(true);
                  return null;
                });
            waitFor(
                () -> main(() -> court().engine.state == PongEngine.State.PAUSED),
                2000,
                "background pause");
            getTargetContext()
                .startActivity(
                    new Intent(getTargetContext(), MainActivity.class)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            waitFor(() -> main(() -> activity.hasWindowFocus()), 3000, "foreground");
            require(main(() -> court().engine.scores[0]) == 2, "Score lost on background");
            shot("10-foreground-paused");
          });
      check(
          "landscape preserves match and keeps overlay inside court",
          () -> {
            main(
                () -> {
                  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                  return null;
                });
            waitFor(
                () ->
                    main(
                        () ->
                            activity.getResources().getConfiguration().orientation
                                == android.content.res.Configuration.ORIENTATION_LANDSCAPE),
                4000,
                "landscape");
            SystemClock.sleep(900);
            require(main(() -> court().engine.scores[0]) == 2, "Rotation lost score");
            require(
                main(() -> court().engine.state) == PongEngine.State.PAUSED,
                "Rotation should pause");
            Bitmap image = capture("11-landscape");
            RectF f = screenField();
            int bottom = greenButtonBottom(image, f);
            image.recycle();
            require(
                bottom <= f.bottom + 2,
                "Landscape overlay button outside court: " + bottom + " > " + f.bottom);
          });
      check(
          "activity recreation restores paused match",
          () -> {
            main(
                () -> {
                  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                  return null;
                });
            waitFor(
                () ->
                    main(
                        () ->
                            activity.getResources().getConfiguration().orientation
                                == android.content.res.Configuration.ORIENTATION_PORTRAIT),
                4000,
                "portrait");
            SystemClock.sleep(900);
            MainActivity old = activity;
            main(
                () -> {
                  activity.recreate();
                  return null;
                });
            waitFor(
                () -> activity != old && main(() -> court().getWidth() > 0), 5000, "recreation");
            require(main(() -> court().engine.scores[0]) == 2, "Recreation lost score");
            require(
                main(() -> court().engine.state) == PongEngine.State.PAUSED,
                "Recreated match is not paused");
            shot("12-restored");
          });
      check(
          "sound toggle initializes native audio without crashing",
          () -> {
            click("SFX");
            require(main(() -> court().sound), "Sound initialization failed");
            click("SFX");
            require(!main(() -> court().sound), "Sound mute failed");
          });
      check(
          "small phone layout at 320dp",
          () -> {
            try {
              shell("wm size 960x1706");
              SystemClock.sleep(1100);
              click("RESET");
              RectF r = fieldOnMain();
              require(
                  r.width() > 0
                      && r.height()
                          > 120 * getTargetContext().getResources().getDisplayMetrics().density,
                  "Small phone court is too short");
              shot("13-small-phone");
              require(main(() -> button("⚙").isShown()), "Compact setup is inaccessible");
            } finally {
              shell("wm size reset");
              SystemClock.sleep(700);
            }
          });
      check(
          "tablet layout and touch mapping",
          () -> {
            try {
              shell("wm size 1600x2560");
              shell("wm density 240");
              SystemClock.sleep(1400);
              click("RESET");
              tapStart();
              drag(.2f, .3f, .2f, .7f);
              float target = main(() -> court().engine.height * .7f);
              require(
                  Math.abs(
                          main(() -> court().engine.paddles[0] + court().engine.paddleHeight / 2)
                              - target)
                      < 3,
                  "Tablet drag mapping");
              shot("14-tablet");
            } finally {
              shell("wm density reset");
              shell("wm size reset");
              SystemClock.sleep(1100);
            }
          });
    } catch (Throwable t) {
      failed++;
      log("HARNESS ERROR " + android.util.Log.getStackTraceString(t));
    }
    try {
      if (output != null)
        try (FileWriter writer = new FileWriter(new File(output, "report.txt"))) {
          writer.write(report + "\nPASSED=" + passed + " FAILED=" + failed + "\n");
        }
    } catch (Exception ignored) {
    }
    result.putString("stream", report + "\nPASSED=" + passed + " FAILED=" + failed + "\n");
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }

  private RectF fieldOnMain() {
    return main(this::field);
  }

  private void shell(String command) throws IOException {
    try (android.os.ParcelFileDescriptor fd = getUiAutomation().executeShellCommand(command);
        InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
      byte[] buffer = new byte[2048];
      while (in.read(buffer) != -1) {}
    }
  }

  private void tapAccessible(String text) {
    waitFor(
        () -> {
          android.view.accessibility.AccessibilityNodeInfo root =
              getUiAutomation().getRootInActiveWindow();
          if (root == null) return false;
          java.util.List<android.view.accessibility.AccessibilityNodeInfo> nodes =
              root.findAccessibilityNodeInfosByText(text);
          if (nodes.isEmpty()) return false;
          Rect r = new Rect();
          nodes.get(0).getBoundsInScreen(r);
          tap(r.centerX(), r.centerY());
          SystemClock.sleep(100);
          return true;
        },
        2000,
        "dialog item " + text);
  }

  private interface Checked {
    void run() throws Exception;
  }

  private void check(String name, Checked action) {
    try {
      action.run();
      passed++;
      log("PASS " + name);
    } catch (Throwable t) {
      failed++;
      log("FAIL " + name + " :: " + t);
      try {
        shot("failure-" + failed);
      } catch (Throwable ignored) {
      }
    }
  }

  private void log(String message) {
    report.append(message).append('\n');
    Bundle b = new Bundle();
    b.putString("stream", message + "\n");
    sendStatus(0, b);
  }

  private static void require(boolean value, String message) {
    if (!value) throw new AssertionError(message);
  }

  @SuppressWarnings("unchecked")
  private <T> T main(Callable<T> action) {
    Object[] value = {null};
    Throwable[] error = {null};
    runOnMainSync(
        () -> {
          try {
            value[0] = action.call();
          } catch (Throwable t) {
            error[0] = t;
          }
        });
    if (error[0] != null) throw new AssertionError(error[0]);
    return (T) value[0];
  }

  private void waitFor(BooleanSupplier condition, long timeout, String label) {
    long end = SystemClock.uptimeMillis() + timeout;
    while (SystemClock.uptimeMillis() < end) {
      if (condition.getAsBoolean()) return;
      SystemClock.sleep(20);
    }
    throw new AssertionError("Timeout: " + label);
  }

  private PongView court() {
    return findCourt(activity.getWindow().getDecorView());
  }

  private PongView findCourt(View view) {
    if (view instanceof PongView) return (PongView) view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        PongView found = findCourt(((ViewGroup) view).getChildAt(i));
        if (found != null) return found;
      }
    return null;
  }

  private Button button(String part) {
    Button found = findButton(activity.getWindow().getDecorView(), part);
    if (found == null) throw new AssertionError("Button missing: " + part);
    return found;
  }

  private Button findButton(View view, String part) {
    if (view instanceof Button && ((Button) view).getText().toString().contains(part))
      return (Button) view;
    if (view instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
        Button b = findButton(((ViewGroup) view).getChildAt(i), part);
        if (b != null) return b;
      }
    return null;
  }

  private RectF field() {
    try {
      Field f = PongView.class.getDeclaredField("field");
      f.setAccessible(true);
      return new RectF((RectF) f.get(court()));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private RectF screenField() {
    return main(
        () -> {
          RectF f = field();
          int[] pos = new int[2];
          court().getLocationOnScreen(pos);
          f.offset(pos[0], pos[1]);
          return f;
        });
  }

  private Collection<?> collection(String name) {
    try {
      Field f = PongView.class.getDeclaredField(name);
      f.setAccessible(true);
      return (Collection<?>) f.get(court());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private void click(String part) {
    float[] xy =
        main(
            () -> {
              Button b = button(part);
              int[] p = new int[2];
              b.getLocationOnScreen(p);
              return new float[] {p[0] + b.getWidth() / 2f, p[1] + b.getHeight() / 2f};
            });
    tap(xy[0], xy[1]);
    SystemClock.sleep(70);
  }

  private void tapStart() {
    RectF r = screenField();
    tap(r.centerX(), r.centerY() + Math.min(40, r.height() * .1f));
    SystemClock.sleep(70);
  }

  private void tap(float x, float y) {
    long down = SystemClock.uptimeMillis();
    touch(down, MotionEvent.ACTION_DOWN, new int[] {0}, new float[] {x}, new float[] {y});
    SystemClock.sleep(40);
    touch(down, MotionEvent.ACTION_UP, new int[] {0}, new float[] {x}, new float[] {y});
  }

  private void drag(float x1, float y1, float x2, float y2) {
    RectF r = screenField();
    long down = SystemClock.uptimeMillis();
    touch(
        down,
        MotionEvent.ACTION_DOWN,
        new int[] {0},
        new float[] {r.left + r.width() * x1},
        new float[] {r.top + r.height() * y1});
    SystemClock.sleep(40);
    touch(
        down,
        MotionEvent.ACTION_MOVE,
        new int[] {0},
        new float[] {r.left + r.width() * x2},
        new float[] {r.top + r.height() * y2});
    touch(
        down,
        MotionEvent.ACTION_UP,
        new int[] {0},
        new float[] {r.left + r.width() * x2},
        new float[] {r.top + r.height() * y2});
    SystemClock.sleep(70);
  }

  private void twoFingers() {
    RectF r = screenField();
    long down = SystemClock.uptimeMillis();
    float lx = r.left + r.width() * .2f,
        rx = r.left + r.width() * .8f,
        top = r.top + r.height() * .22f,
        bottom = r.top + r.height() * .78f;
    touch(
        down, MotionEvent.ACTION_DOWN, new int[] {7}, new float[] {lx}, new float[] {r.centerY()});
    touch(
        down,
        MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
        new int[] {7, 13},
        new float[] {lx, rx},
        new float[] {r.centerY(), r.centerY()});
    touch(
        down,
        MotionEvent.ACTION_MOVE,
        new int[] {7, 13},
        new float[] {rx, lx},
        new float[] {top, bottom});
    SystemClock.sleep(70);
    float h = main(() -> court().engine.height);
    require(
        Math.abs(main(() -> court().engine.paddles[0] + court().engine.paddleHeight / 2) - h * .22f)
            < 3,
        "Left pointer changed sides");
    require(
        Math.abs(main(() -> court().engine.paddles[1] + court().engine.paddleHeight / 2) - h * .78f)
            < 3,
        "Right pointer changed sides");
    float left = main(() -> court().engine.paddles[0]);
    touch(
        down,
        MotionEvent.ACTION_POINTER_UP,
        new int[] {7, 13},
        new float[] {rx, lx},
        new float[] {top, bottom});
    touch(
        down, MotionEvent.ACTION_MOVE, new int[] {13}, new float[] {lx}, new float[] {r.centerY()});
    touch(down, MotionEvent.ACTION_UP, new int[] {13}, new float[] {lx}, new float[] {r.centerY()});
    SystemClock.sleep(70);
    require(
        Math.abs(main(() -> court().engine.paddles[1] + court().engine.paddleHeight / 2) - h * .5f)
            < 3,
        "Remaining pointer stopped moving");
    require(main(() -> court().engine.paddles[0]) == left, "Lifted pointer moved");
  }

  private void touch(long down, int action, int[] ids, float[] xs, float[] ys) {
    MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
    for (int i = 0; i < ids.length; i++) {
      properties[i] = new MotionEvent.PointerProperties();
      properties[i].id = ids[i];
      properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
      coords[i] = new MotionEvent.PointerCoords();
      coords[i].x = xs[i];
      coords[i].y = ys[i];
      coords[i].pressure = action == MotionEvent.ACTION_UP ? 0 : 1;
      coords[i].size = .1f;
    }
    MotionEvent event =
        MotionEvent.obtain(
            down,
            SystemClock.uptimeMillis(),
            action,
            ids.length,
            properties,
            coords,
            0,
            0,
            1,
            1,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0);
    sendPointerSync(event);
    event.recycle();
  }

  private Bitmap capture(String name) throws IOException {
    SystemClock.sleep(70);
    Bitmap image = getUiAutomation().takeScreenshot();
    require(image != null, "Screenshot unavailable");
    try (FileOutputStream out = new FileOutputStream(new File(output, name + ".png"))) {
      image.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    return image;
  }

  private void shot(String name) throws IOException {
    Bitmap b = capture(name);
    b.recycle();
  }

  private int changed(Bitmap a, Bitmap b) {
    RectF r = screenField();
    int changed = 0;
    for (int y = Math.max(0, (int) r.top + 2);
        y < Math.min(a.getHeight(), (int) r.bottom - 2);
        y += 3)
      for (int x = Math.max(0, (int) r.left + 2);
          x < Math.min(a.getWidth(), (int) r.right - 2);
          x += 3) {
        int aa = a.getPixel(x, y), bb = b.getPixel(x, y);
        if (Math.abs(Color.red(aa) - Color.red(bb))
                + Math.abs(Color.green(aa) - Color.green(bb))
                + Math.abs(Color.blue(aa) - Color.blue(bb))
            > 12) changed++;
      }
    return changed;
  }

  private int greenButtonBottom(Bitmap image, RectF r) {
    int bottom = -1, x = (int) r.centerX();
    for (int y = (int) r.centerY(); y < Math.min(image.getHeight(), r.bottom + 200); y++) {
      int p = image.getPixel(x, y);
      if (Color.red(p) > 180 && Color.green(p) > 230 && Color.blue(p) < 160) bottom = y;
    }
    require(bottom >= 0, "Play button not visible");
    return bottom;
  }
}
