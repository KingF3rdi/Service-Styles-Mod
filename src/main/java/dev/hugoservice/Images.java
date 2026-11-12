/*
 * Decompiled with CFR 0.152.
 */
package dev.hugoservice;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

public final class Images {
    public static BufferedImage decode(byte[] png) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        if (src == null) {
            throw new IOException("not a PNG");
        }
        if (src.getType() == 2) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), 2);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    public static byte[] encode(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write((RenderedImage)img, "png", out);
        return out.toByteArray();
    }

    public static void tint(BufferedImage img, int rgb) {
        int tr = rgb >> 16 & 0xFF;
        int tg = rgb >> 8 & 0xFF;
        int tb = rgb & 0xFF;
        Images.forEach(img, (a, r, g, b) -> {
            int nb;
            int ng;
            int nr;
            float l = (0.299f * (float)r + 0.587f * (float)g + 0.114f * (float)b) / 255.0f;
            if (l < 0.5f) {
                float k = l * 2.0f;
                nr = (int)((float)tr * k);
                ng = (int)((float)tg * k);
                nb = (int)((float)tb * k);
            } else {
                float k = (l - 0.5f) * 2.0f;
                nr = (int)((float)tr + (float)(255 - tr) * k);
                ng = (int)((float)tg + (float)(255 - tg) * k);
                nb = (int)((float)tb + (float)(255 - tb) * k);
            }
            return a << 24 | nr << 16 | ng << 8 | nb;
        });
    }

    public static byte[] recolor(String path, byte[] png, int rgb) throws IOException {
        if (path.startsWith("textures/colormap/")) {
            return Images.transform(png, img -> Images.fill(img, rgb));
        }
        if (path.contains("grass_block")) {
            return png;
        }
        return Images.transform(png, img -> Images.tint(img, rgb));
    }

    private static void fill(BufferedImage img, int rgb) {
        Images.forEach(img, (a, r, g, b) -> 0xFF000000 | rgb);
    }

    public static void grey(BufferedImage img) {
        Images.forEach(img, (a, r, g, b) -> {
            int l = (int)(0.299f * (float)r + 0.587f * (float)g + 0.114f * (float)b);
            return a << 24 | l << 16 | l << 8 | l;
        });
    }

    public static void darken(BufferedImage img, float f) {
        Images.forEach(img, (a, r, g, b) -> a << 24 | (int)((float)r * f) << 16 | (int)((float)g * f) << 8 | (int)((float)b * f));
    }

    public static void stripPlate(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w < 2 || h < 2) {
            return;
        }
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        boolean[] seen = new boolean[w * h];
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int i = y * w + x;
                int a = px[i] >>> 24;
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                if (a < 18 || edge && Images.platePixel(px[i])) {
                    seen[i] = true;
                    px[i] = 0;
                    q.add(i);
                }
            }
        }
        int[] dx = new int[]{1, -1, 0, 0, 1, 1, -1, -1};
        int[] dy = new int[]{0, 0, 1, -1, 1, -1, 1, -1};
        while (!q.isEmpty()) {
            int i = q.poll();
            int x = i % w;
            int y = i / w;
            for (int k = 0; k < 8; ++k) {
                int nx = x + dx[k];
                int ny = y + dy[k];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) {
                    continue;
                }
                int j = ny * w + nx;
                if (seen[j] || !Images.platePixel(px[j])) {
                    continue;
                }
                seen[j] = true;
                px[j] = 0;
                q.add(j);
            }
        }
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    private static boolean platePixel(int c) {
        int a = c >>> 24;
        if (a < 18) {
            return true;
        }
        int r = c >> 16 & 0xFF;
        int g = c >> 8 & 0xFF;
        int b = c & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        int sat = max == 0 ? 0 : (max - min) * 255 / max;
        int l = (r * 3 + g * 6 + b) / 10;
        if (sat > 40 && b > r + 18 && b > g + 8 && l < 140) {
            return true;
        }
        if (sat < 50 && l < 78 && a > 180) {
            return true;
        }
        return false;
    }

    public static byte[] transform(byte[] png, Consumer<BufferedImage> op) throws IOException {
        BufferedImage img = Images.decode(png);
        op.accept(img);
        return Images.encode(img);
    }

    private static void forEach(BufferedImage img, PixelOp op) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; ++i) {
            int c = px[i];
            px[i] = op.apply(c >>> 24, c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF);
        }
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int r = 0;
        for (int sh = 0; sh <= 24; sh += 8) {
            int ca = a >>> sh & 0xFF;
            int cb = b >>> sh & 0xFF;
            r |= Math.round((float)ca + (float)(cb - ca) * t) << sh;
        }
        return r;
    }

    private Images() {
    }

    private static interface PixelOp {
        public int apply(int var1, int var2, int var3, int var4);
    }
}

