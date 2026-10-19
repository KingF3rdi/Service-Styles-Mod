package dev.hugoservice;

import dev.hugoservice.Categories.Cube;
import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds preview images: flat item art, isometric blocks and a 3D-looking end crystal. */
final class Previews {
    static final int SIZE = 96;

    static BufferedImage render(Group g, Style s, Integer tint) throws IOException {
        return switch (g.look()) {
            case CUBE -> cube(g.cube(), s, tint);
            case CRYSTAL -> crystal(s, tint);
            case FLAT, WIDE -> flat(g, s, tint);
            case SKY -> s.vanilla ? vanillaSky() : Sky.preview(g, s);
            case KNIFE -> Knives.preview(g, s);
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

    /**
     * End crystal exactly as one placed by a player stands on obsidian: vanilla's model (outer glass at 24 px, inner
     * glass x0.875 inside it and the core x0.765625 inside that, each turned by the spin and 60 degrees about the
     * diagonal), drawn x2 and half a block down like EndCrystalEntityRenderer does, without the bedrock plate (placed
     * crystals have none). Frozen halfway up its bob.
     */
    private static BufferedImage crystal(Style s, Integer tint) throws IOException {
        BufferedImage tex = load(s, "textures/entity/end_crystal/end_crystal.png");
        if (tex == null) return null;
        if (tint != null) Images.tint(tex, tint);
        byte[] obsidianPng = Styles.vanilla("textures/block/obsidian.png"); // plain obsidian below every style
        BufferedImage obsidian = obsidianPng == null ? null : Images.decode(obsidianPng);
        int k = Math.max(1, tex.getWidth() / 64);
        Matrix3f view = new Matrix3f().rotateX((float) Math.toRadians(22)).rotateY((float) Math.toRadians(-30));
        float spin = (float) Math.toRadians(40), tilt = (float) (Math.PI / 3), s45 = (float) Math.sin(Math.PI / 4), bob = -16;
        Matrix4f outer = new Matrix4f().scale(2).translate(0, -0.5f, 0).translate(0, (24 + bob / 2) / 16f, 0)
            .rotate(new Quaternionf().rotationY(spin).rotateAxis(tilt, s45, 0, s45));
        Matrix4f inner = new Matrix4f(outer).rotate(new Quaternionf().setAngleAxis(tilt, s45, 0, s45).rotateY(spin)).scale(0.875f);
        Matrix4f core = new Matrix4f(inner).rotate(new Quaternionf().setAngleAxis(tilt, s45, 0, s45).rotateY(spin)).scale(0.765625f);
        List<Face> faces = new ArrayList<>();
        if (obsidian != null) block(faces, firstFrame(obsidian), view);
        cuboid(faces, tex, k, view, core, 32, 0);
        cuboid(faces, tex, k, view, inner, 0, 0);
        cuboid(faces, tex, k, view, outer, 0, 0);

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Face f : faces) for (float[] p : f.corners()) {
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        int size = SIZE * 2; // the inspector shows it big, so twice the usual detail
        float sc = Math.min((size - 12) / (maxX - minX), (size - 12) / (maxY - minY));
        float ox = (size - (maxX - minX) * sc) / 2 - minX * sc, oy = (size - (maxY - minY) * sc) / 2 - minY * sc;
        faces.sort(Comparator.comparingDouble(Face::depth));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Face f : faces) {
            face(g, f.img(), f.ox() * sc + ox, f.oy() * sc + oy, f.ax() * sc + ox, f.ay() * sc + oy, f.bx() * sc + ox, f.by() * sc + oy);
        }
        g.dispose();
        return out;
    }

    private static final Vector3f LIGHT = new Vector3f(0.3f, 1f, 0.55f).normalize();

    /** A projected quad: texture corner (0,0) at o, (1,0) at a, (0,1) at b (screen y down). */
    private record Face(BufferedImage img, float ox, float oy, float ax, float ay, float bx, float by, float depth) {
        float[][] corners() { return new float[][] {{ox, oy}, {ax, ay}, {bx, by}, {ax + bx - ox, ay + by - oy}}; }
    }

    /** A vanilla 8 px model cuboid at (-4, -4, -4) with its texture at (u, v) in Minecraft's box layout; m: model px/16 to blocks. */
    private static void cuboid(List<Face> out, BufferedImage tex, int k, Matrix3f view, Matrix4f m, int u, int v) {
        float a = -4, b = 4, s = 8;
        quad(out, tex.getSubimage((u + 8) * k, v * k, 8 * k, 8 * k), view, m, a, a, a, s, 0, 0, 0, 0, s, -1);        // bottom
        quad(out, tex.getSubimage((u + 16) * k, v * k, 8 * k, 8 * k), view, m, a, b, b, s, 0, 0, 0, 0, -s, -1);      // top
        quad(out, tex.getSubimage(u * k, (v + 8) * k, 8 * k, 8 * k), view, m, a, b, a, 0, 0, s, 0, -s, 0, -1);       // west
        quad(out, tex.getSubimage((u + 8) * k, (v + 8) * k, 8 * k, 8 * k), view, m, b, b, a, -s, 0, 0, 0, -s, 0, -1); // north
        quad(out, tex.getSubimage((u + 16) * k, (v + 8) * k, 8 * k, 8 * k), view, m, b, b, b, 0, 0, -s, 0, -s, 0, -1); // east
        quad(out, tex.getSubimage((u + 24) * k, (v + 8) * k, 8 * k, 8 * k), view, m, a, b, b, s, 0, 0, 0, -s, 0, -1);  // south
    }

    /** The obsidian block the crystal stands on, with Minecraft's block face shading. */
    private static void block(List<Face> out, BufferedImage tex, Matrix3f view) {
        Matrix4f m = new Matrix4f().translate(-0.5f, -1, -0.5f);
        float s = 16;
        quad(out, tex, view, m, 0, 16, 0, s, 0, 0, 0, 0, s, 1f);     // top
        quad(out, tex, view, m, 0, 16, 0, 0, 0, s, 0, -s, 0, 0.6f);  // west
        quad(out, tex, view, m, 16, 16, 0, -s, 0, 0, 0, -s, 0, 0.8f); // north
        quad(out, tex, view, m, 16, 16, 16, 0, 0, -s, 0, -s, 0, 0.6f); // east
        quad(out, tex, view, m, 0, 16, 16, s, 0, 0, 0, -s, 0, 0.8f);  // south
    }

    /**
     * One face: corner o and edges a, b in px (y up), taken to blocks by m (px / 16 first). Light below 0 means
     * "from the normal", else it is the fixed block shade.
     */
    private static void quad(List<Face> out, BufferedImage img, Matrix3f view, Matrix4f m, float ox, float oy, float oz,
                             float ax, float ay, float az, float bx, float by, float bz, float shade) {
        Vector3f o = m.transformPosition(new Vector3f(ox / 16, oy / 16, oz / 16));
        Vector3f a = m.transformDirection(new Vector3f(ax / 16, ay / 16, az / 16)), b = m.transformDirection(new Vector3f(bx / 16, by / 16, bz / 16));
        float light = shade >= 0 ? shade : 0.78f + 0.22f * Math.abs(new Vector3f(a).cross(b).normalize().dot(LIGHT)); // glass shows both sides, lit like in game
        view.transform(o);
        view.transform(a);
        view.transform(b);
        float depth = o.z + (a.z + b.z) / 2; // face centre; bigger = nearer
        out.add(new Face(shade(img, light), o.x, -o.y, o.x + a.x, -(o.y + a.y), o.x + b.x, -(o.y + b.y), depth));
    }

    /** Vanilla overworld sky colours as a simple gradient. */
    private static BufferedImage vanillaSky() {
        BufferedImage out = new BufferedImage(192, 96, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 96; y++) {
            float t = y / 95f;
            int r = (int) (0x78 + (0xC0 - 0x78) * t), g = (int) (0xA7 + (0xD8 - 0xA7) * t), b = 0xFF;
            for (int x = 0; x < 192; x++) out.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b);
        }
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
    static void face(Graphics2D g, BufferedImage img, float ox, float oy, float ax, float ay, float bx, float by) {
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
        if (path.contains("entity/shield") && img.getWidth() >= 64) { // the front face, not the unfolded texture
            int k = img.getWidth() / 64;
            return img.getSubimage(k, k, 12 * k, 22 * k);
        }
        if (!path.endsWith("gui/container/inventory.png") || img.getWidth() < 256) return img;
        int k = img.getWidth() / 256;
        return img.getSubimage(0, 0, Math.min(176 * k, img.getWidth()), Math.min(166 * k, img.getHeight()));
    }

    private static BufferedImage firstFrame(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        return h > w && h % w == 0 ? img.getSubimage(0, 0, w, w) : img;
    }

    static BufferedImage shade(BufferedImage src, float f) {
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
