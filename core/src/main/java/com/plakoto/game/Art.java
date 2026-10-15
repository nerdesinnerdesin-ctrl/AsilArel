package com.plakoto.game;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/** Procedurally generated art (no external image files): wood grain, shaded checkers, shadows, dice. */
final class Art {
    private Art() {}

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 144665;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535f;
    }
    private static float smooth(float t) { return t * t * (3 - 2 * t); }
    private static float vnoise(float x, float y, int seed) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
        float fx = smooth(x - xi), fy = smooth(y - yi);
        float a = hash(xi, yi, seed), b = hash(xi + 1, yi, seed), c = hash(xi, yi + 1, seed), d = hash(xi + 1, yi + 1, seed);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fy;
    }
    private static float clamp(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }

    private static Texture finish(Pixmap pm) {
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Fine-grained tan suede (no blotchy low-frequency noise). */
    static Texture felt(int w, int h, int seed) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float n = vnoise(x * 1.1f, y * 1.1f, seed) * 0.6f + vnoise(x * 0.45f, y * 0.45f, seed + 4) * 0.4f;
            float k = 0.93f + 0.10f * n;
            pm.setColor(clamp(0.83f * k), clamp(0.73f * k), clamp(0.47f * k), 1f);
            pm.drawPixel(x, y);
        }
        return finish(pm);
    }

    /** Black leather with fine grain. */
    static Texture leather(int w, int h, int seed, float base) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float grain = vnoise(x * 0.55f, y * 0.55f, seed) * 0.6f + vnoise(x * 0.12f, y * 0.12f, seed + 9) * 0.4f;
            float k = base * (0.75f + 0.5f * grain);
            pm.setColor(clamp(k), clamp(k), clamp(k * 1.05f), 1f);
            pm.drawPixel(x, y);
        }
        return finish(pm);
    }

    /** Neutral specks (white/black, low alpha) laid over the field so triangles share the suede texture. */
    static Texture grain(int w, int h, int seed) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float n = hash(x, y, seed), e = Math.abs(n - 0.5f) * 2f;
            float v = n > 0.5f ? 1f : 0f;
            pm.setColor(v, v, v, e * e * 0.16f);
            pm.drawPixel(x, y);
        }
        return finish(pm);
    }

    /** Dark edges + slightly darker bottom-right: gives directional light from the top-left. */
    static Texture vignette() {
        int n = 256; Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float nx = x / (n - 1f) * 2 - 1, ny = 1 - y / (n - 1f) * 2; // y up
            float edge = (float) Math.pow(Math.max(Math.abs(nx), Math.abs(ny)), 3.0);
            float a = clamp(edge * 0.50f + 0.07f * (nx - ny) * 0.5f + 0.05f);
            pm.setColor(0, 0, 0, a); pm.drawPixel(x, y);
        }
        Texture t = finish(pm);
        t.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        return t;
    }

    /**
     * Lit, beveled tavla checker: outer bevel, two concentric grooves, slightly raised centre,
     * material noise (marbling for ivory) and a glossy highlight + rim light.
     */
    static Texture checker(float r, float g, float b, float gloss, float marble, int seed) {
        int n = 192; float c = n / 2f, R = c - 3;
        Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float dx = x - c, dy = y - c, d = (float) Math.sqrt(dx * dx + dy * dy) / R;
            if (d > 1f) { pm.setColor(0, 0, 0, 0); pm.drawPixel(x, y); continue; }
            float nx = dx / R, ny = -dy / R;
            float lit = -nx * 0.5f + ny * 0.7f;
            float light = 0.90f + 0.28f * lit;
            float shade;
            if (d > 0.88f) { float e = (d - 0.88f) / 0.12f; shade = light * (1.06f - 0.55f * e); }
            else if (Math.abs(d - 0.74f) < 0.022f) shade = light * 0.88f;
            else if (Math.abs(d - 0.50f) < 0.018f) shade = light * 0.93f;
            else shade = light * (1.0f - 0.06f * d + (d < 0.45f ? 0.04f : 0f));
            float veins = (vnoise(x * 0.07f, y * 0.07f, seed) - 0.5f) * 2f * marble;
            float fine = (vnoise(x * 0.8f, y * 0.8f, seed + 3) - 0.5f) * 0.05f;
            shade *= 1f + veins * 0.10f + fine;
            float spec = (float) Math.max(0, 1 - Math.hypot(nx + 0.36f, ny - 0.40f) / 0.40f);
            spec = spec * spec * 0.60f * gloss;
            float rim = (d > 0.90f && d < 0.97f && lit > 0) ? lit * 0.22f * gloss : 0f;
            float a = d > 0.97f ? (1f - d) / 0.03f : 1f;
            pm.setColor(clamp(r * shade + spec + rim), clamp(g * shade + spec + rim), clamp(b * shade + spec + rim), a);
            pm.drawPixel(x, y);
        }
        return finish(pm);
    }

    static Texture shadow() {
        int n = 96; float c = n / 2f;
        Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float d = (float) Math.hypot(x - c, y - c) / c;
            float a = clamp(1f - d); a = a * a * 0.75f;
            pm.setColor(0, 0, 0, a); pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static final float[][] PIP = {{.5f, .5f}, {.27f, .73f}, {.73f, .27f}, {.27f, .27f}, {.73f, .73f}, {.27f, .5f}, {.73f, .5f}};
    private static final int[][] PIPS = {{}, {0}, {1, 2}, {0, 1, 2}, {1, 2, 3, 4}, {0, 1, 2, 3, 4}, {1, 2, 3, 4, 5, 6}};

    /** Ivory rounded die face with soft bevel and shaded pips. */
    static Texture die(int value) {
        int n = 128; float rad = 24f, half = n / 2f - 2;
        Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float dx = Math.abs(x - n / 2f) - (half - rad), dy = Math.abs(y - n / 2f) - (half - rad);
            float dist = (float) Math.hypot(Math.max(dx, 0), Math.max(dy, 0)) + Math.min(Math.max(dx, dy), 0) - rad; // <0 inside
            if (dist > 0.5f) { pm.setColor(0, 0, 0, 0); pm.drawPixel(x, y); continue; }
            float edge = clamp(-dist / 10f); // 0 at rim, 1 inside
            float light = 0.80f + 0.20f * edge + 0.10f * ((n - y) / (float) n - 0.5f) - 0.06f * (x / (float) n - 0.5f);
            float a = dist > -0.8f ? clamp((0.5f - dist) / 1.3f) : 1f;
            pm.setColor(clamp(0.98f * light), clamp(0.95f * light), clamp(0.86f * light), a);
            pm.drawPixel(x, y);
        }
        for (int i : PIPS[value]) {
            float px = PIP[i][0] * n, py = (1 - PIP[i][1]) * n, pr = 10f;
            for (int y = (int) (py - pr - 2); y <= py + pr + 2; y++) for (int x = (int) (px - pr - 2); x <= px + pr + 2; x++) {
                float d = (float) Math.hypot(x - px, y - py) / pr;
                if (d > 1f) continue;
                float s = 0.10f + 0.25f * d; // darker center, slightly lighter rim
                pm.setColor(s, s, s * 1.05f, d > 0.92f ? (1f - d) / 0.08f : 1f);
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }
}
