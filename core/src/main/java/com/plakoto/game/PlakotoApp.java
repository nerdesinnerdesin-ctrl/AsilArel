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

/**
 * Local 2-player Plakoto screen styled like a real backgammon table.
 * Board space: u across (0..BW), v away from the viewer (0..BH); project via sx/sy.
 * Checkers lie 5 per row along a point; extra checkers pile on top (3D layers) like on a real board.
 */
public class PlakotoApp extends ApplicationAdapter {
    static final float W = 1280, H = 720;
    static final float BW = 1000, BH = 600, BAR = 50, TRI_H = 255, R = 29, ROW = 5, SLOT = 2 * R * 0.965f, LAYER = 9.5f;
    static final float CX = 640, BY = 70, K = 0.10f, YS = 0.92f, HALF = (BW - BAR) / 2f, PW = HALF / 6f;
    static final float MOVE_TIME = 0.34f, ROLL_TIME = 0.7f;
    static final String TR = "çğıöşüÇĞİÖŞÜ";

    OrthographicCamera cam; FitViewport vp; ShapeRenderer sr; SpriteBatch batch; BitmapFont font;
    Texture felt, grain, vignette, leatherFrame, leatherBar, leatherTray, creamTex, blackTex, shadowTex; Texture[] dieTex = new Texture[7];
    final SecureRandom rnd = new SecureRandom();
    Game game; int selected = -1; String msg = ""; int[] lastDice = new int[0]; int[] openDice = new int[0];
    float rollT = 0; float[] diceU = new float[4], diceV = new float[4], diceAng = new float[4];
    Move animMove; float animT; float au0, av0, al0, au1, av1, al1; boolean animWhite;
    final float[] quad = new float[20];
    final Color tmp = new Color();

    @Override public void create() {
        cam = new OrthographicCamera(); vp = new FitViewport(W, H, cam);
        sr = new ShapeRenderer(); batch = new SpriteBatch();
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("DejaVuSans-Bold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + TR + "’–";
        p.size = 30; p.color = new Color(0.98f, 0.93f, 0.80f, 1); p.shadowOffsetX = 2; p.shadowOffsetY = 2; p.shadowColor = new Color(0, 0, 0, 0.6f);
        font = gen.generateFont(p); gen.dispose();
        felt = Art.felt(256, 256, 5); grain = Art.grain(256, 256, 17); vignette = Art.vignette();
        leatherFrame = Art.leather(256, 256, 3, 0.15f); leatherBar = Art.leather(256, 256, 8, 0.21f); leatherTray = Art.leather(256, 256, 13, 0.08f);
        for (Texture t : new Texture[]{felt, grain, leatherFrame, leatherBar, leatherTray}) t.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        creamTex = Art.checker(0.99f, 0.94f, 0.82f, 0.45f, 0.30f, 21);  // clean ivory, faint marbling
        blackTex = Art.checker(0.14f, 0.14f, 0.16f, 1.0f, 0.15f, 44);  // glossy black
        shadowTex = Art.shadow();
        for (int v = 1; v <= 6; v++) dieTex[v] = Art.die(v);
        newGame();
    }

    void newGame() { game = new Game(); selected = -1; lastDice = new int[0]; openDice = new int[0]; animMove = null; rollT = 0; msg = "Başlamak için dokun: açılış zarı"; }
    int die() { return rnd.nextInt(6) + 1; }
    @Override public void resize(int w, int h) { vp.update(w, h, true); }
    String name(int p) { return p == Board.WHITE ? "Beyaz" : "Siyah"; }

    // ---- projection ----
    static float sc(float v) { return 1 - K * v / BH; }
    static float sx(float u, float v) { return CX + (u - BW / 2) * sc(v); }
    static float sy(float v) { return BY + v * YS; }

    boolean top(int pt) { return pt >= 13; }
    float pu(int pt) { int col = top(pt) ? pt - 13 : 12 - pt; return col < 6 ? (col + 0.5f) * PW : HALF + BAR + (col - 6 + 0.5f) * PW; }
    int pointAt(float u, float v) {
        if (v < 0 || v > BH || u < 0 || u > BW || (u > HALF && u < HALF + BAR)) return -1;
        int col = u < HALF ? (int) (u / PW) : 6 + (int) ((u - HALF - BAR) / PW);
        return v > BH / 2 ? 13 + col : 12 - col;
    }
    int stackTotal(int pt) { Board b = game.board; return b.owner[pt] == 0 ? 0 : b.count[pt] + (b.pinned[pt] ? 1 : 0); }
    /** Position of the i-th checker on a point: board v, screen lift (layers), tiny natural jitter in u. */
    float cv(int pt, int i) { float slot = i % (int) ROW; return top(pt) ? BH - R - 10 - slot * SLOT : R + 10 + slot * SLOT; }
    float clift(int pt, int i) { return (i / (int) ROW) * LAYER * sc(cv(pt, i)); }
    float cu(int pt, int i) { int h = (pt * 31 + i * 17) % 7; return pu(pt) + (h - 3) * 0.7f; }

    // ---- input ----
    void handleTouch(float scrX, float scrY) {
        if (animMove != null || rollT > 0) return;
        float v = (scrY - BY) / YS, u = (scrX - CX) / sc(v) + BW / 2;
        switch (game.phase) {
            case OPENING: {
                openDice = new int[]{die(), die()}; throwDice(2, true);
                if (game.openingRoll(openDice[0], openDice[1])) msg = name(game.current) + " başlıyor. Zar atmak için dokun";
                else msg = "Beraberlik! Tekrar dokun";
                break;
            }
            case ROLL: {
                int a = die(), b = die(); game.roll(a, b); lastDice = new int[]{a, b}; selected = -1; throwDice(2, false);
                msg = game.phase == Game.Phase.ROLL ? "Yasal hamle yok, sıra geçti" : "";
                break;
            }
            case MOVE: {
                List<Move> legal = game.legalMoves();
                int pt = pointAt(u, v); boolean tray = u > BW + 5;
                if (selected > 0) for (Move m : legal) if (m.from == selected && ((tray && m.to == 0) || (!tray && m.to == pt))) { startAnim(m); selected = -1; return; }
                selected = -1;
                for (Move m : legal) if (m.from == pt) { selected = pt; break; }
                break;
            }
            case FINISHED: newGame(); break;
        }
    }

    void throwDice(int n, boolean opening) {
        rollT = ROLL_TIME;
        for (int i = 0; i < n; i++) {
            boolean right = opening ? i == 0 : game.current == Board.WHITE;
            float c = right ? HALF + BAR + HALF / 2 : HALF / 2;
            diceU[i] = c + (rnd.nextFloat() - 0.5f) * 150 + (i == 0 ? -45 : 45) * (opening ? 0 : 1);
            diceV[i] = BH / 2 + (rnd.nextFloat() - 0.5f) * 140;
            diceAng[i] = rnd.nextFloat() * 70 - 35;
        }
    }

    float offV(int p) { return p == Board.WHITE ? BH - 170 : 60; }
    void startAnim(Move m) {
        int from = stackTotal(m.from); animWhite = game.current == Board.WHITE; animMove = m; animT = 0;
        au0 = cu(m.from, from - 1); av0 = cv(m.from, from - 1); al0 = clift(m.from, from - 1);
        if (m.to == 0) { au1 = BW + 52; av1 = offV(game.current); al1 = game.board.off[game.current] * 8.5f * sc(av1); }
        else { int t = stackTotal(m.to); au1 = cu(m.to, t); av1 = cv(m.to, t); al1 = clift(m.to, t); }
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
        if (Gdx.input.justTouched()) { Vector3 w = vp.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)); handleTouch(w.x, w.y); }

        Gdx.gl.glClearColor(0.025f, 0.018f, 0.012f, 1); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        vp.apply(); sr.setProjectionMatrix(cam.combined); batch.setProjectionMatrix(cam.combined);

        // 1. leather frame, trays, suede field, bar
        batch.begin();
        quad(leatherFrame, -112, -28, BW + 112, BH + 28, 1f, true, 1f);
        quad(leatherTray, -94, 0, -10, BH, 1f, true, 1f);
        quad(leatherTray, BW + 10, 0, BW + 94, BH, 1f, true, 1f);
        quad(felt, 0, 0, BW, BH, 1f, true, 1f);
        quad(leatherBar, HALF, -2, HALF + BAR, BH + 2, 1f, true, 1f);
        batch.end();

        // 2. triangles with soft outline/bevel
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Color olive = new Color(0.34f, 0.39f, 0.17f, 1), oliveT = new Color(0.25f, 0.30f, 0.12f, 1);
        Color brown = new Color(0.31f, 0.10f, 0.06f, 1), brownT = new Color(0.18f, 0.06f, 0.035f, 1);
        Color edge = new Color(0, 0, 0, 0.30f);
        for (int pt = 1; pt <= 24; pt++) {
            boolean a = pt % 2 == 0; float u = pu(pt), hw = PW / 2 - 5; float v0 = top(pt) ? BH : 0, v1 = top(pt) ? BH - TRI_H : TRI_H;
            float tipOut = top(pt) ? -4 : 4;
            sr.triangle(sx(u - hw - 2, v0), sy(v0), sx(u + hw + 2, v0), sy(v0), sx(u, v1 + tipOut), sy(v1 + tipOut), edge, edge, edge);
            Color base = a ? olive : brown, tip = a ? oliveT : brownT;
            sr.triangle(sx(u - hw, v0), sy(v0), sx(u + hw, v0), sy(v0), sx(u, v1), sy(v1), base, base, tip);
        }
        sr.end();

        // 3. suede grain over everything on the field + directional light
        batch.begin();
        quad(grain, 0, 0, BW, BH, 1f, true, 1f);
        quad(vignette, 0, 0, BW, BH, 1f, false, 1f);
        batch.end();

        // 4. depth: recessed edges, bar shadows, highlights, stitching
        sr.begin(ShapeRenderer.ShapeType.Filled);
        Color d0 = new Color(0, 0, 0, 0.55f), d1 = new Color(0, 0, 0, 0f), hi = new Color(1, 1, 1, 0.22f), hi0 = new Color(1, 1, 1, 0f);
        gq(0, 0, BW, 20, d0, d1, d1, d0); gq(0, BH - 20, BW, BH, d1, d0, d0, d1);
        gq(0, 0, 20, BH, d0, d0, d1, d1); gq(BW - 20, 0, BW, BH, d1, d1, d0, d0);
        gq(HALF - 16, 0, HALF, BH, d1, d1, d0, d0); gq(HALF + BAR, 0, HALF + BAR + 16, BH, d0, d0, d1, d1);
        gq(HALF + 1, 0, HALF + 5, BH, hi, hi, hi0, hi0);                // bar bevel highlight
        gq(-112, BH + 20, BW + 112, BH + 28, hi0, hi, hi, hi0);         // sheen on the far frame edge
        gq(-94, 0, -10, 14, d0, d1, d1, d0); gq(BW + 10, 0, BW + 94, 14, d0, d1, d1, d0);   // tray recess
        gq(-94, BH - 14, -10, BH, d1, d0, d0, d1); gq(BW + 10, BH - 14, BW + 94, BH, d1, d0, d0, d1);
        sr.setColor(0.75f, 0.72f, 0.65f, 0.30f);
        for (float u = -100; u <= BW + 100; u += 16) { dot(u, -14); dot(u, BH + 14); }
        for (float v = 0; v <= BH; v += 16) { dot(-104, v); dot(BW + 104, v); }
        if (animMove == null && rollT <= 0) {
            List<Move> legal = game.legalMoves();
            if (selected > 0) {
                for (Move m : legal) if (m.from == selected) {
                    sr.setColor(0.25f, 0.95f, 0.45f, 0.60f);
                    if (m.to == 0) quadFill(BW + 12, 2, BW + 92, BH - 2);
                    else { int t = stackTotal(m.to); disc(cu(m.to, t), cv(m.to, t), R + 8, clift(m.to, t)); }
                }
                int t = stackTotal(selected); sr.setColor(0.25f, 0.95f, 0.45f, 0.75f); disc(pu(selected), cv(selected, t - 1), R + 7, clift(selected, t - 1));
            } else {
                sr.setColor(1f, 0.85f, 0.25f, 0.85f);
                for (Move m : legal) { int t = stackTotal(m.from); disc(pu(m.from), cv(m.from, t - 1), 9, clift(m.from, t - 1)); }
            }
        }
        sr.end();

        // 5. checkers, dice, text
        batch.begin();
        for (int pt = 1; pt <= 24; pt++) drawStack(pt);
        drawOff(Board.WHITE); drawOff(Board.BLACK);
        if (animMove != null) {
            float p = Math.min(1, animT / MOVE_TIME), e = p * p * (3 - 2 * p);
            drawChecker(animWhite ? creamTex : blackTex, au0 + (au1 - au0) * e, av0 + (av1 - av0) * e, 1.15f, al0 + (al1 - al0) * e + (float) Math.sin(Math.PI * p) * 60);
        }
        int[] d = game.phase == Game.Phase.OPENING ? openDice : lastDice;
        for (int i = 0; i < d.length; i++) {
            int face = rollT > 0 ? 1 + rnd.nextInt(6) : d[i];
            float s = sc(diceV[i]), x = sx(diceU[i], diceV[i]), y = sy(diceV[i]), size = 78 * s, ang = rollT > 0 ? diceAng[i] + rollT * 400 : diceAng[i];
            batch.setColor(0, 0, 0, 0.55f); batch.draw(shadowTex, x - size * 0.62f + 5, y - size * 0.62f - 8, size * 1.24f, size * 1.24f);
            batch.setColor(1, 1, 1, 1); batch.draw(dieTex[face], x - size / 2, y - size / 2, size / 2, size / 2, size, size, 1, 1, ang, 0, 0, 128, 128, false, false);
        }
        batch.setColor(1, 1, 1, 1);
        String turn = (game.phase == Game.Phase.MOVE || game.phase == Game.Phase.ROLL) ? "Sıra: " + name(game.current) + (game.current == Board.WHITE ? " (krem)" : "") + "   " : "";
        font.draw(batch, turn + msg, 24, H - 10);
        batch.end();
    }

    // ---- helpers ----
    /** Textured quad in board space. tile=true: 1 texel per board unit (repeat); false: stretched over the quad. */
    void quad(Texture t, float u0, float v0, float u1, float v1, float shade, boolean tile, float alpha) {
        float[] us = {u0, u0, u1, u1}, vs = {v0, v1, v1, v0};
        float bits = tmp.set(shade, shade, shade, alpha).toFloatBits();
        for (int i = 0; i < 4; i++) {
            quad[i * 5] = sx(us[i], vs[i]); quad[i * 5 + 1] = sy(vs[i]); quad[i * 5 + 2] = bits;
            quad[i * 5 + 3] = tile ? (us[i] - u0) / t.getWidth() : (us[i] - u0) / (u1 - u0);
            quad[i * 5 + 4] = tile ? (vs[i] - v0) / t.getHeight() : 1f - (vs[i] - v0) / (v1 - v0);
        }
        batch.draw(t, quad, 0, 20);
    }
    void quadFill(float u0, float v0, float u1, float v1) {
        sr.triangle(sx(u0, v0), sy(v0), sx(u0, v1), sy(v1), sx(u1, v1), sy(v1));
        sr.triangle(sx(u0, v0), sy(v0), sx(u1, v1), sy(v1), sx(u1, v0), sy(v0));
    }
    /** Gradient quad; colours for corners (u0,v0),(u0,v1),(u1,v1),(u1,v0). */
    void gq(float u0, float v0, float u1, float v1, Color a, Color b, Color c, Color d) {
        sr.triangle(sx(u0, v0), sy(v0), sx(u0, v1), sy(v1), sx(u1, v1), sy(v1), a, b, c);
        sr.triangle(sx(u0, v0), sy(v0), sx(u1, v1), sy(v1), sx(u1, v0), sy(v0), a, c, d);
    }
    void dot(float u, float v) { sr.circle(sx(u, v), sy(v), 1.7f * sc(v), 6); }
    void disc(float u, float v, float r, float lift) { float s = sc(v); sr.ellipse(sx(u, v) - r * s, sy(v) + lift - r * s * 0.8f, 2 * r * s, 1.6f * r * s); }

    /** Thick checker: shadow on the felt, two darker side-wall slices, flattened lit top face. */
    void drawChecker(Texture t, float u, float v, float scale, float lift) {
        float s = sc(v), w = 2 * R * s * scale, h = w * 0.82f, x = sx(u, v), y = sy(v) + lift, th = 10 * s;
        boolean cream = t == creamTex;
        batch.setColor(0, 0, 0, 0.55f); batch.draw(shadowTex, x - w * 0.62f + 4, y - h * 0.62f - th - 5 - lift * 0.5f, w * 1.24f, h * 1.24f);
        if (cream) batch.setColor(0.72f, 0.63f, 0.50f, 1); else batch.setColor(0.40f, 0.40f, 0.42f, 1);
        batch.draw(t, x - w / 2, y - h / 2 - th, w, h);
        if (cream) batch.setColor(0.88f, 0.80f, 0.67f, 1); else batch.setColor(0.60f, 0.60f, 0.62f, 1);
        batch.draw(t, x - w / 2, y - h / 2 - th * 0.5f, w, h);
        batch.setColor(1, 1, 1, 1); batch.draw(t, x - w / 2, y - h / 2, w, h);
    }

    void drawStack(int pt) {
        Board b = game.board; int total = stackTotal(pt); if (total == 0) return;
        Texture ownerTex = b.owner[pt] == Board.WHITE ? creamTex : blackTex, pinnedTex = b.owner[pt] == Board.WHITE ? blackTex : creamTex;
        int shown = (animMove != null && animMove.from == pt) ? total - 1 : total;
        // draw bottom layer first, farther slots first so nearer checkers overlap them
        for (int layer = 0; layer * (int) ROW < shown; layer++)
            for (int i = layer * (int) ROW; i < Math.min(shown, (layer + 1) * (int) ROW); i++)
                drawChecker((i == 0 && b.pinned[pt]) ? pinnedTex : ownerTex, cu(pt, i), cv(pt, i), 1f, clift(pt, i));
    }

    void drawOff(int p) {
        int n = game.board.off[p]; float v = offV(p);
        for (int i = 0; i < n; i++) drawChecker(p == Board.WHITE ? creamTex : blackTex, BW + 52 + ((i * 13) % 5 - 2) * 0.8f, v, 0.9f, i * 8.5f * sc(v));
    }

    @Override public void dispose() {
        sr.dispose(); batch.dispose(); font.dispose();
        felt.dispose(); grain.dispose(); vignette.dispose(); leatherFrame.dispose(); leatherBar.dispose(); leatherTray.dispose();
        creamTex.dispose(); blackTex.dispose(); shadowTex.dispose();
        for (int v = 1; v <= 6; v++) dieTex[v].dispose();
    }
}
