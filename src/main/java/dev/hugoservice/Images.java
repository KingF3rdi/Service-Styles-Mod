package dev.hugoservice;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** PNG decode/encode and the colour operations used by presets, own colour and grey world. */
public final class Images {
    public static BufferedImage decode(byte[] png) throws IOException {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(png));
        if (src == null) throw new IOException("not a PNG");
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    public static byte[] encode(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** Gradient map: dark pixels -> black, mid -> colour, bright -> white. Keeps alpha and detail. */
    public static void tint(BufferedImage img, int rgb) {
        int tr = rgb >> 16 & 255, tg = rgb >> 8 & 255, tb = rgb & 255;
        forEach(img, (a, r, g, b) -> {
            float l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
            int nr, ng, nb;
            if (l < 0.5f) { float k = l * 2; nr = (int) (tr * k); ng = (int) (tg * k); nb = (int) (tb * k); }
            else { float k = (l - 0.5f) * 2; nr = (int) (tr + (255 - tr) * k); ng = (int) (tg + (255 - tg) * k); nb = (int) (tb + (255 - tb) * k); }
            return a << 24 | nr << 16 | ng << 8 | nb;
        });
    }

    /**
     * Recolours one texture file. Grass is biome-tinted in game, so for grass we recolour the
     * colormap and leave the block textures alone; everything else gets the gradient map.
     */
    public static byte[] recolor(String path, byte[] png, int rgb) throws IOException {
        if (path.startsWith("textures/colormap/")) return transform(png, img -> fill(img, rgb));
        if (path.contains("grass_block")) return png;
        return transform(png, img -> tint(img, rgb));
    }

    private static void fill(BufferedImage img, int rgb) {
        forEach(img, (a, r, g, b) -> 0xFF000000 | rgb);
    }

    public static void grey(BufferedImage img) {
        forEach(img, (a, r, g, b) -> {
            int l = (int) (0.299f * r + 0.587f * g + 0.114f * b);
            return a << 24 | l << 16 | l << 8 | l;
        });
    }

    public static void darken(BufferedImage img, float f) {
        forEach(img, (a, r, g, b) -> a << 24 | (int) (r * f) << 16 | (int) (g * f) << 8 | (int) (b * f));
    }

    public static byte[] transform(byte[] png, java.util.function.Consumer<BufferedImage> op) throws IOException {
        BufferedImage img = decode(png);
        op.accept(img);
        return encode(img);
    }

    private interface PixelOp { int apply(int a, int r, int g, int b); }

    private static void forEach(BufferedImage img, PixelOp op) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            px[i] = op.apply(c >>> 24, c >> 16 & 255, c >> 8 & 255, c & 255);
        }
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    private Images() {}
}
