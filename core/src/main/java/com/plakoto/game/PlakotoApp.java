package com.plakoto.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.plakoto.engine.Board;
import com.plakoto.engine.Game;
import com.plakoto.engine.Move;

import java.security.SecureRandom;
import java.util.List;

/** Stage 4/5 local (same-device, 2 player) Plakoto screen. Placeholder visuals; premium art comes later. */
public class PlakotoApp extends ApplicationAdapter {
    static final float W = 1280, H = 720, BX = 70, BY = 50, BW = 1120, BH = 620, COL = BW / 12f, R = 34;
    static final Color WOOD = new Color(0.20f, 0.11f, 0.06f, 1), FRAME = new Color(0.12f, 0.06f, 0.03f, 1);
    static final Color TRI_A = new Color(0.62f, 0.45f, 0.28f, 1), TRI_B = new Color(0.30f, 0.17f, 0.10f, 1);
    static final Color CREAM = new Color(0.95f, 0.90f, 0.78f, 1), BLACK = new Color(0.10f, 0.10f, 0.11f, 1);

    OrthographicCamera cam; FitViewport vp; ShapeRenderer sr; SpriteBatch batch; BitmapFont font;
    final SecureRandom rnd = new SecureRandom();
    Game game; int selected = -1; String msg = ""; int[] lastDice = new int[0]; int[] openDice = new int[0];

    @Override public void create() {
        cam = new OrthographicCamera(); vp = new FitViewport(W, H, cam);
        sr = new ShapeRenderer(); batch = new SpriteBatch(); font = new BitmapFont(); font.getData().setScale(1.6f);
        newGame();
    }

    void newGame() { game = new Game(); selected = -1; lastDice = new int[0]; openDice = new int[0]; msg = "Basla: ekrana dokun (acilis zari)"; }
    int die() { return rnd.nextInt(6) + 1; }
    @Override public void resize(int w, int h) { vp.update(w, h, true); }

    // ---- geometry ----
    boolean top(int pt) { return pt >= 13; }
    float px(int pt) { int col = top(pt) ? pt - 13 : 12 - pt; return BX + col * COL + COL / 2; }
    int pointAt(float x, float y) {
        if (x < BX || x > BX + BW || y < BY || y > BY + BH) return -1;
        int col = (int) ((x - BX) / COL);
        return y > BY + BH / 2 ? 13 + col : 12 - col;
    }

    // ---- input ----
    void handleTouch(float x, float y) {
        switch (game.phase) {
            case OPENING: {
                openDice = new int[]{die(), die()};
                if (game.openingRoll(openDice[0], openDice[1])) msg = (game.current == Board.WHITE ? "Beyaz" : "Siyah") + " basliyor. Zar icin dokun";
                else msg = "Beraberlik, tekrar dokun";
                break;
            }
            case ROLL: {
                int a = die(), b = die();
                game.roll(a, b); lastDice = new int[]{a, b}; selected = -1;
                msg = game.phase == Game.Phase.ROLL ? "Yasal hamle yok, pas" : "";
                break;
            }
            case MOVE: {
                List<Move> legal = game.legalMoves();
                int pt = pointAt(x, y);
                boolean offPanel = x > BX + BW;
                if (selected > 0) {
                    for (Move m : legal) if (m.from == selected && ((offPanel && m.to == 0) || (!offPanel && m.to == pt))) {
                        game.play(m); selected = -1;
                        if (game.phase == Game.Phase.FINISHED) msg = (game.winner == Board.WHITE ? "Beyaz" : "Siyah") + " kazandi (" + game.points + " puan). Yeni oyun icin dokun";
                        return;
                    }
                }
                selected = -1;
                for (Move m : legal) if (m.from == pt) { selected = pt; break; }
                break;
            }
            case FINISHED: newGame(); break;
        }
    }

    // ---- drawing ----
    @Override public void render() {
        if (Gdx.input.justTouched()) { Vector3 v = vp.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)); handleTouch(v.x, v.y); }
        Gdx.gl.glClearColor(0.05f, 0.03f, 0.02f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        vp.apply(); sr.setProjectionMatrix(cam.combined); batch.setProjectionMatrix(cam.combined);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(FRAME); sr.rect(BX - 20, BY - 20, BW + 120, BH + 40);
        sr.setColor(WOOD); sr.rect(BX, BY, BW, BH);
        for (int pt = 1; pt <= 24; pt++) {
            sr.setColor(pt % 2 == 0 ? TRI_A : TRI_B);
            float cx = px(pt);
            if (top(pt)) sr.triangle(cx - COL / 2 + 4, BY + BH, cx + COL / 2 - 4, BY + BH, cx, BY + BH - 270);
            else sr.triangle(cx - COL / 2 + 4, BY, cx + COL / 2 - 4, BY, cx, BY + 270);
        }
        List<Move> legal = game.legalMoves();
        if (selected > 0) { // highlight destinations
            sr.setColor(0.2f, 0.9f, 0.4f, 0.55f);
            for (Move m : legal) if (m.from == selected) {
                if (m.to == 0) sr.rect(BX + BW + 5, BY, 90, BH);
                else sr.circle(px(m.to), top(m.to) ? BY + BH - R - 6 : BY + R + 6, R + 6, 24);
            }
        } else {
            sr.setColor(1f, 0.85f, 0.2f, 0.5f);
            for (Move m : legal) sr.circle(px(m.from), top(m.from) ? BY + BH - R - 6 : BY + R + 6, 8, 12);
        }
        for (int pt = 1; pt <= 24; pt++) drawStack(pt);
        drawOff(Board.WHITE, BX + BW + 30, BY + BH - 40); drawOff(Board.BLACK, BX + BW + 30, BY + 40);
        int[] d = game.phase == Game.Phase.OPENING ? openDice : lastDice;
        for (int i = 0; i < d.length; i++) drawDie(BX + 380 + i * 90, BY + BH / 2 - 35, d[i]);
        sr.end();
        batch.begin();
        String turn = game.phase == Game.Phase.OPENING ? "" : (game.current == Board.WHITE ? "Sira: Beyaz (krem)  " : "Sira: Siyah  ");
        font.draw(batch, turn + msg, 20, H - 12);
        batch.end();
    }

    void drawStack(int pt) {
        Board b = game.board; int n = 0; Color c = CREAM, below = null;
        if (b.owner[pt] != 0) { n = b.count[pt]; c = b.owner[pt] == Board.WHITE ? CREAM : BLACK; }
        if (b.pinned[pt]) below = b.owner[pt] == Board.WHITE ? BLACK : CREAM;
        int total = n + (below != null ? 1 : 0); if (total == 0) return;
        float step = Math.min(2 * R, (BH / 2 - 2 * R) / Math.max(1, total - 1));
        for (int i = 0; i < total; i++) {
            Color col = (i == 0 && below != null) ? below : c;
            float y = top(pt) ? BY + BH - R - 6 - i * step : BY + R + 6 + i * step;
            if (pt == selected && i == total - 1) { sr.setColor(0.2f, 0.9f, 0.4f, 1); sr.circle(px(pt), y, R + 5, 28); }
            sr.setColor(0, 0, 0, 0.5f); sr.circle(px(pt) + 3, y - 3, R, 28);
            sr.setColor(col); sr.circle(px(pt), y, R, 28);
            sr.setColor(col.r * 0.8f, col.g * 0.8f, col.b * 0.8f, 1); sr.circle(px(pt), y, R * 0.65f, 24);
        }
    }

    void drawOff(int p, float x, float y) {
        int n = game.board.off[p]; sr.setColor(p == Board.WHITE ? CREAM : BLACK);
        for (int i = 0; i < n; i++) sr.rect(x - 22, y + (p == Board.WHITE ? -i : i) * 8 - (p == Board.WHITE ? 0 : 0), 44, 6);
    }

    void drawDie(float x, float y, int v) {
        sr.setColor(0.97f, 0.96f, 0.92f, 1); sr.rect(x, y, 70, 70);
        sr.setColor(0.1f, 0.1f, 0.1f, 1);
        float[][] pos = {{.5f, .5f}, {.25f, .75f}, {.75f, .25f}, {.25f, .25f}, {.75f, .75f}, {.25f, .5f}, {.75f, .5f}};
        int[][] pips = {{}, {0}, {1, 2}, {0, 1, 2}, {1, 2, 3, 4}, {0, 1, 2, 3, 4}, {1, 2, 3, 4, 5, 6}};
        for (int i : pips[v]) sr.circle(x + pos[i][0] * 70, y + pos[i][1] * 70, 6, 12);
    }

    @Override public void dispose() { sr.dispose(); batch.dispose(); font.dispose(); }
}
