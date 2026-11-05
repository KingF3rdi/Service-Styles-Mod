/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 */
package dev.hugoservice;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

final class Shapes {
    static final int SIZE = 2048;
    static final int MAX = 256;
    static final Identifier ID = Identifier.of((String)"hugoservice", (String)"shapes");
    private static NativeImage img;
    private static NativeImageBackedTexture tex;
    private static final Map<Long, Sprite> SPRITES;
    private static int shelfX;
    private static int shelfY;
    private static int shelfH;
    private static boolean dirty;
    static final int GLOW = 4;

    static Sprite get(int w, int h, int r, int t, int corners) {
        if (w > 256 || h > 256 || w <= 0 || h <= 0) {
            return null;
        }
        long key = (long)w | (long)h << 9 | (long)r << 18 | (long)t << 27 | (long)corners << 36;
        Sprite s = SPRITES.get(key);
        if (s != null) {
            return s;
        }
        s = Shapes.alloc(w, h);
        Shapes.raster(s.u, s.v, w, h, r, t, corners);
        SPRITES.put(key, s);
        dirty = true;
        return s;
    }

    static Sprite rim(int w, int h, int r) {
        if (w > 256 || h > 256 || w <= 0 || h <= 0) {
            return null;
        }
        long key = (long)w | (long)h << 9 | (long)r << 18 | 0x10000000000L;
        Sprite s = SPRITES.get(key);
        if (s != null) {
            return s;
        }
        s = Shapes.alloc(w, h);
        double hw = (double)w / 2.0;
        double hh = (double)h / 2.0;
        for (int y = 0; y < h; ++y) {
            float light = Shapes.rimLight(((float)y + 0.5f) / (float)h);
            for (int x = 0; x < w; ++x) {
                double d = Shapes.sdf((double)x + 0.5 - hw, (double)y + 0.5 - hh, hw, hh, r);
                double ring = Shapes.clamp(0.5 - d) - Shapes.clamp(0.5 - (d + 1.0));
                double glow = d < -1.0 ? 0.28 * Math.pow(Shapes.clamp(1.0 + (d + 1.0) / 4.0), 2.0) : 0.0;
                img.setColorArgb(s.u() + x, s.v() + y, (int)Math.round(Shapes.clamp(ring + glow) * (double)light * 255.0) << 24 | 0xFFFFFF);
            }
        }
        SPRITES.put(key, s);
        dirty = true;
        return s;
    }

    static float rimLight(float v) {
        return v < 0.5f ? 0.16f + 0.84f * (float)Math.pow(1.0f - v / 0.5f, 1.6) : 0.16f + 0.4f * (float)Math.pow((v - 0.5f) / 0.5f, 2.0);
    }

    static Sprite glow() {
        long key = -1L;
        Sprite s = SPRITES.get(key);
        if (s != null) {
            return s;
        }
        s = Shapes.alloc(64, 64);
        for (int y = 0; y < 64; ++y) {
            for (int x = 0; x < 64; ++x) {
                double d = Math.min(1.0, Math.hypot((double)x - 31.5, (double)y - 31.5) / 32.0);
                img.setColorArgb(s.u() + x, s.v() + y, (int)(255.0 * Math.pow(1.0 - d, 2.0)) << 24 | 0xFFFFFF);
            }
        }
        SPRITES.put(key, s);
        dirty = true;
        return s;
    }

    private static Sprite alloc(int w, int h) {
        Shapes.ensure();
        if (shelfX + w + 1 > 2048) {
            shelfX = 0;
            shelfY += shelfH + 1;
            shelfH = 0;
        }
        if (shelfY + h + 1 > 2048) {
            Shapes.reset();
        }
        Sprite s = new Sprite(shelfX, shelfY);
        shelfX += w + 1;
        shelfH = Math.max(shelfH, h);
        return s;
    }

    static void flush() {
        if (dirty && tex != null) {
            tex.upload();
            dirty = false;
        }
    }

    private static void ensure() {
        if (img != null) {
            return;
        }
        img = new NativeImage(2048, 2048, true);
        tex = new NativeImageBackedTexture(() -> "hugoservice shapes", img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, (AbstractTexture)tex);
    }

    private static void reset() {
        SPRITES.clear();
        shelfH = 0;
        shelfY = 0;
        shelfX = 0;
        for (int y = 0; y < 2048; ++y) {
            for (int x = 0; x < 2048; ++x) {
                img.setColorArgb(x, y, 0);
            }
        }
    }

    private static void raster(int ox, int oy, int w, int h, int r, int t, int corners) {
        double hw = (double)w / 2.0;
        double hh = (double)h / 2.0;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                double px = (double)x + 0.5 - hw;
                double py = (double)y + 0.5 - hh;
                int bit = (px < 0.0 ? 0 : 1) | (py < 0.0 ? 0 : 2);
                double rr = (corners >> bit & 1) != 0 ? (double)r : 0.0;
                double d = Shapes.sdf(px, py, hw, hh, rr);
                double a = Shapes.clamp(0.5 - d);
                if (t > 0) {
                    a -= Shapes.clamp(0.5 - (d + (double)t));
                }
                img.setColorArgb(ox + x, oy + y, (int)Math.round(Shapes.clamp(a) * 255.0) << 24 | 0xFFFFFF);
            }
        }
    }

    private static double sdf(double px, double py, double hw, double hh, double r) {
        double qx = Math.abs(px) - (hw - r);
        double qy = Math.abs(py) - (hh - r);
        double ox = Math.max(qx, 0.0);
        double oy = Math.max(qy, 0.0);
        return Math.hypot(ox, oy) + Math.min(Math.max(qx, qy), 0.0) - r;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private Shapes() {
    }

    static {
        SPRITES = new HashMap<Long, Sprite>();
    }

    record Sprite(int u, int v) {
    }
}

