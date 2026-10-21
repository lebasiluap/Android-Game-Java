package com.paulsackey.rallyclub;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native Android screen. No WebView, browser runtime or internet permission. */
public final class MainActivity extends Activity {
  static final int BG = Color.rgb(9, 11, 23),
      LIME = Color.rgb(217, 255, 105),
      MUTED = Color.rgb(136, 148, 174);
  private PongView court;
  private TextView hero, footer;
  private Button solo, duel, level, target, pause, fx, sound;
  private LinearLayout root, modeRow, configRow;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(BG);
    root.setPadding(dp(16), dp(8), dp(16), dp(8));
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          int top, bottom, left, right;
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars =
                insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            top = bars.top;
            bottom = bars.bottom;
            left = bars.left;
            right = bars.right;
          } else {
            top = insets.getSystemWindowInsetTop();
            bottom = insets.getSystemWindowInsetBottom();
            left = insets.getSystemWindowInsetLeft();
            right = insets.getSystemWindowInsetRight();
          }
          root.setPadding(dp(16) + left, dp(6) + top, dp(16) + right, dp(6) + bottom);
          return insets;
        });
    LinearLayout header = row();
    TextView brand = label("↗  RALLY CLUB", 17, Color.WHITE);
    brand.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
    header.addView(brand, new LinearLayout.LayoutParams(0, dp(50), 1));
    fx =
        button(
            "FX",
            () -> {
              court.effects = !court.effects;
              fx.setText(court.effects ? "FX ON" : "FX OFF");
            });
    sound =
        button(
            "SFX",
            () -> {
              court.sound = !court.sound;
              sound.setText(court.sound ? "SFX ON" : "SFX OFF");
              court.beep(80);
            });
    Button help =
        button(
            "?",
            () -> {
              court.engine.pause();
              update();
              new AlertDialog.Builder(this)
                  .setTitle("GET IN THE GAME")
                  .setMessage(
                      "First to the selected score wins. Drag to move your paddle. In Local Duel,"
                          + " each player drags their half with their own finger.\n\n"
                          + "Edge hits create sharper angles. Every return speeds up the ball.\n\n"
                          + "Keyboard: W/S left, arrows right. Space pauses. FX turns off motion;"
                          + " SFX toggles sound.\n\n"
                          + "The game pauses when you leave the app. Tap the court to resume.")
                  .setNeutralButton("MATCH SETUP", (d, w) -> setup())
                  .setPositiveButton("GOT IT", (d, w) -> {})
                  .show();
            });
    header.addView(fx);
    header.addView(sound);
    header.addView(help);
    root.addView(header);
    hero = label("ENTER THE\nOVERDRIVE.", 43, LIME);
    hero.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
    hero.setLineSpacing(-dp(5), 1);
    hero.setPadding(0, dp(8), 0, dp(13));
    root.addView(hero);
    LinearLayout modes = row();
    modeRow = modes;
    solo =
        button(
            "01  VS MACHINE",
            () -> {
              court.engine.solo = true;
              reset();
            });
    duel =
        button(
            "02  LOCAL DUEL",
            () -> {
              court.engine.solo = false;
              reset();
            });
    addWeight(modes, solo);
    addWeight(modes, duel);
    Button setupButton = button("⚙", this::setup);
    setupButton.setContentDescription("Match setup: difficulty and winning score");
    setupButton.setTextSize(16);
    modes.addView(setupButton, new LinearLayout.LayoutParams(dp(48), dp(44)));
    root.addView(modes);
    LinearLayout config = row();
    configRow = config;
    level =
        button(
            "LEVEL: LOCKED IN",
            () -> {
              court.engine.difficulty = (court.engine.difficulty + 1) % 3;
              reset();
            });
    target =
        button(
            "FIRST TO: 7",
            () -> {
              court.engine.target =
                  court.engine.target == 5 ? 7 : court.engine.target == 7 ? 11 : 5;
              reset();
            });
    addWeight(config, level);
    addWeight(config, target);
    root.addView(config);
    court = new PongView(this);
    court.onStateChange = this::update;
    root.addView(court, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout controls = row();
    pause =
        button(
            "PAUSE Ⅱ",
            () -> {
              court.engine.togglePause();
              court.clearKeys();
              update();
            });
    Button restart = button("RESET ↻", this::reset);
    addWeight(controls, pause);
    addWeight(controls, restart);
    root.addView(controls);
    footer = label("PAUL SACKEY  /  NATIVE ARCADE EDITION", 9, MUTED);
    footer.setGravity(Gravity.CENTER);
    footer.setPadding(0, dp(5), 0, dp(2));
    root.addView(footer);
    if (saved != null) restore(saved);
    fit();
    update();
    root.requestApplyInsets();
  }

  private LinearLayout row() {
    LinearLayout r = new LinearLayout(this);
    r.setOrientation(LinearLayout.HORIZONTAL);
    r.setGravity(Gravity.CENTER_VERTICAL);
    return r;
  }

  private TextView label(String text, int size, int color) {
    TextView v = new TextView(this);
    v.setText(text);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setGravity(Gravity.CENTER_VERTICAL);
    return v;
  }

  private Button button(String text, Runnable action) {
    Button b = new Button(this);
    b.setText(text);
    b.setTextSize(9);
    b.setTextColor(Color.rgb(199, 209, 231));
    b.setTypeface(Typeface.MONOSPACE);
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setMinHeight(dp(44));
    b.setMinimumHeight(dp(44));
    b.setPadding(dp(9), 0, dp(9), 0);
    b.setStateListAnimator(null);
    b.setBackground(pill(false));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(44));
    lp.setMargins(dp(3), dp(3), dp(3), dp(3));
    b.setLayoutParams(lp);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void addWeight(LinearLayout row, View v) {
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1);
    lp.setMargins(dp(3), dp(3), dp(3), dp(3));
    row.addView(v, lp);
  }

  private GradientDrawable pill(boolean selected) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(selected ? LIME : Color.rgb(19, 23, 40));
    d.setCornerRadius(dp(7));
    d.setStroke(dp(1), selected ? LIME : Color.rgb(48, 54, 77));
    return d;
  }

  private void reset() {
    court.reset();
    update();
  }

  private void update() {
    if (court == null) return;
    PongEngine e = court.engine;
    solo.setBackground(pill(e.solo));
    solo.setTextColor(e.solo ? BG : MUTED);
    duel.setBackground(pill(!e.solo));
    duel.setTextColor(e.solo ? MUTED : BG);
    level.setEnabled(e.solo);
    level.setAlpha(e.solo ? 1 : .4f);
    level.setText(
        getString(
            R.string.level_label, new String[] {"CHILL", "LOCKED IN", "SWEAT"}[e.difficulty]));
    target.setText(getString(R.string.target_label, e.target));
    pause.setText(e.state == PongEngine.State.PAUSED ? "RESUME ▷" : "PAUSE Ⅱ");
    pause.setEnabled(e.state != PongEngine.State.READY && e.state != PongEngine.State.OVER);
    fx.setText(court.effects ? "FX ON" : "FX OFF");
    sound.setText(court.sound ? "SFX ON" : "SFX OFF");
  }

  private void setup() {
    new AlertDialog.Builder(this)
        .setTitle("MATCH SETUP")
        .setItems(
            new String[] {
              "Switch mode: " + (court.engine.solo ? "VS MACHINE" : "LOCAL DUEL"),
              "Cycle difficulty: "
                  + new String[] {"CHILL", "LOCKED IN", "SWEAT"}[court.engine.difficulty],
              "Cycle target: " + court.engine.target + " points"
            },
            (dialog, which) -> {
              if (which == 0) court.engine.solo = !court.engine.solo;
              else if (which == 1) court.engine.difficulty = (court.engine.difficulty + 1) % 3;
              else
                court.engine.target =
                    court.engine.target == 5 ? 7 : court.engine.target == 7 ? 11 : 5;
              reset();
            })
        .setNegativeButton("CANCEL", null)
        .show();
  }

  private void fit() {
    Configuration cfg = getResources().getConfiguration();
    boolean landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;
    boolean compact = cfg.screenHeightDp < 740;
    hero.setVisibility(landscape ? View.GONE : View.VISIBLE);
    hero.setText(compact ? "ENTER THE OVERDRIVE." : "ENTER THE\nOVERDRIVE.");
    hero.setMaxLines(compact ? 1 : 2);
    hero.setLayoutParams(
        new LinearLayout.LayoutParams(
            -1, compact ? dp(60) : LinearLayout.LayoutParams.WRAP_CONTENT));
    hero.setPadding(0, dp(compact ? 6 : 8), 0, dp(compact ? 8 : 13));
    hero.setAutoSizeTextTypeUniformWithConfiguration(
        18, compact ? 28 : 43, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
    modeRow.setVisibility(landscape ? View.GONE : View.VISIBLE);
    configRow.setVisibility(landscape || compact ? View.GONE : View.VISIBLE);
    footer.setVisibility(landscape ? View.GONE : View.VISIBLE);
  }

  @Override
  public void onConfigurationChanged(Configuration c) {
    super.onConfigurationChanged(c);
    court.engine.pause();
    fit();
    update();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (court != null) court.resumeRendering();
  }

  @Override
  public void onPause() {
    if (court != null) {
      court.engine.pause();
      court.clearKeys();
      court.stopRendering();
      update();
    }
    super.onPause();
  }

  @Override
  public void onDestroy() {
    if (court != null) court.dispose();
    super.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    PongEngine e = court.engine;
    out.putBoolean("solo", e.solo);
    out.putInt("level", e.difficulty);
    out.putInt("target", e.target);
    out.putIntArray("scores", e.scores);
    out.putFloatArray("paddles", e.paddles);
    out.putFloatArray("ball", new float[] {e.x, e.y, e.vx, e.vy, e.height, e.timer});
    out.putInt("state", e.resumeState().ordinal());
    out.putInt("rally", e.rally);
    out.putInt("best", e.bestRally);
    out.putInt("winner", e.winner);
    out.putBoolean("fx", court.effects);
    out.putBoolean("sound", court.sound);
    out.putInt("direction", e.serveDirection());
  }

  private void restore(Bundle b) {
    PongEngine e = court.engine;
    e.solo = b.getBoolean("solo", true);
    e.difficulty = b.getInt("level", 1);
    e.target = b.getInt("target", 7);
    int[] scores = b.getIntArray("scores");
    float[] pads = b.getFloatArray("paddles"), ball = b.getFloatArray("ball");
    if (scores != null && pads != null && ball != null) {
      e.resizeHeight(ball[4]);
      System.arraycopy(scores, 0, e.scores, 0, 2);
      System.arraycopy(pads, 0, e.paddles, 0, 2);
      e.x = ball[0];
      e.y = ball[1];
      e.vx = ball[2];
      e.vy = ball[3];
      e.timer = ball[5];
      e.rally = b.getInt("rally");
      e.bestRally = b.getInt("best");
      e.winner = b.getInt("winner", -1);
      e.restoreState(b.getInt("state"), b.getInt("direction", 1));
    }
    court.effects = b.getBoolean("fx", ValueAnimator.areAnimatorsEnabled());
    court.sound = b.getBoolean("sound", false);
  }

  int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }
}
