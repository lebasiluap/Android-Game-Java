import test from "node:test";
import assert from "node:assert/strict";
import { Pong, PH } from "../engine.js";
test("ready, countdown, pause and resume preserve match", () => {
  const g = new Pong(() => 0.5);
  g.start();
  g.step(0.1);
  assert.equal(g.state, "countdown");
  const t = g.timer;
  g.pause();
  g.step(0.1);
  assert.equal(g.timer, t);
  g.pause();
  assert.equal(g.state, "countdown");
  for (let i = 0; i < 30; i++) g.step(0.1);
  assert.equal(g.state, "playing");
});
test("a point is recorded once and match ends at target", () => {
  const g = new Pong();
  g.reset("duo", "normal", 5);
  g.state = "playing";
  g.scores = [4, 2];
  g.ball = { x: 1008, y: 300, vx: 500, vy: 0 };
  g.step(0.02);
  assert.deepEqual(g.scores, [5, 2]);
  assert.equal(g.state, "over");
  assert.equal(g.winner, 0);
  for (let i = 0; i < 100; i++) g.step(0.1);
  assert.deepEqual(g.scores, [5, 2]);
});
test("left exit gives right player one point, then resets serve", () => {
  const g = new Pong(() => 0.5);
  g.state = "playing";
  g.ball = { x: -8, y: 300, vx: -500, vy: 0 };
  g.step(0.02);
  assert.deepEqual(g.scores, [0, 1]);
  assert.equal(g.state, "point");
  for (let i = 0; i < 14; i++) g.step(0.1);
  assert.equal(g.state, "playing");
  assert.deepEqual(g.scores, [0, 1]);
});
test("fast ball hits paddle without tunnelling or repeated reversal", () => {
  const g = new Pong();
  g.reset("duo");
  g.state = "playing";
  g.ball = { x: 100, y: 300, vx: -850, vy: 0 };
  g.step(0.1);
  assert.ok(g.ball.vx > 0);
  assert.equal(g.rally, 1);
  g.step(0.02);
  assert.ok(g.ball.vx > 0);
  assert.equal(g.rally, 1);
});
test("edge returns change angle and speed stays capped", () => {
  const g = new Pong();
  g.reset("duo");
  g.state = "playing";
  g.ball = { x: 65, y: 252, vx: -850, vy: 0 };
  g.step(0.01);
  assert.ok(g.ball.vy < 0);
  assert.ok(Math.hypot(g.ball.vx, g.ball.vy) <= 850.001);
});
test("wall bounces point inward and paddles stay on court", () => {
  const g = new Pong();
  g.state = "playing";
  g.ball = { x: 400, y: 10, vx: 200, vy: -400 };
  g.step(0.02);
  assert.ok(g.ball.vy > 0);
  g.move(0, -900);
  g.move(1, 5000);
  assert.deepEqual(g.paddles, [0, 600 - PH]);
});
test("equivalent frame durations produce consistent trajectories", () => {
  const a = new Pong(() => 0.5),
    b = new Pong(() => 0.5);
  for (const g of [a, b]) {
    g.reset("duo");
    g.state = "playing";
    g.ball = { x: 500, y: 300, vx: 300, vy: 100 };
  }
  for (let i = 0; i < 30; i++) a.step(1 / 30);
  for (let i = 0; i < 144; i++) b.step(1 / 144);
  assert.ok(Math.abs(a.ball.x - b.ball.x) < 0.001);
  assert.ok(Math.abs(a.ball.y - b.ball.y) < 0.001);
});
test("AI is speed limited and reset clears previous match", () => {
  const g = new Pong();
  g.state = "playing";
  g.ball = { x: 700, y: 20, vx: 400, vy: 0 };
  const before = g.paddles[1];
  g.step(0.1);
  assert.ok(Math.abs(g.paddles[1] - before) <= 33.001);
  g.reset();
  assert.deepEqual(g.scores, [0, 0]);
  assert.equal(g.bestRally, 0);
  assert.equal(g.state, "ready");
});
