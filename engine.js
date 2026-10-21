export const W = 1000,
  H = 600,
  PW = 14,
  PH = 104,
  R = 9;
export class Pong {
  constructor(random = Math.random) {
    this.random = random;
    this.reset();
  }
  reset(mode = "solo", difficulty = "normal", target = 7) {
    Object.assign(this, {
      mode,
      difficulty,
      target,
      scores: [0, 0],
      paddles: [248, 248],
      ball: { x: 500, y: 300, vx: 0, vy: 0 },
      state: "ready",
      timer: 0,
      rally: 0,
      bestRally: 0,
      winner: null,
      events: [],
      serveDirection: this.random() < 0.5 ? -1 : 1,
    });
  }
  emit(type, data = {}) {
    this.events.push({ type, ...data });
  }
  drain() {
    return this.events.splice(0);
  }
  start() {
    if (this.state === "ready") {
      this.state = "countdown";
      this.timer = 2.4;
    }
  }
  pause() {
    if (["playing", "countdown", "point"].includes(this.state)) {
      this.previousState = this.state;
      this.state = "paused";
    } else if (this.state === "paused") this.state = this.previousState;
  }
  move(index, y) {
    this.paddles[index] = Math.max(0, Math.min(H - PH, y - PH / 2));
  }
  serve() {
    const angle = (this.random() - 0.5) * 0.9,
      speed = 420;
    this.ball = {
      x: W / 2,
      y: H / 2,
      vx: Math.cos(angle) * speed * this.serveDirection,
      vy: Math.sin(angle) * speed,
    };
    this.rally = 0;
    this.state = "playing";
    this.emit("serve");
  }
  step(dt, inputs = [0, 0]) {
    dt = Math.max(0, Math.min(dt, 0.1));
    if (["ready", "paused", "over"].includes(this.state)) return;
    for (let i = 0; i < 2; i++)
      if (!(i === 1 && this.mode === "solo"))
        this.paddles[i] = Math.max(
          0,
          Math.min(H - PH, this.paddles[i] + inputs[i] * 560 * dt),
        );
    if (this.state === "countdown" || this.state === "point") {
      this.timer -= dt;
      if (this.timer <= 0) this.serve();
      return;
    }
    if (this.mode === "solo") {
      const speed =
        { easy: 210, normal: 330, hard: 465 }[this.difficulty] || 330;
      const aim = this.ball.vx > 0 ? this.ball.y : H / 2;
      let delta = aim - PH / 2 - this.paddles[1];
      this.paddles[1] = Math.max(
        0,
        Math.min(
          H - PH,
          this.paddles[1] + Math.max(-speed * dt, Math.min(speed * dt, delta)),
        ),
      );
    }
    // Small physics steps prevent fast balls passing through paddles.
    let n = Math.max(1, Math.ceil(dt / 0.004));
    for (let k = 0; k < n && this.state === "playing"; k++)
      this.physics(dt / n);
  }
  physics(dt) {
    let b = this.ball;
    const oldX = b.x;
    b.x += b.vx * dt;
    b.y += b.vy * dt;
    if (b.y < R) {
      b.y = 2 * R - b.y;
      b.vy = Math.abs(b.vy);
      this.emit("wall");
    } else if (b.y > H - R) {
      b.y = 2 * (H - R) - b.y;
      b.vy = -Math.abs(b.vy);
      this.emit("wall");
    }
    for (let i = 0; i < 2; i++) {
      const face = i === 0 ? 42 + PW : W - 42 - PW;
      const crossing =
        i === 0
          ? b.vx < 0 && oldX - R >= face && b.x - R <= face
          : b.vx > 0 && oldX + R <= face && b.x + R >= face;
      if (
        crossing &&
        b.y + R >= this.paddles[i] &&
        b.y - R <= this.paddles[i] + PH
      ) {
        const offset = Math.max(
          -1,
          Math.min(1, (b.y - this.paddles[i] - PH / 2) / (PH / 2)),
        );
        const speed = Math.min(850, Math.hypot(b.vx, b.vy) * 1.055);
        const angle = (offset * Math.PI) / 3;
        b.x = face + (i === 0 ? R : -R);
        b.vx = Math.cos(angle) * speed * (i === 0 ? 1 : -1);
        b.vy = Math.sin(angle) * speed;
        this.rally++;
        this.bestRally = Math.max(this.rally, this.bestRally);
        this.emit("hit", { x: b.x, y: b.y, side: i });
      }
    }
    if (b.x < -R || b.x > W + R) {
      let scorer = b.x < 0 ? 1 : 0;
      this.scores[scorer]++;
      this.serveDirection = scorer === 0 ? 1 : -1;
      this.emit("score", { scorer });
      b.vx = 0;
      b.vy = 0;
      if (this.scores[scorer] >= this.target) {
        this.state = "over";
        this.winner = scorer;
        this.emit("win", { winner: scorer });
      } else {
        this.state = "point";
        this.timer = 1.25;
      }
    }
  }
}
