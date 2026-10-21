package com.paulsackey.rallyclub;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.SparseIntArray;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

/** Native Canvas renderer and multi-pointer controller for the Overdrive arena. */
public final class PongView extends View implements Choreographer.FrameCallback {
  public final PongEngine engine = new PongEngine();
  public boolean effects = ValueAnimator.areAnimatorsEnabled(), sound = false;
  public Runnable onStateChange;
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF field = new RectF();
  private Shader fieldGradient;
  private int displayedLeft = -1, displayedRight = -1;
  private String scoreLeft = "00", scoreRight = "00";
  private final Typeface display = Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC),
      mono = Typeface.MONOSPACE;
  private final SparseIntArray pointers = new SparseIntArray();
  private final ArrayList<Spark> sparks = new ArrayList<>();
  private final ArrayList<float[]> trail = new ArrayList<>(), rings = new ArrayList<>();
  private final Random random = new Random();
  private ToneGenerator tones;
  private boolean running, pendingStart;
  private long last;
  private float elapsed, flash, shake;
  private int lastState = -1, keys;
  private static final int CYAN = Color.rgb(96, 246, 239),
      PINK = Color.rgb(255, 108, 189),
      LIME = Color.rgb(217, 255, 105),
      BG = Color.rgb(10, 16, 32);

  private static final class Spark {
    float x, y, vx, vy, life, max;
    int color;

    Spark(float x, float y, int color, Random r, boolean win) {
      this.x = x;
      this.y = y;
      this.color = color;
      vx = (r.nextFloat() - .5f) * (win ? 700 : 320);
      vy = (r.nextFloat() - (win ? .7f : .5f)) * (win ? 600 : 320);
      life = max = win ? 1 + r.nextFloat() : .45f;
    }
  }

  public PongView(Context context) {
    super(context);
    setFocusable(true);
    setFocusableInTouchMode(true);
    setClickable(true);
    setContentDescription(
        "Rally Club Pong arena. Tap to start, then drag to move. In local duel, each player drags"
            + " their half. Space pauses; W/S and arrow keys move paddles.");
    engine.events =
        new PongEngine.Events() {
          public void hit(int side, float x, float y) {
            beep(25);
            if (effects) {
              shake = 3;
              rings.add(new float[] {x, y, .5f, side});
              for (int i = 0; i < 16; i++)
                sparks.add(new Spark(x, y, side == 0 ? CYAN : PINK, random, false));
            }
          }

          public void wall() {
            beep(12);
          }

          public void score(int side, boolean won) {
            beep(won ? 180 : 80);
            flash = 1;
            trail.clear();
            if (won && effects)
              for (int i = 0; i < 90; i++)
                sparks.add(
                    new Spark(
                        500,
                        engine.height * .3f,
                        new int[] {CYAN, PINK, LIME}[i % 3],
                        random,
                        true));
            announceForAccessibility(
                (won ? (side == 0 ? "Player one wins. " : "Player two wins. ") : "Score. ")
                    + engine.scores[0]
                    + " to "
                    + engine.scores[1]);
            changed();
          }
        };
  }

  public void beep(int duration) {
    if (!sound) return;
    try {
      if (tones == null) tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
      tones.startTone(
          duration > 100 ? ToneGenerator.TONE_PROP_ACK : ToneGenerator.TONE_PROP_BEEP, duration);
    } catch (RuntimeException unavailable) {
      sound = false;
    }
  }

  public void reset() {
    engine.reset();
    clearKeys();
    pointers.clear();
    sparks.clear();
    rings.clear();
    trail.clear();
    flash = shake = 0;
    changed();
    invalidate();
  }

  public void clearKeys() {
    keys = 0;
  }

  private void changed() {
    if (onStateChange != null) onStateChange.run();
  }

  public void resumeRendering() {
    if (running) return;
    running = true;
    last = 0;
    Choreographer.getInstance().postFrameCallback(this);
  }

  public void stopRendering() {
    running = false;
    last = 0;
    Choreographer.getInstance().removeFrameCallback(this);
  }

  public void dispose() {
    stopRendering();
    if (tones != null) {
      tones.release();
      tones = null;
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    stopRendering();
    super.onDetachedFromWindow();
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    super.onWindowFocusChanged(focused);
    if (!focused) {
      engine.pause();
      keys = 0;
      pointers.clear();
      changed();
    }
  }

  @Override
  public void doFrame(long now) {
    if (!running) return;
    float dt = last == 0 ? 0 : Math.min(.05f, (now - last) / 1_000_000_000f);
    last = now;
    if (engine.state != PongEngine.State.PAUSED) elapsed += dt;
    if (engine.state == PongEngine.State.PLAYING
        || engine.state == PongEngine.State.COUNTDOWN
        || engine.state == PongEngine.State.POINT) {
      int left = ((keys & 2) != 0 ? 1 : 0) - ((keys & 1) != 0 ? 1 : 0),
          right = ((keys & 8) != 0 ? 1 : 0) - ((keys & 4) != 0 ? 1 : 0);
      engine.move(
          0,
          engine.paddles[0]
              + engine.paddleHeight / 2
              + (engine.solo ? (left != 0 ? left : right) : left) * 560 * dt);
      if (!engine.solo)
        engine.move(1, engine.paddles[1] + engine.paddleHeight / 2 + right * 560 * dt);
    }
    engine.step(dt);
    if (effects && engine.state == PongEngine.State.PLAYING) {
      trail.add(new float[] {engine.x, engine.y});
      if (trail.size() > 20) trail.remove(0);
    } else trail.clear();
    if (!effects) {
      sparks.clear();
      rings.clear();
      shake = 0;
    } else if (engine.state != PongEngine.State.PAUSED) {
      for (int i = sparks.size() - 1; i >= 0; i--) {
        Spark p = sparks.get(i);
        p.life -= dt;
        p.x += p.vx * dt;
        p.y += p.vy * dt;
        p.vy += 90 * dt;
        if (p.life <= 0) sparks.remove(i);
      }
      for (int i = rings.size() - 1; i >= 0; i--) {
        rings.get(i)[2] -= dt;
        if (rings.get(i)[2] <= 0) rings.remove(i);
      }
      shake = Math.max(0, shake - dt * 35);
    }
    flash = Math.max(0, flash - dt * 2);
    if (lastState != engine.state.ordinal()) {
      lastState = engine.state.ordinal();
      changed();
    }
    invalidate();
    Choreographer.getInstance().postFrameCallback(this);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    field.set(dp(9), dp(67), w - dp(9), Math.max(dp(130), h - dp(28)));
    if (field.width() > 0) {
      engine.resizeHeight(1000 * field.height() / field.width());
      fieldGradient =
          new RadialGradient(
              500,
              engine.height / 2,
              Math.max(600, engine.height),
              new int[] {Color.rgb(35, 24, 58), BG},
              null,
              Shader.TileMode.CLAMP);
      trail.clear();
      rings.clear();
      sparks.clear();
    }
  }

  private float dp(float v) {
    return v * getResources().getDisplayMetrics().density;
  }

  private void text(
      Canvas c,
      String value,
      float x,
      float y,
      float size,
      int color,
      Typeface face,
      Paint.Align align) {
    paint.setShader(null);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    paint.setTypeface(face);
    paint.setTextSize(size);
    paint.setTextAlign(align);
    c.drawText(value, x, y, paint);
  }

  private void rect(Canvas c, RectF r, int color, float radius) {
    paint.setShader(null);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    c.drawRoundRect(r, radius, radius, paint);
  }

  private void rect(
      Canvas c, float left, float top, float right, float bottom, int color, float radius) {
    paint.setShader(null);
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(color);
    c.drawRoundRect(left, top, right, bottom, radius, radius, paint);
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    if (field.width() <= 0) return;
    float w = getWidth(), h = getHeight();
    rect(c, 0, 0, w, h, Color.rgb(17, 21, 38), dp(12));
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(1));
    paint.setColor(Color.rgb(71, 66, 99));
    c.drawRoundRect(dp(.5f), dp(.5f), w - dp(.5f), h - dp(.5f), dp(12), dp(12), paint);
    text(c, engine.solo ? "YOU" : "PLAYER 01", dp(14), dp(17), dp(8), CYAN, mono, Paint.Align.LEFT);
    text(
        c,
        engine.solo ? "MACHINE" : "PLAYER 02",
        w - dp(14),
        dp(17),
        dp(8),
        PINK,
        mono,
        Paint.Align.RIGHT);
    if (displayedLeft != engine.scores[0]) {
      displayedLeft = engine.scores[0];
      scoreLeft = String.format(java.util.Locale.ROOT, "%02d", displayedLeft);
    }
    if (displayedRight != engine.scores[1]) {
      displayedRight = engine.scores[1];
      scoreRight = String.format(java.util.Locale.ROOT, "%02d", displayedRight);
    }
    text(c, scoreLeft, dp(14), dp(55), dp(37), CYAN, display, Paint.Align.LEFT);
    text(c, scoreRight, w - dp(14), dp(55), dp(37), PINK, display, Paint.Align.RIGHT);
    text(
        c,
        "RALLY / SYSTEM 001",
        w / 2,
        dp(24),
        dp(6),
        Color.rgb(134, 141, 170),
        mono,
        Paint.Align.CENTER);
    text(c, engine.state.name(), w / 2, dp(40), dp(7), LIME, mono, Paint.Align.CENTER);
    c.save();
    c.clipRect(field);
    c.translate(field.left, field.top);
    float scale = field.width() / 1000;
    c.scale(scale, scale);
    if (effects && shake > 0)
      c.translate((random.nextFloat() - .5f) * shake, (random.nextFloat() - .5f) * shake);
    float eh = engine.height;
    paint.setStyle(Paint.Style.FILL);
    paint.setShader(fieldGradient);
    c.drawRect(0, 0, 1000, eh, paint);
    paint.setShader(null);
    paint.setColor(Color.argb(24, 120, 130, 190));
    paint.setStrokeWidth(1);
    for (int x = 0; x <= 1000; x += 50) c.drawLine(x, 0, x, eh, paint);
    float drift = effects ? (elapsed * 8) % 40 : 0;
    for (float y = -40 + drift; y < eh; y += 40) c.drawLine(0, y, 1000, y, paint);
    paint.setColor(Color.argb(80, 144, 156, 201));
    paint.setStrokeWidth(2);
    for (int y = 0; y < eh; y += 18) c.drawLine(500, y, 500, y + 6, paint);
    paint.setStyle(Paint.Style.STROKE);
    c.drawRect(22, 22, 978, eh - 22, paint);
    c.drawCircle(500, eh / 2, 75, paint);
    paint.setStyle(Paint.Style.FILL);
    text(
        c,
        "OVERDRIVE",
        500,
        eh / 2 + 30,
        115,
        Color.argb(12, 180, 170, 255),
        display,
        Paint.Align.CENTER);
    for (int i = 1; i < trail.size(); i++) {
      float[] p = trail.get(i), prev = trail.get(i - 1);
      paint.setColor(Color.argb((int) (95f * i / trail.size()), 217, 255, 105));
      paint.setStrokeCap(Paint.Cap.ROUND);
      paint.setStrokeWidth(18f * i / trail.size());
      c.drawLine(prev[0], prev[1], p[0], p[1], paint);
    }
    paint.setStrokeCap(Paint.Cap.BUTT);
    for (int side = 0; side < 2; side++) {
      float x = side == 0 ? 42 : 944, y = engine.paddles[side];
      int color = side == 0 ? CYAN : PINK;
      rect(
          c,
          x - 6,
          y - 5,
          x + 20,
          y + engine.paddleHeight + 5,
          Color.argb(25, Color.red(color), Color.green(color), Color.blue(color)),
          8);
      rect(c, x, y, x + 14, y + engine.paddleHeight, color, 5);
      rect(c, x + 4, y + 8, x + 6, y + engine.paddleHeight - 8, Color.argb(180, 255, 255, 255), 1);
    }
    if (engine.state != PongEngine.State.READY && engine.state != PongEngine.State.OVER) {
      if (effects) {
        paint.setColor(Color.argb(25, 217, 255, 105));
        c.drawCircle(engine.x, engine.y, 22, paint);
      }
      paint.setColor(LIME);
      c.drawCircle(engine.x, engine.y, 9, paint);
    }
    for (float[] ring : rings) {
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2);
      paint.setColor(ring[3] == 0 ? CYAN : PINK);
      paint.setAlpha((int) (255 * ring[2] / .5f));
      c.drawCircle(ring[0], ring[1], 12 + (.5f - ring[2]) * 150, paint);
    }
    paint.setAlpha(255);
    paint.setStyle(Paint.Style.FILL);
    for (Spark p : sparks) {
      paint.setColor(p.color);
      paint.setAlpha((int) (255 * Math.max(0, p.life / p.max)));
      c.drawRect(p.x, p.y, p.x + 5, p.y + 5, paint);
    }
    paint.setAlpha(255);
    c.restore();
    if (engine.state == PongEngine.State.READY
        || engine.state == PongEngine.State.PAUSED
        || engine.state == PongEngine.State.OVER) overlay(c);
    if (engine.state == PongEngine.State.COUNTDOWN)
      text(
          c,
          "" + Math.max(1, (int) Math.ceil(engine.timer / .8f)),
          field.centerX(),
          field.centerY() + dp(28),
          dp(90),
          LIME,
          display,
          Paint.Align.CENTER);
    if (engine.state == PongEngine.State.POINT)
      text(
          c,
          "+1 POINT",
          field.centerX(),
          field.centerY(),
          dp(36),
          LIME,
          display,
          Paint.Align.CENTER);
    float foot = h - dp(10);
    text(c, "RALLY  " + engine.rally, dp(14), foot, dp(8), CYAN, mono, Paint.Align.LEFT);
    text(c, "BEST  " + engine.bestRally, w - dp(14), foot, dp(8), LIME, mono, Paint.Align.RIGHT);
  }

  private void overlay(Canvas c) {
    rect(c, field, Color.argb(150, 8, 12, 27), 0);
    float cx = field.centerX(), cy = field.centerY();
    boolean ready = engine.state == PongEngine.State.READY;
    float amp = effects ? (float) Math.sin(elapsed * 2) * dp(6) : 0;
    if (ready && field.height() > dp(230)) {
      paint.setColor(Color.argb(70, 130, 103, 211));
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(dp(1));
      c.drawOval(cx - dp(72), cy - dp(86), cx + dp(72), cy - dp(51), paint);
      paint.setStyle(Paint.Style.FILL);
      rect(c, cx - dp(49), cy - dp(96) + amp, cx - dp(37), cy - dp(45) + amp, CYAN, dp(5));
      rect(c, cx + dp(37), cy - dp(96) - amp, cx + dp(49), cy - dp(45) - amp, PINK, dp(5));
      paint.setColor(LIME);
      c.drawCircle(
          cx + (effects ? (float) Math.sin(elapsed * 2.5) * dp(28) : 0), cy - dp(69), dp(5), paint);
    }
    String title =
        ready
            ? "READY TO RALLY?"
            : engine.state == PongEngine.State.PAUSED
                ? "TAKE A BREATHER."
                : engine.solo
                    ? (engine.winner == 0 ? "YOU WIN." : "BOT WINS.")
                    : (engine.winner == 0 ? "PLAYER 01 WINS." : "PLAYER 02 WINS.");
    boolean compact = field.height() < dp(190);
    float size = Math.min(dp(compact ? 28 : 38), field.width() / 10);
    text(c, title, cx, cy + dp(compact ? -10 : 3), size, Color.WHITE, display, Paint.Align.CENTER);
    if (!compact)
      text(
          c,
          ready
              ? "QUICK HANDS. FAST RALLIES."
              : engine.state == PongEngine.State.PAUSED
                  ? "YOUR MATCH IS RIGHT HERE."
                  : engine.scores[0] + " — " + engine.scores[1] + "  //  GOOD GAME",
          cx,
          cy + dp(24),
          dp(8),
          Color.rgb(162, 171, 199),
          mono,
          Paint.Align.CENTER);
    rect(
        c,
        cx - dp(84),
        cy + dp(compact ? 5 : 42),
        cx + dp(84),
        cy + dp(compact ? 53 : 90),
        LIME,
        dp(6));
    text(
        c,
        ready
            ? "LET'S PLAY  ↗"
            : engine.state == PongEngine.State.PAUSED ? "RESUME  ↗" : "PLAY AGAIN  ↗",
        cx,
        cy + dp(compact ? 34 : 71),
        dp(11),
        BG,
        mono,
        Paint.Align.CENTER);
  }

  @Override
  public boolean performClick() {
    super.performClick();
    engine.start();
    requestFocus();
    beep(60);
    changed();
    return true;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!field.contains(e.getX(), e.getY()) && e.getActionMasked() == MotionEvent.ACTION_DOWN)
      return false;
    int action = e.getActionMasked(), index = e.getActionIndex(), id = e.getPointerId(index);
    if (action == MotionEvent.ACTION_DOWN
        && (engine.state == PongEngine.State.READY
            || engine.state == PongEngine.State.PAUSED
            || engine.state == PongEngine.State.OVER)) {
      pendingStart = true;
      return true;
    }
    if (pendingStart) {
      if (action == MotionEvent.ACTION_UP) {
        pendingStart = false;
        performClick();
      } else if (action == MotionEvent.ACTION_CANCEL) pendingStart = false;
      return true;
    }
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
      pointers.put(id, engine.solo ? 0 : e.getX(index) < field.centerX() ? 0 : 1);
      requestFocus();
    }
    if (engine.state == PongEngine.State.PLAYING
        || engine.state == PongEngine.State.COUNTDOWN
        || engine.state == PongEngine.State.POINT)
      for (int i = 0; i < e.getPointerCount(); i++) {
        int side = pointers.get(e.getPointerId(i), -1);
        if (side >= 0) engine.move(side, (e.getY(i) - field.top) / field.height() * engine.height);
      }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
      pointers.delete(id);
    if (action == MotionEvent.ACTION_CANCEL) pointers.clear();
    return true;
  }

  private int key(int code) {
    return code == KeyEvent.KEYCODE_W
        ? 1
        : code == KeyEvent.KEYCODE_S
            ? 2
            : code == KeyEvent.KEYCODE_DPAD_UP ? 4 : code == KeyEvent.KEYCODE_DPAD_DOWN ? 8 : 0;
  }

  @Override
  public boolean onKeyDown(int code, KeyEvent event) {
    if (code == KeyEvent.KEYCODE_SPACE || code == KeyEvent.KEYCODE_P) {
      if (event.getRepeatCount() == 0) {
        if (engine.state == PongEngine.State.READY
            || engine.state == PongEngine.State.OVER
            || engine.state == PongEngine.State.PAUSED) performClick();
        else {
          engine.pause();
          changed();
        }
      }
      return true;
    }
    int bit = key(code);
    if (bit != 0) {
      keys |= bit;
      return true;
    }
    return super.onKeyDown(code, event);
  }

  @Override
  public boolean onKeyUp(int code, KeyEvent event) {
    int bit = key(code);
    if (bit != 0) {
      keys &= ~bit;
      return true;
    }
    return super.onKeyUp(code, event);
  }
}
