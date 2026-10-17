package dev.hugoservice;

import dev.hugoservice.Categories.Cube;
import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** Builds preview images: flat item art, isometric blocks and a 3D-looking end crystal. */
final class Previews {
    static final int SIZE = 96;

    static BufferedImage render(Group g, Style s, Integer tint) throws IOException {
        return switch (g.look()) {
            case CUBE -> cube(g.cube(), s, tint);
            case CRYSTAL -> crystal(s, tint);
            case FLAT, WIDE -> flat(g, s, tint);
        };
    }

    private static BufferedImage flat(Group g, Style s, Integer tint) throws IOException {
        BufferedImage img = null;
        for (String f : g.files()) {
            byte[] png = s.read(f);
            if (png != null) { img = crop(f, firstFrame(Images.decode(png))); break; }
        }
        if (img == null) return null;
        if (tint != null) Images.tint(img, tint);
        return img;
    }

    private static BufferedImage cube(Cube c, Style s, Integer tint) throws IOException {
        BufferedImage top = load(s, c.top()), side = load(s, c.side());
        if (top == null || side == null) return null;
        if (c.topTint() != null) {
            // grass: colour comes from the colormap (preset or own colour), faces stay as they are
            int grass = tint != null ? tint : s.color != null ? s.color : c.topTint();
            tint = null;
            multiply(top, grass);
            BufferedImage overlay = load(s, c.sideOverlay());
            if (overlay != null && overlay.getWidth() == side.getWidth() && overlay.getHeight() == side.getHeight()) {
                multiply(overlay, grass);
                Graphics2D g = side.createGraphics();
                g.drawImage(overlay, 0, 0, null);
                g.dispose();
            }
        }
        if (tint != null) { Images.tint(top, tint); Images.tint(side, tint); }
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        drawCube(g, top, side, side, SIZE / 2f, 8, SIZE - 14);
        g.dispose();
        return out;
    }

    private static BufferedImage crystal(Style s, Integer tint) throws IOException {
        BufferedImage tex = load(s, "textures/entity/end_crystal/end_crystal.png");
        if (tex == null) return null;
        if (tint != null) Images.tint(tex, tint);
        int k = Math.max(1, tex.getWidth() / 64);
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        // core (texOffs 32,0) inside, glass (texOffs 0,0) around it
        drawCube(g, sub(tex, 40, 0, k), sub(tex, 40, 8, k), sub(tex, 48, 8, k), SIZE / 2f, 30, 40);
        drawCube(g, sub(tex, 8, 0, k), sub(tex, 8, 8, k), sub(tex, 16, 8, k), SIZE / 2f, 10, 76);
        g.dispose();
        return out;
    }

    // ---- effect previews (Extras) ----

    /** Steve seen from the front, 16x32, built from the vanilla skin. */
    private static BufferedImage doll() throws IOException {
        byte[] png = Styles.vanilla("textures/entity/player/wide/steve.png");
        if (png == null) return null;
        BufferedImage skin = Images.decode(png), out = new BufferedImage(16, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        part(g, skin, 8, 8, 8, 8, 4, 0);     // head
        part(g, skin, 40, 8, 8, 8, 4, 0);    // hat layer
        part(g, skin, 20, 20, 8, 12, 4, 8);  // body
        part(g, skin, 44, 20, 4, 12, 0, 8);  // right arm
        part(g, skin, 36, 52, 4, 12, 12, 8); // left arm
        part(g, skin, 4, 20, 4, 12, 4, 20);  // right leg
        part(g, skin, 20, 52, 4, 12, 8, 20); // left leg
        g.dispose();
        return out;
    }

    private static void part(Graphics2D g, BufferedImage skin, int u, int v, int w, int h, int x, int y) {
        g.drawImage(skin.getSubimage(u, v, w, h), x, y, null);
    }

    /** Player in the hurt flash colour. */
    static BufferedImage hit(int rgb) throws IOException {
        BufferedImage d = doll();
        if (d == null) return null;
        int tr = rgb >> 16 & 255, tg = rgb >> 8 & 255, tb = rgb & 255;
        for (int y = 0; y < d.getHeight(); y++) for (int x = 0; x < d.getWidth(); x++) {
            int c = d.getRGB(x, y);
            if (c >>> 24 == 0) continue;
            int r = ((c >> 16 & 255) * 2 + tr * 3) / 5, gg = ((c >> 8 & 255) * 2 + tg * 3) / 5, b = ((c & 255) * 2 + tb * 3) / 5;
            d.setRGB(x, y, 0xFF000000 | r << 16 | gg << 8 | b);
        }
        return d;
    }

    /** Player with a 1px glow outline around the silhouette. */
    static BufferedImage glow(int rgb) throws IOException {
        BufferedImage d = doll();
        if (d == null) return null;
        BufferedImage out = new BufferedImage(20, 36, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 36; y++) for (int x = 0; x < 20; x++) {
            boolean edge = false;
            for (int dy = -1; dy <= 1 && !edge; dy++) for (int dx = -1; dx <= 1 && !edge; dx++) {
                int sx = x - 2 + dx, sy = y - 2 + dy;
                edge = sx >= 0 && sy >= 0 && sx < 16 && sy < 32 && d.getRGB(sx, sy) >>> 24 != 0;
            }
            if (edge) out.setRGB(x, y, 0xFF000000 | rgb);
        }
        Graphics2D g = out.createGraphics();
        g.drawImage(d, 2, 2, null);
        g.dispose();
        return out;
    }

    /** Stone block with a coloured selection outline. */
    static BufferedImage outline(int rgb) throws IOException {
        byte[] png = Styles.vanilla("textures/block/stone.png");
        if (png == null) return null;
        BufferedImage stone = Images.decode(png);
        return wireCube(stone, stone, rgb, true);
    }

    /** Cobweb inside a coloured wire cube. */
    static BufferedImage cobweb(int rgb) throws IOException {
        byte[] png = Styles.vanilla("textures/block/cobweb.png");
        if (png == null) return null;
        BufferedImage out = wireCube(null, null, rgb, false);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Images.decode(png), SIZE / 2 - 24, 26, 48, 48, null);
        g.dispose();
        return out;
    }

    /** Two shields: ready colour and disabled colour, tinted like in game. */
    static BufferedImage shield(int ready, int disabled) throws IOException {
        byte[] png = Styles.vanilla("textures/entity/shield_base_nopattern.png");
        if (png == null) return null;
        BufferedImage tex = Images.decode(png);
        int k = Math.max(1, tex.getWidth() / 64);
        BufferedImage face = tex.getSubimage(k, k, 12 * k, 22 * k);
        BufferedImage out = new BufferedImage(30 * k, 24 * k, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        BufferedImage a = copy(face), b = copy(face);
        multiply(a, ready);
        multiply(b, disabled);
        g.drawImage(a, k, k, null);
        g.drawImage(b, 17 * k, k, null);
        g.dispose();
        return out;
    }

    /** Plain shield front (12x22 at pack resolution). */
    static BufferedImage shieldFace() throws IOException {
        byte[] png = Styles.vanilla("textures/entity/shield_base_nopattern.png");
        if (png == null) return null;
        BufferedImage tex = Images.decode(png);
        int k = Math.max(1, tex.getWidth() / 64);
        return copy(tex.getSubimage(k, k, 12 * k, 22 * k));
    }

    /** Multiplies the alpha channel, for opacity previews. */
    static BufferedImage alpha(BufferedImage img, int percent) {
        if (img == null || percent >= 100) return img;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int c = img.getRGB(x, y);
            img.setRGB(x, y, ((c >>> 24) * percent / 100) << 24 | c & 0xFFFFFF);
        }
        return img;
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = c.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return c;
    }

    /** Iso cube (optional faces) with its visible edges drawn in a colour. */
    private static BufferedImage wireCube(BufferedImage top, BufferedImage side, int rgb, boolean faces) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        float cx = SIZE / 2f, y0 = 8, w = SIZE - 14;
        if (faces && top != null) drawCube(g, top, side, side, cx, y0, w);
        float half = w / 2, q = w / 4, h = w * 0.58f;
        int[][] e = {
            {(int) (cx - half), (int) (y0 + q), (int) cx, (int) y0}, {(int) cx, (int) y0, (int) (cx + half), (int) (y0 + q)},
            {(int) (cx + half), (int) (y0 + q), (int) cx, (int) (y0 + 2 * q)}, {(int) cx, (int) (y0 + 2 * q), (int) (cx - half), (int) (y0 + q)},
            {(int) (cx - half), (int) (y0 + q), (int) (cx - half), (int) (y0 + q + h)}, {(int) cx, (int) (y0 + 2 * q), (int) cx, (int) (y0 + 2 * q + h)},
            {(int) (cx + half), (int) (y0 + q), (int) (cx + half), (int) (y0 + q + h)},
            {(int) (cx - half), (int) (y0 + q + h), (int) cx, (int) (y0 + 2 * q + h)}, {(int) cx, (int) (y0 + 2 * q + h), (int) (cx + half), (int) (y0 + q + h)}
        };
        g.setColor(new java.awt.Color(0xFF000000 | rgb, true));
        g.setStroke(new java.awt.BasicStroke(2.5f));
        for (int[] l : e) g.drawLine(l[0], l[1], l[2], l[3]);
        g.dispose();
        return out;
    }

    /** Isometric cube whose top vertex is at (cx, y0), width w. */
    static void drawCube(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right, float cx, float y0, float w) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        float half = w / 2, q = w / 4, h = w * 0.58f;
        float lx = cx - half, ly = y0 + q, rx = cx + half, ry = y0 + q, ccx = cx, ccy = y0 + 2 * q;
        face(g, top, lx, ly, cx, y0, ccx, ccy);
        face(g, shade(left, 0.82f), lx, ly, ccx, ccy, lx, ly + h);
        face(g, shade(right, 0.62f), ccx, ccy, rx, ry, ccx, ccy + h);
    }

    /** Maps the image's unit square so (0,0)->o, (1,0)->a, (0,1)->b. */
    private static void face(Graphics2D g, BufferedImage img, float ox, float oy, float ax, float ay, float bx, float by) {
        AffineTransform at = new AffineTransform(ax - ox, ay - oy, bx - ox, by - oy, ox, oy);
        // tiny overdraw closes hairline seams between faces
        at.scale(1.012 / img.getWidth(), 1.012 / img.getHeight());
        g.drawImage(img, at, null);
    }

    private static BufferedImage load(Style s, String path) throws IOException {
        if (path == null) return null;
        byte[] png = s.read(path);
        if (png == null) png = Styles.vanilla(path); // packs that skip a face still preview
        return png == null ? null : firstFrame(Images.decode(png));
    }

    /** GUI sheets carry empty space; show only the part you see in game. */
    private static BufferedImage crop(String path, BufferedImage img) {
        if (!path.endsWith("gui/container/inventory.png") || img.getWidth() < 256) return img;
        int k = img.getWidth() / 256;
        return img.getSubimage(0, 0, Math.min(176 * k, img.getWidth()), Math.min(166 * k, img.getHeight()));
    }

    private static BufferedImage firstFrame(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        return h > w && h % w == 0 ? img.getSubimage(0, 0, w, w) : img;
    }

    private static BufferedImage sub(BufferedImage tex, int x, int y, int k) {
        return tex.getSubimage(x * k, y * k, 8 * k, 8 * k);
    }

    private static BufferedImage shade(BufferedImage src, float f) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        Images.darken(copy, f);
        return copy;
    }

    private static void multiply(BufferedImage img, int rgb) {
        int mr = rgb >> 16 & 255, mg = rgb >> 8 & 255, mb = rgb & 255;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            int c = img.getRGB(x, y);
            int r = (c >> 16 & 255) * mr / 255, gg = (c >> 8 & 255) * mg / 255, b = (c & 255) * mb / 255;
            img.setRGB(x, y, c & 0xFF000000 | r << 16 | gg << 8 | b);
        }
    }

    private Previews() {}
}
