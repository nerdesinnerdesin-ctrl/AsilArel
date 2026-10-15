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

    /** Seamless-ish wood grain, horizontal rings. dark/light are RGB triples. */
    static Texture wood(int w, int h, float[] dark, float[] light, int seed) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float warp = vnoise(x * 0.006f, y * 0.04f, seed) * 6f;
            float ring = 0.5f + 0.5f * (float) Math.sin((y * 0.05f + warp) * 6.2832f * 0.6f);
            float fine = vnoise(x * 0.35f, y * 0.9f, seed + 7);
            float streak = vnoise(x * 0.02f, y * 0.5f, seed + 3);
            float t = clamp(ring * 0.55f + fine * 0.18f + streak * 0.27f);
            pm.setColor(dark[0] + (light[0] - dark[0]) * t, dark[1] + (light[1] - dark[1]) * t, dark[2] + (light[2] - dark[2]) * t, 1f);
            pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Beveled, lit checker disc with groove ring and specular highlight. */
    static Texture checker(float r, float g, float b) {
        int n = 160; float c = n / 2f, R = c - 3;
        Pixmap pm = new Pixmap(n, n, Pixmap.Format.RGBA8888);
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            float dx = x - c, dy = y - c, d = (float) Math.sqrt(dx * dx + dy * dy) / R;
            if (d > 1f) { pm.setColor(0, 0, 0, 0); pm.drawPixel(x, y); continue; }
            float nx = dx / R, ny = -dy / R; // y up
            float light = 0.78f + 0.30f * (-nx * 0.5f + ny * 0.7f); // light from top-left
            float shade;
            if (d > 0.86f) { float e = (d - 0.86f) / 0.14f; shade = light * (1.1f - 0.6f * e) ; }       // outer bevel
            else if (d > 0.60f && d < 0.66f) shade = light * 0.72f;                                      // groove
            else shade = light * (1.0f - 0.10f * d);                                                    // face
            float spec = (float) Math.max(0, 1 - Math.hypot(nx + 0.38f, ny - 0.42f) / 0.38f);
            spec = spec * spec * 0.55f;
            float a = d > 0.97f ? (1f - d) / 0.03f : 1f;
            pm.setColor(clamp(r * shade + spec), clamp(g * shade + spec), clamp(b * shade + spec), a);
            pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
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

    /** Tan suede/felt playing surface. */
    static Texture felt(int w, int h, int seed) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float n = vnoise(x * 0.9f, y * 0.9f, seed) * 0.5f + vnoise(x * 0.07f, y * 0.07f, seed + 5) * 0.5f;
            float k = 0.90f + 0.16f * n;
            pm.setColor(clamp(0.80f * k), clamp(0.70f * k), clamp(0.44f * k), 1f);
            pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    /** Black leather with fine grain and slight sheen. */
    static Texture leather(int w, int h, int seed, float base) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float grain = vnoise(x * 0.55f, y * 0.55f, seed) * 0.6f + vnoise(x * 0.12f, y * 0.12f, seed + 9) * 0.4f;
            float k = base * (0.75f + 0.5f * grain);
            pm.setColor(clamp(k), clamp(k), clamp(k * 1.05f), 1f);
            pm.drawPixel(x, y);
        }
        Texture t = new Texture(pm); pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }
}
