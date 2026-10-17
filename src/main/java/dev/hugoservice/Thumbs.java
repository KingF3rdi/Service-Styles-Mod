package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Preview thumbnails. Images are built on background threads and packed into ONE atlas texture,
 * so all previews on screen are drawn in a single batch (separate textures cost a lot of FPS).
 */
final class Thumbs {
    static final int SIZE = 2048, MAX = 128;
    static final Identifier ATLAS = Identifier.of("hugoservice", "thumbs");

    /** Region in the atlas; avg = average colour of the visible pixels (for the glow). */
    record Thumb(int u, int v, int w, int h, int avg) {}

    interface Source { BufferedImage get() throws Exception; }

    private record Done(String key, BufferedImage img) {}

    private static final Map<String, Optional<Thumb>> CACHE = new HashMap<>();
    private static final Map<String, Boolean> PENDING = new HashMap<>();
    private static final Queue<Done> FINISHED = new ConcurrentLinkedQueue<>();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(
        Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)), r -> {
            Thread t = new Thread(r, "HugoService-Preview");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    private static NativeImage atlas;
    private static NativeImageBackedTexture texture;
    private static int shelfX, shelfY, shelfH;
    private static Identifier rainbow;

    /** Returns the thumbnail, or null while it is still being built or when the texture is missing. */
    static Thumb get(String key, Source src) {
        Optional<Thumb> t = CACHE.get(key);
        if (t != null) return t.orElse(null);
        if (PENDING.putIfAbsent(key, Boolean.TRUE) == null) {
            WORKERS.execute(() -> {
                BufferedImage img = null;
                try {
                    img = shrink(src.get());
                } catch (Throwable e) {
                    HugoService.LOG.debug("thumbnail failed for {}", key, e);
                }
                FINISHED.add(new Done(key, img));
            });
        }
        return null;
    }

    /** Packs finished previews into the atlas; call once per frame on the render thread. */
    static void upload() {
        boolean changed = false;
        Done d;
        while ((d = FINISHED.poll()) != null) {
            PENDING.remove(d.key());
            Thumb t = d.img() == null ? null : pack(d.img());
            CACHE.put(d.key(), Optional.ofNullable(t));
            changed |= t != null;
        }
        if (changed) texture.upload(); // one upload per frame, however many arrived
    }

    static boolean known(String key) { return CACHE.containsKey(key); }

    /** HD pack textures are reduced (whole-pixel steps keep pixel art crisp). */
    private static BufferedImage shrink(BufferedImage img) {
        if (img == null) return null;
        int w = img.getWidth(), h = img.getHeight(), k = (Math.max(w, h) + MAX - 1) / MAX;
        if (k <= 1) return img;
        BufferedImage out = new BufferedImage(Math.max(1, w / k), Math.max(1, h / k), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, img.getRGB(x * k, y * k));
        return out;
    }

    private static Thumb pack(BufferedImage img) {
        ensure();
        int w = img.getWidth(), h = img.getHeight();
        if (shelfX + w + 1 > SIZE) { shelfX = 0; shelfY += shelfH + 1; shelfH = 0; }
        if (shelfY + h + 1 > SIZE) reset();
        int u = shelfX, v = shelfY;
        shelfX += w + 1;
        shelfH = Math.max(shelfH, h);

        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = px[y * w + x];
            atlas.setColorArgb(u + x, v + y, c);
            if (c >>> 24 > 128) { r += c >> 16 & 255; g += c >> 8 & 255; b += c & 255; n++; }
        }
        int avg = n == 0 ? 0x6CC4F0 : (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
        return new Thumb(u, v, w, h, avg);
    }

    private static void ensure() {
        if (atlas != null) return;
        atlas = new NativeImage(SIZE, SIZE, true);
        texture = new NativeImageBackedTexture(() -> "hugoservice thumbs", atlas);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ATLAS, texture);
    }

    /** Atlas full: forget everything; visible previews are rebuilt in the background. */
    private static void reset() {
        CACHE.clear();
        shelfX = shelfY = shelfH = 0;
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) atlas.setColorArgb(x, y, 0);
    }

    static Identifier rainbow() {
        if (rainbow == null) {
            NativeImage ni = new NativeImage(128, 1, false);
            for (int x = 0; x < 128; x++) ni.setColorArgb(x, 0, 0xFF000000 | Color.HSBtoRGB(x / 128f, 0.7f, 1f));
            rainbow = Identifier.of("hugoservice", "rainbow");
            MinecraftClient.getInstance().getTextureManager().registerTexture(rainbow, new NativeImageBackedTexture(() -> "hugoservice rainbow", ni));
        }
        return rainbow;
    }

    private Thumbs() {}
}
