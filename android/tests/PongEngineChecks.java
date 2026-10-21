import com.paulsackey.rallyclub.PongEngine;
import java.util.Random;

public final class PongEngineChecks {
  static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  static PongEngine game() {
    PongEngine e = new PongEngine(new Random(1));
    e.solo = false;
    e.resizeHeight(600);
    e.state = PongEngine.State.PLAYING;
    return e;
  }

  public static void main(String[] args) {
    PongEngine e = game();
    e.scores[0] = 6;
    e.x = 1008;
    e.y = 300;
    e.vx = 500;
    e.step(.02f);
    require(
        e.scores[0] == 7 && e.state == PongEngine.State.OVER && e.winner == 0,
        "single scoring / winner");
    for (int i = 0; i < 100; i++) e.step(.1f);
    require(e.scores[0] == 7, "no repeated scoring");
    e = game();
    e.x = 100;
    e.y = 300;
    e.vx = -850;
    e.vy = 0;
    e.step(.1f);
    require(e.vx > 0 && e.rally == 1, "fast collision");
    e.step(.02f);
    require(e.rally == 1 && e.vx > 0, "no repeated reversal");
    e = game();
    e.x = 400;
    e.y = 10;
    e.vx = 200;
    e.vy = -400;
    e.step(.02f);
    require(e.vy > 0, "wall return");
    e.move(0, -100);
    e.move(1, 5000);
    require(e.paddles[0] == 0 && e.paddles[1] == 600 - e.paddleHeight, "paddle bounds");
    e = game();
    e.pause();
    float old = e.x;
    e.step(.1f);
    require(e.x == old && e.state == PongEngine.State.PAUSED, "paused simulation");
    e.start();
    require(e.state == PongEngine.State.PLAYING, "resume");
    e = game();
    e.state = PongEngine.State.READY;
    e.start();
    for (int i = 0; i < 25; i++) e.step(.1f);
    require(e.state == PongEngine.State.PLAYING, "countdown serve");
    e = game();
    e.x = -8;
    e.vx = -500;
    e.step(.02f);
    require(e.scores[1] == 1 && e.state == PongEngine.State.POINT, "right score");
    for (int i = 0; i < 14; i++) e.step(.1f);
    require(e.state == PongEngine.State.PLAYING && e.scores[1] == 1, "next serve");
    PongEngine a = game(), b = game();
    a.x = b.x = 500;
    a.y = b.y = 300;
    a.vx = b.vx = 300;
    a.vy = b.vy = 100;
    for (int i = 0; i < 30; i++) a.step(1f / 30);
    for (int i = 0; i < 144; i++) b.step(1f / 144);
    require(Math.abs(a.x - b.x) < .05 && Math.abs(a.y - b.y) < .05, "frame-rate consistency");
    e = game();
    e.y = 150;
    e.move(0, 150);
    float center = e.paddles[0] + e.paddleHeight / 2;
    e.resizeHeight(1200);
    require(
        Math.abs(e.y - 300) < .01
            && Math.abs((e.paddles[0] + e.paddleHeight / 2) - center * 2) < .01,
        "resize preserves normalized positions");
    e.restoreState(PongEngine.State.PLAYING.ordinal(), -1);
    require(
        e.state == PongEngine.State.PAUSED && e.serveDirection() == -1, "restore safely pauses");
    e.start();
    require(e.state == PongEngine.State.PLAYING, "restored match resumes");
    System.out.println("8 native engine checks passed.");
  }
}
