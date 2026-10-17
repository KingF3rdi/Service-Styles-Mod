package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * One atlas texture holding anti-aliased white rounded rectangles and rings in exact screen-pixel
 * sizes. Every UI shape is then a single tinted quad from the same texture, so Minecraft batches
 * them into one draw and the GUI layer bookkeeping stays tiny (that bookkeeping is what cost FPS).
 */
final class Shapes {
    static final int SIZE = 2048, MAX = 256;
    static final Identifier ID = Identifier.of("hugoservice", "shapes");

    /** Region of a sprite inside the atlas. */
    record Sprite(int u, int v) {}

    private static NativeImage img;
    private static NativeImageBackedTexture tex;
    private static final Map<Long, Sprite> SPRITES = new HashMap<>();
    private static int shelfX, shelfY, shelfH;
    private static boolean dirty;

    /**
     * Rounded rect w*h (screen pixels), corner radius r, ring thickness t (0 = filled),
     * corners = bit mask of rounded corners: 1 top-left, 2 top-right, 4 bottom-left, 8 bottom-right.
     * Returns null when the shape is too large for the atlas.
     */
    static Sprite get(int w, int h, int r, int t, int corners) {
        if (w > MAX || h > MAX || w <= 0 || h <= 0) return null;
        long key = (long) w | (long) h << 9 | (long) r << 18 | (long) t << 27 | (long) corners << 36;
        Sprite s = SPRITES.get(key);
        if (s != null) return s;
        s = alloc(w, h);
        raster(s.u, s.v, w, h, r, t, corners);
        SPRITES.put(key, s);
        dirty = true;
        return s;
    }

    /** Soft white radial spot (64x64) for the glow behind previews. */
    static Sprite glow() {
        long key = -1L;
        Sprite s = SPRITES.get(key);
        if (s != null) return s;
        s = alloc(64, 64);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            double d = Math.min(1, Math.hypot(x - 31.5, y - 31.5) / 32);
            img.setColorArgb(s.u() + x, s.v() + y, (int) (255 * Math.pow(1 - d, 2)) << 24 | 0xFFFFFF);
        }
        SPRITES.put(key, s);
        dirty = true;
        return s;
    }

    private static Sprite alloc(int w, int h) {
        ensure();
        if (shelfX + w + 1 > SIZE) { shelfX = 0; shelfY += shelfH + 1; shelfH = 0; }
        if (shelfY + h + 1 > SIZE) reset(); // atlas full: start over, sprites get redrawn on demand
        Sprite s = new Sprite(shelfX, shelfY);
        shelfX += w + 1;
        shelfH = Math.max(shelfH, h);
        return s;
    }

    /** Uploads new sprites; call once at the end of a frame's drawing. */
    static void flush() {
        if (dirty && tex != null) { tex.upload(); dirty = false; }
    }

    private static void ensure() {
        if (img != null) return;
        img = new NativeImage(SIZE, SIZE, true);
        tex = new NativeImageBackedTexture(() -> "hugoservice shapes", img);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ID, tex);
    }

    private static void reset() {
        SPRITES.clear();
        shelfX = shelfY = shelfH = 0;
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) img.setColorArgb(x, y, 0);
    }

    private static void raster(int ox, int oy, int w, int h, int r, int t, int corners) {
        double hw = w / 2.0, hh = h / 2.0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double px = x + 0.5 - hw, py = y + 0.5 - hh;
            int bit = (px < 0 ? 0 : 1) | (py < 0 ? 0 : 2);         // quadrant -> 0 TL,1 TR,2 BL,3 BR
            double rr = (corners >> bit & 1) != 0 ? r : 0;
            double d = sdf(px, py, hw, hh, rr);
            double a = clamp(0.5 - d);
            if (t > 0) a -= clamp(0.5 - (d + t));
            img.setColorArgb(ox + x, oy + y, (int) Math.round(clamp(a) * 255) << 24 | 0xFFFFFF);
        }
    }

    /** Signed distance to a rounded box centred at 0 with half size (hw, hh). */
    private static double sdf(double px, double py, double hw, double hh, double r) {
        double qx = Math.abs(px) - (hw - r), qy = Math.abs(py) - (hh - r);
        double ox = Math.max(qx, 0), oy = Math.max(qy, 0);
        return Math.hypot(ox, oy) + Math.min(Math.max(qx, qy), 0) - r;
    }

    private static double clamp(double v) { return Math.max(0, Math.min(1, v)); }

    private Shapes() {}
}
