package com.paulsackey.rallyclub;

import java.util.Random;

/** Pure Java game simulation, independent of Android UI and frame rate. */
public final class PongEngine {
  public enum State {
    READY,
    COUNTDOWN,
    PLAYING,
    POINT,
    PAUSED,
    OVER
  }

  public float width = 1000, height = 1000, paddleHeight = 170;
  public static final float PADDLE_WIDTH = 14, RADIUS = 9;
  public final float[] paddles = {415, 415};
  public final int[] scores = {0, 0};
  public float x = 500, y = 500, vx, vy, timer;
  public int rally, bestRally, winner = -1, difficulty = 1, target = 7;
  public boolean solo = true;
  public State state = State.READY;
  private State previous = State.READY;
  private int direction = 1;
  private final Random random;

  public interface Events {
    void hit(int side, float x, float y);

    void score(int side, boolean won);

    void wall();
  }

  public Events events;

  public PongEngine() {
    this(new Random());
  }

  public PongEngine(Random random) {
    this.random = random;
    reset();
  }

  public void reset() {
    scores[0] = scores[1] = 0;
    paddles[0] = paddles[1] = (height - paddleHeight) / 2;
    x = width / 2;
    y = height / 2;
    vx = vy = 0;
    rally = bestRally = 0;
    winner = -1;
    timer = 0;
    state = State.READY;
    direction = random.nextBoolean() ? 1 : -1;
  }

  public void resizeHeight(float next) {
    if (next <= 0) return;
    float ratio = next / height;
    height = next;
    paddleHeight = next * .17f;
    paddles[0] *= ratio;
    paddles[1] *= ratio;
    y *= ratio;
  }

  public void start() {
    if (state == State.OVER) reset();
    if (state == State.READY) {
      state = State.COUNTDOWN;
      timer = 2.4f;
    } else if (state == State.PAUSED) state = previous;
  }

  public void pause() {
    if (state == State.PLAYING || state == State.COUNTDOWN || state == State.POINT) {
      previous = state;
      state = State.PAUSED;
    }
  }

  public void togglePause() {
    if (state == State.PAUSED) start();
    else pause();
  }

  public void move(int side, float center) {
    paddles[side] = Math.max(0, Math.min(height - paddleHeight, center - paddleHeight / 2));
  }

  public State resumeState() {
    return state == State.PAUSED ? previous : state;
  }

  public int serveDirection() {
    return direction;
  }

  public void restoreState(int ordinal, int serveDirection) {
    direction = serveDirection;
    State restored = State.values()[Math.max(0, Math.min(State.values().length - 1, ordinal))];
    if (restored == State.READY || restored == State.OVER) state = restored;
    else {
      previous = restored == State.PAUSED ? State.PLAYING : restored;
      state = State.PAUSED;
    }
  }

  private void serve() {
    float angle = (random.nextFloat() - .5f) * .9f;
    x = width / 2;
    y = height / 2;
    vx = (float) Math.cos(angle) * 420 * direction;
    vy = (float) Math.sin(angle) * 420;
    rally = 0;
    state = State.PLAYING;
  }

  public void step(float delta) {
    if (state == State.READY || state == State.PAUSED || state == State.OVER) return;
    float dt = Math.max(0, Math.min(.1f, delta));
    if (state == State.COUNTDOWN || state == State.POINT) {
      timer -= dt;
      if (timer <= 0) serve();
      return;
    }
    if (solo) {
      float speed = (difficulty == 0 ? 210 : difficulty == 1 ? 330 : 465),
          aim = vx > 0 ? y : height / 2,
          change = aim - paddleHeight / 2 - paddles[1];
      paddles[1] =
          Math.max(
              0,
              Math.min(
                  height - paddleHeight,
                  paddles[1] + Math.max(-speed * dt, Math.min(speed * dt, change))));
    }
    int steps = Math.max(1, (int) Math.ceil(dt / .004f));
    for (int i = 0; i < steps && state == State.PLAYING; i++) physics(dt / steps);
  }

  private void physics(float dt) {
    float oldX = x;
    x += vx * dt;
    y += vy * dt;
    if (y < RADIUS) {
      y = 2 * RADIUS - y;
      vy = Math.abs(vy);
      if (events != null) events.wall();
    } else if (y > height - RADIUS) {
      y = 2 * (height - RADIUS) - y;
      vy = -Math.abs(vy);
      if (events != null) events.wall();
    }
    for (int side = 0; side < 2; side++) {
      float face = side == 0 ? 42 + PADDLE_WIDTH : width - 42 - PADDLE_WIDTH;
      boolean crossing =
          side == 0
              ? (vx < 0 && oldX - RADIUS >= face && x - RADIUS <= face)
              : (vx > 0 && oldX + RADIUS <= face && x + RADIUS >= face);
      if (crossing && y + RADIUS >= paddles[side] && y - RADIUS <= paddles[side] + paddleHeight) {
        float
            offset =
                Math.max(
                    -1, Math.min(1, (y - paddles[side] - paddleHeight / 2) / (paddleHeight / 2))),
            speed = Math.min(850, (float) Math.hypot(vx, vy) * 1.055f),
            angle = offset * (float) Math.PI / 3;
        x = face + (side == 0 ? RADIUS : -RADIUS);
        vx = (float) Math.cos(angle) * speed * (side == 0 ? 1 : -1);
        vy = (float) Math.sin(angle) * speed;
        rally++;
        bestRally = Math.max(bestRally, rally);
        if (events != null) events.hit(side, x, y);
      }
    }
    if (x < -RADIUS || x > width + RADIUS) {
      int side = x < 0 ? 1 : 0;
      scores[side]++;
      direction = side == 0 ? 1 : -1;
      vx = vy = 0;
      if (scores[side] >= target) {
        winner = side;
        state = State.OVER;
      } else {
        state = State.POINT;
        timer = 1.25f;
      }
      if (events != null) events.score(side, state == State.OVER);
    }
  }
}
