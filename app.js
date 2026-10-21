import { Pong, W, H, PW, PH, R } from "./engine.js";
const $ = (s) => document.querySelector(s),
  game = new Pong(),
  canvas = $("#court"),
  ctx = canvas.getContext("2d");
let lastUI = "";
let elapsed = 0,
  rings = [],
  shake = 0;
let mode = "solo",
  sound = false,
  audio = null,
  last = 0,
  particles = [],
  trail = [],
  keys = new Set(),
  pointers = new Map(),
  reduced = matchMedia("(prefers-reduced-motion: reduce)").matches;
function applyMotion() {
  document.documentElement.classList.toggle("motion-off", reduced);
  $("#motion").setAttribute("aria-pressed", String(!reduced));
  $("#motion").textContent = reduced ? "FX OFF" : "FX ON";
  if (reduced) {
    particles = [];
    rings = [];
    trail = [];
    shake = 0;
  }
}
applyMotion();
$("#motion").addEventListener("click", () => {
  reduced = !reduced;
  applyMotion();
});
matchMedia("(prefers-reduced-motion: reduce)").addEventListener(
  "change",
  (e) => {
    reduced = e.matches;
    applyMotion();
  },
);
$("#fullscreen").addEventListener("click", async () => {
  try {
    if (document.fullscreenElement) await document.exitFullscreen();
    else if ($("#arena").requestFullscreen)
      await $("#arena").requestFullscreen();
    else announce("Full screen is unavailable in this browser.");
  } catch {
    announce("Full screen is unavailable in this browser.");
  }
});
document.addEventListener("fullscreenchange", () => {
  $("#fullscreen").textContent = document.fullscreenElement
    ? "SHRINK ⛶"
    : "EXPAND ⛶";
});
function pulse(element) {
  if (!reduced)
    element.animate(
      [
        { transform: "translateY(-7px) scale(1.15)", filter: "brightness(2)" },
        { transform: "translateY(0) scale(1)", filter: "brightness(1)" },
      ],
      { duration: 380, easing: "cubic-bezier(.2,.8,.2,1)" },
    );
}
function impact(text, color) {
  if (reduced) return;
  const pop = $("#point-pop");
  pop.textContent = text;
  pop.style.color = color;
  pop.getAnimations().forEach((a) => a.cancel());
  pop.animate(
    [
      { opacity: 0, transform: "translate(-50%,-30%) scale(.8)" },
      { opacity: 1, offset: 0.18, transform: "translate(-50%,-50%) scale(1)" },
      { opacity: 1, offset: 0.7 },
      { opacity: 0, transform: "translate(-50%,-70%) scale(1.06)" },
    ],
    { duration: 1000, easing: "ease-out" },
  );
}
function tone(freq = 440, duration = 0.07) {
  if (!sound) return;
  try {
    audio ??= new (window.AudioContext || window.webkitAudioContext)();
    audio.resume();
    const osc = audio.createOscillator(),
      gain = audio.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(freq, audio.currentTime);
    gain.gain.setValueAtTime(0.075, audio.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, audio.currentTime + duration);
    osc.connect(gain).connect(audio.destination);
    osc.start();
    osc.stop(audio.currentTime + duration);
  } catch {
    sound = false;
    $("#sound").textContent = "SOUND UNAVAILABLE";
  }
}
function announce(text) {
  $("#announcement").textContent = text;
}
function reset() {
  game.reset(mode, $("#difficulty").value, Number($("#target").value));
  particles = [];
  rings = [];
  shake = 0;
  trail = [];
  $("#point-pop")
    .getAnimations()
    .forEach((a) => a.cancel());
  keys.clear();
  pointers.clear();
  sync();
}
function sync() {
  const signature = JSON.stringify([
    mode,
    game.state,
    game.scores,
    game.rally,
    game.bestRally,
    game.winner,
    Math.ceil(game.timer / 0.8),
  ]);
  if (signature === lastUI) return;
  lastUI = signature;
  const state = game.state,
    left = mode === "solo" ? "YOU" : "PLAYER 01",
    right = mode === "solo" ? "THE MACHINE" : "PLAYER 02";
  $("#left-name").textContent = left;
  $("#right-name").textContent = right;
  $("#left-score").textContent = String(game.scores[0]).padStart(2, "0");
  $("#right-score").textContent = String(game.scores[1]).padStart(2, "0");
  $("#rally").textContent = game.rally;
  $("#best").textContent = game.bestRally;
  $("#energy").style.width = `${Math.min(100, (game.rally / 15) * 100)}%`;
  $("#arena").dataset.state = state;
  $("#difficulty").disabled = mode === "duo";
  $("#control-hint").textContent =
    mode === "solo"
      ? "W / S · ↑ / ↓ · DRAG TO MOVE"
      : "P1: W / S · P2: ↑ / ↓ · DRAG YOUR HALF";
  const labels = {
    ready: "READY WHEN YOU ARE",
    playing: "KEEP IT IN PLAY",
    paused: "TAKE YOUR TIME",
    countdown: "HERE WE GO",
    point: "NICE POINT",
    over: "THAT’S A MATCH",
  };
  $("#status").textContent = labels[state];
  $("#pause").disabled = ["ready", "over"].includes(state);
  $("#pause").innerHTML =
    state === "paused" ? "RESUME <span>▷</span>" : "PAUSE <span>Ⅱ</span>";
  const visible = ["ready", "paused", "over"].includes(state);
  $("#overlay").hidden = !visible;
  if (visible) {
    $("#overlay-kicker").textContent =
      state === "over"
        ? "MATCH COMPLETE // RUN IT BACK?"
        : state === "paused"
          ? "SYSTEM PAUSED"
          : "THE NEXT POINT IS YOURS";
    $("#overlay-title").textContent =
      state === "over"
        ? mode === "solo"
          ? game.winner === 0
            ? "YOU WIN."
            : "BOT WINS."
          : game.winner === 0
            ? "PLAYER 01 WINS."
            : "PLAYER 02 WINS."
        : state === "paused"
          ? "TAKE A BREATHER."
          : "READY TO RALLY?";
    $("#overlay-description").textContent =
      state === "over"
        ? `${game.scores[0]} — ${game.scores[1]} · Best rally: ${game.bestRally} returns`
        : state === "paused"
          ? "Your match will be right here."
          : mode === "solo"
            ? "Quick hands. Fast rallies. Zero excuses."
            : "Two players. One screen. Settle it on the court.";
    $("#overlay-title").classList.toggle("ready-title", state === "ready");
    $("#start").innerHTML =
      "<span>" +
      (state === "over"
        ? "PLAY AGAIN"
        : state === "paused"
          ? "RESUME GAME"
          : "LET'S PLAY") +
      "</span><b>↗</b>";
    $(".paddle-scene").hidden = state !== "ready";
    $(".start-hint").hidden = state !== "ready";
  }
  $("#countdown").textContent =
    state === "countdown" ? Math.max(1, Math.ceil(game.timer / 0.8)) : "";
}
function start() {
  if (game.state === "over") reset();
  if (game.state === "paused") game.pause();
  else game.start();
  tone(350);
  canvas.focus({ preventScroll: true });
  sync();
}
$("#start").addEventListener("click", start);
$("#restart").addEventListener("click", () => {
  reset();
  announce("New match ready.");
});
$("#pause").addEventListener("click", () => {
  game.pause();
  keys.clear();
  sync();
});
$("#difficulty").addEventListener("change", reset);
$("#target").addEventListener("change", reset);
for (const button of document.querySelectorAll("[data-mode]"))
  button.addEventListener("click", () => {
    mode = button.dataset.mode;
    for (const b of document.querySelectorAll("[data-mode]"))
      b.setAttribute("aria-pressed", String(b === button));
    reset();
  });
$("#sound").addEventListener("click", () => {
  sound = !sound;
  $("#sound").setAttribute("aria-pressed", String(sound));
  $("#sound").textContent = sound ? "SOUND ON" : "SOUND OFF";
  tone(500);
});
function pauseForInterruption() {
  keys.clear();
  pointers.clear();
  if (["playing", "point", "countdown"].includes(game.state)) {
    game.pause();
    sync();
  }
}
$("#help").addEventListener("click", () => {
  pauseForInterruption();
  $("#help-dialog").showModal();
});
$("#close-help").addEventListener("click", () => $("#help-dialog").close());
document.addEventListener("visibilitychange", () => {
  if (document.hidden) pauseForInterruption();
});
window.addEventListener("blur", pauseForInterruption);
window.addEventListener("keydown", (e) => {
  if (
    ["SELECT", "BUTTON", "INPUT"].includes(e.target.tagName) ||
    $("#help-dialog").open
  )
    return;
  const key = e.key.toLowerCase();
  if (["arrowup", "arrowdown", "w", "s", " ", "p"].includes(key)) {
    e.preventDefault();
    if ((key === " " || key === "p") && !e.repeat) {
      if (game.state === "ready" || game.state === "over") start();
      else {
        game.pause();
        sync();
      }
    } else keys.add(key);
  }
});
window.addEventListener("keyup", (e) => keys.delete(e.key.toLowerCase()));
function pointerMove(e) {
  if (!["playing", "countdown", "point"].includes(game.state)) return;
  const rect = canvas.getBoundingClientRect(),
    x = ((e.clientX - rect.left) / rect.width) * W,
    y = ((e.clientY - rect.top) / rect.height) * H;
  let side =
    mode === "solo" ? 0 : (pointers.get(e.pointerId) ?? (x < W / 2 ? 0 : 1));
  game.move(side, y);
}
canvas.addEventListener("pointerdown", (e) => {
  const rect = canvas.getBoundingClientRect();
  pointers.set(
    e.pointerId,
    mode === "solo" ? 0 : e.clientX - rect.left < rect.width / 2 ? 0 : 1,
  );
  canvas.setPointerCapture(e.pointerId);
  canvas.focus({ preventScroll: true });
  pointerMove(e);
});
canvas.addEventListener("pointermove", (e) => {
  if (e.pointerType === "mouse" || pointers.has(e.pointerId)) pointerMove(e);
});
for (const event of ["pointerup", "pointercancel", "lostpointercapture"])
  canvas.addEventListener(event, (e) => pointers.delete(e.pointerId));
function resize() {
  const rect = canvas.getBoundingClientRect(),
    dpr = Math.min(2, devicePixelRatio || 1);
  canvas.width = Math.round(rect.width * dpr);
  canvas.height = Math.round(rect.height * dpr);
}
new ResizeObserver(resize).observe(canvas);
function draw(dt) {
  if (game.state !== "paused") elapsed += dt;
  ctx.setTransform(canvas.width / W, 0, 0, canvas.height / H, 0, 0);
  ctx.fillStyle = "#0a1020";
  ctx.fillRect(0, 0, W, H);
  ctx.save();
  if (!reduced && shake > 0) {
    ctx.translate((Math.random() - 0.5) * shake, (Math.random() - 0.5) * shake);
    shake = Math.max(0, shake - dt * 45);
  }
  let glow = ctx.createRadialGradient(W / 2, H / 2, 15, W / 2, H / 2, 620);
  glow.addColorStop(0, "#30204b55");
  glow.addColorStop(1, "#080d1b00");
  ctx.fillStyle = glow;
  ctx.fillRect(0, 0, W, H);
  // A slow drifting grid adds depth without moving the playing surface.
  ctx.lineWidth = 1;
  ctx.strokeStyle = "#5c6a9b18";
  const drift = reduced ? 0 : (elapsed * 8) % 40;
  for (let x = 0; x <= W; x += 50) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, H);
    ctx.stroke();
  }
  for (let y = -40 + drift; y < H; y += 40) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(W, y);
    ctx.stroke();
  }
  ctx.strokeStyle = "#58688b48";
  ctx.lineWidth = 2;
  ctx.strokeRect(22, 22, W - 44, H - 44);
  ctx.setLineDash([4, 12]);
  ctx.strokeStyle = "#a0b7ef55";
  ctx.beginPath();
  ctx.moveTo(W / 2, 22);
  ctx.lineTo(W / 2, H - 22);
  ctx.stroke();
  ctx.setLineDash([]);
  ctx.beginPath();
  ctx.arc(W / 2, H / 2, 75, 0, Math.PI * 2);
  ctx.strokeStyle = "#7d7ca12b";
  ctx.stroke();
  ctx.fillStyle = "#a099df09";
  ctx.font = "italic 900 145px Impact, Arial";
  ctx.textAlign = "center";
  ctx.fillText("OVERDRIVE", W / 2, H / 2 + 48);
  // Colored goal rails frame the play field.
  for (let side = 0; side < 2; side++) {
    ctx.strokeStyle = side ? "#ff6cbd50" : "#60f6ef50";
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(side ? W - 22 : 22, 36);
    ctx.lineTo(side ? W - 22 : 22, H - 36);
    ctx.stroke();
  }
  if (!reduced && game.state === "playing") {
    trail.push({ x: game.ball.x, y: game.ball.y });
    if (trail.length > 22) trail.shift();
  } else trail = [];
  if (trail.length > 1) {
    for (let i = 1; i < trail.length; i++) {
      ctx.beginPath();
      ctx.strokeStyle = `rgba(217,255,105,${(i / trail.length) * 0.45})`;
      ctx.lineWidth = (R * 2 * i) / trail.length;
      ctx.lineCap = "round";
      ctx.moveTo(trail[i - 1].x, trail[i - 1].y);
      ctx.lineTo(trail[i].x, trail[i].y);
      ctx.stroke();
    }
  }
  for (let i = 0; i < 2; i++) {
    const x = i === 0 ? 42 : W - 42 - PW,
      y = game.paddles[i],
      color = i === 0 ? "#60f6ef" : "#ff6cbd";
    ctx.shadowColor = color;
    ctx.shadowBlur = reduced ? 0 : 20;
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.roundRect(x, y, PW, PH, 5);
    ctx.fill();
    ctx.shadowBlur = 0;
    ctx.fillStyle = "#ffffffb0";
    ctx.fillRect(x + 4, y + 8, 2, PH - 16);
    ctx.fillStyle = i === 0 ? "#60f6ef20" : "#ff6cbd20";
    ctx.beginPath();
    ctx.roundRect(x - 6, y - 5, PW + 12, PH + 10, 8);
    ctx.fill();
  }
  if (!["ready", "over"].includes(game.state)) {
    ctx.shadowColor = "#d9ff69";
    ctx.shadowBlur = reduced ? 0 : 22;
    ctx.fillStyle = "#edffb3";
    ctx.beginPath();
    ctx.arc(game.ball.x, game.ball.y, R, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;
  }
  if (game.state !== "paused") {
    particles = particles.filter((p) => p.life > 0);
    for (const p of particles) {
      p.life -= dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vy += 30 * dt;
      ctx.globalAlpha = Math.max(0, p.life / p.maxLife);
      ctx.fillStyle = p.color || "#d9ff69";
      ctx.fillRect(p.x, p.y, p.size || 4, p.size || 4);
    }
    ctx.globalAlpha = 1;
    rings = rings.filter((r) => r.life > 0);
    for (const r of rings) {
      r.life -= dt;
      ctx.globalAlpha = Math.max(0, r.life / 0.5);
      ctx.strokeStyle = r.color;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(r.x, r.y, (0.5 - r.life) * 160 + 12, 0, Math.PI * 2);
      ctx.stroke();
    }
    ctx.globalAlpha = 1;
  }
  ctx.restore();
}
function frame(time) {
  const dt = Math.min((time - last) / 1000 || 0, 0.05);
  last = time;
  const left = (keys.has("s") ? 1 : 0) - (keys.has("w") ? 1 : 0),
    right = (keys.has("arrowdown") ? 1 : 0) - (keys.has("arrowup") ? 1 : 0);
  game.step(dt, [mode === "solo" ? left || right : left, right]);
  for (const event of game.drain()) {
    if (event.type === "hit") {
      tone(400 + game.rally * 12);
      if (!reduced) {
        shake = 4;
        rings.push({
          x: event.x,
          y: event.y,
          life: 0.5,
          color: event.side === 0 ? "#60f6ef" : "#ff6cbd",
        });
      }
      if (!reduced)
        for (let i = 0; i < 16; i++)
          particles.push({
            x: event.x,
            y: event.y,
            vx: (Math.random() - 0.5) * 260,
            vy: (Math.random() - 0.5) * 260,
            life: 0.4,
            maxLife: 0.4,
            color: event.side === 0 ? "#60f6ef" : "#ff6cbd",
          });
    }
    if (event.type === "wall") tone(240, 0.04);
    if (event.type === "score") {
      tone(160, 0.2);
      pulse($(event.scorer === 0 ? "#left-score" : "#right-score"));
      impact(
        event.scorer === 0 ? "+1 CYAN" : "+1 PINK",
        event.scorer === 0 ? "#60f6ef" : "#ff6cbd",
      );
      trail = [];
      announce(`Score: ${game.scores[0]} to ${game.scores[1]}.`);
    }
    if (event.type === "win") {
      tone(650, 0.3);
      if (!reduced)
        for (let i = 0; i < 100; i++) {
          const life = 1 + Math.random() * 1.2;
          particles.push({
            x: W / 2,
            y: H / 3,
            vx: (Math.random() - 0.5) * 600,
            vy: (Math.random() - 0.65) * 500,
            life,
            maxLife: life,
            color: ["#60f6ef", "#ff6cbd", "#d9ff69"][i % 3],
            size: 3 + Math.random() * 4,
          });
        }
      announce(
        `Match finished. ${game.winner === 0 ? "Player one" : "Player two"} wins ${game.scores[game.winner]} to ${game.scores[1 - game.winner]}.`,
      );
    }
  }
  sync();
  draw(dt);
  requestAnimationFrame(frame);
}
sync();
requestAnimationFrame(frame);
