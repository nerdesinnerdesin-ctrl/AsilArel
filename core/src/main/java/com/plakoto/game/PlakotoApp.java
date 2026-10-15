package com.plakoto.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.plakoto.engine.Board;
import com.plakoto.engine.Game;
import com.plakoto.engine.Move;

import java.security.SecureRandom;
import java.util.List;

/** Local 2-player Plakoto screen with procedural premium art and animations. */
public class PlakotoApp extends ApplicationAdapter {
    static final float W = 1280, H = 720, BX = 70, BY = 50, BW = 1120, BH = 620, COL = BW / 12f, R = 33, TRI_H = 275;
    static final float MOVE_TIME = 0.32f, ROLL_TIME = 0.7f;
    static final String TR = "çğıöşüÇĞİÖŞÜ";

    OrthographicCamera cam; FitViewport vp; ShapeRenderer sr; SpriteBatch batch; BitmapFont font, small;
    Texture frameWood, boardWood, creamTex, blackTex, shadowTex; Texture[] dieTex = new Texture[7];
    final SecureRandom rnd = new SecureRandom();
    Game game; int selected = -1; String msg = ""; int[] lastDice = new int[0]; int[] openDice = new int[0];
    float rollT = 0; // >0 while dice tumble
    // checker move animation
    Move animMove; float animT; float ax0, ay0, ax1, ay1; boolean animWhite;

    @Override public void create() {
        cam = new OrthographicCamera(); vp = new FitViewport(W, H, cam);
        sr = new ShapeRenderer(); batch = new SpriteBatch();
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("DejaVuSans-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + TR + "’–";
        p.size = 30; p.color = new Color(0.98f, 0.93f, 0.80f, 1); p.shadowOffsetX = 2; p.shadowOffsetY = 2; p.shadowColor = new Color(0, 0, 0, 0.6f);
        font = gen.generateFont(p);
        p.size = 20; small = gen.generateFont(p);
        gen.dispose();
        frameWood = Art.wood(512, 256, new float[]{0.10f, 0.05f, 0.025f}, new float[]{0.26f, 0.14f, 0.07f}, 11);
        boardWood = Art.wood(512, 512, new float[]{0.20f, 0.10f, 0.05f}, new float[]{0.42f, 0.24f, 0.12f}, 29);
        frameWood.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        boardWood.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        creamTex = Art.checker(0.96f, 0.91f, 0.78f); blackTex = Art.checker(0.16f, 0.16f, 0.18f); shadowTex = Art.shadow();
        for (int v = 1; v <= 6; v++) dieTex[v] = Art.die(v);
        newGame();
    }

    void newGame() { game = new Game(); selected = -1; lastDice = new int[0]; openDice = new int[0]; animMove = null; rollT = 0; msg = "Başlamak için dokun: açılış zarı"; }
    int die() { return rnd.nextInt(6) + 1; }
    @Override public void resize(int w, int h) { vp.update(w, h, true); }
    String name(int p) { return p == Board.WHITE ? "Beyaz" : "Siyah"; }

    // ---- geometry ----
    boolean top(int pt) { return pt >= 13; }
    float px(int pt) { int col = top(pt) ? pt - 13 : 12 - pt; return BX + col * COL + COL / 2; }
    int pointAt(float x, float y) {
        if (x < BX || x > BX + BW || y < BY || y > BY + BH) return -1;
        int col = (int) ((x - BX) / COL);
        return y > BY + BH / 2 ? 13 + col : 12 - col;
    }
    int stackTotal(int pt) {
        Board b = game.board; if (b.owner[pt] == 0) return 0;
        return b.count[pt] + (b.pinned[pt] ? 1 : 0);
    }
    float step(int total) { return Math.min(2 * R, (BH / 2 - 2 * R - 10) / Math.max(1, total - 1)); }
    float cy(int pt, int i, int total) { float s = step(total); return top(pt) ? BY + BH - R - 8 - i * s : BY + R + 8 + i * s; }

    // ---- input ----
    void handleTouch(float x, float y) {
        if (animMove != null || rollT > 0) return;
        switch (game.phase) {
            case OPENING: {
                openDice = new int[]{die(), die()}; rollT = ROLL_TIME;
                if (game.openingRoll(openDice[0], openDice[1])) msg = name(game.current) + " başlıyor. Zar atmak için dokun";
                else msg = "Beraberlik! Tekrar dokun";
                break;
            }
            case ROLL: {
                int a = die(), b = die(); game.roll(a, b); lastDice = new int[]{a, b}; selected = -1; rollT = ROLL_TIME;
                msg = game.phase == Game.Phase.ROLL ? "Yasal hamle yok, sıra geçti" : "";
                break;
            }
            case MOVE: {
                List<Move> legal = game.legalMoves();
                int pt = pointAt(x, y); boolean offPanel = x > BX + BW;
                if (selected > 0) for (Move m : legal) if (m.from == selected && ((offPanel && m.to == 0) || (!offPanel && m.to == pt))) { startAnim(m); selected = -1; return; }
                selected = -1;
                for (Move m : legal) if (m.from == pt) { selected = pt; break; }
                break;
            }
            case FINISHED: newGame(); break;
        }
    }

    void startAnim(Move m) {
        int from = stackTotal(m.from); animWhite = game.current == Board.WHITE; animMove = m; animT = 0;
        ax0 = px(m.from); ay0 = cy(m.from, from - 1, from);
        if (m.to == 0) { ax1 = BX + BW + 55; ay1 = animWhite ? BY + BH - 40 : BY + 40; }
        else { int t = stackTotal(m.to); ax1 = px(m.to); ay1 = cy(m.to, t, t + 1); }
    }

    // ---- frame ----
    @Override public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        if (rollT > 0) rollT = Math.max(0, rollT - dt);
        if (animMove != null) {
            animT += dt;
            if (animT >= MOVE_TIME) {
                game.play(animMove); animMove = null;
                if (game.phase == Game.Phase.FINISHED) msg = name(game.winner) + " kazandı (" + game.points + " puan)" + (game.points == 2 ? " – mars!" : "") + ". Yeni oyun için dokun";
            }
        }
        if (Gdx.input.justTouched()) { Vector3 v = vp.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)); handleTouch(v.x, v.y); }

        Gdx.gl.glClearColor(0.04f, 0.025f, 0.015f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        vp.apply(); sr.setProjectionMatrix(cam.combined); batch.setProjectionMatrix(cam.combined);

        // wood layers
        batch.begin();
        batch.setColor(1, 1, 1, 1);
        batch.draw(frameWood, BX - 22, BY - 22, 0, 0, (int) (BW + 122 + 0), (int) (BH + 44)); // tiled, 1:1 pixels
        batch.draw(boardWood, BX, BY, 0, 0, (int) BW, (int) BH);
        batch.end();

        sr.begin(ShapeRenderer.ShapeType.Filled);
        Color dk = new Color(0.17f, 0.08f, 0.04f, 0.92f), dk2 = new Color(0.10f, 0.045f, 0.02f, 0.92f);
        Color lt = new Color(0.80f, 0.62f, 0.38f, 0.90f), lt2 = new Color(0.62f, 0.45f, 0.26f, 0.90f);
        for (int pt = 1; pt <= 24; pt++) {
            boolean light = pt % 2 == 0; float cx = px(pt);
            Color base = light ? lt : dk, tip = light ? lt2 : dk2;
            if (top(pt)) sr.triangle(cx - COL / 2 + 5, BY + BH, cx + COL / 2 - 5, BY + BH, cx, BY + BH - TRI_H, base, base, tip);
            else sr.triangle(cx - COL / 2 + 5, BY, cx + COL / 2 - 5, BY, cx, BY + TRI_H, base, base, tip);
        }
        // centre rim + inner shadows for depth
        sr.setColor(0, 0, 0, 0.35f); sr.rect(BX, BY + BH / 2 - 3, BW, 6);
        sr.rect(BX, BY + BH - 6, BW, 6); sr.rect(BX, BY, BW, 6);
        List<Move> legal = game.legalMoves();
        if (animMove == null && rollT <= 0) {
            if (selected > 0) {
                sr.setColor(0.25f, 0.95f, 0.45f, 0.55f);
                for (Move m : legal) if (m.from == selected) {
                    if (m.to == 0) sr.rect(BX + BW + 8, BY, 92, BH);
                    else { int t = stackTotal(m.to); sr.circle(px(m.to), cy(m.to, t, t + 1), R + 7, 28); }
                }
            } else {
                sr.setColor(1f, 0.85f, 0.25f, 0.65f);
                for (Move m : legal) { int t = stackTotal(m.from); sr.circle(px(m.from), cy(m.from, t - 1, t) , 9, 14); }
            }
        }
        sr.end();

        // checkers
        batch.begin();
        for (int pt = 1; pt <= 24; pt++) drawStack(pt);
        drawOff(Board.WHITE); drawOff(Board.BLACK);
        if (animMove != null) {
            float p = Math.min(1, animT / MOVE_TIME), e = p * p * (3 - 2 * p);
            float x = ax0 + (ax1 - ax0) * e, y = ay0 + (ay1 - ay0) * e + (float) Math.sin(Math.PI * p) * 55;
            drawChecker(animWhite ? creamTex : blackTex, x, y, 1.18f);
        }
        // dice
        int[] d = game.phase == Game.Phase.OPENING ? openDice : lastDice;
        for (int i = 0; i < d.length; i++) {
            int face = rollT > 0 ? 1 + rnd.nextInt(6) : d[i];
            float dx = BX + BW / 2 - (d.length * 100) / 2f + i * 100 + 8, dy = BY + BH / 2 - 44;
            batch.setColor(0, 0, 0, 0.5f); batch.draw(shadowTex, dx - 14, dy - 18, 124, 110);
            batch.setColor(1, 1, 1, 1); batch.draw(dieTex[face], dx, dy, 88, 88);
        }
        if (game.phase == Game.Phase.OPENING && openDice.length == 2 && rollT <= 0) {
            small.draw(batch, "Beyaz", BX + BW / 2 - 100 + 8 + 14, BY + BH / 2 - 52);
            small.draw(batch, "Siyah", BX + BW / 2 + 8 + 14, BY + BH / 2 - 52);
        }
        // status bar
        batch.setColor(1, 1, 1, 1);
        String turn = (game.phase == Game.Phase.MOVE || game.phase == Game.Phase.ROLL) ? "Sıra: " + name(game.current) + (game.current == Board.WHITE ? " (krem)" : "") + "   " : "";
        font.draw(batch, turn + msg, 24, H - 10);
        batch.end();
    }

    void drawChecker(Texture t, float x, float y, float scale) {
        float s = R * 2 * scale;
        batch.setColor(0, 0, 0, 0.55f); batch.draw(shadowTex, x - s * 0.6f + 4, y - s * 0.6f - 7, s * 1.2f, s * 1.2f);
        batch.setColor(1, 1, 1, 1); batch.draw(t, x - s / 2, y - s / 2, s, s);
    }

    void drawStack(int pt) {
        Board b = game.board; int total = stackTotal(pt); if (total == 0) return;
        Texture ownerTex = b.owner[pt] == Board.WHITE ? creamTex : blackTex;
        Texture pinnedTex = b.owner[pt] == Board.WHITE ? blackTex : creamTex;
        int shown = (animMove != null && animMove.from == pt) ? total - 1 : total; // top checker is flying
        for (int i = 0; i < shown; i++) {
            Texture t = (i == 0 && b.pinned[pt]) ? pinnedTex : ownerTex;
            drawChecker(t, px(pt), cy(pt, i, total), (pt == selected && i == total - 1) ? 1.12f : 1f);
        }
    }

    void drawOff(int p) {
        int n = game.board.off[p];
        Texture t = p == Board.WHITE ? creamTex : blackTex;
        for (int i = 0; i < n; i++) {
            float x = BX + BW + 55, y = p == Board.WHITE ? BY + BH - 40 - i * 11 : BY + 40 + i * 11;
            batch.setColor(0, 0, 0, 0.45f); batch.draw(shadowTex, x - 36, y - 20, 72, 36);
            batch.setColor(1, 1, 1, 1); batch.draw(t, x - 30, y - 12, 60, 24);
        }
    }

    @Override public void dispose() {
        sr.dispose(); batch.dispose(); font.dispose(); small.dispose();
        frameWood.dispose(); boardWood.dispose(); creamTex.dispose(); blackTex.dispose(); shadowTex.dispose();
        for (int v = 1; v <= 6; v++) dieTex[v].dispose();
    }
}
