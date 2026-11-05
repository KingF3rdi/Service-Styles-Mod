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

import dev.hugoservice.HugoService;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

final class Thumbs {
    static final int SIZE = 2048;
    static final int MAX = 128;
    static final Identifier ATLAS = Identifier.of((String)"hugoservice", (String)"thumbs");
    private static final Map<String, Optional<Thumb>> CACHE = new HashMap<String, Optional<Thumb>>();
    private static final Map<String, Boolean> PENDING = new HashMap<String, Boolean>();
    private static final Queue<Done> FINISHED = new ConcurrentLinkedQueue<Done>();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)), r -> {
        Thread t = new Thread(r, "HugoService-Preview");
        t.setDaemon(true);
        t.setPriority(1);
        return t;
    });
    private static NativeImage atlas;
    private static NativeImageBackedTexture texture;
    private static int shelfX;
    private static int shelfY;
    private static int shelfH;
    private static Identifier rainbow;

    static Thumb get(String key, Source src) {
        Optional<Thumb> t = CACHE.get(key);
        if (t != null) {
            return t.orElse(null);
        }
        if (PENDING.putIfAbsent(key, Boolean.TRUE) == null) {
            WORKERS.execute(() -> {
                BufferedImage img = null;
                try {
                    img = Thumbs.shrink(src.get());
                }
                catch (Throwable e) {
                    HugoService.LOG.debug("thumbnail failed for {}", key, e);
                }
                FINISHED.add(new Done(key, img));
            });
        }
        return null;
    }

    static void upload() {
        Done d;
        boolean changed = false;
        while ((d = FINISHED.poll()) != null) {
            PENDING.remove(d.key());
            Thumb t = d.img() == null ? null : Thumbs.pack(d.img());
            CACHE.put(d.key(), Optional.ofNullable(t));
            changed |= t != null;
        }
        if (changed) {
            texture.upload();
        }
    }

    static boolean known(String key) {
        return CACHE.containsKey(key);
    }

    private static BufferedImage shrink(BufferedImage img) {
        int h;
        if (img == null) {
            return null;
        }
        int w = img.getWidth();
        int k = (Math.max(w, h = img.getHeight()) + 128 - 1) / 128;
        if (k <= 1) {
            return img;
        }
        BufferedImage out = new BufferedImage(Math.max(1, w / k), Math.max(1, h / k), 2);
        for (int y = 0; y < out.getHeight(); ++y) {
            for (int x = 0; x < out.getWidth(); ++x) {
                out.setRGB(x, y, img.getRGB(x * k, y * k));
            }
        }
        return out;
    }

    private static Thumb pack(BufferedImage img) {
        Thumbs.ensure();
        int w = img.getWidth();
        int h = img.getHeight();
        if (shelfX + w + 1 > 2048) {
            shelfX = 0;
            shelfY += shelfH + 1;
            shelfH = 0;
        }
        if (shelfY + h + 1 > 2048) {
            Thumbs.reset();
        }
        int u = shelfX;
        int v = shelfY;
        shelfX += w + 1;
        shelfH = Math.max(shelfH, h);
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        double r = 0.0;
        double g = 0.0;
        double b = 0.0;
        double n = 0.0;
        double sat = 0.0;
        double seen = 0.0;
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int c = px[y * w + x];
                atlas.setColorArgb(u + x, v + y, c);
                if (c >>> 24 <= 128) continue;
                int cr = c >> 16 & 0xFF;
                int cg = c >> 8 & 0xFF;
                int cb = c & 0xFF;
                int max = Math.max(cr, Math.max(cg, cb));
                int min = Math.min(cr, Math.min(cg, cb));
                double s = max == 0 ? 0.0 : (double)(max - min) / (double)max;
                double wgt = 0.02 + s * s * (double)max / 255.0;
                r += (double)cr * wgt;
                g += (double)cg * wgt;
                b += (double)cb * wgt;
                n += wgt;
                sat += s;
                seen += 1.0;
            }
        }
        int avg = 0;
        if (n > 0.0 && seen > 0.0 && sat / seen > 0.08) {
            float[] hsb = Color.RGBtoHSB((int)(r / n), (int)(g / n), (int)(b / n), null);
            avg = Color.HSBtoRGB(hsb[0], Math.max(0.55f, hsb[1]), 1.0f) & 0xFFFFFF;
        }
        return new Thumb(u, v, w, h, avg);
    }

    private static void ensure() {
        if (atlas != null) {
            return;
        }
        atlas = new NativeImage(2048, 2048, true);
        texture = new NativeImageBackedTexture(() -> "hugoservice thumbs", atlas);
        MinecraftClient.getInstance().getTextureManager().registerTexture(ATLAS, (AbstractTexture)texture);
    }

    private static void reset() {
        CACHE.clear();
        shelfH = 0;
        shelfY = 0;
        shelfX = 0;
        for (int y = 0; y < 2048; ++y) {
            for (int x = 0; x < 2048; ++x) {
                atlas.setColorArgb(x, y, 0);
            }
        }
    }

    static Identifier rainbow() {
        if (rainbow == null) {
            NativeImage ni = new NativeImage(128, 1, false);
            for (int x = 0; x < 128; ++x) {
                ni.setColorArgb(x, 0, 0xFF000000 | Color.HSBtoRGB((float)x / 128.0f, 0.7f, 1.0f));
            }
            rainbow = Identifier.of((String)"hugoservice", (String)"rainbow");
            MinecraftClient.getInstance().getTextureManager().registerTexture(rainbow, (AbstractTexture)new NativeImageBackedTexture(() -> "hugoservice rainbow", ni));
        }
        return rainbow;
    }

    private Thumbs() {
    }

    record Thumb(int u, int v, int w, int h, int avg) {
    }

    static interface Source {
        public BufferedImage get() throws Exception;
    }

    private record Done(String key, BufferedImage img) {
    }
}

